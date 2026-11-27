package com.github.ysbbbbbb.kaleidoscopedoll.event;

import com.github.ysbbbbbb.kaleidoscopedoll.config.GeneralConfig;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import org.apache.commons.lang3.StringUtils;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class DollClickEntityEvent {
    private static final int MAX_RIDING_LAYERS = 5;

    public static void register() {
        UseEntityCallback.EVENT.register(DollClickEntityEvent::onUseEntity);
    }

    private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        ItemStack handItem = player.getItemInHand(hand);

        if (handItem.is(ModItems.DOLL_ENTITY_ITEM) && GeneralConfig.DOLL_CAN_MOUNT_ENTITIES.get()) {
            // 检查骑乘层数
            int ridingLayers = getRidingLayers(entity);
            if (ridingLayers >= MAX_RIDING_LAYERS) {
                return InteractionResult.PASS;
            }

            // 26.3 中实体 ID 在加入世界时才分配，客户端本地创建的玩偶没有 ID，
            // 不能在客户端创建并 startRiding 未注册的实体，否则客户端 tick 乘客时 getId() 会直接崩溃
            // 客户端只做预测，实际的生成、音效、消耗物品全部由服务端处理
            if (level.isClientSide()) {
                return entity.getPassengers().isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }

            DollEntity dollEntity = DollEntityItem.getDollEntity(level, handItem);
            // 空玩偶回退紫色礼盒，避免实体玩偶因显示状态为空气而不可见
            if (StringUtils.isBlank(dollEntity.getCustomDollId()) && dollEntity.getDisplayBlockState().isAir()) {
                dollEntity.setDisplayBlockState(ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState());
            }
            dollEntity.setPos(entity.getX(), entity.getY() + 1.5, entity.getZ());
            dollEntity.setYRot(player.getYRot() - 180);

            if (dollEntity.startRiding(entity)) {
                level.addFreshEntity(dollEntity);
                // 玩偶音效统一走 BLOCKS 通道，避免目标实体（如马）的 NEUTRAL 通道被静音后听不到
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    handItem.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private static int getRidingLayers(Entity entity) {
        int layers = 1;
        Entity current = entity;

        while (current.getVehicle() != null) {
            current = current.getVehicle();
            layers++;
        }

        return layers;
    }
}
