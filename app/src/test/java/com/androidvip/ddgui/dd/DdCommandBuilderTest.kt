package com.androidvip.ddgui.dd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DdCommandBuilderTest {
    @Test
    fun quotesPathsAndAddsConversions() {
        val request = DdRequest(
            inputPath = "/sdcard/backup image.img",
            outputPath = "/dev/block/by-name/boot",
            conversions = listOf(DdConversion.NO_TRUNCATE, DdConversion.SYNC)
        )

        assertEquals(
            "dd if='/sdcard/backup image.img' of='/dev/block/by-name/boot' conv=notrunc,sync",
            DdCommandBuilder.build(request)
        )
    }

    @Test
    fun escapesSingleQuotesAndShellMetacharactersInPaths() {
        val request = DdRequest(
            inputPath = "/sdcard/it's; still a path.img",
            outputPath = "/tmp/out.img",
            conversions = emptyList()
        )

        assertEquals(
            "dd if='/sdcard/it'\\''s; still a path.img' of='/tmp/out.img'",
            DdCommandBuilder.build(request)
        )
    }

    @Test
    fun omitsConversionsWhenNoneAreSelected() {
        val request = DdRequest("/input.img", "/output.img", emptyList())

        assertEquals(
            "dd if='/input.img' of='/output.img'",
            DdCommandBuilder.build(request)
        )
    }

    @Test
    fun removesDuplicateConversionsWhilePreservingOrder() {
        val request = DdRequest(
            "/input.img",
            "/output.img",
            listOf(DdConversion.SWAB, DdConversion.SWAB, DdConversion.FSYNC)
        )

        assertEquals(
            "dd if='/input.img' of='/output.img' conv=swab,fsync",
            DdCommandBuilder.build(request)
        )
    }

    @Test
    fun rejectsEmptyPaths() {
        assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(DdRequest("", "/output.img", emptyList()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(DdRequest("/input.img", "", emptyList()))
        }
    }

    @Test
    fun rejectsNullCharactersInPaths() {
        assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(DdRequest("/input\u0000.img", "/output.img", emptyList()))
        }
    }
}
