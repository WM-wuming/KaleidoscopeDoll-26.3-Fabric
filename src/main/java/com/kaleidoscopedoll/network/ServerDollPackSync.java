package com.kaleidoscopedoll.network;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.config.GeneralConfig;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.kaleidoscopedoll.data.custom.FolderDollPack;
import com.kaleidoscopedoll.data.custom.TransferableDollPack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.NameAndId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public final class ServerDollPackSync {
    private static final int CHUNKS_PER_TICK = 2;
    private static final Map<ServerGamePacketListenerImpl, Transfer> TRANSFERS = new IdentityHashMap<>();
    private static final byte[] NO_KEY = new byte[0];
    private static volatile Snapshot snapshot = new Snapshot(List.of(), NO_KEY);
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
        int remaining = GeneralConfig.SERVER_DOLL_MAX_TOTAL_MIB.get() * DollPackPackets.BYTES_PER_MIB;
        int maxPacks = GeneralConfig.SERVER_DOLL_MAX_PACK_COUNT.get();
        try (var paths = Files.list(directory)) {
            for (var path : paths.sorted().toList()) {
                if (loaded.size() == maxPacks || remaining == 0) break;
                try {
                    byte[] bytes = FolderDollPack.isPack(path) ? readFolder(path, remaining)
                            : CustomDollConfigPacks.isArchive(path) ? readZip(path, remaining) : null;
                    if (bytes == null) continue;
                    loaded.add(new Archive(DollPackPackets.PackInfo.of(bytes), bytes));
                    remaining -= bytes.length;
                } catch (IOException e) {
                    KaleidoscopeDoll.LOGGER.warn("Cannot offer custom doll pack {} to clients: {}", path, e.getMessage());
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        byte[] key = NO_KEY;
        if (!loaded.isEmpty() && GeneralConfig.ENCRYPT_SERVER_DOLL_PACKS.get()) {
            try {
                key = serverKey(directory.resolveSibling("server-doll-sync.key"));
                for (int i = 0; i < loaded.size(); i++) {
                    var archive = loaded.get(i);
                    loaded.set(i, new Archive(archive.info(), DollPackEncryption.seal(archive.data(), key)));
                }
            } catch (IOException e) {
                snapshot = new Snapshot(List.of(), NO_KEY);
                KaleidoscopeDoll.LOGGER.error("Server doll sync is unavailable because encryption failed", e);
                return;
            }
        }
        snapshot = new Snapshot(List.copyOf(loaded), key);
    }

    private static byte[] readFolder(Path path, int remaining) throws IOException {
        byte[] bytes = FolderDollPack.read(path).archive(remaining);
        TransferableDollPack.validate(bytes);
        return bytes;
    }

    private static byte[] readZip(Path path, int remaining) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            if (!CustomDollConfigPacks.hasJavaModels(zip)) return null;
            if (Files.size(path) > remaining) throw new IOException("Server doll ZIP transfer limit exceeded");
            TransferableDollPack.validate(zip);
            try (var input = Files.newInputStream(path)) {
                byte[] bytes = input.readNBytes(remaining + 1);
                if (bytes.length == 0 || bytes.length > remaining) throw new IOException("Invalid server doll ZIP size");
                return bytes;
            }
        }
    }

    private static byte[] serverKey(Path path) throws IOException {
        try {
            Files.write(path, DollPackEncryption.newKey(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException ignored) {
            // Keep the same key across reloads and restarts so existing caches remain usable.
        }
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] key = input.readNBytes(DollPackEncryption.KEY_BYTES + 1);
            if (key.length != DollPackEncryption.KEY_BYTES) {
                throw new IOException("Invalid server doll encryption key file");
            }
            return key;
        }
    }

    public static void offer(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, DollPackPackets.Offer.TYPE)
                || !ServerPlayNetworking.canSend(player, DollPackPackets.Chunk.TYPE)
                || player.level().getServer().isSingleplayerOwner(new NameAndId(player.getGameProfile()))) {
            return;
        }
        var current = snapshot;
        int version = ++nextVersion;
        TRANSFERS.remove(player.connection);
        if (!current.archives().isEmpty()) {
            TRANSFERS.put(player.connection, new Transfer(version, current.archives()));
        }
        ServerPlayNetworking.send(player, new DollPackPackets.Offer(version,
                current.archives().stream().map(Archive::info).toList(), current.key()));
    }

    private record Snapshot(List<Archive> archives, byte[] key) {
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
