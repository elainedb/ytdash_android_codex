package dev.elainedb.ytdash_android_codex.data.config

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigHelperPrecedenceTest {

    @Test
    fun `prefers local config over ci and template`() {
        val assetManager = mockk<AssetManager>()
        every { assetManager.open("config.properties") } returns ByteArrayInputStream(
            """
            authorized_emails=edbpmc@gmail.com
            youtubeApiKey=LOCAL_KEY
            """.trimIndent().toByteArray()
        )
        every { assetManager.open("config.properties.ci") } returns ByteArrayInputStream(
            """
            authorized_emails=ci@example.com
            youtubeApiKey=CI_KEY
            """.trimIndent().toByteArray()
        )
        every { assetManager.open("config.properties.template") } returns ByteArrayInputStream(
            """
            authorized_emails=template@example.com
            youtubeApiKey=TEMPLATE_KEY
            """.trimIndent().toByteArray()
        )

        val context = mockk<Context>()
        every { context.assets } returns assetManager

        val config = ConfigHelper(context).getConfig()

        assertEquals(setOf("edbpmc@gmail.com"), config.authorizedEmails)
        assertEquals("LOCAL_KEY", config.youtubeApiKey)
    }
}
