package com.kaleidoscopedoll.event;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.block.DollBlock;
import com.kaleidoscopedoll.config.GeneralConfig;
import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.init.ModEntities;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Phantom;

import java.util.List;

public class PhantomSpawnEvent {
    private static List<DollBlock> DOLL_BLOCKS = null;

    /**
     * 幻翼是否已经掷过玩偶概率的持久化标记：
     * tickCount 不持久化，无法区分新生成和存档重载的幻翼，
     * 用附件保证每只幻翼一生只掷一次，存档重载后不会重复触发
     */
    public static final AttachmentType<Boolean> ROLLED_ATTACHMENT =
            AttachmentRegistry.createPersistent(
                    Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "phantom_doll_rolled"),
                    Codec.BOOL);

    public static void register() {
        // 注册实体加载事件
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
            if (!(entity instanceof Phantom phantom)) {
                return;
            }
            if (!GeneralConfig.ENABLE_PHANTOM_DOLL_SPAWN.get()) {
                return;
            }
            // 存档重载的幻翼已带标记，不重复掷概率
            if (phantom.hasAttached(ROLLED_ATTACHMENT)) {
                return;
            }
            phantom.setAttached(ROLLED_ATTACHMENT, true);
            if (phantom.getRandom().nextDouble() > GeneralConfig.PHANTOM_DOLL_SPAWN_CHANCE.get()) {
                return;
            }
            attachRandomDollToPhantom(phantom, serverLevel);
        });
    }

    private static void attachRandomDollToPhantom(Phantom phantom, ServerLevel level) {
        // 随机选择一个玩偶外观
        DollBlock randomBlock = getRandomDollBlock(level.getRandom());
        if (randomBlock == null) {
            return;
        }

        // 创建玩偶实体
        DollEntity doll = new DollEntity(ModEntities.DOLL, level);
        doll.setDisplayBlockState(randomBlock.defaultBlockState());
        doll.setPos(phantom.getX(), phantom.getY() + 0.35, phantom.getZ());
        doll.setYRot(phantom.getYRot());
        doll.setDropFromPhantom(true);

        // 将玩偶附加到幻翼上
        level.addFreshEntity(doll);
        doll.startRiding(phantom);
    }

    private static DollBlock getRandomDollBlock(RandomSource random) {
        if (DOLL_BLOCKS == null) {
            DOLL_BLOCKS = Lists.newArrayList(ModRegisterEvent.DOLL_BLOCKS.values());
        }
        if (DOLL_BLOCKS.isEmpty()) {
            return null;
        }
        return DOLL_BLOCKS.get(random.nextInt(DOLL_BLOCKS.size()));
    }
}
