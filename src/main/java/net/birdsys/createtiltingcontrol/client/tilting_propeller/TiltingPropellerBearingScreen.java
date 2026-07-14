package net.birdsys.createtiltingcontrol.client.tilting_propeller;

import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.client.AbstractTiltConfigScreen;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.menu.TiltingPropellerBearingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TiltingPropellerBearingScreen extends AbstractTiltConfigScreen<TiltingPropellerBearingMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateTiltingControlMod.loc("textures/gui/tilting_propeller_bearing.png");

    public TiltingPropellerBearingScreen(TiltingPropellerBearingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected ResourceLocation background() {
        return BACKGROUND;
    }

    @Override
    protected String translationRoot() {
        return "create_tilting_control.gui.tilting_propeller_bearing";
    }

    @Override
    protected Config.TiltRanges ranges() {
        return Config.TILTING_PROPELLER_BEARING;
    }
}
