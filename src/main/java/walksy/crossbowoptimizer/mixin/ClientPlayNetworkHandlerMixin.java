package walksy.crossbowoptimizer.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.crossbowoptimizer.api.config.Config;

@Mixin(ClientConnection.class)
public class ClientPlayNetworkHandlerMixin
{

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void receivePacket(ChannelHandlerContext channelHandlerContext, Packet<?> buffer, CallbackInfo ci)
    {
        //TODO, fix offhanding not updating & fix non ghost arrows, For this, we can check if the components of the itemstack data differs from the one in the player's inventory, and apply them if they're different
        if (MinecraftClient.getInstance().player == null
                || MinecraftClient.getInstance().isInSingleplayer()
                || !Config.CONFIG.instance().enabled) return;
        if (buffer instanceof ScreenHandlerSlotUpdateS2CPacket b && (MinecraftClient.getInstance().player.getMainHandStack().isOf(Items.CROSSBOW) || MinecraftClient.getInstance().player.getOffHandStack().isOf(Items.CROSSBOW)))
        {
            if (b.getStack().isOf(Items.CROSSBOW)) {
                ci.cancel();
            }
        }
    }
}
