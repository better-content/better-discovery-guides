package com.bettercontent.betterdiscoveryguides;

/** Connected non-spectator ticks only; the remainder survives logout and player replacement. */
final class OnboardingTimer {
    private int remaining;
    private boolean awarded;

    OnboardingTimer(int remaining) { this.remaining = Math.max(0, remaining); }
    int remaining() { return remaining; }
    boolean tick(boolean spectator) {
        if (spectator || awarded) return false;
        if (remaining > 0) remaining--;
        if (remaining != 0) return false;
        awarded = true;
        return true;
    }
}
