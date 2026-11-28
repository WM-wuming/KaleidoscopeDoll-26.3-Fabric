package com.kaleidoscopedoll.client.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.network.DollPackPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.server.packs.PackResources;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClientDollPackSync {
    private static ClientPacketListener connection;
    private static ServerDollPackCache cache;
    private static DollPackPackets.Offer latestOffer;
    private static Download download;
    private static Selection active = Selection.EMPTY;
    private static boolean mounted;

    private ClientDollPackSync() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DollPackPackets.Offer.TYPE,
                (offer, context) -> receiveOffer(context.client(), context.player().connection, offer));
        ClientPlayNetworking.registerGlobalReceiver(DollPackPackets.Chunk.TYPE,
                (chunk, context) -> receiveChunk(context.client(), context.player().connection, chunk));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            if (connection == handler) {
                connection = null;
                cache = null;
                latestOffer = null;
                download = null;
                deactivate(client);
            }
        }));
    }

    private static void receiveOffer(Minecraft client, ClientPacketListener handler, DollPackPackets.Offer offer) {
        if (client.getConnection() != handler || handler.getConnection().isMemoryConnection()
                || !offer.isWithinLimits()) {
            return;
        }
        if (connection != handler) {
            deactivate(client);
            connection = handler;
            latestOffer = null;
            var server = handler.getServerData();
            String address = server != null ? server.ip : handler.getConnection().getRemoteAddress().toString();
            cache = new ServerDollPackCache(address);
        }
        if (latestOffer != null && offer.version() <= latestOffer.version()) {
            return;
        }
        latestOffer = offer;
        download = null;
        if (!Arrays.equals(active.key(), offer.key())) {
            deactivate(client);
        }
        if (offer.key().length != 0) {
            cache.removePlaintext();
        }
        if (offer.packs().stream().allMatch(info -> cache.contains(info, offer.key()))) {
            apply(client, offer);
        } else if (ClientPlayNetworking.canSend(DollPackPackets.Request.TYPE)) {
            download = new Download(offer);
            ClientPlayNetworking.send(new DollPackPackets.Request(offer.version()));
        }
    }

    private static void receiveChunk(Minecraft client, ClientPacketListener handler, DollPackPackets.Chunk chunk) {
        if (handler != connection || client.getConnection() != handler
                || download == null || chunk.version() != latestOffer.version()) {
            return;
        }
        try {
            if (download.accept(chunk)) {
                var complete = download;
                download = null;
                for (int i = 0; i < complete.archives.length; i++) {
                    cache.store(complete.offer.packs().get(i), complete.archives[i], complete.offer.key());
                }
                apply(client, complete.offer);
            }
        } catch (IOException e) {
            download = null;
            cache.prune(active.paths(cache));
            KaleidoscopeDoll.LOGGER.warn("Could not sync server doll config: {}", e.getMessage());
        }
    }

    private static void deactivate(Minecraft client) {
        if (active != Selection.EMPTY) {
            active = Selection.EMPTY;
            client.reloadResourcePacks();
        }
    }

    private static void apply(Minecraft client, DollPackPackets.Offer offer) {
        var selection = new Selection(offer.packs().stream().distinct().toList(), offer.key());
        var selectedCache = cache;
        if (active.packs().equals(selection.packs()) && Arrays.equals(active.key(), selection.key())) {
            selectedCache.prune(selection.paths(selectedCache));
            return;
        }
        active = selection;
        client.reloadResourcePacks().thenRunAsync(() -> {
            if (cache == selectedCache && active == selection) {
                selectedCache.prune(selection.paths(selectedCache));
            }
        }, client);
    }

    public static List<PackResources> addToReload(Minecraft client, List<PackResources> packs, boolean recovery) {
        mounted = false;
        if (recovery || connection == null || client.getConnection() != connection || active.packs().isEmpty()) {
            return packs;
        }
        var sources = new ArrayList<PackResources>();
        for (var info : active.packs()) {
            try {
                sources.add(cache.open(info, active.key()));
            } catch (IOException e) {
                sources.forEach(PackResources::close);
                KaleidoscopeDoll.LOGGER.warn("Could not open server doll config {}: {}", info.sha256(), e.getMessage());
                return packs;
            }
        }
        // The resource manager owns these handles; the pack repository never sees them.
        sources.addAll(packs);
        mounted = true;
        return sources;
    }

    public static boolean retryWithoutServerDolls(Minecraft client) {
        if (!mounted) {
            return false;
        }
        mounted = false;
        deactivate(client);
        return true;
    }

    public static boolean isCurrentSource(String sourceId) {
        return !sourceId.startsWith(ServerDollPackCache.SOURCE_PREFIX)
                || (connection != null && Minecraft.getInstance().getConnection() == connection
                && active.packs().stream().anyMatch(info ->
                        ServerDollPackCache.sourceId(cache.path(info, active.key())).equals(sourceId)));
    }

    private record Selection(List<DollPackPackets.PackInfo> packs, byte[] key) {
        private static final Selection EMPTY = new Selection(List.of(), new byte[0]);

        List<Path> paths(ServerDollPackCache cache) {
            return packs.stream().map(info -> cache.path(info, key)).toList();
        }
    }

    static final class Download {
        private final DollPackPackets.Offer offer;
        private final byte[][] archives;
        private int index;
        private int offset;

        Download(DollPackPackets.Offer offer) {
            this.offer = offer;
            archives = new byte[offer.packs().size()][];
        }

        boolean accept(DollPackPackets.Chunk chunk) throws IOException {
            if (index >= archives.length || chunk.packIndex() != index || chunk.offset() != offset) {
                throw new IOException("Out of order server doll ZIP chunk");
            }
            int size = offer.transferSize(index);
            int length = chunk.data().length;
            if (length != Math.min(DollPackPackets.CHUNK_BYTES, size - offset)) {
                throw new IOException("Invalid server doll ZIP chunk size");
            }
            if (offset == 0) {
                archives[index] = new byte[size];
            }
            System.arraycopy(chunk.data(), 0, archives[index], offset, length);
            offset += length;
            if (offset == size) {
                index++;
                offset = 0;
            }
            return index == archives.length;
        }
    }
}
