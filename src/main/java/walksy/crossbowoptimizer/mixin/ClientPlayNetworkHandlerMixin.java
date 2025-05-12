package walksy.crossbowoptimizer.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.crossbowoptimizer.CrossbowOptimizerMod;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin
{

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V"), cancellable = true)
    public void handleSlotUpdates(ScreenHandlerSlotUpdateS2CPacket buffer, CallbackInfo ci)
    {
        ItemStack itemStack = buffer.getStack();
        if (itemStack.getItem() instanceof CrossbowItem && CrossbowOptimizerMod.isHandHeld(itemStack))
        {
            ci.cancel();
        }
    }
}
