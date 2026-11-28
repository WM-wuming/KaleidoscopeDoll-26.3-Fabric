package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class JavaDollModels {
    private static volatile Map<ResourceLocation, Model> models = Map.of();

    private JavaDollModels() {
    }

    public static void register() {
        PreparableModelLoadingPlugin.register((resources, executor) -> CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, Model> prepared = new HashMap<>();
            resources.listResources(CustomDollConfigPacks.MODEL_PREFIX, file -> file.getPath().endsWith(".json"))
                    .forEach((file, resource) -> {
                ResourceLocation id = CustomDollConfigPacks.modelIdFromFile(file);
                if (id != null) {
                    var target = CustomDollConfigPacks.renderModel(id);
                    prepared.put(id, new Model(target, resource.sourcePackId()));
                }
            });
            return prepared;
        }, executor), (prepared, context) -> {
            models = Map.copyOf(prepared);
            context.addModels(prepared.values().stream().map(Model::location).distinct().toList());
        });
    }

    public static boolean render(String modelId, ItemDisplayContext display, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation id = CustomDollConfigPacks.modelLocation(modelId);
        Model entry = id == null ? null : models.get(id);
        if (entry == null || !ClientDollPackSync.isCurrentSource(entry.source())) return false;

        Minecraft client = Minecraft.getInstance();
        var model = ((FabricBakedModelManager) client.getModelManager()).getModel(entry.location());
        if (model == client.getModelManager().getMissingModel() || model.isCustomRenderer()) return false;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        client.getItemRenderer().render(new ItemStack(Items.STICK), display, false, pose, buffers, light, overlay, model);
        pose.popPose();
        return true;
    }

    private record Model(ResourceLocation location, String source) {}
}
