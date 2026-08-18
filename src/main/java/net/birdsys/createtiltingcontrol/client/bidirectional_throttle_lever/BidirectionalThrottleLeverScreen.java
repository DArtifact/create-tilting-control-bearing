package net.birdsys.createtiltingcontrol.client.bidirectional_throttle_lever;

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
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.ThrottleLinkBehaviour;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.menu.BidirectionalThrottleLeverMenu;
import net.birdsys.createtiltingcontrol.content.config_menu.ConfigureThrottleLeverPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import static net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.menu.BidirectionalThrottleLeverMenu.BG_WIDTH;

public class BidirectionalThrottleLeverScreen
        extends AbstractSimiContainerScreen<BidirectionalThrottleLeverMenu> {

    public static final ResourceLocation BACKGROUND =
            CreateTiltingControlMod.loc("textures/gui/bidirectional_throttle_lever.png");

    private static final int TITLE_X = 7, TITLE_Y = 5;
    private static final int LABEL_ROW_Y = 17;
    private static final int SLOT_W = 18;
    private static final int CAPTION_Y = 71;
    private static final int FIELD_Y = 82, FIELD_W = 52, FIELD_H = 18;

    private static final Component FORWARD_INITIAL = Component.literal("D.");
    private static final Component BACKWARD_INITIAL = Component.literal("R.");
    private static final int DIAMOND_CYAN = 0x55FFFF;
    private static final int REDSTONE_RED = 0xFF5555;

    private static final int LABEL_ENABLED = 0xFFFFFF;
    private static final int LABEL_DISABLED = 0x808080;
    private static final int CAPTION_ENABLED = 0x555555;
    private static final int CAPTION_DISABLED = 0x999999;

    private Boolean autoReturn;
    private Integer returnTicks;

    private IconButton modeButton;
    private ScrollInput returnSpeedInput;
    private Label returnSpeedLabel;

    public BidirectionalThrottleLeverScreen(BidirectionalThrottleLeverMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private MutableComponent guiText(String suffix) {
        return Component.translatable("create_tilting_control.gui.bidirectional_throttle_lever." + suffix);
    }

    private static int returnSpeedX() {
        return BidirectionalThrottleLeverMenu.centeredX(FIELD_W);
    }

    @Override
    protected void init() {
        setWindowSize(BG_WIDTH, menu.bgHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        int x = leftPos;
        int y = topPos;
        int bgHeight = menu.bgHeight();

        BidirectionalThrottleLeverBlockEntity be = menu.contentHolder;
        if (autoReturn == null)
            autoReturn = be != null ? be.isAutoReturn()
                    : BidirectionalThrottleLeverBlockEntity.DEFAULT_AUTO_RETURN;
        if (returnTicks == null)
            returnTicks = be != null ? be.getReturnTicksPerLevel()
                    : BidirectionalThrottleLeverBlockEntity.DEFAULT_RETURN_TICKS_PER_LEVEL;

        int cxSpeed = returnSpeedX();
        returnSpeedLabel = new Label(x + cxSpeed - 14, y + FIELD_Y + 15, Component.empty()).withShadow();
        returnSpeedInput = new ScrollInput(x + cxSpeed - 16, y + 10 + FIELD_Y, FIELD_W, FIELD_H)
                .withRange(BidirectionalThrottleLeverBlockEntity.MIN_RETURN_TICKS_PER_LEVEL,
                        BidirectionalThrottleLeverBlockEntity.MAX_RETURN_TICKS_PER_LEVEL + 1)
                .withShiftStep(5)
                .titled(guiText("return_speed"))
                .format(value -> Component.literal(String.format("%dt/lvl", value)))
                .writingTo(returnSpeedLabel)
                .setState(returnTicks)
                .calling(value -> returnTicks = value);
        returnSpeedInput.onChanged();

        IconButton resetButton = new IconButton(x + BG_WIDTH - 192, y + bgHeight - 24, AllIcons.I_TRASH);
        resetButton.withCallback(() -> {
            menu.clearContents();
            menu.sendClearPacket();
            autoReturn = BidirectionalThrottleLeverBlockEntity.DEFAULT_AUTO_RETURN;
            modeButton.setIcon(modeIcon());
            updateModeTooltip();
            returnTicks = BidirectionalThrottleLeverBlockEntity.DEFAULT_RETURN_TICKS_PER_LEVEL;
            returnSpeedInput.setState(returnTicks);
            returnSpeedInput.onChanged();
            updateReturnSpeedEnabled();
        });

        modeButton = new IconButton(x + BG_WIDTH - 168, y + bgHeight - 24, modeIcon());
        modeButton.withCallback(() -> {
            autoReturn = !autoReturn;
            modeButton.setIcon(modeIcon());
            updateModeTooltip();
            updateReturnSpeedEnabled();
        });
        updateModeTooltip();

        IconButton infoButton = new IconButton(x + BG_WIDTH - 48, y + bgHeight - 24, AllIcons.I_VIEW_SCHEDULE);
        infoButton.setToolTip(guiText("info.title").withStyle(ChatFormatting.GOLD));
        List<Component> infoLines = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            infoLines.add(guiText("info.line" + i).withStyle(ChatFormatting.GRAY));
        infoButton.getToolTip().addAll(infoLines);

        IconButton confirmButton = new IconButton(x + BG_WIDTH - 24, y + bgHeight - 24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> {
            assert Objects.requireNonNull(minecraft).player != null;
            minecraft.player.closeContainer();
        });

        addRenderableWidget(returnSpeedInput);
        addRenderableWidget(returnSpeedLabel);
        addRenderableWidget(resetButton);
        addRenderableWidget(modeButton);
        addRenderableWidget(confirmButton);
        addRenderableWidget(infoButton);

        updateReturnSpeedEnabled();
    }

    private AllIcons modeIcon() {
        return autoReturn != null && autoReturn ? AllIcons.I_CENTERED : AllIcons.I_TARGET;
    }

    private void updateModeTooltip() {
        List<Component> tooltip = modeButton.getToolTip();
        tooltip.clear();
        boolean on = autoReturn != null && autoReturn;
        tooltip.add(guiText("mode.title").withStyle(ChatFormatting.GOLD));
        tooltip.add(guiText(on ? "mode.auto_return" : "mode.hold").withStyle(ChatFormatting.WHITE));
        tooltip.add(guiText(on ? "mode.auto_return.desc" : "mode.hold.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(guiText("mode.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void updateReturnSpeedEnabled() {
        boolean enabled = autoReturn != null && autoReturn;
        returnSpeedInput.setActive(enabled);
        returnSpeedLabel.colored(enabled ? LABEL_ENABLED : LABEL_DISABLED);
    }

    @Override
    protected void renderBg(@NonNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int bgHeight = menu.bgHeight();
        int invX = leftPos + ((BG_WIDTH - AllGuiTextures.PLAYER_INVENTORY.getWidth()) / 2) - 1;
        int invY = topPos + bgHeight + 4;
        renderPlayerInventory(graphics, invX, invY);

        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, BG_WIDTH, bgHeight);
        graphics.drawString(font, title, x + TITLE_X, y + TITLE_Y, 0x3F3F3F, false);

        for (int i = 0; i < ThrottleLinkBehaviour.LINK_COUNT; i++) {
            boolean forward = i == ThrottleLinkBehaviour.FORWARD;
            Component label = forward ? FORWARD_INITIAL : BACKWARD_INITIAL;
            int cx = x + BidirectionalThrottleLeverMenu.columnX(i) + SLOT_W / 2;
            graphics.drawString(font, label, cx - font.width(label) / 2, y + 6 + LABEL_ROW_Y,
                    0xFFFFFF, true);
        }

        boolean enabled = autoReturn != null && autoReturn;
        graphics.drawString(font, guiText("return_speed"),
                x + returnSpeedX() - 14, y + CAPTION_Y + 10,
                enabled ? CAPTION_ENABLED : CAPTION_DISABLED, false);
    }

    @Override
    public void removed() {
        if (menu.contentHolder != null && autoReturn != null && returnTicks != null)
            PacketDistributor.sendToServer(new ConfigureThrottleLeverPacket(
                    menu.contentHolder.getBlockPos(), autoReturn, returnTicks));
        super.removed();
    }
}
