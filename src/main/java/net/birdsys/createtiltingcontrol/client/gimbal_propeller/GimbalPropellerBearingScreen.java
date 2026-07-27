package net.birdsys.createtiltingcontrol.client.gimbal_propeller;

import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.client.AbstractTiltConfigScreen;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureGyroSettingsPacket;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.GimbalPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.menu.GimbalPropellerBearingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

public class GimbalPropellerBearingScreen extends AbstractTiltConfigScreen<GimbalPropellerBearingMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateTiltingControlMod.loc("textures/gui/gimbal_propeller_bearing.png");

    private static final int CAPTION_Y2 = 106;
    private static final int FIELD_Y2 = 117;

    protected ScrollInput gyroStrengthInput;
    protected ScrollInput onTiltInput;
    private int lastCapState = -1;

    public GimbalPropellerBearingScreen(GimbalPropellerBearingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected ResourceLocation background() {
        return BACKGROUND;
    }

    @Override
    protected String translationRoot() {
        return "create_tilting_control.gui.gimbal_propeller_bearing";
    }

    @Override
    protected Config.TiltRanges ranges() {
        return Config.GIMBAL_PROPELLER_BEARING;
    }

    @Override
    protected int infoLineCount() {
        return 6;
    }

    @Override
    protected void addExtraWidgets(int x, int y) {
        GimbalPropellerBearingBlockEntity be = menu.contentHolder;
        double currentStrength = gyroStrengthInput != null ? gyroStrengthInput.getState() / 10.0
                : be != null ? be.getGyroStrength() : 0.0;
        double currentMult = onTiltInput != null ? onTiltInput.getState() / 100.0
                : be != null ? be.getOnTiltGyroMult()
                  : GimbalPropellerBearingBlockEntity.DEFAULT_ON_TILT_GYRO_MULT;

        lastCapState = capState();

        int cx_strength = columnX(0, 2, 72, FIELD_W);
        Label strengthLabel = new Label(x + cx_strength - 2, y + FIELD_Y2 + 18, Component.empty()).withShadow();
        gyroStrengthInput = new ScrollInput(x + cx_strength + 1, y + 14 + FIELD_Y2, FIELD_W, FIELD_H)
                .withRange(0, lastCapState + 1)
                .withShiftStep(10)
                .titled(strengthTitle(lastCapState))
                .format(value -> Component.literal(String.format("%.1f", value / 10.0)))
                .writingTo(strengthLabel)
                .setState((int) Math.round(currentStrength * 10));
        gyroStrengthInput.onChanged();

        int cx_mult = columnX(1, 2, 72, FIELD_W);
        Label multLabel = new Label(x + cx_mult - 2, y + FIELD_Y2 + 18, Component.empty()).withShadow();
        onTiltInput = new ScrollInput(x + cx_mult + 1, y + 14 + FIELD_Y2, FIELD_W, FIELD_H)
                .withRange(0, 101)
                .withShiftStep(10)
                .titled(guiText("on_tilt_mult"))
                .format(value -> Component.literal(value + "%"))
                .writingTo(multLabel)
                .setState((int) Math.round(currentMult * 100));
        onTiltInput.onChanged();

        addRenderableWidget(gyroStrengthInput);
        addRenderableWidget(strengthLabel);
        addRenderableWidget(onTiltInput);
        addRenderableWidget(multLabel);
    }

    private int capState() {
        double speed = menu.contentHolder != null ? menu.contentHolder.getSpeed() : 0;
        return (int) Math.floor(GimbalPropellerBearingBlockEntity.gyroStrengthCap(speed) * 10);
    }

    private MutableComponent strengthTitle(int capState) {
        MutableComponent title = guiText("gyro_strength");
        int maxState = (int) Math.floor(Config.GIMBAL_MAX_GYRO_STRENGTH.get() * 10);
        if (capState < maxState && menu.contentHolder != null) {
            title.append(Component.literal(String.format(" (%s %.1f @ %d RPM)",
                            guiText("gyro_cap").getString(),
                            capState / 10.0,
                            (int) Math.abs(menu.contentHolder.getSpeed())))
                    .withStyle(ChatFormatting.GRAY));
        }
        return title;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (gyroStrengthInput == null)
            return;
        int capState = capState();
        if (capState == lastCapState)
            return;
        lastCapState = capState;
        gyroStrengthInput.withRange(0, capState + 1);
        gyroStrengthInput.titled(strengthTitle(capState));
        gyroStrengthInput.setState(Math.min(gyroStrengthInput.getState(), capState));
        gyroStrengthInput.onChanged();
    }

    @Override
    protected void renderBg(@NonNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;
        int cx_strength = columnX(0, 2, 72, FIELD_W);
        int cx_mult = columnX(1, 2, 72, FIELD_W);

        graphics.drawString(font, guiText("gyro_strength"),
                x + cx_strength - 6, y + CAPTION_Y2 + 14, 0x555555, false);
        graphics.drawString(font, guiText("on_tilt_mult"),
                x + cx_mult - 6, y + CAPTION_Y2 + 14, 0x555555, false);
    }

    @Override
    protected void onReset() {
        if (gyroStrengthInput == null || onTiltInput == null)
            return;
        gyroStrengthInput.setState(0);
        gyroStrengthInput.onChanged();
        onTiltInput.setState((int) Math.round(GimbalPropellerBearingBlockEntity.DEFAULT_ON_TILT_GYRO_MULT * 100));
        onTiltInput.onChanged();
    }

    @Override
    public void removed() {
        if (menu.contentHolder != null && gyroStrengthInput != null && onTiltInput != null)
            PacketDistributor.sendToServer(new ConfigureGyroSettingsPacket(
                    menu.contentHolder.getBlockPos(),
                    gyroStrengthInput.getState() / 10.0,
                    onTiltInput.getState() / 100.0));
        super.removed();
    }
}
