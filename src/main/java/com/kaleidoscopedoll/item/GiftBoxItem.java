package com.kaleidoscopedoll.item;

import com.kaleidoscopedoll.compat.DollInventory;
import com.kaleidoscopedoll.datagen.TagItem;
import com.kaleidoscopedoll.init.ModBlocks;
import com.kaleidoscopedoll.init.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.ArrayList;
import java.util.List;

/**
 * 礼品盲盒物品：右键打开（消耗盲盒并取出玩偶），潜行右键放置，
 * 放置后的盲盒也可以直接破坏获取玩偶（破坏时只掉落预装的玩偶，盲盒没有 loot table）。
 */
public class GiftBoxItem extends BlockItem {
    private static final String NBT_DOLL_ITEMSTACK = "DollItemStack";

    public GiftBoxItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static void setDoll(ItemStack doll, ItemStack giftBox) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        output.store(NBT_DOLL_ITEMSTACK, ItemStack.CODEC, doll);
        setBlockEntityData(giftBox, ModBlocks.DOLL_GIFT_BOX_BE, output);
    }

    /**
     * 从盲盒物品中取出里面预装的玩偶（抽奖机抽出时预装），没有则返回 EMPTY
     */
    public static ItemStack getDollFromBox(Level level, ItemStack giftBox) {
        var blockEntityData = giftBox.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return ItemStack.EMPTY;
        }
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), blockEntityData.getUnsafe());
        return input.read(NBT_DOLL_ITEMSTACK, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    /**
     * 盲盒颜色对应的抽奖等级：绿色 0 级、黄色 1 级、紫色 2 级
     */
    private int getTier() {
        if (this == ModItems.YELLOW_DOLL_GIFT_BOX) {
            return 1;
        }
        if (this == ModItems.PURPLE_DOLL_GIFT_BOX) {
            return 2;
        }
        return 0;
    }

    /**
     * 从对应等级的玩偶列表随机抽取一个玩偶
     */
    private static ItemStack drawRandomDoll(ServerLevel serverLevel, int tier, RandomSource random) {
        TagKey<Item> tag = switch (tier) {
            case 1 -> TagItem.TIER_1_DOLLS;
            case 2 -> TagItem.TIER_2_DOLLS;
            default -> TagItem.TIER_0_DOLLS;
        };
        List<Item> dolls = new ArrayList<>();
        for (Holder<Item> holder : serverLevel.registryAccess().lookupOrThrow(Registries.ITEM).getTagOrEmpty(tag)) {
            dolls.add(holder.value());
        }
        if (dolls.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(dolls.get(random.nextInt(dolls.size())));
    }

    /**
     * 打开盲盒的音效和粒子效果
     */
    private static void playOpenEffects(ServerLevel serverLevel, double x, double y, double z) {
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 30, 0.5, 0.5, 0.5, 0.2);
        serverLevel.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 0.5f, 2f);
    }

    /**
     * 手上右键：打开盲盒，取出玩偶放进背包，盲盒被消耗；
     * 潜行右键：返回 PASS，交给 useOn 正常放置盲盒
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack giftBox = player.getItemInHand(hand);
            ItemStack doll = getDollFromBox(level, giftBox);
            if (doll.isEmpty()) {
                doll = drawRandomDoll(serverLevel, this.getTier(), serverLevel.getRandom());
            }
            if (doll.isEmpty()) {
                return InteractionResult.FAIL;
            }

            // Creative block interactions restore the original stack's count, so replace it.
            player.setItemInHand(hand, giftBox.copyWithCount(giftBox.getCount() - 1));
            DollInventory.returnToInventory(player, doll);
            playOpenEffects(serverLevel, player.getX(), player.getY() + 1, player.getZ());
            return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键方块面：不潜行时同样打开盲盒，潜行时正常放置
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            return this.use(context.getLevel(), player, context.getHand());
        }
        return super.useOn(context);
    }
}
