package com.github.ysbbbbbb.kaleidoscopedoll;

import com.github.ysbbbbbb.kaleidoscopedoll.command.RootCommand;
import com.github.ysbbbbbb.kaleidoscopedoll.config.GeneralConfig;
import com.github.ysbbbbbb.kaleidoscopedoll.data.resources.ServerCustomDollReloadListener;
import com.github.ysbbbbbb.kaleidoscopedoll.event.DollClickEntityEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.event.EnterServerEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.event.PhantomSpawnEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.init.*;
import com.github.ysbbbbbb.kaleidoscopedoll.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

public class KaleidoscopeDoll implements ModInitializer {
    public static final String MOD_ID = "kaleidoscope_doll";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.COMMON, GeneralConfig.init());
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
