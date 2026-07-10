package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.TiltingBearingBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateTiltingControlMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TiltingBearingBlockEntity>> TILTING_BEARING =
            BLOCK_ENTITY_TYPES.register("tilting_bearing",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TiltingBearingBlockEntity(ModBlockEntities.TILTING_BEARING.get(), pos, state),
                            ModBlocks.TILTING_BEARING.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
