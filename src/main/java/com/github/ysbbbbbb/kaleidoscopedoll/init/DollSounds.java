package com.github.ysbbbbbb.kaleidoscopedoll.init;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩偶→音效的数据驱动映射：
 * 数据包 JSON data/kaleidoscope_doll/doll_sounds.json，格式 { "doll_83": "minecraft:entity.creeper.hurt", ... }
 * key 为玩偶方块的注册路径（doll_83），value 为音效事件 id；
 * 未映射的玩偶回退播放默认的鸭子叫音效
 */
public class DollSounds {
    private static final Map<String, Identifier> DOLL_SOUND_MAP = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_sounds");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                DOLL_SOUND_MAP.clear();
                resourceManager.getResource(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_sounds.json"))
                        .ifPresent(resource -> {
                            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                                Map<String, String> parsed = GSON.fromJson(reader, MAP_TYPE);
                                if (parsed != null) {
                                    parsed.forEach((dollPath, soundId) -> {
                                        Identifier soundIdentifier = Identifier.tryParse(soundId);
                                        if (soundIdentifier != null) {
                                            DOLL_SOUND_MAP.put(dollPath, soundIdentifier);
                                        }
                                    });
                                }
                                KaleidoscopeDoll.LOGGER.info("[DollSounds] loaded {} doll sound mappings", DOLL_SOUND_MAP.size());
                            } catch (Exception e) {
                                KaleidoscopeDoll.LOGGER.error("[DollSounds] failed to load doll_sounds.json", e);
                            }
                        });
            }
        });
    }

    /**
     * 根据玩偶方块的注册路径（如 doll_83）获取对应音效，未映射则返回默认鸭子叫音效
     */
    public static SoundEvent getDollSound(String dollPath) {
        Identifier soundId = DOLL_SOUND_MAP.get(dollPath);
        if (soundId != null && BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(soundId);
            if (soundEvent != null) {
                return soundEvent;
            }
        }
        return ModSounds.DUCK_TOY;
    }
}
