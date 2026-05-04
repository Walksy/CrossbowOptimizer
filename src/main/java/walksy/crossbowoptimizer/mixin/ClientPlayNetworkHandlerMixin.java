package walksy.crossbowoptimizer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
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
import walksy.crossbowoptimizer.config.Config;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (!Config.shouldOptimize()) {
            return;
        }
        if (this.anyDirtyCrossbowItems(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPlaySound", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!Config.shouldOptimize()) {
            return;
        }
        if (this.isSoundOverridenByClient(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    public void onEntityData(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (!Config.shouldOptimize() || minecraft.world.getEntityById(packet.id()) != minecraft.player) {
            return;
        }
        packet.trackedValues().forEach(value -> {
            if (this.hasActiveCrossbowItem()) {
                List<DataTracker.SerializedEntry<?>> filtered = packet.trackedValues().stream()
                        //0 = starting item use
                        //1 = stopping item use
                        //8 = consumption of the item, in this case shooting the crossbow
                        //TODO: 2 & 3 might be needed for offhand values...
                        .filter(e -> e != null && e.id() != 1 && e.id() != 0 && e.id() != 8)
                        .toList();
                minecraft.player.getDataTracker().writeUpdatedEntries(filtered);
                ci.cancel();
            }
        });
    }

    @Unique
    public boolean hasActiveCrossbowItem() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player.isUsingItem()) {
            ItemStack stack = minecraft.player.getActiveItem();
            return stack.getItem() instanceof CrossbowItem;
        }
        return false;
    }

    @Unique
    public boolean isSoundOverridenByClient(PlaySoundS2CPacket packet) {
        for (SoundEvent playedSound : CrossbowOptimizer.getSoundsPlayedByClient()) {
            if (packet.getSound().value() == playedSound) {
                CrossbowOptimizer.getSoundsPlayedByClient().remove(playedSound);
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean anyDirtyCrossbowItems(ScreenHandlerSlotUpdateS2CPacket packet) {
        ItemStack pItem = packet.getStack(); //incoming server stack
        ItemStack cItem = MinecraftClient.getInstance().player.playerScreenHandler.getSlot(packet.getSlot()).getStack(); //the existing item on the client in the slot being updated by the packet
        if (!(pItem.getItem() instanceof CrossbowItem) || !(cItem.getItem() instanceof CrossbowItem)) {
            return false;
        }
        ChargedProjectilesComponent pComp = pItem.get(DataComponentTypes.CHARGED_PROJECTILES);
        ChargedProjectilesComponent cComp = cItem.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (!pComp.getProjectiles().isEmpty()) {
            return true;
        }
        return pComp.getProjectiles().equals(cComp.getProjectiles());
    }
}
