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

@Mixin(ClientConnection.class)
public class ClientConnectionMixin
{

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void receivePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci)
    {
        if (MinecraftClient.getInstance().player == null) return;
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket && MinecraftClient.getInstance().player.getMainHandStack().isOf(Items.CROSSBOW))
        {
            //TODO - Fix items in inventory not getting updated. See ClientPlayNetworkHandler#onScreenHandlerSlotUpdate
            //A potential fix could be to instead modify the setStackInSlot, instead of overriding the whole packet itself
            //which cancels inventory updates
            ci.cancel();
        }
    }
}
