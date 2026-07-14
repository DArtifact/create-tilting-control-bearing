package net.birdsys.createtiltingcontrol.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.birdsys.createtiltingcontrol.Config;
import net.birdsys.createtiltingcontrol.content.TiltControlledBearing;
import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureTiltSettingsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu.BG_HEIGHT;
import static net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu.BG_WIDTH;

public abstract class AbstractTiltConfigScreen<M extends AbstractTiltConfigMenu<?>>
        extends AbstractSimiContainerScreen<M> {

    private static final int TITLE_X = 7, TITLE_Y = 5;
    private static final int LABEL_ROW_Y = 17;
    private static final int COLUMN_STRIDE = 44, SLOT_W = 18;
    private static final int CAPTION_Y = 71;
    private static final int FIELD_Y = 82, FIELD_W = 52, FIELD_H = 18;

    protected ScrollInput maxTiltInput;
    protected ScrollInput tiltSpeedInput;

    protected AbstractTiltConfigScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    protected abstract ResourceLocation background();

    protected abstract String translationRoot();

    protected abstract Config.TiltRanges ranges();

    protected int infoLineCount() {
        return 4;
    }

    protected void addExtraWidgets(int x, int y) {}

    protected MutableComponent guiText(String suffix) {
        return Component.translatable(translationRoot() + "." + suffix);
    }

    @Override
    protected void init() {
        setWindowSize(BG_WIDTH, BG_HEIGHT + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        int x = leftPos;
        int y = topPos;

        TiltControlledBearing be = menu.contentHolder;
        double currentMaxTilt = maxTiltInput != null ? maxTiltInput.getState()
                : be != null ? be.getMaxTiltAngle() : TiltControlledBearing.DEFAULT_MAX_TILT;
        double currentSpeed = tiltSpeedInput != null ? tiltSpeedInput.getState() / 10.0
                : be != null ? be.getTiltSpeed() : TiltControlledBearing.DEFAULT_TILT_SPEED;

        Config.TiltRanges ranges = ranges();
        int tiltMin = (int) Math.ceil(Math.min(ranges.minTiltAngle.get(), ranges.maxTiltAngle.get()));
        int tiltMax = (int) Math.floor(Math.max(ranges.minTiltAngle.get(), ranges.maxTiltAngle.get()));

        int cx_max_tilt = columnX(0, 2, 72, FIELD_W);

        Label maxTiltLabel = new Label(x + cx_max_tilt + 6, y + FIELD_Y + 18, Component.empty()).withShadow();
        maxTiltInput = new ScrollInput(x + cx_max_tilt + 1, y + 14 + FIELD_Y, FIELD_W, FIELD_H)
                .withRange(tiltMin, tiltMax + 1)
                .withShiftStep(5)
                .titled(guiText("max_tilt"))
                .format(value -> Component.literal(value + "°"))
                .writingTo(maxTiltLabel)
                .setState((int) Math.round(currentMaxTilt));
        maxTiltInput.onChanged();

        int cx_tilt_speed = columnX(1, 2, 72, FIELD_W);

        int speedMin = (int) Math.ceil(Math.min(ranges.minTiltSpeed.get(), ranges.maxTiltSpeed.get()) * 10);
        int speedMax = (int) Math.floor(Math.max(ranges.minTiltSpeed.get(), ranges.maxTiltSpeed.get()) * 10);
        Label tiltSpeedLabel = new Label(x + cx_tilt_speed + 6, y + FIELD_Y + 18, Component.empty()).withShadow();
        tiltSpeedInput = new ScrollInput(x + cx_tilt_speed + 1, y + 14 + FIELD_Y, FIELD_W, FIELD_H)
                .withRange(speedMin, speedMax + 1)
                .withShiftStep(10)
                .titled(guiText("tilt_speed"))
                .format(value -> Component.literal(String.format("%.1f°/t", value / 10.0)))
                .writingTo(tiltSpeedLabel)
                .setState((int) Math.round(currentSpeed * 10));
        tiltSpeedInput.onChanged();

        IconButton infoButton = new IconButton(x + BG_WIDTH - 48, y + BG_HEIGHT - 24, AllIcons.I_VIEW_SCHEDULE);
        infoButton.setToolTip(guiText("info.title").withStyle(ChatFormatting.GOLD));
        List<Component> infoLines = new ArrayList<>();
        for (int i = 1; i <= infoLineCount(); i++)
            infoLines.add(guiText("info.line" + i).withStyle(ChatFormatting.GRAY));
        infoButton.getToolTip().addAll(infoLines);

        IconButton resetButton = new IconButton(x + BG_WIDTH - 192, y + BG_HEIGHT - 24, AllIcons.I_TRASH);
        resetButton.withCallback(() -> {
            menu.clearContents();
            menu.sendClearPacket();
            maxTiltInput.setState((int) Math.round(TiltControlledBearing.DEFAULT_MAX_TILT));
            maxTiltInput.onChanged();
            tiltSpeedInput.setState((int) Math.round(TiltControlledBearing.DEFAULT_TILT_SPEED * 10));
            tiltSpeedInput.onChanged();
        });
        IconButton confirmButton = new IconButton(x + BG_WIDTH - 24, y + BG_HEIGHT - 24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> {
            assert Objects.requireNonNull(minecraft).player != null;
            minecraft.player.closeContainer();
        });

        addRenderableWidget(maxTiltInput);
        addRenderableWidget(maxTiltLabel);
        addRenderableWidget(tiltSpeedInput);
        addRenderableWidget(tiltSpeedLabel);
        addRenderableWidget(resetButton);
        addRenderableWidget(confirmButton);
        addRenderableWidget(infoButton);

        addExtraWidgets(x, y);
    }

    @Override
    protected void renderBg(@NonNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int invX = leftPos + ((BG_WIDTH - AllGuiTextures.PLAYER_INVENTORY.getWidth()) / 2) - 1;
        int invY = topPos + BG_HEIGHT + 4;
        renderPlayerInventory(graphics, invX, invY);

        int x = leftPos;
        int y = topPos;
        graphics.blit(background(), x, y, 0, 0, BG_WIDTH, BG_HEIGHT);
        graphics.drawString(font, title, x + TITLE_X, y + TITLE_Y, 0x3F3F3F, false);

        // Direction label over each frequency column (depends on the block's facing).
        TiltControlledBearing be = menu.contentHolder;
        if (be != null && be.getLevel() != null) {
            Direction facing = be.getBlockState()
                    .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            for (int i = 0; i < 4; i++) {
                Direction side = TiltControlledBearing.linkDirection(facing, i);
                Component label = Component.translatable("create_tilting_control.gui.direction." + side.getName());
                int columnx = columnX(i, 4, COLUMN_STRIDE, 18);
                int cx = leftPos + columnx + SLOT_W / 2;
                graphics.drawString(font, label, cx - font.width(label) / 2, y + 6 + LABEL_ROW_Y, 0xFFFFFF, true);
            }
        }

        int cx_max_tilt = columnX(0, 2, 72, FIELD_W);
        int cx_tilt_speed = columnX(1, 2, 72, FIELD_W);

        graphics.drawString(font, guiText("max_tilt"),
                x + cx_max_tilt + 1, y + CAPTION_Y + 14, 0x555555, false);
        graphics.drawString(font, guiText("tilt_speed"),
                x + cx_tilt_speed + 1, y + CAPTION_Y + 14, 0x555555, false);
    }

    @Override
    public void removed() {
        if (menu.contentHolder != null && maxTiltInput != null && tiltSpeedInput != null)
            PacketDistributor.sendToServer(new ConfigureTiltSettingsPacket(
                    menu.contentHolder.getBlockPos(),
                    maxTiltInput.getState(),
                    tiltSpeedInput.getState() / 10.0));
        super.removed();
    }

    public static int columnX(int order, int columns, int stride, int slot_width) {
        return AbstractTiltConfigMenu.columnX(order, columns, stride, slot_width);
    }
}
