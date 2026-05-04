package walksy.crossbowoptimizer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public class CrossbowOptimizer implements ModInitializer {

    private static final CopyOnWriteArrayList<SoundEvent> SOUNDS = new CopyOnWriteArrayList<>();

    @Override
    public void onInitialize() {

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