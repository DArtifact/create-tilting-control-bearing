package net.birdsys.createtiltingcontrol.client;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.birdsys.createtiltingcontrol.registry.ModBlocks;
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
        SimpleBlockEntityVisualizer.builder(ModBlockEntities.TILTING_BEARING.get())
                .factory(TiltingBearingVisual::new)
                .apply();

        event.enqueueWork(() -> {
            Item item = ModBlocks.TILTING_BEARING_ITEM.get();
            TooltipModifier.REGISTRY.register(item,
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TILTING_BEARING.get(), TiltingBearingRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(net.birdsys.createtiltingcontrol.registry.ModMenuTypes.TILTING_BEARING.get(),
                TiltingBearingScreen::new);
    }
}
