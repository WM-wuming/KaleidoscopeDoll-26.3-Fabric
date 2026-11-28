package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.compat.DollPoseStack;
import com.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class CustomDollSpecialRenderer implements SpecialModelRenderer<ItemStack> {
    @Override
    public @Nullable ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    @Override
    public void submit(@Nullable ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (stack == null) {
            return;
        }

        BedrockModel model;
        Identifier texture;

        String modelId = CustomDollItem.getModelId(stack);
        if (StringUtils.isBlank(modelId)) {
            model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
            texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
        } else {
            model = CustomDollLoader.getModel(modelId);
            if (model == null) {
                model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
                texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
            } else {
                texture = CustomDollLoader.getTexture(modelId);
                if (texture == null) {
                    texture = MissingTextureAtlasSprite.getLocation();
                }
            }
        }

        if (model == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        DollPoseStack.rotate(poseStack, Axis.ZN.rotationDegrees(180));

        final BedrockModel finalModel = model;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, buffer) -> {
            PoseStack modelPose = new PoseStack();
            modelPose.last().set(pose);
            finalModel.renderToBuffer(modelPose, buffer, lightCoords, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });

        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.5f, 0, -0.5f));
        output.accept(new Vector3f(0.5f, 1.0f, 0.5f));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(BakingContext context) {
            return new CustomDollSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<ItemStack>> type() {
            return MAP_CODEC;
        }
    }
}
