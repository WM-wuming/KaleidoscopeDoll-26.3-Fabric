package com.github.ysbbbbbb.kaleidoscopedoll.gametest;

import com.github.ysbbbbbb.kaleidoscopedoll.block.DollMachineBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.TransferableDollPack;
import com.github.ysbbbbbb.kaleidoscopedoll.datagen.TagItem;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopedoll.inventory.ComputerMenu;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.GiftBoxItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class DollPortTests {
    private static final String CUSTOM_ID = "java:port_smoke:fixture";

    @GameTest
    public void computerChargesOnlyDeliveredDolls(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var inventory = player.getInventory();
        var menu = new ComputerMenu(1, inventory);
        var input = menu.getSlot(0);
        var output = menu.getSlot(1);
        Item wool = firstItem(TagItem.COMPUTER_TOKENS);
        ItemStack doll = firstItem(TagItem.PLAYER_DOLLS).getDefaultInstance();

        check(!menu.clickDollButton(doll, true), "Computer redeemed a doll without wool");
        input.set(new ItemStack(wool, 6));
        check(!menu.clickDollButton(new ItemStack(Items.DIRT), true), "Computer accepted an unrelated item");
        check(menu.clickDollButton(doll, false) && output.getItem().is(doll.getItem())
                && input.getItem().getCount() == 6, "Preview charged wool or failed to show the doll");
        check(menu.clickDollButton(doll, true), "Batch redemption failed");
        check(inventory.getItem(0).is(doll.getItem()) && inventory.getItem(0).getCount() == 6,
                "Batch did not deliver six dolls");
        check(!input.hasItem() && !output.hasItem(), "Paid batch left wool or a free preview");
        test.succeed();
    }

    @GameTest(maxTicks = 90)
    public void lotteryProducesOneFilledGiftBox(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var level = test.getLevel();
        var relative = new BlockPos(3, 2, 3);
        var lower = test.absolutePos(relative);
        var upper = lower.above();
        test.setBlock(relative.below(), Blocks.STONE);
        test.setBlock(relative, ModBlocks.DOLL_MACHINE);
        ModBlocks.DOLL_MACHINE.setPlacedBy(level, lower, level.getBlockState(lower), player, ItemStack.EMPTY);
        check(level.getBlockState(upper).is(ModBlocks.DOLL_MACHINE), "Machine did not place its upper half");

        var token = new ItemStack(firstItem(TagItem.TIER_0_MACHINE_TOKENS), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, token);
        var hit = new BlockHitResult(Vec3.atCenterOf(upper), Direction.NORTH, upper, false);
        level.getBlockState(upper).useItemOn(token, level, player, InteractionHand.MAIN_HAND, hit);
        check(token.getCount() == 1 && DollMachineBlock.isRunning(level.getBlockState(lower))
                && DollMachineBlock.isRunning(level.getBlockState(upper)),
                "Starting from the upper half did not consume one token and start both halves");
        level.getBlockState(upper).useItemOn(token, level, player, InteractionHand.MAIN_HAND, hit);
        check(token.getCount() == 1, "Running lottery consumed another token");

        test.runAfterDelay(65, () -> {
            check(!DollMachineBlock.isRunning(level.getBlockState(lower))
                    && !DollMachineBlock.isRunning(level.getBlockState(upper)), "Lottery failed to reset");
            var boxes = level.getEntitiesOfClass(ItemEntity.class, new AABB(lower).inflate(2),
                    entity -> entity.getItem().is(ModItems.GREEN_DOLL_GIFT_BOX));
            check(boxes.size() == 1, "Lottery did not spawn exactly one tier-zero gift box");
            check(GiftBoxItem.getDollFromBox(level, boxes.get(0).getItem()).is(TagItem.TIER_0_DOLLS),
                    "Lottery gift box lost its prize or contains the wrong tier");
            test.succeed();
        });
    }

    @GameTest
    public void dollItemRetainsVanillaAndCustomData(GameTestHelper test) {
        Item item = firstItem(TagItem.PLAYER_DOLLS);
        check(item instanceof DollItem, "Computer doll tag does not contain a block doll");
        var block = ((DollItem) item).getBlock();
        var stack = DollEntityItem.createItemWithBlockState(block.defaultBlockState());
        check(DollEntityItem.getBlockFromItemStack(stack.copy()) == block, "Doll item lost its block state");
        var custom = DollEntityItem.createItemWithCustomDollId(CUSTOM_ID);
        check(CUSTOM_ID.equals(DollEntityItem.getCustomDollIdFromItemStack(custom.copy())), "Doll item lost its custom id");

        var totem = DollFunctionRecipe.assembleResult(item.getDefaultInstance(), new ItemStack(Items.TOTEM_OF_UNDYING));
        check(totem.is(Items.TOTEM_OF_UNDYING) && DollFunctionRecipe.isDollTotem(totem.copy()), "Totem recipe lost its item or doll model");
        test.succeed();
    }

    @GameTest
    public void javaPackCanBeRegisteredAndRedeemed(GameTestHelper test) {
        try {
            Files.createDirectories(CustomDollConfigPacks.ROOT);
            var file = Files.createTempFile(CustomDollConfigPacks.ROOT, "port-smoke-", ".zip");
            try {
                try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
                    entry(zip, "pack.mcmeta", """
                            {"pack":{"description":"Port smoke test","pack_format":15}}
                            """);
                    entry(zip, "custom_dolls.json", "[\"" + CUSTOM_ID + "\"]");
                    entry(zip, "assets/port_smoke/items/fixture.json",
                            """
                            {"model":{"type":"minecraft:model","model":"port_smoke:block/fixture"}}
                            """);
                    entry(zip, "assets/port_smoke/models/block/fixture.json", "{}");
                }
                try (var zip = new ZipFile(file.toFile())) {
                    TransferableDollPack.validate(zip);
                }
                ServerCustomDollLoader.init();
                check(ServerCustomDollLoader.getModels().contains(CUSTOM_ID), "Server did not register the custom model");
                var inventory = test.makeMockPlayer(GameType.SURVIVAL).getInventory();
                var menu = new ComputerMenu(1, inventory);
                var doll = ModItems.CUSTOM_DOLL.getDefaultInstance();
                menu.getSlot(0).set(new ItemStack(firstItem(TagItem.COMPUTER_TOKENS), 2));
                CustomDollItem.setModelId(doll, "java:port_smoke:unregistered");
                check(!menu.clickDollButton(doll, true), "Computer accepted an unregistered model");
                CustomDollItem.setModelId(doll, CUSTOM_ID);
                check(menu.clickDollButton(doll, true) && inventory.getItem(0).getCount() == 2
                        && CUSTOM_ID.equals(CustomDollItem.getModelId(inventory.getItem(0))),
                        "Computer did not deliver two copies preserving the registered custom id");
            } finally {
                Files.deleteIfExists(file);
                ServerCustomDollLoader.init();
            }
            test.succeed();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Item firstItem(TagKey<Item> tag) {
        var values = BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator();
        check(values.hasNext(), "Required item tag is empty: " + tag);
        return values.next().value();
    }

    private static void entry(ZipOutputStream zip, String path, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
