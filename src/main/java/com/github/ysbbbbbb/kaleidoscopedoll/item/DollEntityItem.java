package com.github.ysbbbbbb.kaleidoscopedoll.item;

import com.github.ysbbbbbb.kaleidoscopedoll.client.ClientHooks;
import com.github.ysbbbbbb.kaleidoscopedoll.config.GeneralConfig;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.init.DollSounds;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModEntities;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

import static com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity.TAG_BLOCK_STATE;
import static com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity.TAG_CUSTOM_DOLL_ID;

public class DollEntityItem extends Item {
    private static final String TAG_DOLL_ENTITY = "doll_entity";
    private static final int THROW_DURATION = 5;

    public DollEntityItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createItemWithEntity(DollEntity entity) {
        ItemStack stack = new ItemStack(ModItems.DOLL_ENTITY_ITEM);
        saveDollEntity(stack, entity);
        return stack;
    }

    public static ItemStack createItemWithBlockState(BlockState state) {
        ItemStack stack = new ItemStack(ModItems.DOLL_ENTITY_ITEM);
        CompoundTag entityTag = new CompoundTag();
        entityTag.put(TAG_BLOCK_STATE, NbtUtils.writeBlockState(state));
        stack.set(ModDataComponent.TAG_DOLL_ENTITY, entityTag);
        return stack;
    }

    public static ItemStack createItemWithCustomDollId(String customDollId) {
        ItemStack stack = new ItemStack(ModItems.DOLL_ENTITY_ITEM);
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString(TAG_CUSTOM_DOLL_ID, customDollId);
        stack.set(ModDataComponent.TAG_DOLL_ENTITY, entityTag);
        return stack;
    }

    public static void saveDollEntity(ItemStack stack, DollEntity entity) {
        if (!stack.is(ModItems.DOLL_ENTITY_ITEM)) {
            return;
        }

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.addAdditionalSaveData(output);
        CompoundTag entityTag = output.buildResult();
        entity.removePhantomRecord(entityTag);

        stack.set(ModDataComponent.TAG_DOLL_ENTITY, entityTag);
    }

    public static Block getBlockFromItemStack(ItemStack stack) {
        if (!stack.is(ModItems.DOLL_ENTITY_ITEM)) {
            return ModBlocks.PURPLE_DOLL_GIFT_BOX;
        }

        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        if (entityTag != null && entityTag.contains(TAG_BLOCK_STATE)) {
            CompoundTag compound = entityTag.getCompound(TAG_BLOCK_STATE).orElse(null);
            if (compound != null) {
                return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, compound).getBlock();
            }
        }

