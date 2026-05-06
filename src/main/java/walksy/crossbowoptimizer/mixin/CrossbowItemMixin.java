package walksy.crossbowoptimizer.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.crossbowoptimizer.CrossbowOptimizer;
import walksy.crossbowoptimizer.ICrossbowItem;
import walksy.crossbowoptimizer.config.Config;

import java.util.List;
import java.util.Set;


@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin extends RangedWeaponItem implements ICrossbowItem {

    @Shadow
    abstract CrossbowItem.LoadingSounds getLoadingSounds(ItemStack stack);

    @Shadow
    private boolean charged;

    @Shadow
    private boolean loaded;

    @Unique
    private int arrowCount$client;

    public CrossbowItemMixin(Settings settings) {
        super(settings);
        this.arrowCount$client = -1;
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CrossbowItem;shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FFLnet/minecraft/entity/LivingEntity;)V"))
    private void onShoot(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!Config.shouldOptimize()) {
            return;
        }
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        final ItemStack itemStack = user.getStackInHand(hand);
        final int projectileSize = this.hasMultiShot(itemStack) ? 3 : 1;
        for (int i = 0; i < projectileSize; ++i) {
            minecraft.world.playSound(minecraft.player, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.5F, this.getSoundPitch(minecraft.player.getRandom(), i));
            CrossbowOptimizer.getSoundsPlayedByClient().add(SoundEvents.ITEM_CROSSBOW_SHOOT);
        }
        itemStack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
        CrossbowOptimizer.onShoot();
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V"), cancellable = true)
    private void onBeginUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!Config.shouldOptimize()) {
            return;
        }
        if (this.arrowCount$client <= 0) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
    public void tickUse(World serverWorld, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!Config.shouldOptimize()) {
            return;
        }
        final CrossbowItem.LoadingSounds loadingSounds = this.getLoadingSounds(stack);
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        final ClientWorld world = minecraft.world;
        final ClientPlayerEntity player = minecraft.player;
        final float f = (float)(stack.getMaxUseTime(user) - remainingUseTicks) / (float)CrossbowItem.getPullTime(stack, user);
        if (f < 0.2F) {
            this.charged = false;
            this.loaded = false;
        }

        if (f >= 0.2F && !this.charged) {
            this.charged = true;
            loadingSounds.start().ifPresent((sound) -> {
                final SoundEvent v = sound.value();
                world.playSound(player, user.getX(), user.getY(), user.getZ(), v, SoundCategory.PLAYERS, 0.5F, 1.0F);
                CrossbowOptimizer.getSoundsPlayedByClient().add(v);
            });
        }

        if (f >= 0.5F && !this.loaded) {
            this.loaded = true;
            loadingSounds.mid().ifPresent((sound) -> {
                final SoundEvent v = sound.value();
                world.playSound(player, user.getX(), user.getY(), user.getZ(), (SoundEvent)sound.value(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                CrossbowOptimizer.getSoundsPlayedByClient().add(v);
            });
        }

        if (f >= 1.0F && !CrossbowItem.isCharged(stack) && this.loadProjectiles(user, stack)) {
            this.arrowCount$client = CrossbowOptimizer.getArrowCount() - 1;
            loadingSounds.end().ifPresent((sound) -> {
                final SoundEvent v = sound.value();
                world.playSound(player, user.getX(), user.getY(), user.getZ(), (SoundEvent)sound.value(), user.getSoundCategory(), 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
                CrossbowOptimizer.getSoundsPlayedByClient().add(v);
            });
        }
        ci.cancel();
    }

    @Override
    public void setArrowCount$client(int count) {
        this.arrowCount$client = count;
    }

    @Unique
    private boolean hasMultiShot(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> enchantments = stack.getEnchantments().getEnchantmentEntries();
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments) {
            if (entry.getKey().matchesKey(Enchantments.MULTISHOT)) {
                return entry.getIntValue() > 0;
            }
        }

        return false;
    }

    @Unique
    private boolean loadProjectiles(final LivingEntity shooter, final ItemStack crossbow) {
        final List<ItemStack> list = load(crossbow, shooter.getProjectileType(crossbow), shooter);
        if (!list.isEmpty()) {
            crossbow.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(list));
            return true;
        } else {
            return false;
        }
    }

    @Unique
    private float getSoundPitch(final Random random, final int index) {
        return index == 0 ? 1.0F : this.getSoundPitch((index & 1) == 1, random);
    }

    @Unique
    private float getSoundPitch(final boolean flag, final Random random) {
        final float f = flag ? 0.63F : 0.43F;
        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
    }
}