package walksy.crossbowoptimizer.api.config.impl;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.text.Text;
import walksy.crossbowoptimizer.api.config.Config;

public class CategoryBank {



    public static ConfigCategory general(Config config, Config defaults)
    {
        var modSettingsGroupBuilder = OptionGroup.createBuilder()
                .name(Text.literal("Mod Settings"));

        modSettingsGroupBuilder.option(Option.<Boolean>createBuilder()
                .name(Text.literal("Mod Enabled"))
                .binding(
                        defaults.enabled,
                        () -> config.enabled,
                        value -> config.enabled = value
                )
                .controller(BooleanControllerBuilder::create)
                .build()
        );

        return ConfigCategory.createBuilder()
                .name(Text.literal("General"))
                .group(modSettingsGroupBuilder.build())
                .build();
    }
}
