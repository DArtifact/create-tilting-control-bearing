package net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.menu;

import net.birdsys.createtiltingcontrol.content.config_menu.AbstractTiltConfigMenu;
import net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing.GimbalPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GimbalPropellerBearingMenu extends AbstractTiltConfigMenu<GimbalPropellerBearingBlockEntity> {

    public static final int GIMBAL_BG_HEIGHT = 180;

    public GimbalPropellerBearingMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public GimbalPropellerBearingMenu(MenuType<?> type, int id, Inventory inv, GimbalPropellerBearingBlockEntity be) {
        super(type, id, inv, be);
    }

    public static GimbalPropellerBearingMenu create(int id, Inventory inv, GimbalPropellerBearingBlockEntity be) {
        return new GimbalPropellerBearingMenu(ModMenuTypes.GIMBAL_PROPELLER_BEARING.get(), id, inv, be);
    }

    @Override
    public int bgHeight() {
        return GIMBAL_BG_HEIGHT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected GimbalPropellerBearingBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        return readBlockEntity(extraData, GimbalPropellerBearingBlockEntity.class);
    }
}
