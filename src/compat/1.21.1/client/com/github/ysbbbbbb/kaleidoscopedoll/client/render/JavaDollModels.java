package com.github.ysbbbbbb.kaleidoscopedoll.client.render;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class JavaDollModels {
    private static final FileToIdConverter ITEMS = FileToIdConverter.json("items");
    private static volatile Map<ResourceLocation, Model> models = Map.of();

    private JavaDollModels() {
    }

    public static void register() {
        PreparableModelLoadingPlugin.register((resources, executor) -> CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, Model> prepared = new HashMap<>();
            ITEMS.listMatchingResources(resources).forEach((file, resource) -> {
                ResourceLocation id = ITEMS.fileToId(file);
                if (!KaleidoscopeDoll.MOD_ID.equals(id.getNamespace())) {
                    ResourceLocation target = readPlainModel(resource);
                    if (target != null) prepared.put(id, new Model(target, resource.sourcePackId()));
                }
            });
            return prepared;
        }, executor), (prepared, context) -> {
            models = Map.copyOf(prepared);
            context.addModels(prepared.values().stream().map(Model::location).distinct().toList());
        });
    }

    private static @Nullable ResourceLocation readPlainModel(Resource resource) {
        try (var reader = resource.openAsReader()) {
            var model = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("model");
            if (model != null && model.has("type") && model.has("model")
                    && "minecraft:model".equals(model.get("type").getAsString())) {
                return ResourceLocation.tryParse(model.get("model").getAsString());
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return null;
    }

    public static boolean render(String modelId, ItemDisplayContext display, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (modelId == null || !modelId.startsWith("java:")) return false;
        String name = modelId.substring(5);
        ResourceLocation id = name.indexOf(':') > 0 ? ResourceLocation.tryParse(name) : null;
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
