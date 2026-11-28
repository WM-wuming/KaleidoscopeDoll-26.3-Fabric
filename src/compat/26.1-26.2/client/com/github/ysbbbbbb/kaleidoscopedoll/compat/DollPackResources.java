package com.github.ysbbbbbb.kaleidoscopedoll.compat;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import java.util.List;

public final class DollPackResources {
    private DollPackResources() {}

    public static void add(Pack pack, List<PackResources> target) {
        target.add(pack.open());
    }
}
