package net.birdsys.createtiltingcontrol.content.linked_joystick;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;

import net.birdsys.createtiltingcontrol.content.link.TransmitterLinkBehaviour;

public class JoystickLinkBehaviour extends TransmitterLinkBehaviour {

    private static final String KEY_PREFIX = "JoyLink";

    @SuppressWarnings("unchecked")
    public static final BehaviourType<JoystickLinkBehaviour>[] TYPES = new BehaviourType[] {
            new BehaviourType<JoystickLinkBehaviour>(),
            new BehaviourType<JoystickLinkBehaviour>(),
            new BehaviourType<JoystickLinkBehaviour>(),
            new BehaviourType<JoystickLinkBehaviour>(),
            new BehaviourType<JoystickLinkBehaviour>(),
            new BehaviourType<JoystickLinkBehaviour>()
    };

    public JoystickLinkBehaviour(SmartBlockEntity be, int index) {
        super(be, TYPES[index], KEY_PREFIX, index);
    }
}
