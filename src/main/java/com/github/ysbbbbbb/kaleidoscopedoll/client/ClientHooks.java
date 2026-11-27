package com.github.ysbbbbbb.kaleidoscopedoll.client;

import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 客户端钩子，由客户端初始化时注入实现，避免公共代码直接引用客户端类
 */
public final class ClientHooks {
    public static Runnable openTweaksScreen = () -> {};
    public static BiConsumer<String, Consumer<Component>> customDollTooltip = (modelId, consumer) -> {};
}
