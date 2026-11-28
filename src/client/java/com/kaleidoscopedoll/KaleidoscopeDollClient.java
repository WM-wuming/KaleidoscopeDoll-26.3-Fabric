package com.kaleidoscopedoll;

import com.kaleidoscopedoll.client.ClientHooks;
import com.kaleidoscopedoll.client.DollTotemActivation;
import com.kaleidoscopedoll.client.custom.ClientDollPackSync;
import com.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.kaleidoscopedoll.client.gui.ComputerMenuScreen;
import com.kaleidoscopedoll.client.gui.TweaksToolScreen;
import com.kaleidoscopedoll.client.render.CustomDollItemModel;
import com.kaleidoscopedoll.client.render.CustomDollRender;
import com.kaleidoscopedoll.client.render.CustomDollSpecialRenderer;
import com.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.init.ModBlocks;
import com.kaleidoscopedoll.init.ModContainer;
import com.kaleidoscopedoll.init.ModCreativeTabs;
import com.kaleidoscopedoll.init.ModEntities;
import com.kaleidoscopedoll.item.DollItem;
import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.network.NetworkHandler;
import com.kaleidoscopedoll.render.DollBlockRender;
import com.kaleidoscopedoll.render.DollEntityItemModel;
import com.kaleidoscopedoll.render.DollEntityRender;
import com.kaleidoscopedoll.render.DollEntitySpecialRenderer;
import com.kaleidoscopedoll.render.DollMachineRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;

import java.io.IOException;
import java.util.Optional;

public class KaleidoscopeDollClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientDollPackSync.register();
        EntityRendererRegistry.register(ModEntities.DOLL, DollEntityRender::new);
        BlockEntityRendererRegistry.register(ModBlocks.CUSTOM_DOLL_BE, CustomDollRender::new);
        BlockEntityRendererRegistry.register(ModBlocks.DOLL_BE, DollBlockRender::new);
        BlockEntityRendererRegistry.register(ModBlocks.DOLL_MACHINE_BE, DollMachineRender::new);

        SpecialModelRenderers.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_entity"),
                DollEntitySpecialRenderer.Unbaked.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom_doll"),
                CustomDollSpecialRenderer.Unbaked.MAP_CODEC);

        ModelLoadingPlugin.register(plugin -> {
            DollMachineRender.registerModels(plugin);
            plugin.modifyItemModelAfterBake().register((model, context) -> {
                if (DollEntityItemModel.ID.equals(context.itemId())) {
                    return new DollEntityItemModel(model);
                }
                return CustomDollItemModel.ID.equals(context.itemId()) ? new CustomDollItemModel(model) : model;
            });
        });

        MenuScreens.register(ModContainer.COMPUTER_CONTAINER, ComputerMenuScreen::new);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new CustomDollReloadListener());

        ClientHooks.openTweaksScreen = TweaksToolScreen::openScreen;
        ClientHooks.customDollTooltip = (modelId, consumer) -> {
            String locale = Minecraft.getInstance().getLanguageManager().getSelected();
            String[] lines = CustomDollLoader.getLanguage(locale, modelId).split("\n");
            for (String line : lines) {
                consumer.accept(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
            }
        };

        // 铁砧合成的头盔（doll_on_helmet 组件）与图腾合成的玩偶图腾（item_model 指向玩偶模型）
        // 在 tooltip 中显示对应玩偶的介绍
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipDisplay, lines) -> {
            String dollPath = stack.get(ModDataComponent.DOLL_ON_HELMET);
            if (dollPath == null || dollPath.isEmpty()) {
                Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
                if (modelId != null && KaleidoscopeDoll.MOD_ID.equals(modelId.getNamespace())
                        && modelId.getPath().startsWith("doll_")) {
                    dollPath = modelId.getPath();
                }
            }
            if (dollPath == null || dollPath.isEmpty()) {
                return;
            }
            Item dollItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, dollPath));
            if (dollItem instanceof DollItem dollItemCast) {
                lines.add(Component.translatable(dollItemCast.tooltipKey()).withStyle(ChatFormatting.GRAY));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.CustomDollReloadPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> onCustomDollReload(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.DollTotemActivationPayload.TYPE, (payload, context) -> {
            ((DollTotemActivation) context.player().connection).kaleidoscopeDoll$setConsumedTotem(payload.stack());
        });
    }

    private static void onCustomDollReload(NetworkHandler.CustomDollReloadPayload payload) {
        try {
            CustomDollLoader.init();

            // 如自己不是主机，则使用服务端同步的模型 ID
            Minecraft mc = Minecraft.getInstance();
            if (!mc.isLocalServer()) {
                ServerCustomDollLoader.getModels().clear();
                ServerCustomDollLoader.getModels().addAll(payload.modelIds());
            }

            FeatureFlagSet features = Optional.ofNullable(mc.player)
                    .map(p -> p.connection)
                    .map(ClientPacketListener::enabledFeatures)
                    .orElse(FeatureFlagSet.of());

            boolean hasOperatorItemsTabPermissions =
                    mc.options.operatorItemsTab().get() ||
                    Optional.ofNullable(mc.player)
                            .map(Player::canUseGameMasterBlocks)
                            .orElse(false);

            if (mc.level != null) {
                RegistryAccess registryAccess = mc.level.registryAccess();
                CreativeModeTab.ItemDisplayParameters parameters =
                        new CreativeModeTab.ItemDisplayParameters(features, hasOperatorItemsTabPermissions, registryAccess);

                if (ModCreativeTabs.CUSTOM_DOLL_TAB_INSTANCE != null) {
                    ModCreativeTabs.CUSTOM_DOLL_TAB_INSTANCE.buildContents(parameters);
                }
                if (ModCreativeTabs.ENTITY_DOLL_TAB_INSTANCE != null) {
                    ModCreativeTabs.ENTITY_DOLL_TAB_INSTANCE.buildContents(parameters);
                }
            }
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to reload custom dolls", e);
        }
    }
}
