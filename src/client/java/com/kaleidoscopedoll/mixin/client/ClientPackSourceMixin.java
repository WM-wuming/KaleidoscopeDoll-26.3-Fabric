package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.client.custom.CustomDollConfigPackSource;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Mixin(ClientPackSource.class)
public abstract class ClientPackSourceMixin {
    @Inject(method = "populatePackList", at = @At("RETURN"))
    private void kaleidoscopeDoll$addConfigPacks(BiConsumer<String, Function<String, Pack>> output, CallbackInfo ci) {
        new CustomDollConfigPackSource(CustomDollConfigPacks.ROOT).loadPacks(pack -> output.accept(pack.getId(), id -> pack));
    }
}
