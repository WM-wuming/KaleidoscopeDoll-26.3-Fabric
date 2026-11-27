package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.client.render.JavaDollModels;
import com.github.ysbbbbbb.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DollEntitySpecialRenderer implements SpecialModelRenderer<ItemStack> {
    private final Cache<ItemStack, ItemStack> dollCache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();
    private final ItemStackRenderState itemState = new ItemStackRenderState();

    @Override
    public @Nullable ItemStack extractArgument(ItemStack stack) {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return ItemStack.EMPTY;
        }

        ItemStack dollShowItem = dollCache.getIfPresent(stack);
        if (dollShowItem == null) {
            dollShowItem = createDisplayStack(stack);
            dollCache.put(stack, dollShowItem);
        }
        return dollShowItem;
    }

    static ItemStack createDisplayStack(ItemStack stack) {
        String dollId = DollEntityItem.getCustomDollIdFromItemStack(stack);
        if (StringUtils.isNotBlank(dollId)) {
            ItemStack displayStack = new ItemStack(ModItems.CUSTOM_DOLL);
            CustomDollItem.setModelId(displayStack, dollId);
            return displayStack;
        }

        Block displayBlock = DollEntityItem.getBlockFromItemStack(stack);
        return displayBlock == Blocks.AIR ? new ItemStack(ModItems.PURPLE_DOLL_GIFT_BOX) : new ItemStack(displayBlock);
    }

    @Override
    public void submit(@Nullable ItemStack argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (argument == null || argument.isEmpty()) {
            return;
        }

        if (argument.is(ModItems.CUSTOM_DOLL)) {
            ItemStack display = JavaDollModels.withDollModel(argument);
            if (display == argument) {
                // Bedrock models already have their own transform.
                renderCustomModel(CustomDollItem.getModelId(argument), poseStack, submitNodeCollector, lightCoords);
                return;
            }
            argument = display;
        }

        ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // 用 GUI 上下文解析玩偶模型：应用模型自带的 gui 显示变换（旋转 30/225），
        // 背包里显示玩偶正面
        itemModelResolver.updateForTopItem(itemState, argument, ItemDisplayContext.GUI, Minecraft.getInstance().level, null, 0);
        itemState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }

    /**
     * 直接绘制自定义玩偶的基岩模型，变换与 CustomDollSpecialRenderer 完全一致
     */
    private void renderCustomModel(String modelId, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        BedrockModel model;
        Identifier texture;

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
        poseStack.rotate(Axis.ZN.rotationDegrees(180));

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
        output.accept(new Vector3f(-0.5f, -0.5f, -0.5f));
        output.accept(new Vector3f(0.5f, 0.5f, 0.5f));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(BakingContext context) {
            return new DollEntitySpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<ItemStack>> type() {
            return MAP_CODEC;
        }
    }
}
