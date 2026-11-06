package com.kunzisoft.keepass.security

import com.kunzisoft.keepass.timeout.ClipboardExpiry
import org.junit.Assert.*
import org.junit.Test

class ClipboardExpiryTest {
    @Test fun deniedReadRemainsPendingUntilFocusReturns() {
        var visible: String? = null
        var clears = 0
        val expiry = ClipboardExpiry({ visible }, { clears++; true })
        expiry.copied("first")
        expiry.expire("first")
        assertEquals(0, clears)
        visible = "first"
        expiry.retry()
        expiry.retry()
        assertEquals(1, clears)
    }

    @Test fun newerExternalClipIsPreservedEvenWithTheSameText() {
        var visible: String? = null
        var clears = 0
        val expiry = ClipboardExpiry({ visible }, { clears++; true })
        expiry.copied("owned-token")
        expiry.expire("owned-token")
        visible = "" // Another clip has no ownership token, regardless of its text.
        expiry.retry()
        visible = "owned-token"
        expiry.retry()
        assertEquals(0, clears)
    }

    @Test fun staleTimerCannotExpireANewerCopyOrAnUntimedCopy() {
        var visible = "second"
        var clears = 0
        val expiry = ClipboardExpiry({ visible }, { clears++; true })
        expiry.copied("first")
        expiry.copied("second")
        expiry.expire("first")
        expiry.retry()
        assertEquals(0, clears)
        expiry.expire("second")
        assertEquals(1, clears)
        visible = "untimed"
        expiry.copied(visible)
        expiry.retry()
        assertEquals(1, clears)
    }

    @Test fun failedClearCanRetryWithoutClearingAReplacement() {
        var visible = "owned"
        var calls = 0
        val expiry = ClipboardExpiry({ visible }, { calls++; false })
        expiry.copied("owned")
        expiry.expire("owned")
        visible = "new"
        expiry.retry()
        assertEquals(1, calls)
    }
}
