package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;

public final class AnimatedRootModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "interactions");
    private final Identifier model;

    public AnimatedRootModel() {
        this("animated_root");
    }

    public AnimatedRootModel(String modelPath) {
        this.model = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, modelPath);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return MissingTextureAtlasSprite.getLocation();
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return ANIMATIONS;
    }

    // Animate the original model as one bone so its textures and resource pack overrides still work.
    public static <R extends GeoRenderState> RenderPassInfo<R> apply(GeoRenderer<?, ?, R> renderer, R state,
                                                                    PoseStack pose, CameraRenderState camera,
                                                                    double pivotXZ) {
        RenderPassInfo<R> pass = RenderPassInfo.create(renderer, state, pose, camera, false);
        pass.addBoneUpdater((info, bones) -> bones.ifPresent("root", root -> transform(pose, root, pivotXZ, 0, pivotXZ)));
        pass.renderPosed(() -> {});
        return pass;
    }

    static void transform(PoseStack pose, BoneSnapshot bone, double x, double y, double z) {
        if (bone.hasTranslation() || bone.hasRotation() || bone.hasScale()) {
            pose.translate(x, y, z);
            bone.translate(pose);
            bone.rotate(pose);
            bone.scale(pose);
            pose.translate(-x, -y, -z);
        }
    }
}
