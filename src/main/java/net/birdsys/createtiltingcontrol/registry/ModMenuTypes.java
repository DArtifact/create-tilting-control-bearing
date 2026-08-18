package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.menu.BidirectionalThrottleLeverMenu;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.menu.GimbalPropellerBearingMenu;
import net.birdsys.createtiltingcontrol.content.linked_joystick.menu.LinkedJoystickMenu;
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

    public static final DeferredHolder<MenuType<?>, MenuType<GimbalPropellerBearingMenu>> GIMBAL_PROPELLER_BEARING =
            MENU_TYPES.register("gimbal_propeller_bearing",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new GimbalPropellerBearingMenu(ModMenuTypes.GIMBAL_PROPELLER_BEARING.get(), id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<TiltingSwivelBearingMenu>> TILTING_SWIVEL_BEARING =
            MENU_TYPES.register("tilting_swivel_bearing",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new TiltingSwivelBearingMenu(ModMenuTypes.TILTING_SWIVEL_BEARING.get(), id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<LinkedJoystickMenu>> LINKED_JOYSTICK =
            MENU_TYPES.register("linked_joystick",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new LinkedJoystickMenu(ModMenuTypes.LINKED_JOYSTICK.get(), id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<BidirectionalThrottleLeverMenu>> BIDIRECTIONAL_THROTTLE_LEVER =
            MENU_TYPES.register("bidirectional_throttle_lever",
                    () -> IMenuTypeExtension.create((id, inv, buf) ->
                            new BidirectionalThrottleLeverMenu(ModMenuTypes.BIDIRECTIONAL_THROTTLE_LEVER.get(), id, inv, buf)));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}