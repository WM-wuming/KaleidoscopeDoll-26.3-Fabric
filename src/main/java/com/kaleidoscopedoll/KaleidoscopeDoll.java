package com.kaleidoscopedoll;

import com.kaleidoscopedoll.command.RootCommand;
import com.kaleidoscopedoll.config.GeneralConfig;
import com.kaleidoscopedoll.data.resources.ServerCustomDollReloadListener;
import com.kaleidoscopedoll.event.DollClickEntityEvent;
import com.kaleidoscopedoll.event.EnterServerEvent;
import com.kaleidoscopedoll.event.PhantomSpawnEvent;
import com.kaleidoscopedoll.init.*;
import com.kaleidoscopedoll.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class KaleidoscopeDoll implements ModInitializer {
    public static final String MOD_ID = "kaleidoscope_doll";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        String configPath = MOD_ID + "/" + MOD_ID + "-common.toml";
        try {
            Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
        } catch (IOException e) {
            LOGGER.warn("Could not prepare nested doll config {}", configPath, e);
        }
        ConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.COMMON, GeneralConfig.init(), configPath);
        ModBlocks.registerBlocks();
        ModItems.registerItems();
        ModCreativeTabs.registerTabs();
        ModSounds.registerSounds();
        DollSounds.registerReloadListener();
        ModEntities.registerEntities();
        ModRecipes.registerRecipe();
        ModDataComponent.registerDataComponent();
        ModContainer.registerContainers();

        NetworkHandler.init();

        DollClickEntityEvent.register();
        PhantomSpawnEvent.register();
        EnterServerEvent.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RootCommand.register(dispatcher));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ServerCustomDollReloadListener());
    }
}
