package net.birdsys.createtiltingcontrol.content.bidirectional_throttle_lever;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;

import net.birdsys.createtiltingcontrol.content.link.TransmitterLinkBehaviour;

public class ThrottleLinkBehaviour extends TransmitterLinkBehaviour {

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LINK_COUNT = 2;

    private static final String KEY_PREFIX = "ThrottleLink";

    @SuppressWarnings("unchecked")
    public static final BehaviourType<ThrottleLinkBehaviour>[] TYPES = new BehaviourType[] {
            new BehaviourType<ThrottleLinkBehaviour>(),
            new BehaviourType<ThrottleLinkBehaviour>()
    };

    public ThrottleLinkBehaviour(SmartBlockEntity be, int index) {
        super(be, TYPES[index], KEY_PREFIX, index);
    }
}
