package com.bettercontent.betterdiscoveryguides;

/** Scalar classifications only; never interpret arbitrary provider values or personal history. */
final class SignalObservation {
    private SignalObservation() {}
    static String unchangedReason(boolean matched) { return matched ? "already_credited_or_ineligible" : "no_route"; }
    static String deliveryReason(int submittedNotices) {
        return submittedNotices > 0 ? "notice_and_sync_returned_pending_cleared" : "sync_returned_no_notice_definition";
    }
}
