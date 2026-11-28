package com.github.ysbbbbbb.kaleidoscopedoll.compat;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

public final class DollKeybinds {
    private DollKeybinds() {}

    public static boolean isDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }
}
