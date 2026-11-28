package com.kaleidoscopedoll.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class DollPackResources extends AbstractPackMetadataResources implements PackResources {
    protected DollPackResources(String id) {
        super(location(id));
    }

    private static PackLocationInfo location(String id) {
        return new PackLocationInfo(id, Component.literal(id), PackSource.BUILT_IN, Optional.empty());
    }

    protected static Pack createPack(String id, Supplier<PackResources> resources, boolean required) {
        return Pack.readMetaAndCreate(location(id), new Pack.ResourcesSupplier() {
            @Override
            public PackMetadataResources openMetadata(PackLocationInfo location) {
                return resources.get();
            }

            @Override
            public Stream<PackResources> openResources(PackLocationInfo location, Pack.Metadata metadata) {
                return Stream.of(resources.get());
            }
        }, PackType.CLIENT_RESOURCES, new PackSelectionConfig(required, Pack.Position.BOTTOM, required));
    }
}
