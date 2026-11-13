package com.respect.community

import com.respect.community.data.repository.CommunityRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityRepositoryTest {
    @Test
    fun sanitizeSlug_cleansUrlsAndSpaces() {
        assertEquals("xqc", CommunityRepository.sanitizeSlug("  https://kick.com/xQc "))
        assertEquals("respect", CommunityRepository.sanitizeSlug("www.kick.com/Respect?tab=about"))
    }

    @Test
    fun adminCredentials_matchSpecification() {
        assertEquals("zt.99.respect@gmail.com", BuildConfig.ADMIN_EMAIL)
        assertEquals("Slayer_rt", BuildConfig.ADMIN_PASSWORD)
    }
}
