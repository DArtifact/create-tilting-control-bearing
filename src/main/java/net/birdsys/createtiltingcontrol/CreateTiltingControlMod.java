package net.birdsys.createtiltingcontrol;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;

import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
import net.birdsys.createtiltingcontrol.registry.ModCreativeTabs;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.birdsys.createtiltingcontrol.registry.ModPartialModels;
import net.birdsys.createtiltingcontrol.registry.ModSounds;
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
        ModPartialModels.register();

        BlockStressValues.IMPACTS.registerProvider(block ->
                block == ModBlocks.TILTING_BEARING.get() ? Config.STRESS_IMPACT::get : null);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModMenuTypes.register(modEventBus);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
