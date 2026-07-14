package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.menu.TiltingPropellerBearingMenu;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.menu.TiltingSwivelBearingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CreateTiltingControlMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TiltingPropellerBearingMenu>> TILTING_PROPELLER_BEARING =
            MENU_TYPES.register("tilting_propeller_bearing",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new TiltingPropellerBearingMenu(ModMenuTypes.TILTING_PROPELLER_BEARING.get(), id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<TiltingSwivelBearingMenu>> TILTING_SWIVEL_BEARING =
            MENU_TYPES.register("tilting_swivel_bearing",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new TiltingSwivelBearingMenu(ModMenuTypes.TILTING_SWIVEL_BEARING.get(), id, inv, buf)));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
