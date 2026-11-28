package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class JavaDollModels {
    private JavaDollModels() {
    }

    public static Identifier runtimeItemId(Identifier id) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "dynamic_dolls/" + id.getNamespace() + "/" + id.getPath());
    }

    static @Nullable Identifier resolve(ResourceManager resources, @Nullable String modelId) {
        Identifier id = CustomDollConfigPacks.modelLocation(modelId);
        if (id == null) return null;
        return resources.getResource(CustomDollConfigPacks.modelFile(id))
                .filter(resource -> ClientDollPackSync.isCurrentSource(resource.sourcePackId()))
                .isPresent() ? runtimeItemId(id) : null;
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
