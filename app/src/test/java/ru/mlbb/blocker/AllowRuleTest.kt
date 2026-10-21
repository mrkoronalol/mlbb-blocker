package ru.mlbb.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.AllowRule

class AllowRuleTest {

    @Test
    fun permanentRuleIsAlwaysActive() {
        val rule = AllowRule("1", "154.85.91.151/32", expiresAt = 0)
        assertTrue(rule.isActive(now = 0))
        assertTrue(rule.isActive(now = Long.MAX_VALUE / 2))
    }

    @Test
    fun temporaryRuleExpires() {
        val rule = AllowRule("1", "154.85.91.151/32", expiresAt = 10_000)
        assertTrue(rule.isActive(now = 9_999))
        assertFalse(rule.isActive(now = 10_000))
        assertFalse(rule.isActive(now = 20_000))
    }
}
