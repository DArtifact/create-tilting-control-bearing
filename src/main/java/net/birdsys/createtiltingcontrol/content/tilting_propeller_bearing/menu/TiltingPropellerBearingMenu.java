package net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.menu;

import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TiltingPropellerBearingMenu extends AbstractTiltConfigMenu<TiltingPropellerBearingBlockEntity> {

    public TiltingPropellerBearingMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public TiltingPropellerBearingMenu(MenuType<?> type, int id, Inventory inv, TiltingPropellerBearingBlockEntity be) {
        super(type, id, inv, be);
    }

    public static TiltingPropellerBearingMenu create(int id, Inventory inv, TiltingPropellerBearingBlockEntity be) {
        return new TiltingPropellerBearingMenu(ModMenuTypes.TILTING_PROPELLER_BEARING.get(), id, inv, be);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected TiltingPropellerBearingBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        return readBlockEntity(extraData, TiltingPropellerBearingBlockEntity.class);
    }
}
