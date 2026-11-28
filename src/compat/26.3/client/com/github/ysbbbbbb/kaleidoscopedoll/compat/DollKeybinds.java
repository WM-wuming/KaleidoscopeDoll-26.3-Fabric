package com.github.ysbbbbbb.kaleidoscopedoll.compat;

import com.mojang.blaze3d.platform.InputConstants;

public final class DollKeybinds {
    private DollKeybinds() {}

    public static boolean isDown(int key) {
        return InputConstants.isKeyDown(key);
    }
}
