package com.kaleidoscopedoll.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.kaleidoscopedoll.datagen.TagItem;
import com.kaleidoscopedoll.init.ModBlocks;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DollMachineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String NBT_KEY_TIER = "Tier";
    private static final String NBT_KEY_AVAILABLE_DOLLS = "AvailableDolls";
    private static final Codec<List<Identifier>> DOLL_LIST_CODEC = Identifier.CODEC.listOf();
    private static final String LOTTERY = "lottery";
    private static final RawAnimation LOTTERY_ANIMATION = RawAnimation.begin().thenPlay("animation.doll_machine.lottery");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Int2ObjectMap<List<Identifier>> availableDolls = new Int2ObjectOpenHashMap<>();
    private int tier = 0;

    public DollMachineBlockEntity(BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(entityType, pos, state);
    }

    public DollMachineBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.DOLL_MACHINE_BE, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DollMachineBlockEntity>(LOTTERY, 0, state -> PlayState.STOP)
                .triggerableAnim(LOTTERY, LOTTERY_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public void playLotteryAnimation() {
        this.triggerAnim(LOTTERY, LOTTERY);
    }

    public void onTokensClicked(ItemStack itemStack, Level level) {
        if (itemStack.is(TagItem.TIER_0_MACHINE_TOKENS)) {
            tier = 0;
        } else if (itemStack.is(TagItem.TIER_1_MACHINE_TOKENS)) {
            tier = 1;
        } else if (itemStack.is(TagItem.TIER_2_MACHINE_TOKENS)) {
            tier = 2;
        }
        List<Identifier> dolls = availableDolls.get(tier);
        if (dolls == null || dolls.isEmpty()) {
            TagKey<Item> dollTag = switch (tier) {
                case 0 -> TagItem.TIER_0_DOLLS;
                case 1 -> TagItem.TIER_1_DOLLS;
                case 2 -> TagItem.TIER_2_DOLLS;
                default -> null;
            };
            if (dollTag != null) {
                availableDolls.put(tier, shuffleDolls(dollTag));
            }
        }
        itemStack.shrink(1);
        this.setChanged();
    }

    public ItemStack onFinishLottery() {
        List<Identifier> dolls = availableDolls.get(tier);
        if (dolls == null || dolls.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(dolls.removeLast());
        this.setChanged();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static List<Identifier> shuffleDolls(TagKey<Item> tagKey) {
        List<Identifier> dolls = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
            dolls.add(BuiltInRegistries.ITEM.getKey(holder.value()));
        }
        Collections.shuffle(dolls);
        return dolls;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(NBT_KEY_TIER, tier);
        for (int i = 0; i < 3; i++) {
            List<Identifier> dolls = availableDolls.get(i);
            // Saved pools are ordered from the next prize onward.
            output.store(NBT_KEY_AVAILABLE_DOLLS + i, DOLL_LIST_CODEC, dolls == null ? List.of() : dolls.reversed());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tier = input.getIntOr(NBT_KEY_TIER, 0);
        availableDolls.clear();
        for (int i = 0; i < 3; i++) {
            List<Identifier> dolls = input.read(NBT_KEY_AVAILABLE_DOLLS + i, DOLL_LIST_CODEC).orElse(List.of());
            if (!dolls.isEmpty()) {
                availableDolls.put(i, new ArrayList<>(dolls.reversed()));
            }
        }
    }

    public int getTier() {
        return tier;
    }
}