        return ModBlocks.PURPLE_DOLL_GIFT_BOX;
    }

    public static String getCustomDollIdFromItemStack(ItemStack stack) {
        if (!stack.is(ModItems.DOLL_ENTITY_ITEM)) {
            return StringUtils.EMPTY;
        }
        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        if (entityTag != null && entityTag.contains(TAG_CUSTOM_DOLL_ID)) {
            return entityTag.getStringOr(TAG_CUSTOM_DOLL_ID, StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    public static DollEntity getDollEntity(Level level, ItemStack stack) {
        if (!stack.is(ModItems.DOLL_ENTITY_ITEM)) {
            return new DollEntity(ModEntities.DOLL, level);
        }
        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        if (entityTag == null) {
            return new DollEntity(ModEntities.DOLL, level);
        }
        DollEntity entity = new DollEntity(ModEntities.DOLL, level);
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entityTag));
        return entity;
    }

    public static boolean hasEntityData(ItemStack stack) {
        return stack.has(ModDataComponent.TAG_DOLL_ENTITY);
    }

    public static void clearEntityData(ItemStack stack) {
        stack.remove(ModDataComponent.TAG_DOLL_ENTITY);
    }

    public static void addCreativeTab(CreativeModeTab.Output output) {
        // 自定义玩偶
        ServerCustomDollLoader.getModels().forEach(id -> output.accept(createItemWithCustomDollId(id)));

        // 基础方块玩偶
        ModRegisterEvent.DOLL_BLOCKS.values().forEach(block ->
                output.accept(createItemWithBlockState(block.defaultBlockState())));
    }

    private boolean mayPlace(Player pPlayer, Direction pDirection, ItemStack pItemStack, BlockPos pPos) {
        return !pPlayer.level().isOutsideBuildHeight(pPos) && pPlayer.mayUseItemAt(pPos, pDirection, pItemStack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!GeneralConfig.DOLL_CAN_BE_THROWN.get()) {
                return InteractionResult.PASS;
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide()) {
            Block dollBlock = getBlockFromItemStack(player.getItemInHand(hand));
            SoundEvent sound = DollSounds.getDollSound(BuiltInRegistries.BLOCK.getKey(dollBlock).getPath());
            float pitch = 0.75F + level.getRandom().nextFloat() * 0.5F;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // 计算使用时间
        int useTime = this.getUseDuration(stack, entity) - timeLeft;

        // 如果使用时间达到要求，则投掷
        if (useTime >= THROW_DURATION && GeneralConfig.DOLL_CAN_BE_THROWN.get()) {
            int strength = useTime - THROW_DURATION;
            throwDollEntity(level, entity, stack, strength);
            return true;
        }
        return false;
    }

    private void throwDollEntity(Level level, LivingEntity entity, ItemStack stack, int strength) {
        if (!level.isClientSide()) {
            // 从物品获取或创建实体
            DollEntity dollEntity = getDollEntity(level, stack);
            dollEntity.setInThrowing(true);

            // 如果实体的 BlockState 为空气，设置为紫色礼盒
            if (StringUtils.isBlank(dollEntity.getCustomDollId()) && dollEntity.getDisplayBlockState().isAir()) {
                dollEntity.setDisplayBlockState(ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState());
            }

            // 设置实体的初始位置（在玩家眼部高度）
            Vec3 playerEyePos = entity.getEyePosition();
            dollEntity.setPos(playerEyePos.x, playerEyePos.y - 0.1, playerEyePos.z);

            // 获取玩家的视线方向
            Vec3 lookAngle = entity.getLookAngle();

            // 设置投掷速度
            double throwPower = Mth.clamp(strength * 0.1, 0.1, 3);
            dollEntity.setDeltaMovement(lookAngle.x * throwPower, lookAngle.y * throwPower, lookAngle.z * throwPower);

            // 设置实体的旋转角度
            dollEntity.setYRot(entity.getYRot() - 180);

            // 播放投掷音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            // 添加实体到世界
            level.addFreshEntity(dollEntity);

            if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        if (player == null || !this.mayPlace(player, clickedFace, stack, clickedPos)) {
            return InteractionResult.FAIL;
        }

        // 与 1.20 版本一致：潜行时不进行放置，返回 PASS 让投掷蓄力逻辑处理（潜行右键拉弓）
        // 不潜行时才正常放置玩偶
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        // 从物品获取或创建实体
        DollEntity dollEntity = getDollEntity(level, stack);

        // 如果实体的BlockState为空气，设置为紫色礼盒
        if (StringUtils.isBlank(dollEntity.getCustomDollId()) && dollEntity.getDisplayBlockState().isAir()) {
            dollEntity.setDisplayBlockState(ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState());
        }

        // 设置实体位置和朝向
        dollEntity.snapTo(clickedPos.relative(clickedFace), player.getYRot() - 180, dollEntity.getXRot());
        if (!dollEntity.canSurvives() || !level.addFreshEntity(dollEntity)) {
            return InteractionResult.FAIL;
        }

        dollEntity.playSound(SoundEvents.WOOL_PLACE, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, dollEntity.position());
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltips, TooltipFlag isAdvanced) {
        String modelId = getCustomDollIdFromItemStack(stack);
        if (!modelId.isBlank()) {
            ClientHooks.customDollTooltip.accept(modelId, tooltips);
        } else {
            Block dollBlock = getBlockFromItemStack(stack);
            Identifier dollId = BuiltInRegistries.BLOCK.getKey(dollBlock);
            if (BuiltInRegistries.ITEM.getValue(dollId) instanceof DollItem dollItem) {
                tooltips.accept(Component.translatable(dollItem.tooltipKey()).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltips.accept(Component.translatable("item.kaleidoscopedoll.doll_entity_item.throw.tooltip").withStyle(ChatFormatting.GRAY));
        tooltips.accept(Component.translatable("item.kaleidoscopedoll.doll_entity_item.place.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
