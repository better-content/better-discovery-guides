package com.bettercontent.betterdiscoveryguides;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class SignalObservationTest {
    @Test void repeatedOrIneligibleSignalIsNotMisclassifiedAsUnmatchedOrSaved() {
        assertEquals("already_credited_or_ineligible", SignalObservation.unchangedReason(true));
        assertEquals("no_route", SignalObservation.unchangedReason(false));
    }
    @Test void clearingUnknownPendingCardsDoesNotClaimNoticeSubmission() {
        assertEquals("sync_returned_no_notice_definition", SignalObservation.deliveryReason(0));
        assertEquals("notice_and_sync_returned_pending_cleared", SignalObservation.deliveryReason(2));
    }
}
