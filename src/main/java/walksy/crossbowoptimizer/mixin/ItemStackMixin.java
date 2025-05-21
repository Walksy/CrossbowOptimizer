package walksy.crossbowoptimizer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.crossbowoptimizer.api.config.Config;

@Mixin(ItemStack.class)
public class ItemStackMixin {


    @Shadow @Final private MergedComponentMap components;

    /**
     * Prevents recalling ItemStack#set
     * In CrossbowItemMixin#onUse we set the ChargedProjectilesComponent of the crossbow to default
     * which essentially tells the client whether there should be arrows in the crossbow or not
     *
     * Normally this is called through serverside packets, which implements a delay on high ms
     * Since we called ItemStack#set client side, it allows the crossbow to be pulled sooner. However, if the player
     * is pulling the crossnow and ItemStack#set is called declaring ChargedProjectilesComponent as defaukt,
     * it will create what I call 'ghost arrows' within the crossbow which tricks the client (desync) into thinking
     * there are arrows in the crossbow, stopping proper calls to fill the crossbow
     */

    @Inject(method = "set", at = @At("HEAD"))
    public <T> void setCrossbowDirty(ComponentType<? super T> type, T value, CallbackInfoReturnable<T> cir)
    {
        if (MinecraftClient.getInstance().player == null
                || MinecraftClient.getInstance().isInSingleplayer()
                || !Config.CONFIG.instance().enabled) return;

        ItemStack stack = (ItemStack)(Object) this;

        if (stack.getItem() instanceof CrossbowItem)
        {
            if (value == ChargedProjectilesComponent.DEFAULT && this.components.get(type) == ChargedProjectilesComponent.DEFAULT)
            {
                cir.cancel();
            }
        }
    }
}

