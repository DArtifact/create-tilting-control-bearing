package net.birdsys.createtiltingcontrol.content.linked_joystick.menu;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.linked_joystick.JoystickMath;
import net.birdsys.createtiltingcontrol.content.linked_joystick.LinkedJoystickBlockEntity;
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

public class LinkedJoystickMenu extends GhostItemMenu<LinkedJoystickBlockEntity> {

    public static final int GHOST_SLOTS = JoystickMath.LINK_COUNT * 2;

    public static final int BG_WIDTH = AbstractTiltConfigMenu.BG_WIDTH;
    public static final int BG_HEIGHT = 190;

    public static final int AXIS_ROW_1_Y = 37;
    public static final int AXIS_ROW_2_Y = 55;
    public static final int MOD_ROW_1_Y = 84;
    public static final int MOD_ROW_2_Y = 102;

    public LinkedJoystickMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public LinkedJoystickMenu(MenuType<?> type, int id, Inventory inv, LinkedJoystickBlockEntity be) {
        super(type, id, inv, be);
    }

    public static LinkedJoystickMenu create(int id, Inventory inv, LinkedJoystickBlockEntity be) {
        return new LinkedJoystickMenu(ModMenuTypes.LINKED_JOYSTICK.get(), id, inv, be);
    }

    public int bgHeight() {
        return BG_HEIGHT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected LinkedJoystickBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        assert Minecraft.getInstance().level != null;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
        return be instanceof LinkedJoystickBlockEntity joystick ? joystick : null;
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler handler = new ItemStackHandler(GHOST_SLOTS);
        if (contentHolder != null && contentHolder.getLinks() != null)
            for (int i = 0; i < JoystickMath.LINK_COUNT; i++) {
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
        for (int column = 0; column < 4; column++) {
            int x = AbstractTiltConfigMenu.columnX(column, 4, 44, 18);
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, AXIS_ROW_1_Y));
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, AXIS_ROW_2_Y));
        }
        for (int column = 0; column < 2; column++) {
            int x = AbstractTiltConfigMenu.columnX(column, 2, 72, 18);
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, MOD_ROW_1_Y));
            addSlot(new SlotItemHandler(ghostInventory, slot++, x + 1, MOD_ROW_2_Y));
        }
    }

    @Override
    protected void saveData(LinkedJoystickBlockEntity contentHolder) {
        if (contentHolder == null || contentHolder.getLinks() == null)
            return;
        if (player == null || player.level().isClientSide)
            return;
        for (int i = 0; i < JoystickMath.LINK_COUNT; i++) {
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
