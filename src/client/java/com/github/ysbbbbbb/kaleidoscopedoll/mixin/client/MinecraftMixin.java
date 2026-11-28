package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @WrapOperation(method = {
            "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",
            "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;"
    },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;createReload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/server/packs/resources/ReloadInstance;"))
    private ReloadInstance kaleidoscopeDoll$loadServerDolls(ReloadableResourceManager manager, Executor prepare,
                                                         Executor apply, CompletableFuture<?> initial,
                                                         List<PackResources> packs, Operation<ReloadInstance> original,
                                                         boolean recovery) {
        return original.call(manager, prepare, apply, initial,
                ClientDollPackSync.addToReload((Minecraft) (Object) this, packs, recovery));
    }

    @Inject(method = "rollbackResourcePacks", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$skipBrokenServerDolls(Throwable error, @Coerce Object cookie, CallbackInfo ci) {
        if (ClientDollPackSync.retryWithoutServerDolls((Minecraft) (Object) this)) {
            KaleidoscopeDoll.LOGGER.warn("Resource reload failed with server doll configs; retrying without them", error);
            ci.cancel();
        }
    }
}
