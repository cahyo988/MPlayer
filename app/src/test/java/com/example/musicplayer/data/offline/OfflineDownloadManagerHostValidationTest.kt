package com.example.musicplayer.data.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadManagerHostValidationTest {

    @Test
    fun allowsOfficialDriveHosts() {
        assertTrue(OfflineDownloadManager.isTrustedDownloadHost("drive.google.com"))
        assertTrue(OfflineDownloadManager.isTrustedDownloadHost("www.drive.google.com"))
    }

    @Test
    fun allowsGoogleusercontentSubdomainsForRedirects() {
        assertTrue(
            OfflineDownloadManager.isTrustedDownloadHost(
                "docs.googleusercontent.com",
                allowDocsGoogleusercontent = true
            )
        )
        assertTrue(
            OfflineDownloadManager.isTrustedDownloadHost(
                "doc-0s-xx.docs.googleusercontent.com",
                allowDocsGoogleusercontent = true
            )
        )
    }

    @Test
    fun rejectsGoogleusercontentWhenNotRedirectContext() {
        assertFalse(OfflineDownloadManager.isTrustedDownloadHost("docs.googleusercontent.com"))
        assertFalse(OfflineDownloadManager.isTrustedDownloadHost("doc-0s-xx.docs.googleusercontent.com"))
    }

    @Test
    fun rejectsNonGoogleHosts() {
        assertFalse(OfflineDownloadManager.isTrustedDownloadHost("example.com"))
        assertFalse(OfflineDownloadManager.isTrustedDownloadHost("evil-googleusercontent.com"))
    }
}
