package walksy.crossbowoptimizer.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.crossbowoptimizer.CrossbowOptimizerMod;

@Mixin(ClientConnection.class)
public class ClientPlayNetworkHandlerMixin
{

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE),
            cancellable = true
    )
    public void handleSlotUpdates(ChannelHandlerContext channelHandlerContext, Packet<?> buffer, CallbackInfo ci)
    {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().isInSingleplayer()) return;
        if (buffer instanceof ScreenHandlerSlotUpdateS2CPacket b && (MinecraftClient.getInstance().player.getMainHandStack().isOf(Items.CROSSBOW) || MinecraftClient.getInstance().player.getOffHandStack().isOf(Items.CROSSBOW)))
        {
            if (b.getStack().isOf(Items.CROSSBOW))
            {
                ci.cancel();
            }
        }

    }
}
