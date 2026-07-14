package net.birdsys.createtiltingcontrol.client;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockRenderer;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.client.tilting_propeller.TiltingPropellerBearingRenderer;
import net.birdsys.createtiltingcontrol.client.tilting_propeller.TiltingPropellerBearingScreen;
import net.birdsys.createtiltingcontrol.client.tilting_propeller.TiltingPropellerBearingVisual;
import net.birdsys.createtiltingcontrol.client.tilting_swivel.TiltingSwivelBearingRenderer;
import net.birdsys.createtiltingcontrol.client.tilting_swivel.TiltingSwivelBearingScreen;
import net.birdsys.createtiltingcontrol.client.tilting_swivel.TiltingSwivelBearingVisual;
import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TILTING_PROPELLER_BEARING.get())
                .factory(TiltingPropellerBearingVisual::new)
                .apply();

        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TILTING_SWIVEL_BEARING.get())
                .factory(TiltingSwivelBearingVisual::new)
                .apply();

        event.enqueueWork(() -> {
            registerKineticTooltip(ModBlocks.TILTING_PROPELLER_BEARING_ITEM.get());
            registerKineticTooltip(ModBlocks.TILTING_SWIVEL_BEARING_ITEM.get());
        });
    }

    private static void registerKineticTooltip(Item item) {
        TooltipModifier.REGISTRY.register(item,
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TILTING_PROPELLER_BEARING.get(), TiltingPropellerBearingRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TILTING_SWIVEL_BEARING.get(), TiltingSwivelBearingRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TILTING_SWIVEL_BEARING_PLATE.get(),
                SwivelBearingPlateBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.TILTING_PROPELLER_BEARING.get(), TiltingPropellerBearingScreen::new);
        event.register(ModMenuTypes.TILTING_SWIVEL_BEARING.get(), TiltingSwivelBearingScreen::new);
    }
}
