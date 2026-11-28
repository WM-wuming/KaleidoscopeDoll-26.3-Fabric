package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.client.render.JavaDollModels;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader.LoadedClientInfos;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ClientItemInfoLoader.class)
public abstract class ClientItemInfoLoaderMixin {
    @Inject(method = "scheduleLoad", at = @At("RETURN"), cancellable = true)
    private static void kaleidoscopeDoll$loadDollModels(ResourceManager resources, Executor executor,
                                                      CallbackInfoReturnable<CompletableFuture<LoadedClientInfos>> callback) {
        callback.setReturnValue(callback.getReturnValue().thenApplyAsync(loaded -> {
            var items = new HashMap<>(loaded.contents());
            resources.listResources(CustomDollConfigPacks.MODEL_PREFIX, file -> file.getPath().endsWith(".json"))
                    .forEach((file, resource) -> {
                        var id = CustomDollConfigPacks.modelIdFromFile(file);
                        if (id != null) {
                            var model = new CuboidItemModelWrapper.Unbaked(CustomDollConfigPacks.renderModel(id), Optional.empty(), List.of());
                            items.put(JavaDollModels.runtimeItemId(id), new ClientItem(model, ClientItem.Properties.DEFAULT));
                        }
                    });
            return new LoadedClientInfos(items);
        }, executor));
    }
}
