package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.compat.DollBlockSubmission;
import com.github.ysbbbbbb.kaleidoscopedoll.compat.DollPoseStack;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.block.DollMachineBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class DollMachineRender extends GeoBlockRenderer<DollMachineBlockEntity, DollBlockRenderState> {
    private static final ExtraModelKey<List<BlockStateModelPart>> SHELL = ExtraModelKey.create(() -> "doll_machine_shell");
    private static final ExtraModelKey<List<List<BlockStateModelPart>>> PRIZES = ExtraModelKey.create(() -> "doll_machine_prizes");
    private static final int[] NO_TINTS = new int[0];

    public DollMachineRender(BlockEntityRendererProvider.Context context) {
        super(context, new AnimatedRootModel<>("doll_machine"));
    }

    public static void registerModels(ModelLoadingPlugin.Context plugin) {
        plugin.addModel(SHELL, new MachinePart("doll_machine_shell"));
        var prizes = IntStream.rangeClosed(1, DollMachinePrizeMotion.PRIZE_COUNT)
                .mapToObj(index -> new MachinePart("doll_machine_prize_" + index)).toList();
        plugin.addModel(PRIZES, new UnbakedExtraModel<>() {
            @Override
            public void resolveDependencies(ResolvableModel.Resolver resolver) {
                prizes.forEach(prize -> prize.resolveDependencies(resolver));
            }

            @Override
            public List<List<BlockStateModelPart>> bake(ModelBaker baker) {
                return prizes.stream().map(prize -> prize.bake(baker)).toList();
            }
        });
    }

    @Override
    public DollBlockRenderState createRenderState() {
        return new DollBlockRenderState();
    }

    @Override
    public void extractRenderState(DollMachineBlockEntity machine, DollBlockRenderState state, float partialTicks,
                                   @Nullable Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(machine, state, partialTicks, cameraPosition, breakProgress);
        state.captureBlock(machine);
    }

    @Override
    public void submit(DollBlockRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!DollMachineBlock.isRunning(state.movingBlockRenderState.blockState)) {
            return;
        }

        var models = Minecraft.getInstance().getModelManager();
        var shell = models.getModel(SHELL);
        var prizes = models.getModel(PRIZES);
        if (shell == null || prizes == null) {
            DollBlockSubmission.submit(collector, pose, state.movingBlockRenderState, 0);
            return;
        }

        var facing = state.movingBlockRenderState.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float yaw = 180 - facing.toYRot();
        pose.pushPose();
        pose.rotateAround(Axis.YP.rotationDegrees(yaw), 0.5F, 0, 0.5F);
        var pass = RenderPassInfo.create(this, state, pose, camera, false);
        pass.renderPosed(() -> {
            pass.model().getBone("root").ifPresent(bone -> {
                var roll = bone.frameSnapshot;
                if (roll != null) {
                    // Pivot on the grounded edge as the cabinet rocks from side to side.
                    double edge = roll.getRotZ() >= 0 ? 1 / 16.0 : 15 / 16.0;
                    AnimatedRootModel.transform(pose, roll, edge, 0, 0.5);
                }
            });
            submitPart(collector, pose, shell, state.lightCoords);

            float time = pass.model().getBone("prize_clock").map(bone -> bone.frameSnapshot)
                    .map(clock -> clock.getTranslateX()).orElse(0F);
            for (int i = 0; i < prizes.size(); i++) {
                var home = DollMachinePrizeMotion.home(i);
                var frame = DollMachinePrizeMotion.sample(i, time);
                pose.pushPose();
                pose.translate(frame.center().scale(1 / 16.0));
                DollPoseStack.rotate(pose, Axis.YP.rotation((float) frame.spin()));
                pose.scale(frame.scale(), frame.scale(), frame.scale());
                pose.translate(home.scale(-1 / 16.0));
                submitPart(collector, pose, prizes.get(i), state.lightCoords);
                pose.popPose();
            }
        });
        pose.popPose();
    }

    private static void submitPart(SubmitNodeCollector collector, PoseStack pose, List<BlockStateModelPart> part, int light) {
        collector.submitBlockModel(pose, RenderTypes.cutoutMovingBlock(), part, NO_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
    }

    private record MachinePart(Variant variant) implements UnbakedExtraModel<List<BlockStateModelPart>> {
        private MachinePart(String path) {
            this(new Variant(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "block/" + path)));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            variant.resolveDependencies(resolver);
        }

        @Override
        public List<BlockStateModelPart> bake(ModelBaker baker) {
            return List.of(variant.bake(baker));
        }
    }
}
