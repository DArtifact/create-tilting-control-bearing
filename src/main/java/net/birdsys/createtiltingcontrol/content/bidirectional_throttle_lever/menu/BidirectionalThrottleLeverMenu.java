package net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.menu;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.BidirectionalThrottleLeverBlockEntity;
import net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever.ThrottleLinkBehaviour;
import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BidirectionalThrottleLeverMenu extends GhostItemMenu<BidirectionalThrottleLeverBlockEntity> {

    public static final int GHOST_SLOTS = ThrottleLinkBehaviour.LINK_COUNT * 2;

    public static final int BG_WIDTH = AbstractTiltConfigMenu.BG_WIDTH;
    public static final int BG_HEIGHT = AbstractTiltConfigMenu.BG_HEIGHT;

    public static final int COLUMN_STRIDE = 72;
    public static final int SLOT_ROW_1_Y = 37;
    public static final int SLOT_ROW_2_Y = 55;

    public BidirectionalThrottleLeverMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public BidirectionalThrottleLeverMenu(MenuType<?> type, int id, Inventory inv,
                                          BidirectionalThrottleLeverBlockEntity be) {
        super(type, id, inv, be);
    }

    public static BidirectionalThrottleLeverMenu create(int id, Inventory inv,
                                                        BidirectionalThrottleLeverBlockEntity be) {
        return new BidirectionalThrottleLeverMenu(ModMenuTypes.BIDIRECTIONAL_THROTTLE_LEVER.get(), id, inv, be);
    }

    public int bgHeight() {
        return BG_HEIGHT;
    }

    public static int columnX(int order) {
        return AbstractTiltConfigMenu.columnX(order, ThrottleLinkBehaviour.LINK_COUNT, COLUMN_STRIDE, 18);
    }

    public static int centeredX(int width) {
        return AbstractTiltConfigMenu.columnX(0, 1, 0, width);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected BidirectionalThrottleLeverBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        assert Minecraft.getInstance().level != null;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
        return be instanceof BidirectionalThrottleLeverBlockEntity lever ? lever : null;
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler handler = new ItemStackHandler(GHOST_SLOTS);
        if (contentHolder != null && contentHolder.getLinks() != null)
            for (int i = 0; i < ThrottleLinkBehaviour.LINK_COUNT; i++) {
                handler.setStackInSlot(2 * i, contentHolder.getLinks()[i].getFrequency(true).getStack().copy());
                handler.setStackInSlot(2 * i + 1, contentHolder.getLinks()[i].getFrequency(false).getStack().copy());
            }
        return handler;
    }

    @Override
    protected void addSlots() {
        int invX = (BG_WIDTH - 162) / 2;
        int invY = bgHeight() + 22;
        addPlayerSlots(invX, invY);

        int slot = 0;
        for (int column = 0; column < ThrottleLinkBehaviour.LINK_COUNT; column++) {
            int x = columnX(column);
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, SLOT_ROW_1_Y));
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, SLOT_ROW_2_Y));
        }
    }

    @Override
    protected void saveData(BidirectionalThrottleLeverBlockEntity contentHolder) {
        if (contentHolder == null || contentHolder.getLinks() == null)
            return;
        if (player == null || player.level().isClientSide)
            return;
        for (int i = 0; i < ThrottleLinkBehaviour.LINK_COUNT; i++) {
            contentHolder.getLinks()[i].setFrequency(true, ghostInventory.getStackInSlot(2 * i));
            contentHolder.getLinks()[i].setFrequency(false, ghostInventory.getStackInSlot(2 * i + 1));
        }
        contentHolder.setChanged();
    }

    @Override
    protected boolean allowRepeats() {
        return true;
    }
}
