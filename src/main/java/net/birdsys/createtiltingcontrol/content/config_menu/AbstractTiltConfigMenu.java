package net.birdsys.createtiltingcontrol.content.config_menu;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.birdsys.createtiltingcontrol.content.TiltControlledBearing;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public abstract class AbstractTiltConfigMenu<T extends TiltControlledBearing> extends GhostItemMenu<T> {

    public static final int GHOST_SLOTS = 8;

    public static final int BG_WIDTH = 197;
    public static final int BG_HEIGHT = 145;

    protected AbstractTiltConfigMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    protected AbstractTiltConfigMenu(MenuType<?> type, int id, Inventory inv, T be) {
        super(type, id, inv, be);
    }

    @OnlyIn(Dist.CLIENT)
    protected static <B extends TiltControlledBearing> B readBlockEntity(RegistryFriendlyByteBuf extraData,
                                                                         Class<B> bearingClass) {
        assert Minecraft.getInstance().level != null;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
        return bearingClass.isInstance(be) ? bearingClass.cast(be) : null;
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler handler = new ItemStackHandler(GHOST_SLOTS);
        if (contentHolder != null && contentHolder.getLinks() != null)
            for (int i = 0; i < 4; i++) {
                handler.setStackInSlot(2 * i, contentHolder.getLinks()[i].getFrequency(true).getStack().copy());
                handler.setStackInSlot(2 * i + 1, contentHolder.getLinks()[i].getFrequency(false).getStack().copy());
            }
        return handler;
    }

    public static int columnX(int order, int columns, int stride, int slot_width) {
        int clusterWidth = (columns - 1) * stride + slot_width;
        return (BG_WIDTH - clusterWidth) / 2 + order * stride;
    }

    @Override
    protected void addSlots() {
        int invX = (BG_WIDTH - 162) / 2;
        int invY = BG_HEIGHT + 22;
        addPlayerSlots(invX, invY);

        int slot = 0;
        for (int column = 0; column < 4; column++) {
            int x = columnX(column, 4, 44, 18);
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, 37));
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, 55));
        }
    }

    @Override
    protected void saveData(T contentHolder) {
        if (contentHolder == null || contentHolder.getLinks() == null)
            return;
        if (player == null || player.level().isClientSide)
            return;
        for (int i = 0; i < 4; i++) {
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
