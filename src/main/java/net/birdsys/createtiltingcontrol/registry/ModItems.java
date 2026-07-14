package net.birdsys.createtiltingcontrol.registry;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateTiltingControlMod.MODID);

    public static final DeferredItem<Item> SOAR_V2_MUSIC_DISC = ITEMS.register("soar_v2_music_disc",
            () -> new Item(new Item.Properties().jukeboxPlayable(ModSounds.SOAR_V2_KEY).stacksTo(1).rarity(Rarity.RARE)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
