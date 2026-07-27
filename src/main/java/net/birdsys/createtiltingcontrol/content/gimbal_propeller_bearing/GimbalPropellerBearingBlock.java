package net.birdsys.createtiltingcontrol.content.gimbal_propeller_bearing;

import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlock;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class GimbalPropellerBearingBlock extends TiltingPropellerBearingBlock {

    public GimbalPropellerBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TiltingPropellerBearingBlockEntity> getBlockEntityClass() {
        return TiltingPropellerBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TiltingPropellerBearingBlockEntity> getBlockEntityType() {
        return ModBlockEntities.GIMBAL_PROPELLER_BEARING.get();
    }
}
