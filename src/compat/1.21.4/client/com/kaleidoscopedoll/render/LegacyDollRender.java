package com.kaleidoscopedoll.render;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

public final class LegacyDollRender {
    private LegacyDollRender() {
    }

    public static void block(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        model(state, Minecraft.getInstance().getBlockRenderer().getBlockModel(state), pose, buffers, light, overlay);
    }

    static void model(BlockState state, BakedModel model, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state, model, 1, 1, 1, light, overlay);
    }

    public static void bedrock(String id, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BedrockModel model = StringUtils.isBlank(id) ? null : CustomDollLoader.getModel(id);
        ResourceLocation texture;
        if (model == null) {
            model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
            texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
        } else {
            texture = CustomDollLoader.getTexture(id);
            if (texture == null) texture = MissingTextureAtlasSprite.getLocation();
        }
        if (model == null) return;
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.ZN.rotationDegrees(180));
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 1, 1, 1, 1);
        pose.popPose();
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path);
    }

    public static final class Animation<T extends GeoAnimatable> extends GeoModel<T> {
        private final ResourceLocation model;

        public Animation(String model) {
            this.model = id("geo/" + model + ".geo.json");
        }

        @Override
        public ResourceLocation getModelResource(T animatable, GeoRenderer<T> renderer) {
            return model;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable, GeoRenderer<T> renderer) {
            return MissingTextureAtlasSprite.getLocation();
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return id("animations/interactions.animation.json");
        }

        public void prepare(T animatable, long instance, float partialTick) {
            getBakedModel(model);
            handleAnimations(animatable, instance, new AnimationState<>(animatable, 0, 0, partialTick, false), partialTick);
        }

        public void applyRoot(PoseStack pose, double pivot) {
            getBone("root").ifPresent(root -> transform(pose, root, pivot, pivot));
        }
    }

    static void transform(PoseStack pose, GeoBone bone, double pivotX, double pivotZ) {
        pose.translate(pivotX, 0, pivotZ);
        RenderUtil.translateMatrixToBone(pose, bone);
        RenderUtil.rotateMatrixAroundBone(pose, bone);
        RenderUtil.scaleMatrixForBone(pose, bone);
        pose.translate(-pivotX, 0, -pivotZ);
    }
}
