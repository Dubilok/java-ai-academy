package com.javaacademy.plugin

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginDescriptorTest {

    @Test
    fun `plugin xml is on the classpath`() {
        val stream = javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")
        assertNotNull(stream, "META-INF/plugin.xml must be on the test classpath")
        stream.use { inputStream ->
            val content = inputStream.readBytes().decodeToString()
            assertTrue(content.contains("com.javaacademy.plugin"), "plugin id must appear in plugin.xml")
            assertTrue(content.contains("Java AI Academy"), "plugin name must appear in plugin.xml")
            assertTrue(
                content.contains("AcademyToolWindowFactory"),
                "tool window factory class must be declared"
            )
        }
    }
}
