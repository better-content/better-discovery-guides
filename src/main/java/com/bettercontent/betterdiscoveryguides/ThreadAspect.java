package com.bettercontent.betterdiscoveryguides;

import java.util.Arrays;

public enum ThreadAspect {
    POWER("power", 0xFF4055),
    MOTION("motion", 0xE0B01F),
    ENDURANCE("endurance", 0x52606A),
    RENEWAL("renewal", 0x6CCAF0),
    CONTROL("control", 0x8E5BB7);

    private final String id;
    private final int color;

    ThreadAspect(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public static ThreadAspect parse(String id) {
        return Arrays.stream(values()).filter(aspect -> aspect.id.equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unknown systemic salience aspect " + id));
    }
}
