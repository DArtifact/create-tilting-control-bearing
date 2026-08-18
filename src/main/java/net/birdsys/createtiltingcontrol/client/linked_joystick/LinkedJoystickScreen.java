package net.birdsys.createtiltingcontrol.client.linked_joystick;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureJoystickSettingsPacket;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
import net.birdsys.createtiltingcontrol.content.linked_joystick.menu.LinkedJoystickMenu;
import net.birdsys.createtiltingcontrol.registry.ModKeyMappings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import static net.birdsys.createtiltingcontrol.content.linked_joystick.menu.LinkedJoystickMenu.BG_HEIGHT;
import static net.birdsys.createtiltingcontrol.content.linked_joystick.menu.LinkedJoystickMenu.BG_WIDTH;

public class LinkedJoystickScreen extends AbstractSimiContainerScreen<LinkedJoystickMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateTiltingControlMod.loc("textures/gui/linked_joystick.png");

    private static final int TITLE_X = 7, TITLE_Y = 5;
    private static final int AXIS_LABEL_Y = 25;
    private static final int MOD_LABEL_Y = 72;
    private static final int FIELD_CAPTION_Y = 122;
    private static final int FIELD_Y = 133, FIELD_W = 52, FIELD_H = 18;

    private static final String[] AXIS_LABELS = { "\u25B2", "\u25BC", "\u25C0", "\u25B6" };

    private ScrollInput deadzoneInput;
    private ScrollInput maxSignalInput;
    private Boolean holdMode;
    private IconButton modeButton;
    private Boolean latchMode;
    private IconButton latchButton;

    public LinkedJoystickScreen(LinkedJoystickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private MutableComponent guiText(String suffix) {
        return Component.translatable("create_tilting_control.gui.linked_joystick." + suffix);
    }

    @Override
    protected void init() {
        setWindowSize(BG_WIDTH, menu.bgHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        int x = leftPos;
        int y = topPos;

        LinkedJoystickBlockEntity be = menu.contentHolder;
        int currentDeadzone = deadzoneInput != null ? deadzoneInput.getState()
                : (int) Math.round((be != null ? be.getDeadzone() : LinkedJoystickBlockEntity.DEFAULT_DEADZONE) * 100);
        int currentMaxSignal = maxSignalInput != null ? maxSignalInput.getState()
                : be != null ? be.getMaxSignal() : LinkedJoystickBlockEntity.DEFAULT_MAX_SIGNAL;
        if (holdMode == null)
            holdMode = be != null && be.isHoldMode();
        if (latchMode == null)
            latchMode = be != null && be.isLatchMode();

        int cxDeadzone = AbstractTiltConfigMenu.columnX(0, 2, 72, FIELD_W);
        Label deadzoneLabel = new Label(x + cxDeadzone - 8, y + FIELD_Y + 7, Component.empty()).withShadow();
        deadzoneInput = new ScrollInput(x + cxDeadzone - 8, y + FIELD_Y, FIELD_W, FIELD_H)
                .withRange(0, 46)
                .withShiftStep(5)
                .titled(guiText("deadzone"))
                .format(value -> Component.literal(value + "%"))
                .writingTo(deadzoneLabel)
                .setState(currentDeadzone);
        deadzoneInput.onChanged();

        int cxMaxSignal = AbstractTiltConfigMenu.columnX(1, 2, 72, FIELD_W);
        Label maxSignalLabel = new Label(x + cxMaxSignal - 2, y + FIELD_Y + 7, Component.empty()).withShadow();
        maxSignalInput = new ScrollInput(x + cxMaxSignal - 2, y + FIELD_Y, FIELD_W, FIELD_H)
                .withRange(1, 16)
                .withShiftStep(5)
                .titled(guiText("max_signal"))
                .format(value -> Component.literal(String.valueOf(value)))
                .writingTo(maxSignalLabel)
                .setState(currentMaxSignal);
        maxSignalInput.onChanged();

        modeButton = new IconButton(x + BG_WIDTH - 168, y + BG_HEIGHT - 26,
                holdMode ? AllIcons.I_TARGET : AllIcons.I_CENTERED);
        modeButton.withCallback(() -> {
            holdMode = !holdMode;
            modeButton.setIcon(holdMode ? AllIcons.I_TARGET : AllIcons.I_CENTERED);
            updateModeTooltip();
        });
        updateModeTooltip();

        latchButton = new IconButton(x + BG_WIDTH - 144, y + BG_HEIGHT - 26,
                latchMode ? AllIcons.I_CONFIG_LOCKED : AllIcons.I_CONFIG_UNLOCKED);
        latchButton.withCallback(() -> {
            latchMode = !latchMode;
            latchButton.setIcon(latchMode ? AllIcons.I_CONFIG_LOCKED : AllIcons.I_CONFIG_UNLOCKED);
            updateLatchTooltip();
        });
        updateLatchTooltip();

        IconButton infoButton = new IconButton(x + BG_WIDTH - 48, y + BG_HEIGHT - 26, AllIcons.I_VIEW_SCHEDULE);
        infoButton.setToolTip(guiText("info.title").withStyle(ChatFormatting.GOLD));
        List<Component> infoLines = new ArrayList<>();
        for (int i = 1; i <= 6; i++)
            infoLines.add(guiText("info.line" + i).withStyle(ChatFormatting.GRAY));
        infoButton.getToolTip().addAll(infoLines);

        IconButton resetButton = new IconButton(x + BG_WIDTH - 192, y + BG_HEIGHT - 26, AllIcons.I_TRASH);
        resetButton.withCallback(() -> {
            menu.clearContents();
            menu.sendClearPacket();
            deadzoneInput.setState((int) Math.round(LinkedJoystickBlockEntity.DEFAULT_DEADZONE * 100));
            deadzoneInput.onChanged();
            maxSignalInput.setState(LinkedJoystickBlockEntity.DEFAULT_MAX_SIGNAL);
            maxSignalInput.onChanged();
            holdMode = false;
            modeButton.setIcon(AllIcons.I_CENTERED);
            updateModeTooltip();
            latchMode = false;
            latchButton.setIcon(AllIcons.I_CONFIG_UNLOCKED);
            updateLatchTooltip();
        });

        IconButton confirmButton = new IconButton(x + BG_WIDTH - 24, y + BG_HEIGHT - 26, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> {
            assert Objects.requireNonNull(minecraft).player != null;
            minecraft.player.closeContainer();
        });

        addRenderableWidget(deadzoneInput);
        addRenderableWidget(deadzoneLabel);
        addRenderableWidget(maxSignalInput);
        addRenderableWidget(maxSignalLabel);
        addRenderableWidget(modeButton);
        addRenderableWidget(latchButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(confirmButton);
        addRenderableWidget(infoButton);
    }

    private void updateModeTooltip() {
        List<Component> tooltip = modeButton.getToolTip();
        tooltip.clear();
        tooltip.add(guiText("mode.title").withStyle(ChatFormatting.GOLD));
        tooltip.add(guiText(holdMode ? "mode.hold" : "mode.toggle").withStyle(ChatFormatting.WHITE));
        tooltip.add(guiText(holdMode ? "mode.hold.desc" : "mode.toggle.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(guiText("mode.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void updateLatchTooltip() {
        List<Component> tooltip = latchButton.getToolTip();
        tooltip.clear();
        tooltip.add(guiText("latch.title").withStyle(ChatFormatting.GOLD));
        tooltip.add(guiText(latchMode ? "latch.on" : "latch.off").withStyle(ChatFormatting.WHITE));
        tooltip.add(guiText(latchMode ? "latch.on.desc" : "latch.off.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(guiText("mode.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void renderBg(@NonNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int invX = leftPos + ((BG_WIDTH - AllGuiTextures.PLAYER_INVENTORY.getWidth()) / 2) - 1;
        int invY = topPos + BG_HEIGHT + 4;
        renderPlayerInventory(graphics, invX, invY);

        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, BG_WIDTH, BG_HEIGHT);
        graphics.drawString(font, title, x + TITLE_X, y + TITLE_Y, 0x3F3F3F, false);

        for (int i = 0; i < 4; i++) {
            int columnX = AbstractTiltConfigMenu.columnX(i, 4, 44, 18);
            int cx = x + columnX + 9;
            Component label = Component.literal(AXIS_LABELS[i]);
            graphics.drawString(font, label, cx - font.width(label) / 2, y + AXIS_LABEL_Y - 2, 0xFFFFFF, true);
        }

        int columnL1 = AbstractTiltConfigMenu.columnX(-1, 2, 72, 18);
        int cxl1 = x + columnL1 + 34;
        Component label_m1 =  ModKeyMappings.JOYSTICK_MODIFIER_1.getTranslatedKeyMessage();
        graphics.drawString(font, label_m1, cxl1 - font.width(label_m1) / 2, y + MOD_LABEL_Y + 26, 0xFFFFFF, true);

        int columnL2 = AbstractTiltConfigMenu.columnX(1, 2, 72, 18);
        int cxl2 = x + columnL2 + 26;
        Component label_m2 =  ModKeyMappings.JOYSTICK_MODIFIER_2.getTranslatedKeyMessage();
        graphics.drawString(font, label_m2, cxl2 - font.width(label_m2) / 2, y + MOD_LABEL_Y + 26, 0xFFFFFF, true);


        graphics.drawString(font, guiText("deadzone"),
                x + AbstractTiltConfigMenu.columnX(0, 2, 72, FIELD_W) - 10, y + FIELD_CAPTION_Y + 2, 0x555555, false);
        graphics.drawString(font, guiText("max_signal"),
                x + AbstractTiltConfigMenu.columnX(1, 2, 72, FIELD_W) - 4, y + FIELD_CAPTION_Y + 2, 0x555555, false);
    }

    @Override
    public void removed() {
        if (menu.contentHolder != null && deadzoneInput != null && maxSignalInput != null
                && holdMode != null && latchMode != null)
            PacketDistributor.sendToServer(new ConfigureJoystickSettingsPacket(
                    menu.contentHolder.getBlockPos(),
                    deadzoneInput.getState() / 100.0D,
                    maxSignalInput.getState(),
                    holdMode,
                    latchMode));
        super.removed();
    }
}