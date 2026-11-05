package com.androidvip.ddgui.data.dd

import com.androidvip.ddgui.domain.dd.DdConversion
import com.androidvip.ddgui.domain.dd.DdRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DdCommandBuilderTest {
    @Test
    fun `given paths and conversions when building command then quotes paths and adds conversions`() {
        // Given
        val request = DdRequest(
            inputPath = "/sdcard/backup image.img",
            outputPath = "/dev/block/by-name/boot",
            conversions = listOf(DdConversion.NO_TRUNCATE, DdConversion.SYNC)
        )

        // When
        val command = DdCommandBuilder.build(request)

        // Then
        assertEquals(
            "dd if='/sdcard/backup image.img' of='/dev/block/by-name/boot' conv=notrunc,sync",
            command
        )
    }

    @Test
    fun `given shell metacharacters when building command then escapes them inside quoted paths`() {
        // Given
        val request = DdRequest(
            inputPath = "/sdcard/it's; still a path.img",
            outputPath = "/tmp/out.img",
            conversions = emptyList()
        )

        // When
        val command = DdCommandBuilder.build(request)

        // Then
        assertEquals(
            "dd if='/sdcard/it'\\''s; still a path.img' of='/tmp/out.img'",
            command
        )
    }

    @Test
    fun `given no conversions when building command then omits conv argument`() {
        // Given
        val request = DdRequest("/input.img", "/output.img", emptyList())

        // When
        val command = DdCommandBuilder.build(request)

        // Then
        assertEquals(
            "dd if='/input.img' of='/output.img'",
            command
        )
    }

    @Test
    fun `given duplicate conversions when building command then keeps unique order`() {
        // Given
        val request = DdRequest(
            "/input.img",
            "/output.img",
            listOf(DdConversion.SWAB, DdConversion.SWAB, DdConversion.FSYNC)
        )

        // When
        val command = DdCommandBuilder.build(request)

        // Then
        assertEquals(
            "dd if='/input.img' of='/output.img' conv=swab,fsync",
            command
        )
    }

    @Test
    fun `given empty input or output when building command then rejects the request`() {
        // Given
        val emptyInput = DdRequest("", "/output.img", emptyList())
        val emptyOutput = DdRequest("/input.img", "", emptyList())

        // When
        val inputError = assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(emptyInput)
        }
        val outputError = assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(emptyOutput)
        }

        // Then
        assertEquals("Input path must not be empty", inputError.message)
        assertEquals("Output path must not be empty", outputError.message)
    }

    @Test
    fun `given null character in path when building command then rejects the request`() {
        // Given
        val request = DdRequest("/input\u0000.img", "/output.img", emptyList())

        // When
        val error = assertThrows(IllegalArgumentException::class.java) {
            DdCommandBuilder.build(request)
        }

        // Then
        assertEquals("Input path contains a null character", error.message)
    }
}
