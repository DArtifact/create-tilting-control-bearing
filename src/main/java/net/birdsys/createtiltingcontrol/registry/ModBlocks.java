package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.birdsys.createtiltingcontrol.content.tilting_bearing.TiltingBearingBlock;
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

    public static final DeferredBlock<TiltingBearingBlock> TILTING_BEARING = BLOCKS.register("tilting_bearing",
            () -> new TiltingBearingBlock(Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.COPPER)
                    .strength(2.0f, 4.0f)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> TILTING_BEARING_ITEM = BLOCK_ITEMS.register("tilting_bearing",
            () -> new BlockItem(TILTING_BEARING.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ITEMS.register(modEventBus);
    }
}
