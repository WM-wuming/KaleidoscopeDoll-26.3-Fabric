package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.block.DollMachineBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.IntStream;

public class DollMachineRender implements BlockEntityRenderer<DollMachineBlockEntity> {
    private static final ResourceLocation SHELL = LegacyDollRender.id("block/doll_machine_shell");
    private static final List<ResourceLocation> PRIZES = IntStream.rangeClosed(1, DollMachinePrizeMotion.PRIZE_COUNT)
            .mapToObj(index -> LegacyDollRender.id("block/doll_machine_prize_" + index)).toList();
    private final LegacyDollRender.Animation<DollMachineBlockEntity> animation = new LegacyDollRender.Animation<>("doll_machine");

    public DollMachineRender(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelLoadingPlugin.Context plugin) {
        plugin.addModels(SHELL);
        plugin.addModels(PRIZES);
    }

    @Override
    public void render(DollMachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = machine.getBlockState();
        if (!DollMachineBlock.isRunning(state)) return;

        var models = (FabricBakedModelManager) Minecraft.getInstance().getModelManager();
        pose.pushPose();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.rotateAround(Axis.YP.rotationDegrees(180 - facing.toYRot()), 0.5f, 0, 0.5f);
        animation.prepare(machine, machine.getBlockPos().asLong(), partialTick);
        animation.getBone("root").ifPresent(root ->
                LegacyDollRender.transform(pose, root, root.getRotZ() >= 0 ? 1 / 16.0 : 15 / 16.0, 0.5));
        LegacyDollRender.model(state, models.getModel(SHELL), pose, buffers, light, overlay);
        float time = animation.getBone("prize_clock").map(clock -> clock.getPosX()).orElse(0F);
        for (int i = 0; i < PRIZES.size(); i++) {
            Vec3 home = DollMachinePrizeMotion.home(i);
            var frame = DollMachinePrizeMotion.sample(i, time);
            Vec3 center = frame.center();
            pose.pushPose();
            pose.translate(center.x / 16, center.y / 16, center.z / 16);
            pose.mulPose(Axis.YP.rotation((float) frame.spin()));
            pose.scale(frame.scale(), frame.scale(), frame.scale());
            pose.translate(-home.x / 16, -home.y / 16, -home.z / 16);
            LegacyDollRender.model(state, models.getModel(PRIZES.get(i)), pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }
}
