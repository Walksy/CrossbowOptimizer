package walksy.crossbowoptimizer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.crossbowoptimizer.CrossbowOptimizer;
import walksy.crossbowoptimizer.ICrossbowItem;
import walksy.crossbowoptimizer.config.Config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onScreenHandlerSlotUpdate$beforeUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (!Config.shouldOptimize()) {
            return;
        }

        if (this.isCrossbowDirty(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "TAIL"))
    public void onScreenHandlerSlotUpdate$afterUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (!Config.shouldOptimize() || !(packet.getStack().getItem() instanceof ArrowItem)) {
            return;
        }

        this.getCrossbowStacks().forEach(crossbow -> {
            ((ICrossbowItem)(CrossbowItem)crossbow.getItem()).setArrowCount$client(CrossbowOptimizer.getArrowCount());
        });
    }

    @Inject(method = "onPlaySound", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!Config.shouldOptimize()) {
            return;
        }
        if (this.wasSoundOverridenByClient(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onEntityData(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        if (!Config.shouldOptimize() || minecraft.world.getEntityById(packet.id()) != minecraft.player) {
            return;
        }
        packet.trackedValues().forEach(value -> {
            if (this.hasActiveCrossbowItem()) {
                final List<DataTracker.SerializedEntry<?>> filtered = packet.trackedValues().stream()
                        .filter(e -> {
                            if (e == null) {
                                return false;
                            }
                            if (e.id() == 8 && e.value() instanceof Byte b) {
                                return (b & 1) != 0;
                            }
                            return true;
                        })
                        .toList();
                minecraft.player.getDataTracker().writeUpdatedEntries(filtered);
                ci.cancel();
            }
        });
    }

    @Unique
    private Stream<ItemStack> getCrossbowStacks() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return Stream.of(
                        player.getInventory().getMainStacks())
                .flatMap(Collection::stream)
                .filter(stack -> stack.getItem() instanceof CrossbowItem);
    }

    @Unique
    public boolean hasActiveCrossbowItem() {
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player.isUsingItem()) {
            return minecraft.player.getActiveItem().getItem() instanceof CrossbowItem;
        }
        return false;
    }

    @Unique
    public boolean wasSoundOverridenByClient(final PlaySoundS2CPacket packet) {
        for (final SoundEvent playedSound : CrossbowOptimizer.getSoundsPlayedByClient()) {
            if (packet.getSound().value() == playedSound) {
                CrossbowOptimizer.getSoundsPlayedByClient().remove(playedSound);
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean isCrossbowDirty(final ScreenHandlerSlotUpdateS2CPacket packet) {
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.currentScreen != null) {
            return false;
        }
        final ItemStack pItem = packet.getStack(); //incoming server stack
        final ItemStack cItem = minecraft.player.playerScreenHandler.getSlot(packet.getSlot()).getStack(); //the existing item on the client in the slot being updated by the packet
        if (!(pItem.getItem() instanceof CrossbowItem) || !(cItem.getItem() instanceof CrossbowItem)) {
            return false;
        }
        this.syncCrossbows(cItem, pItem);
        final ChargedProjectilesComponent pComp = pItem.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
        final ChargedProjectilesComponent cComp = cItem.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
        if (!pComp.getProjectiles().isEmpty() && CrossbowOptimizer.shotRecently()) {
            return true;
        }
        return pComp.getProjectiles().equals(cComp.getProjectiles());
    }

    @Unique
    private void syncCrossbows(final ItemStack clientItem, final ItemStack serverItem) {
        final ChargedProjectilesComponent clientChargedState = clientItem.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
        clientItem.setCount(serverItem.getCount());
        clientItem.setDamage(serverItem.getDamage());
        final ItemEnchantmentsComponent serverEnchantComponents = serverItem.getEnchantments();
        EnchantmentHelper.set(clientItem, serverEnchantComponents);

        //clientItem.applyChanges doesn't seem to work here...
        if (serverItem.contains(DataComponentTypes.CUSTOM_NAME)) {
            clientItem.set(DataComponentTypes.CUSTOM_NAME, serverItem.get(DataComponentTypes.CUSTOM_NAME));
        } else {
            clientItem.remove(DataComponentTypes.CUSTOM_NAME);
        }

        if (serverItem.contains(DataComponentTypes.LORE)) {
            clientItem.set(DataComponentTypes.LORE, serverItem.get(DataComponentTypes.LORE));
        } else {
            clientItem.remove(DataComponentTypes.LORE);
        }

        if (serverItem.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            clientItem.set(DataComponentTypes.CUSTOM_MODEL_DATA, serverItem.get(DataComponentTypes.CUSTOM_MODEL_DATA));
        } else {
            clientItem.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
        }

        if (clientChargedState.isEmpty()) {
            clientItem.remove(DataComponentTypes.CHARGED_PROJECTILES);
        } else {
            clientItem.set(DataComponentTypes.CHARGED_PROJECTILES, clientChargedState);
        }
    }
}
