package net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.menu;

import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.TiltingSwivelBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TiltingSwivelBearingMenu extends AbstractTiltConfigMenu<TiltingSwivelBearingBlockEntity> {

    public TiltingSwivelBearingMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public TiltingSwivelBearingMenu(MenuType<?> type, int id, Inventory inv, TiltingSwivelBearingBlockEntity be) {
        super(type, id, inv, be);
    }

    public static TiltingSwivelBearingMenu create(int id, Inventory inv, TiltingSwivelBearingBlockEntity be) {
        return new TiltingSwivelBearingMenu(ModMenuTypes.TILTING_SWIVEL_BEARING.get(), id, inv, be);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected TiltingSwivelBearingBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        return readBlockEntity(extraData, TiltingSwivelBearingBlockEntity.class);
    }
}