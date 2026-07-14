package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_propeller_bearing.TiltingPropellerBearingBlock;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.TiltingSwivelBearingBlock;
import net.birdsys.createtiltingcontrol.content.tilting_swivel_bearing.plate.TiltingSwivelBearingPlateBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateTiltingControlMod.MODID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(CreateTiltingControlMod.MODID);

    public static final DeferredBlock<TiltingPropellerBearingBlock> TILTING_PROPELLER_BEARING = BLOCKS.register("tilting_propeller_bearing",
            () -> new TiltingPropellerBearingBlock(Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.COPPER)
                    .strength(2.0f, 4.0f)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> TILTING_PROPELLER_BEARING_ITEM = BLOCK_ITEMS.register("tilting_propeller_bearing",
            () -> new BlockItem(TILTING_PROPELLER_BEARING.get(), new Item.Properties()));

    public static final DeferredBlock<TiltingSwivelBearingBlock> TILTING_SWIVEL_BEARING =
            BLOCKS.register("tilting_swivel_bearing",
                    () -> new TiltingSwivelBearingBlock(Block.Properties.of()
                            .mapColor(MapColor.METAL)
                            .sound(SoundType.COPPER)
                            .strength(2.0f, 4.0f)
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> TILTING_SWIVEL_BEARING_ITEM =
            BLOCK_ITEMS.register("tilting_swivel_bearing",
                    () -> new BlockItem(TILTING_SWIVEL_BEARING.get(), new Item.Properties()));

    public static final DeferredBlock<TiltingSwivelBearingPlateBlock> TILTING_SWIVEL_BEARING_PLATE =
            BLOCKS.register("tilting_swivel_bearing_plate",
                    () -> new TiltingSwivelBearingPlateBlock(Block.Properties.of()
                            .mapColor(MapColor.METAL)
                            .sound(SoundType.COPPER)
                            .strength(2.0f, 4.0f)
                            .noOcclusion()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.addAlias(
                CreateTiltingControlMod.loc("tilting_bearing"),
                CreateTiltingControlMod.loc("tilting_propeller_bearing"));
        BLOCK_ITEMS.addAlias(
                CreateTiltingControlMod.loc("tilting_bearing"),
                CreateTiltingControlMod.loc("tilting_propeller_bearing"));

        BLOCKS.register(modEventBus);
        BLOCK_ITEMS.register(modEventBus);
    }
}
