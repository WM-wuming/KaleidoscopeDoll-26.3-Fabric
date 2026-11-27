package com.github.ysbbbbbb.kaleidoscopedoll.client.custom;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.network.DollPackPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.packs.PackResources;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClientDollPackSync {
    private static ClientPacketListener connection;
    private static ServerDollPackCache cache;
    private static DollPackPackets.Offer latestOffer;
    private static Download download;
    private static List<Path> active = List.of();
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
                apply(client, List.of());
            }
        }));
    }

    private static void receiveOffer(Minecraft client, ClientPacketListener handler, DollPackPackets.Offer offer) {
        if (client.getConnection() != handler || handler.getConnection().isMemoryConnection()
                || !offer.isWithinLimits()) {
            return;
        }
        ServerData server = handler.getServerData();
        if (server != null && server.getResourcePackStatus() == ServerData.ServerPackStatus.DISABLED) {
            return;
        }
        if (connection != handler) {
            connection = handler;
            latestOffer = null;
            String address = server != null ? server.ip : handler.getConnection().getRemoteAddress().toString();
            cache = new ServerDollPackCache(address);
            apply(client, List.of());
        }
        if (latestOffer != null && offer.version() <= latestOffer.version()) {
            return;
        }
        latestOffer = offer;
        download = null;
        if (offer.packs().stream().allMatch(cache::contains)) {
            apply(client, offer.packs().stream().map(cache::path).distinct().toList());
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
                    var info = latestOffer.packs().get(i);
                    if (!info.equals(DollPackPackets.PackInfo.of(complete.archives[i]))) {
                        throw new IOException("Server doll ZIP checksum mismatch");
                    }
                    if (!cache.contains(info)) {
                        cache.store(info, complete.archives[i]);
                    }
                }
                apply(client, latestOffer.packs().stream().map(cache::path).distinct().toList());
            }
        } catch (IOException e) {
            download = null;
            cache.prune(active);
            KaleidoscopeDoll.LOGGER.warn("Could not sync server doll config: {}", e.getMessage());
        }
    }

    private static void apply(Minecraft client, List<Path> paths) {
        if (active.equals(paths)) {
            if (cache != null) {
                cache.prune(paths);
            }
            return;
        }
        active = paths;
        var selectedCache = cache;
        client.reloadResourcePacks().thenRunAsync(() -> {
            if (selectedCache != null && cache == selectedCache && active == paths) {
                selectedCache.prune(paths);
            }
        }, client);
    }

    public static List<PackResources> addToReload(Minecraft client, List<PackResources> packs, boolean recovery) {
        mounted = false;
        if (recovery || connection == null || client.getConnection() != connection || active.isEmpty()) {
            return packs;
        }
        var sources = new ArrayList<PackResources>();
        for (Path path : active) {
            var pack = ServerDollPackCache.openConfig(path);
            if (pack == null || !pack.getCompatibility().isCompatible()) {
                sources.forEach(PackResources::close);
                KaleidoscopeDoll.LOGGER.warn("Could not open server doll config {}", path);
                return packs;
            }
            pack.open().forEach(sources::add);
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
        active = List.of();
        client.reloadResourcePacks();
        return true;
    }

    public static boolean isCurrentSource(String sourceId) {
        return !sourceId.startsWith(ServerDollPackCache.SOURCE_PREFIX)
                || (connection != null && Minecraft.getInstance().getConnection() == connection
                && active.stream().anyMatch(path -> ServerDollPackCache.sourceId(path).equals(sourceId)));
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
            int size = offer.packs().get(index).size();
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
