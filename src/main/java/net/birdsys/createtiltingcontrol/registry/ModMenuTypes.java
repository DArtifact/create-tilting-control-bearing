package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.menu.TiltingBearingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CreateTiltingControlMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TiltingBearingMenu>> TILTING_BEARING =
            MENU_TYPES.register("tilting_bearing",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new TiltingBearingMenu(ModMenuTypes.TILTING_BEARING.get(), id, inv, buf)));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
