package walksy.crossbowoptimizer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public class CrossbowOptimizer implements ModInitializer {

    private static final CopyOnWriteArrayList<SoundEvent> SOUNDS = new CopyOnWriteArrayList<>();
    private static int timeSinceLastShot = 0;

    @Override
    public void onInitialize() {
        ClientTickEvents.START_CLIENT_TICK.register(this::tick);
    }

    void tick(MinecraftClient minecraft) {
        timeSinceLastShot++;
    }

    public static void onShoot() {
        timeSinceLastShot = 0;
    }

    public static boolean shotRecently() {
        return timeSinceLastShot < 10;
    }

    public static int getArrowCount() {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            final ItemStack slot = player.getInventory().getStack(i);
            if (!slot.isEmpty() && slot.getItem() instanceof ArrowItem) {
                total += slot.getCount();
            }
        }
        return total;
    }

    public static CopyOnWriteArrayList<SoundEvent> getSoundsPlayedByClient() {
        return SOUNDS;
    }
}