package com.kaleidoscopedoll.render;

import com.kaleidoscopedoll.block.DollMachineBlock;
import com.kaleidoscopedoll.block.entity.DollMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.IntStream;

public final class DollMachineRender implements BlockEntityRenderer<DollMachineBlockEntity> {
    private static final ExtraModelKey<BlockStateModel> SHELL = ExtraModelKey.create(() -> "doll_machine_shell");
    private static final List<ExtraModelKey<BlockStateModel>> PRIZES = IntStream.rangeClosed(1, DollMachinePrizeMotion.PRIZE_COUNT)
            .<ExtraModelKey<BlockStateModel>>mapToObj(index -> ExtraModelKey.create(() -> "doll_machine_prize_" + index)).toList();
    private final LegacyDollRender.Animation<DollMachineBlockEntity> animation = new LegacyDollRender.Animation<>("doll_machine");

    public DollMachineRender(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelLoadingPlugin.Context plugin) {
        plugin.addModel(SHELL, new MachinePart("doll_machine_shell"));
        for (int i = 0; i < PRIZES.size(); ++i) {
            plugin.addModel(PRIZES.get(i), new MachinePart("doll_machine_prize_" + (i + 1)));
        }
    }

    @Override
    public void render(DollMachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay, Vec3 cameraPosition) {
        var state = machine.getBlockState();
        if (!DollMachineBlock.isRunning(state)) return;
        var models = (FabricBakedModelManager) Minecraft.getInstance().getModelManager();
        var shell = models.getModel(SHELL);
        if (shell == null) {
            LegacyDollRender.block(state, pose, buffers, light, overlay);
            return;
        }

        pose.pushPose();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.rotateAround(Axis.YP.rotationDegrees(180 - facing.toYRot()), 0.5f, 0, 0.5f);
        animation.prepare(machine, machine.getBlockPos().asLong(), partialTick);
        animation.getBone("root").ifPresent(root ->
                LegacyDollRender.transform(pose, root, root.getRotZ() >= 0 ? 1 / 16.0 : 15 / 16.0, 0.5));
        var vertices = buffers.getBuffer(Sheets.cutoutBlockSheet());
        ModelBlockRenderer.renderModel(pose.last(), vertices, shell, 1, 1, 1, light, overlay);
        float time = animation.getBone("prize_clock").map(bone -> bone.getPosX()).orElse(0F);
        for (int i = 0; i < PRIZES.size(); i++) {
            var prize = models.getModel(PRIZES.get(i));
            if (prize == null) continue;
            var frame = DollMachinePrizeMotion.sample(i, time);
            pose.pushPose();
            pose.translate(frame.center().scale(1 / 16.0));
            pose.mulPose(Axis.YP.rotation((float) frame.spin()));
            pose.scale(frame.scale(), frame.scale(), frame.scale());
            pose.translate(DollMachinePrizeMotion.home(i).scale(-1 / 16.0));
            ModelBlockRenderer.renderModel(pose.last(), vertices, prize, 1, 1, 1, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    private record MachinePart(Variant variant) implements UnbakedExtraModel<BlockStateModel> {
        private MachinePart(String name) {
            this(new Variant(LegacyDollRender.id("block/" + name)));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            variant.resolveDependencies(resolver);
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new SingleVariant(variant.bake(baker));
        }
    }
}
