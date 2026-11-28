package com.kaleidoscopedoll.gametest;

import com.kaleidoscopedoll.block.DollMachineBlock;
import com.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.kaleidoscopedoll.data.custom.FolderDollPack;
import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.data.custom.TransferableDollPack;
import com.kaleidoscopedoll.datagen.TagItem;
import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.init.ModBlocks;
import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.inventory.ComputerMenu;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.item.DollItem;
import com.kaleidoscopedoll.item.GiftBoxItem;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.kaleidoscopedoll.network.DollPackEncryption;
import com.kaleidoscopedoll.network.DollPackPackets;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
                    entry(zip, "assets/port_smoke/models/block/doll/fixture.json", "{}");
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

    @GameTest
    public void folderModelsAndLootLoadWithoutDeclarations(GameTestHelper test) {
        var entries = List.of(
                "pack.mcmeta", "custom_dolls/models/prize.json", "custom_dolls/models/plain.json",
                "custom_dolls/models/empty.json", "custom_dolls/loot_table/prize.json", "custom_dolls/loot_table/empty.json");
        try {
            Files.createDirectories(CustomDollConfigPacks.ROOT);
            Path folder = Files.createTempDirectory(CustomDollConfigPacks.ROOT, "folder-smoke-");
            try {
                Files.createDirectories(folder.resolve("custom_dolls/models"));
                Files.createDirectories(folder.resolve("custom_dolls/loot_table"));
                Files.writeString(folder.resolve(entries.get(0)), """
                        {"pack":{"description":"Folder smoke test","min_format":[97,1],"max_format":[97,1]}}
                        """);
                String name = folder.getFileName().toString();
                Files.writeString(folder.resolve(entries.get(1)), "{\"parent\":\"" + name + ":block/doll/plain\"}");
                Files.writeString(folder.resolve(entries.get(2)), "{\"parent\":\"minecraft:block/stone\"}");
                Files.writeString(folder.resolve(entries.get(3)), "{\"elements\":[]}");
                // Nested tables read "name" before 1.20.5 and "value" from 1.20.5 onward.
                Files.writeString(folder.resolve(entries.get(4)), """
                        {"pools":[{"rolls":1,"bonus_rolls":0,"entries":[{"type":"minecraft:item","name":"minecraft:diamond"}]},
                                  {"rolls":1,"bonus_rolls":0,"entries":[{"type":"minecraft:loot_table","name":"minecraft:blocks/dirt","value":"minecraft:blocks/dirt"}]}]}
                        """);
                Files.writeString(folder.resolve(entries.get(5)), "{\"pools\":[]}");
                byte[] archive = FolderDollPack.read(folder).archive(1024 * 1024);
                TransferableDollPack.validate(archive);
                check(Arrays.equals(archive, FolderDollPack.read(folder).archive(archive.length)), "Folder archive was not deterministic");
                try {
                    FolderDollPack.read(folder).archive(archive.length - 1);
                    throw new IllegalStateException("Folder archive exceeded the server size limit");
                } catch (IOException expected) {
                    // The limit includes the ZIP directory, not only the compressed entries.
                }
                try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
                    var paths = new java.util.HashSet<String>();
                    for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) paths.add(entry.getName());
                    check(paths.equals(Set.of("pack.mcmeta", "assets/" + name + "/models/block/doll/prize.json",
                            "assets/" + name + "/models/block/doll/plain.json", "assets/" + name + "/models/block/doll/empty.json")),
                            "Folder archive contained something other than client models: " + paths);
                }

                ServerCustomDollLoader.init();
                String base = "java:" + name + ":";
                check(ServerCustomDollLoader.getModels().containsAll(List.of(base + "prize", base + "plain", base + "empty")),
                        "Server did not register models directly");
                var pos = new BlockPos(1, 2, 1);
                test.setBlock(pos, ModBlocks.CUSTOM_DOLL);
                var absolute = test.absolutePos(pos);
                var level = test.getLevel();
                var blockEntity = (CustomDollBlockEntity) level.getBlockEntity(absolute);
                var state = level.getBlockState(absolute);
                blockEntity.setModelId(base + "prize");
                var replacement = Block.getDrops(state, level, absolute, blockEntity);
                check(replacement.size() == 2 && replacement.get(0).is(Items.DIAMOND) && replacement.get(1).is(Items.DIRT),
                        "Block did not replace the doll with the direct and referenced loot table contents");
                blockEntity.setModelId(base + "plain");
                var fallback = Block.getDrops(state, level, absolute, blockEntity);
                check(fallback.size() == 1 && fallback.get(0).is(ModItems.CUSTOM_DOLL)
                        && (base + "plain").equals(CustomDollItem.getModelId(fallback.get(0))),
                        "Block did not preserve its original doll drop without a table");
                blockEntity.setModelId(base + "empty");
                check(Block.getDrops(state, level, absolute, blockEntity).isEmpty(),
                        "An empty loot table unexpectedly dropped the original doll");

                Vec3 location = Vec3.atCenterOf(test.absolutePos(new BlockPos(3, 2, 3)));
                var doll = new DollEntity(level, location.x, location.y, location.z, 0);
                doll.setCustomDollId(base + "prize");
                doll.setHoldItem(new ItemStack(Items.APPLE));
                level.addFreshEntity(doll);
                var player = test.makeMockPlayer(GameType.SURVIVAL);
                doll.hurt(level.damageSources().playerAttack(player), 1);
                var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(location, location).inflate(1));
                check(items.size() == 3 && items.stream().map(item -> item.getItem().getItem()).collect(java.util.stream.Collectors.toSet())
                        .equals(Set.of(Items.DIAMOND, Items.DIRT, Items.APPLE)),
                        "Entity did not drop the loot results and keep its held item drop");
            } finally {
                for (String entry : entries) Files.deleteIfExists(folder.resolve(entry));
                Files.deleteIfExists(folder.resolve("custom_dolls/loot_table"));
                Files.deleteIfExists(folder.resolve("custom_dolls/models"));
                Files.deleteIfExists(folder.resolve("custom_dolls"));
                Files.deleteIfExists(folder);
                ServerCustomDollLoader.init();
            }
            test.succeed();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GameTest
    public void dollTransferAuthenticatesEncryptedArchives(GameTestHelper test) {
        try {
            byte[] archive = "doll archive".getBytes(StandardCharsets.UTF_8);
            byte[] key = DollPackEncryption.newKey();
            byte[] sealed = DollPackEncryption.seal(archive, key);
            var packs = List.of(DollPackPackets.PackInfo.of(archive));
            var encrypted = new DollPackPackets.Offer(1, packs, key);
            var plain = new DollPackPackets.Offer(1, packs, new byte[0]);
            check(encrypted.isWithinLimits() && encrypted.transferSize(0) == sealed.length, "Invalid encrypted transfer size");
            check(plain.isWithinLimits() && plain.transferSize(0) == archive.length, "Invalid unencrypted transfer size");
            check(!new DollPackPackets.Offer(1, packs, new byte[16]).isWithinLimits(), "Accepted an invalid key size");
            check(Arrays.equals(archive, DollPackEncryption.open(sealed, key)), "Could not decrypt the archive");
            check(!Arrays.equals(sealed, DollPackEncryption.seal(archive, key)), "Encryption reused a nonce");
            check(rejectsArchive(sealed, DollPackEncryption.newKey()), "A different server key decrypted the archive");
            sealed[sealed.length - 1] ^= 1;
            check(rejectsArchive(sealed, key), "Accepted tampered ciphertext");
            test.succeed();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean rejectsArchive(byte[] data, byte[] key) {
        try {
            DollPackEncryption.open(data, key);
            return false;
        } catch (IOException expected) {
            return true;
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
