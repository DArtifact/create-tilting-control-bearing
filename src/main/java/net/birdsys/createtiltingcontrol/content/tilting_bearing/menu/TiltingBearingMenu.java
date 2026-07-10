package net.birdsys.createtiltingcontrol.content.tilting_bearing.menu;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import mezz.jei.api.runtime.IScreenHelper;
import net.birdsys.createtiltingcontrol.client.TiltingBearingScreen;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.TiltingBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import static net.birdsys.createtiltingcontrol.client.TiltingBearingScreen.*;

public class TiltingBearingMenu extends GhostItemMenu<TiltingBearingBlockEntity> {

    public static final int GHOST_SLOTS = 8;

    public TiltingBearingMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public TiltingBearingMenu(MenuType<?> type, int id, Inventory inv, TiltingBearingBlockEntity be) {
        super(type, id, inv, be);
    }

    public static TiltingBearingMenu create(int id, Inventory inv, TiltingBearingBlockEntity be) {
        return new TiltingBearingMenu(ModMenuTypes.TILTING_BEARING.get(), id, inv, be);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected TiltingBearingBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        assert Minecraft.getInstance().level != null;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
        return be instanceof TiltingBearingBlockEntity bearing ? bearing : null;
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler handler = new ItemStackHandler(GHOST_SLOTS);
        if (contentHolder != null && contentHolder.links != null)
            for (int i = 0; i < 4; i++) {
                handler.setStackInSlot(2 * i, contentHolder.links[i].getFrequency(true).getStack().copy());
                handler.setStackInSlot(2 * i + 1, contentHolder.links[i].getFrequency(false).getStack().copy());
            }
        return handler;
    }

    public static int columnX(int order, int columns, int stride, int slot_width) {
        int clusterWidth = (columns - 1) * stride + slot_width;
        return (BG_WIDTH - clusterWidth) / 2 + order * stride;
    }

    @Override
    protected void addSlots() {
        // Player slots MUST come first: GhostItemMenu.clicked() treats indices < 36 as inventory.
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
    protected void saveData(TiltingBearingBlockEntity contentHolder) {
        if (contentHolder == null || contentHolder.links == null)
            return;
        if (player == null || player.level().isClientSide)
            return;
        for (int i = 0; i < 4; i++) {
            contentHolder.links[i].setFrequency(true, ghostInventory.getStackInSlot(2 * i));
            contentHolder.links[i].setFrequency(false, ghostInventory.getStackInSlot(2 * i + 1));
        }
        contentHolder.setChanged();
    }

    @Override
    protected boolean allowRepeats() {
        return true;
    }

}
