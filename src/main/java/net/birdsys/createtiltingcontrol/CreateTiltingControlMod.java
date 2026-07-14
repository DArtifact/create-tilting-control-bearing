package net.birdsys.createtiltingcontrol;

import com.simibubi.create.api.stress.BlockStressValues;

import net.birdsys.createtiltingcontrol.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CreateTiltingControlMod.MODID)
public class CreateTiltingControlMod {
    public static final String MODID = "create_tilting_control";

    public CreateTiltingControlMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModItems.register(modEventBus);
        ModPartialModels.register();

        BlockStressValues.IMPACTS.registerProvider(block -> {
            if (block == ModBlocks.TILTING_PROPELLER_BEARING.get())
                return Config.STRESS_IMPACT::get;
            if (block == ModBlocks.TILTING_SWIVEL_BEARING.get())
                return Config.SWIVEL_STRESS_IMPACT::get;
            return null;
        });

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModMenuTypes.register(modEventBus);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
