package walksy.crossbowoptimizer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.crossbowoptimizer.config.Config;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Shadow @Final private MergedComponentMap components;

    @Inject(method = "set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    public <T> void setComponent(ComponentType<? super T> type, T value, CallbackInfoReturnable<T> cir) {
        if (!Config.shouldOptimize()) {
            return;
        }
        final ItemStack stack = ItemStack.class.cast(this);
        if (stack.getItem() instanceof CrossbowItem) {
            if (value == ChargedProjectilesComponent.DEFAULT && this.components.get(type) == ChargedProjectilesComponent.DEFAULT) {
                cir.cancel();
            }
        }
    }
}

