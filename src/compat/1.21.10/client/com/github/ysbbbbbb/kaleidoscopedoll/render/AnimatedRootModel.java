package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

public final class AnimatedRootModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation ANIMATIONS = location("interactions");
    private final ResourceLocation model;

    public AnimatedRootModel() {
        this("animated_root");
    }

    public AnimatedRootModel(String modelPath) {
        this.model = location(modelPath);
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(GeoRenderState state) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState state) {
        return MissingTextureAtlasSprite.getLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATIONS;
    }

    static <T extends GeoAnimatable, R extends GeoRenderState> GeoModel<T> animate(GeoRenderer<T, ?, R> renderer, R state) {
        var model = renderer.getGeoModel();
        model.getBakedModel(model.getModelResource(state));
        model.handleAnimations(renderer.createAnimationState(state));
        return model;
    }

    static <T extends GeoAnimatable, R extends GeoRenderState> void withAnimation(
            GeoRenderer<T, ?, R> renderer, R state, java.util.function.Consumer<GeoModel<T>> submit) {
        submit.accept(animate(renderer, state));
    }

    public static <T extends GeoAnimatable, R extends GeoRenderState> void apply(
            GeoRenderer<T, ?, R> renderer, R state, PoseStack pose, CameraRenderState camera, double pivotXZ) {
        animate(renderer, state).getBone("root")
                .ifPresent(bone -> transform(pose, bone, pivotXZ, 0, pivotXZ));
    }

    static void transform(PoseStack pose, GeoBone bone, double x, double y, double z) {
        pose.translate(x, y, z);
        RenderUtil.translateMatrixToBone(pose, bone);
        RenderUtil.rotateMatrixAroundBone(pose, bone);
        RenderUtil.scaleMatrixForBone(pose, bone);
        pose.translate(-x, -y, -z);
    }
}
