package com.kaleidoscopedoll.network;

import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.inventory.ComputerMenu;
import com.kaleidoscopedoll.network.LegacyServerNetwork.Channel;
import com.kaleidoscopedoll.network.LegacyServerNetwork.Payload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.kaleidoscopedoll.network.DollPackPackets.channel;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void init() {
        ServerDollPackSync.register();
        LegacyServerNetwork.registerGlobalReceiver(DollTweakersPayload.TYPE, (payload, context) -> handleTweakers(payload, context.player()));
        LegacyServerNetwork.registerGlobalReceiver(ComputerDollClickPayload.TYPE, (payload, context) ->
                handleComputerClick(payload.doll(), false, context.player()));
        LegacyServerNetwork.registerGlobalReceiver(ComputerDollBatchClickPayload.TYPE, (payload, context) ->
                handleComputerClick(payload.doll(), true, context.player()));
    }

    private static void handleTweakers(DollTweakersPayload payload, ServerPlayer player) {
        if (player.level().getEntity(payload.entityId()) instanceof DollEntity doll) {
            doll.setDisplayScale(limit(payload.scale(), 0.1f, 10f));
            doll.setDisplayTranslation(limit(payload.translation(), -5f, 5f));
            doll.setXRot(Mth.clamp(payload.rotation().x, -90f, 90f));
            doll.setYRot(Mth.positiveModulo(payload.rotation().y, 360f));
            var held = payload.itemDisplay() == null ? new CompoundTag() : payload.itemDisplay();
            doll.setItemScale(vector(held.getCompound("item_scale")));
            doll.setItemTranslation(vector(held.getCompound("item_translation")));
            doll.setItemRotation(vector(held.getCompound("item_rotation")));
        }
    }

    private static Vector3f limit(Vector3f v, float min, float max) {
        return new Vector3f(Mth.clamp(v.x, min, max), Mth.clamp(v.y, min, max), Mth.clamp(v.z, min, max));
    }

    private static Vector3f vector(CompoundTag tag) {
        return new Vector3f(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"));
    }

    private static void handleComputerClick(ItemStack doll, boolean batch, ServerPlayer player) {
        player.resetLastActionTime();
        if (player.containerMenu instanceof ComputerMenu menu && !player.isSpectator() && menu.clickDollButton(doll, batch)) {
            menu.broadcastChanges();
        }
    }

    public record DollTweakersPayload(int entityId, Vector3f scale, Vector3f translation, Vector2f rotation,
                                     CompoundTag itemDisplay) implements Payload {
        public static final Channel<DollTweakersPayload> TYPE = channel("doll_tweakers", buffer ->
                new DollTweakersPayload(buffer.readVarInt(), buffer.readVector3f(), buffer.readVector3f(),
                        new Vector2f(buffer.readFloat(), buffer.readFloat()), buffer.readNbt()));

        @Override public Channel<DollTweakersPayload> type() { return TYPE; }

        @Override public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(entityId);
            buffer.writeVector3f(scale);
            buffer.writeVector3f(translation);
            buffer.writeFloat(rotation.x);
            buffer.writeFloat(rotation.y);
            buffer.writeNbt(itemDisplay);
        }
    }

    public record ComputerDollClickPayload(ItemStack doll) implements Payload {
        public static final Channel<ComputerDollClickPayload> TYPE = channel("computer_doll_click",
                buffer -> new ComputerDollClickPayload(buffer.readItem()));
        @Override public Channel<ComputerDollClickPayload> type() { return TYPE; }
        @Override public void write(FriendlyByteBuf buffer) { buffer.writeItem(doll); }
    }

    public record ComputerDollBatchClickPayload(ItemStack doll) implements Payload {
        public static final Channel<ComputerDollBatchClickPayload> TYPE = channel("computer_doll_batch_click",
                buffer -> new ComputerDollBatchClickPayload(buffer.readItem()));
        @Override public Channel<ComputerDollBatchClickPayload> type() { return TYPE; }
        @Override public void write(FriendlyByteBuf buffer) { buffer.writeItem(doll); }
    }

    public record DollTotemActivationPayload(ItemStack stack) implements Payload {
        public static final Channel<DollTotemActivationPayload> TYPE = channel("doll_totem_activation",
                buffer -> new DollTotemActivationPayload(buffer.readItem()));
        @Override public Channel<DollTotemActivationPayload> type() { return TYPE; }
        @Override public void write(FriendlyByteBuf buffer) { buffer.writeItem(stack); }
    }

    public record CustomDollReloadPayload(Set<String> modelIds) implements Payload {
        public static final Channel<CustomDollReloadPayload> TYPE = channel("custom_doll_reload", buffer ->
                new CustomDollReloadPayload(buffer.readCollection(
                        FriendlyByteBuf.limitValue(LinkedHashSet::new, 65536), FriendlyByteBuf::readUtf)));
        @Override public Channel<CustomDollReloadPayload> type() { return TYPE; }
        @Override public void write(FriendlyByteBuf buffer) { buffer.writeCollection(modelIds, FriendlyByteBuf::writeUtf); }
    }
}
