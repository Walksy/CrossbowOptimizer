package walksy.crossbowoptimizer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.sound.SoundEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public class CrossbowOptimizer implements ModInitializer {

    private static final CopyOnWriteArrayList<SoundEvent> SOUNDS = new CopyOnWriteArrayList<>();

    @Override
    public void onInitialize() {

    }

    public static CopyOnWriteArrayList<SoundEvent> getSoundsPlayedByClient() {
        return SOUNDS;
    }
}