package com.kaleidoscopedoll.client.custom;

import com.kaleidoscopedoll.compat.DollPackResources;
import com.kaleidoscopedoll.data.custom.TransferableDollPack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

final class MemoryDollPack extends DollPackResources {
    private static final String ASSETS = "assets/";
    private final ZipFile zip;
    private final boolean ownsZip;

    private MemoryDollPack(String id, ZipFile zip, boolean ownsZip) {
        super(id);
        this.zip = zip;
        this.ownsZip = ownsZip;
    }

    static MemoryDollPack open(String id, byte[] bytes) throws IOException {
        var zip = openZip(bytes);
        boolean compatible = false;
        try {
            TransferableDollPack.validate(zip.getEntries(), zip::getEntry, zip::getInputStream);
            var pack = createPack(id, () -> new MemoryDollPack(id, zip, false), false);
            compatible = pack != null && pack.getCompatibility().isCompatible();
            if (!compatible) {
                throw new IOException("Incompatible server doll config format");
            }
            return new MemoryDollPack(id, zip, true);
        } catch (RuntimeException e) {
            throw new IOException("Invalid server doll ZIP", e);
        } finally {
            if (!compatible) {
                ZipFile.closeQuietly(zip);
            }
        }
    }

    static Pack localConfig(String id, byte[] bytes) throws IOException {
        try (var validated = open(id, bytes)) {
            return createPack(id, () -> {
                try {
                    return new MemoryDollPack(id, openZip(bytes), true);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, true);
        }
    }

    @SuppressWarnings("deprecation") // This constructor exists in all supported clients.
    private static ZipFile openZip(byte[] bytes) throws IOException {
        return new ZipFile(new SeekableInMemoryByteChannel(bytes));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return resource(String.join("/", segments));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        return type == PackType.CLIENT_RESOURCES ? resource(ASSETS + id.getNamespace() + "/" + id.getPath()) : null;
    }

    private IoSupplier<InputStream> resource(String name) {
        var entry = zip.getEntry(name);
        return entry == null || entry.isDirectory() ? null : () -> zip.getInputStream(entry);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        String namespacePath = ASSETS + namespace + "/";
        String prefix = path.isEmpty() ? namespacePath : namespacePath + path + "/";
        var entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().startsWith(prefix)) {
                var id = Identifier.fromNamespaceAndPath(namespace, entry.getName().substring(namespacePath.length()));
                output.accept(id, () -> zip.getInputStream(entry));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.CLIENT_RESOURCES) {
            return Set.of();
        }
        var namespaces = new HashSet<String>();
        var entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(ASSETS)) {
                namespaces.add(name.substring(ASSETS.length(), name.indexOf('/', ASSETS.length())));
            }
        }
        return namespaces;
    }

    @Override
    public void close() {
        if (ownsZip) {
            ZipFile.closeQuietly(zip);
        }
    }
}
