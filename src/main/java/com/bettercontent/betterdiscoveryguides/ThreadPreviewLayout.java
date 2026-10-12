package com.bettercontent.betterdiscoveryguides;

/** Shared geometry only; no client or registry initialization is needed to verify wrapping. */
final class ThreadPreviewLayout {
    private ThreadPreviewLayout() {}
    static int columns(int panelWidth) { return Math.max(1,(panelWidth-4)/20); }
}
