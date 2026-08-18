package net.birdsys.createtiltingcontrol.content.linked_joystick;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.birdsys.createtiltingcontrol.CreateTiltingControlMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = CreateTiltingControlMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class JoystickSessions {

    private static final Map<UUID, LinkedJoystickBlockEntity> ACTIVE = new ConcurrentHashMap<>();

    private JoystickSessions() {}

    @Nullable
    public static LinkedJoystickBlockEntity get(UUID player) {
        return ACTIVE.get(player);
    }

    static void put(UUID player, LinkedJoystickBlockEntity be) {
        ACTIVE.put(player, be);
    }

    static void remove(UUID player, LinkedJoystickBlockEntity be) {
        ACTIVE.remove(player, be);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        disconnect(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        disconnect(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        disconnect(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide)
            ACTIVE.values().removeIf(be -> be.getLevel() == level || be.isRemoved());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels())
            ACTIVE.values().removeIf(be -> be.getLevel() == level || be.isRemoved());
        ACTIVE.clear();
    }

    private static void disconnect(Player player) {
        if (player.level().isClientSide)
            return;
        LinkedJoystickBlockEntity be = ACTIVE.get(player.getUUID());
        if (be != null)
            be.unlink();
    }
}
