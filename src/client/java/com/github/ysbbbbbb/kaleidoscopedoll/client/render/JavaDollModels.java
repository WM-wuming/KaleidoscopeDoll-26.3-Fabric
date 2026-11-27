package com.github.ysbbbbbb.kaleidoscopedoll.client.render;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class JavaDollModels {
    private static final String PREFIX = "java:";
    private static final FileToIdConverter ITEM_DEFINITIONS = FileToIdConverter.json("items");

    private JavaDollModels() {
    }

    static @Nullable Identifier resolve(ResourceManager resources, @Nullable String modelId) {
        if (modelId == null || !modelId.startsWith(PREFIX)) {
            return null;
        }
        String target = modelId.substring(PREFIX.length());
        Identifier id = target.indexOf(':') > 0 ? Identifier.tryParse(target) : null;
        // Use a pack namespace, never this mod's own special item models.
        if (id == null || id.getPath().isEmpty() || KaleidoscopeDoll.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        return resources.getResource(ITEM_DEFINITIONS.idToFile(id))
                .filter(resource -> ClientDollPackSync.isCurrentSource(resource.sourcePackId()))
                .isPresent() ? id : null;
    }

    public static ItemStack withDollModel(ItemStack stack) {
        Identifier id = resolve(Minecraft.getInstance().getResourceManager(), CustomDollItem.getModelId(stack));
        if (id == null) {
            return stack;
        }
        ItemStack display = stack.copy();
        display.set(DataComponents.ITEM_MODEL, id);
        return display;
    }

    public static void extractWorldModel(ItemStackRenderState output, @Nullable String modelId) {
        Minecraft client = Minecraft.getInstance();
        Identifier id = resolve(client.getResourceManager(), modelId);
        if (id == null) {
            output.clear();
            return;
        }
        ItemStack display = new ItemStack(Items.STICK);
        display.set(DataComponents.ITEM_MODEL, id);
        client.getItemModelResolver().updateForTopItem(output, display, ItemDisplayContext.NONE, client.level, null, 0);
    }

    public static void submitCentered(ItemStackRenderState model, PoseStack pose, SubmitNodeCollector collector, int light, int outline) {
        pose.pushPose();
        // Item quads are centered; move them back to block coordinates.
        pose.translate(0.5, 0.5, 0.5);
        model.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outline);
        pose.popPose();
    }
}
