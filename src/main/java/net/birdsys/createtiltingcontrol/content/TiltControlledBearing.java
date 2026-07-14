package net.birdsys.createtiltingcontrol.content;

import net.birdsys.createtiltingcontrol.content.tilt_link.TiltLinkBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface TiltControlledBearing {

    double DEFAULT_MAX_TILT = 15.0D;
    double DEFAULT_TILT_SPEED = 2.5D;

    TiltLinkBehaviour[] getLinks();

    double getMaxTiltAngle();

    double getTiltSpeed();

    void setTiltSettings(double maxTiltAngle, double tiltSpeed);

    BlockPos getBlockPos();

    BlockState getBlockState();

    Level getLevel();

    void setChanged();

    static Direction linkDirection(Direction facing, int index) {
        int i = 0;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == facing.getAxis())
                continue;
            if (i == index)
                return direction;
            i++;
        }
        throw new IllegalArgumentException("[TILTING CONTROL] Link index out of range: " + index);
    }
}
