package com.github.ysbbbbbb.kaleidoscopedoll.animation;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public final class DollAnimations {
    public static final String SQUEEZE = "squeeze";
    private static final RawAnimation SQUEEZE_ANIMATION = RawAnimation.begin().thenPlay("animation.doll.squeeze");

    private DollAnimations() {
    }

    public static void registerSqueeze(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeoAnimatable>(SQUEEZE, 0, state -> PlayState.STOP)
                .triggerableAnim(SQUEEZE, SQUEEZE_ANIMATION));
    }
}
