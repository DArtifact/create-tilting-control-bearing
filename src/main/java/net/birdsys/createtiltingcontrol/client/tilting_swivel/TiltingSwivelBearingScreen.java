package net.birdsys.createtiltingcontrol.client.tilting_swivel;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.client.AbstractTiltConfigScreen;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureTurretModePacket;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.TiltingSwivelBearingBlockEntity;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.menu.TiltingSwivelBearingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu.BG_HEIGHT;
import static net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu.BG_WIDTH;

public class TiltingSwivelBearingScreen extends AbstractTiltConfigScreen<TiltingSwivelBearingMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateTiltingControlMod.loc("textures/gui/tilting_swivel_bearing.png");

    private Boolean turretMode;
    private IconButton modeButton;

    public TiltingSwivelBearingScreen(TiltingSwivelBearingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected ResourceLocation background() {
        return BACKGROUND;
    }

    @Override
    protected String translationRoot() {
        return "create_tilting_control.gui.tilting_swivel_bearing";
    }

    @Override
    protected Config.TiltRanges ranges() {
        return Config.TILTING_SWIVEL_BEARING;
    }

    @Override
    protected void addExtraWidgets(int x, int y) {
        if (turretMode == null)
            turretMode = menu.contentHolder instanceof TiltingSwivelBearingBlockEntity be && be.isTurretMode();

        modeButton = new IconButton(x + BG_WIDTH - 168, y + BG_HEIGHT - 24,
                turretMode ? AllIcons.I_TARGET : AllIcons.I_CENTERED);
        modeButton.withCallback(() -> {
            turretMode = !turretMode;
            modeButton.setIcon(turretMode ? AllIcons.I_TARGET : AllIcons.I_CENTERED);
            updateModeTooltip();
        });
        updateModeTooltip();
        addRenderableWidget(modeButton);
    }

    private void updateModeTooltip() {
        List<Component> tooltip = modeButton.getToolTip();
        tooltip.clear();
        tooltip.add(guiText("mode.title").withStyle(ChatFormatting.GOLD));
        tooltip.add(guiText(turretMode ? "mode.turret" : "mode.fixed").withStyle(ChatFormatting.WHITE));
        tooltip.add(guiText(turretMode ? "mode.turret.desc" : "mode.fixed.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(guiText("mode.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void removed() {
        if (menu.contentHolder != null && turretMode != null)
            PacketDistributor.sendToServer(new ConfigureTurretModePacket(
                    menu.contentHolder.getBlockPos(), turretMode));
        super.removed();
    }
}
