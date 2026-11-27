package com.github.ysbbbbbb.kaleidoscopedoll.network;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.TransferableDollPack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.NameAndId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public final class ServerDollPackSync {
    private static final int CHUNKS_PER_TICK = 2;
    private static final Map<ServerGamePacketListenerImpl, Transfer> TRANSFERS = new IdentityHashMap<>();
    private static volatile List<Archive> archives = List.of();
    private static int nextVersion;

    private ServerDollPackSync() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TRANSFERS.remove(handler));
        ServerPlayNetworking.registerGlobalReceiver(DollPackPackets.Request.TYPE, (request, context) -> {
            var transfer = TRANSFERS.get(context.player().connection);
            if (transfer != null && transfer.version == request.version()) {
                transfer.requested = true;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var transfers = TRANSFERS.entrySet().iterator();
            while (transfers.hasNext()) {
                var entry = transfers.next();
                if (entry.getValue().sendChunks(entry.getKey().player)) {
                    transfers.remove();
                }
            }
        });
    }

    public static void refresh(Path directory) throws IOException {
        var loaded = new ArrayList<Archive>();
        int remaining = DollPackPackets.MAX_TOTAL_BYTES;
        try (var paths = Files.list(directory)) {
            for (var path : paths.filter(CustomDollConfigPacks::isArchive).sorted().toList()) {
                try (var zip = new ZipFile(path.toFile())) {
                    if (!CustomDollConfigPacks.hasManifest(zip)) {
                        continue;
                    }
                    if (loaded.size() == DollPackPackets.MAX_PACKS || Files.size(path) > remaining) {
                        throw new IOException("Server doll ZIP transfer limit exceeded");
                    }
                    TransferableDollPack.validate(zip);
                    try (var input = Files.newInputStream(path)) {
                        byte[] bytes = input.readNBytes(remaining + 1);
                        if (bytes.length == 0 || bytes.length > remaining) {
                            throw new IOException("Invalid server doll ZIP size");
                        }
                        loaded.add(new Archive(DollPackPackets.PackInfo.of(bytes), bytes));
                        remaining -= bytes.length;
                    }
                } catch (IOException e) {
                    KaleidoscopeDoll.LOGGER.warn("Cannot offer custom doll pack {} to clients: {}", path, e.getMessage());
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        archives = List.copyOf(loaded);
    }

    public static void offer(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, DollPackPackets.Offer.TYPE)
                || !ServerPlayNetworking.canSend(player, DollPackPackets.Chunk.TYPE)
                || player.level().getServer().isSingleplayerOwner(new NameAndId(player.getGameProfile()))) {
            return;
        }
        var snapshot = archives;
        int version = ++nextVersion;
        TRANSFERS.remove(player.connection);
        if (!snapshot.isEmpty()) {
            TRANSFERS.put(player.connection, new Transfer(version, snapshot));
        }
        ServerPlayNetworking.send(player, new DollPackPackets.Offer(version, snapshot.stream().map(Archive::info).toList()));
    }

    private record Archive(DollPackPackets.PackInfo info, byte[] data) {
    }

    private static final class Transfer {
        private final int version;
        private final List<Archive> archives;
        private boolean requested;
        private int index;
        private int offset;

        private Transfer(int version, List<Archive> archives) {
            this.version = version;
            this.archives = archives;
        }

        private boolean sendChunks(ServerPlayer player) {
            if (!requested) {
                return false;
            }
            for (int i = 0; i < CHUNKS_PER_TICK && index < archives.size(); i++) {
                byte[] archive = archives.get(index).data();
                int end = Math.min(offset + DollPackPackets.CHUNK_BYTES, archive.length);
                ServerPlayNetworking.send(player, new DollPackPackets.Chunk(version, index, offset, Arrays.copyOfRange(archive, offset, end)));
                offset = end;
                if (offset == archive.length) {
                    index++;
                    offset = 0;
                }
            }
            return index == archives.size();
        }
    }
}
