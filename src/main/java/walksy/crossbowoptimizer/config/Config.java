package walksy.crossbowoptimizer.config;

import main.walksy.lib.api.WalksyLibConfig;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.utils.PathUtils;
import net.minecraft.client.MinecraftClient;

public class Config implements WalksyLibConfig {

    public static boolean modEnabled = true;

    private static final Option<Boolean> modEnabledOption = BooleanOption.createBuilder("Mod Enabled", () -> modEnabled, modEnabled, newValue -> modEnabled = newValue)
            .build();

    public static boolean shouldOptimize() {
        final MinecraftClient minecraft = MinecraftClient.getInstance();
        return minecraft.player != null && !minecraft.isInSingleplayer() && Config.modEnabled;
    }

    @Override
    public LocalConfig define() {
        return LocalConfig.createBuilder("Crossbow Optimizer")
                .category(Category.createBuilder("General")
                        .group(OptionGroup.createBuilder("Global Options")
                                .addOption(modEnabledOption)
                                .build())
                        .build())
                .path(PathUtils.ofConfigDir("crossbowoptimizer"))
                .build();
    }
}
