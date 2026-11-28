package com.kaleidoscopedoll.network;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.inventory.ComputerMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class NetworkHandler {
    public static void init() {
        DollPackPackets.register();
        ServerDollPackSync.register();
        PayloadTypeRegistry.serverboundPlay().register(DollTweakersPayload.TYPE, DollTweakersPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ComputerDollClickPayload.TYPE, ComputerDollClickPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ComputerDollBatchClickPayload.TYPE, ComputerDollBatchClickPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CustomDollReloadPayload.TYPE, CustomDollReloadPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DollTotemActivationPayload.TYPE, DollTotemActivationPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DollTweakersPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleTweakers(payload, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(ComputerDollClickPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleComputerClick(payload.doll(), false, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(ComputerDollBatchClickPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleComputerClick(payload.doll(), true, player));
        });
    }

    private static void handleTweakers(DollTweakersPayload payload, ServerPlayer player) {
        if (player != null && player.level().getEntity(payload.entityId()) instanceof DollEntity dollEntity) {
            Vector3f scale = limitVector3f(new Vector3f(payload.scale()), 0.1f, 10f);
            Vector3f translation = limitVector3f(new Vector3f(payload.translation()), -5f, 5f);
            float rotX = Math.max(-90f, Math.min(90f, payload.rotation().x));
            float rotY = ((payload.rotation().y % 360) + 360) % 360;

            dollEntity.setDisplayScale(scale);
            dollEntity.setDisplayTranslation(translation);
            dollEntity.setYRot(rotY);
            dollEntity.setXRot(rotX);
            dollEntity.setItemScale(readVector3f(payload.itemDisplay().getCompound("item_scale").orElse(new CompoundTag())));
            dollEntity.setItemTranslation(readVector3f(payload.itemDisplay().getCompound("item_translation").orElse(new CompoundTag())));
            dollEntity.setItemRotation(readVector3f(payload.itemDisplay().getCompound("item_rotation").orElse(new CompoundTag())));
        }
    }

    private static void handleComputerClick(ItemStack doll, boolean batch, ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.resetLastActionTime();
        if (player.containerMenu instanceof ComputerMenu computerMenu
            && !player.isSpectator()
            && computerMenu.clickDollButton(doll, batch)) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static Vector3f limitVector3f(Vector3f vec, float min, float max) {
        vec.x = Math.max(min, Math.min(max, vec.x));
        vec.y = Math.max(min, Math.min(max, vec.y));
        vec.z = Math.max(min, Math.min(max, vec.z));
        return vec;
    }

    private static Vector3f readVector3f(CompoundTag tag) {
        return new Vector3f(tag.getFloatOr("x", 0), tag.getFloatOr("y", 0), tag.getFloatOr("z", 0));
    }

    public record DollTweakersPayload(int entityId, Vector3f scale, Vector3f translation, Vector2f rotation, CompoundTag itemDisplay) implements CustomPacketPayload {
        public static final Type<DollTweakersPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_tweakers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DollTweakersPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DollTweakersPayload decode(RegistryFriendlyByteBuf buf) {
                int entityId = buf.readVarInt();
                Vector3f scale = buf.readVector3f();
                Vector3f translation = buf.readVector3f();
                float rotX = buf.readFloat();
                float rotY = buf.readFloat();
                CompoundTag itemDisplay = buf.readNbt();
                return new DollTweakersPayload(entityId, scale, translation, new Vector2f(rotX, rotY), itemDisplay);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, DollTweakersPayload payload) {
                buf.writeVarInt(payload.entityId());
                buf.writeVector3f(payload.scale());
                buf.writeVector3f(payload.translation());
                buf.writeFloat(payload.rotation().x);
                buf.writeFloat(payload.rotation().y);
                buf.writeNbt(payload.itemDisplay());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ComputerDollClickPayload(ItemStack doll) implements CustomPacketPayload {
        public static final Type<ComputerDollClickPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "computer_doll_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ComputerDollClickPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ComputerDollClickPayload::doll,
                ComputerDollClickPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ComputerDollBatchClickPayload(ItemStack doll) implements CustomPacketPayload {
        public static final Type<ComputerDollBatchClickPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "computer_doll_batch_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ComputerDollBatchClickPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ComputerDollBatchClickPayload::doll,
                ComputerDollBatchClickPayload::new);

        @Override
        public Type<ComputerDollBatchClickPayload> type() {
            return TYPE;
        }
    }

    public record DollTotemActivationPayload(ItemStack stack) implements CustomPacketPayload {
        public static final Type<DollTotemActivationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_totem_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DollTotemActivationPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, DollTotemActivationPayload::stack,
                DollTotemActivationPayload::new);

        @Override
        public Type<DollTotemActivationPayload> type() {
            return TYPE;
        }
    }

    public record CustomDollReloadPayload(java.util.Set<String> modelIds) implements CustomPacketPayload {
        public static final Type<CustomDollReloadPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom_doll_reload"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CustomDollReloadPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CustomDollReloadPayload decode(RegistryFriendlyByteBuf buf) {
                int size = buf.readVarInt();
                java.util.Set<String> ids = new java.util.LinkedHashSet<>();
                for (int i = 0; i < size; i++) {
                    ids.add(buf.readUtf());
                }
                return new CustomDollReloadPayload(ids);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, CustomDollReloadPayload payload) {
                buf.writeVarInt(payload.modelIds().size());
                for (String id : payload.modelIds()) {
                    buf.writeUtf(id);
                }
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
