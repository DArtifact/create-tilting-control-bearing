package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlockEntity;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.TiltingSwivelBearingBlockEntity;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.plate.TiltingSwivelBearingPlateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateTiltingControlMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TiltingPropellerBearingBlockEntity>> TILTING_PROPELLER_BEARING =
            BLOCK_ENTITY_TYPES.register("tilting_propeller_bearing",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TiltingPropellerBearingBlockEntity(ModBlockEntities.TILTING_PROPELLER_BEARING.get(), pos, state),
                            ModBlocks.TILTING_PROPELLER_BEARING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TiltingSwivelBearingBlockEntity>> TILTING_SWIVEL_BEARING =
            BLOCK_ENTITY_TYPES.register("tilting_swivel_bearing",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TiltingSwivelBearingBlockEntity(ModBlockEntities.TILTING_SWIVEL_BEARING.get(), pos, state),
                            ModBlocks.TILTING_SWIVEL_BEARING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TiltingSwivelBearingPlateBlockEntity>> TILTING_SWIVEL_BEARING_PLATE =
            BLOCK_ENTITY_TYPES.register("tilting_swivel_bearing_plate",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TiltingSwivelBearingPlateBlockEntity(ModBlockEntities.TILTING_SWIVEL_BEARING_PLATE.get(), pos, state),
                            ModBlocks.TILTING_SWIVEL_BEARING_PLATE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.addAlias(
                CreateTiltingControlMod.loc("tilting_bearing"),
                CreateTiltingControlMod.loc("tilting_propeller_bearing"));

        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
