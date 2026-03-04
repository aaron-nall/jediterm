package com.jediterm.terminal.emulator

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class InlineImageCommandTest {

  private val samplePngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic
  private val sampleBase64 = Base64.getEncoder().encodeToString(samplePngBytes)

  @Test
  fun `basic inline image with all params`() {
    val nameBase64 = Base64.getEncoder().encodeToString("test.png".toByteArray())
    val args = listOf("1337", "File=name=$nameBase64;size=1234;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals("test.png", cmd.name)
    assertTrue(cmd.inline)
    assertArrayEquals(samplePngBytes, cmd.imageData)
  }

  @Test
  fun `pixel dimensions`() {
    val args = listOf("1337", "File=width=100px;height=200px;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals(InlineImageCommand.DimensionSpec.Pixels(100), cmd.widthSpec)
    assertEquals(InlineImageCommand.DimensionSpec.Pixels(200), cmd.heightSpec)
  }

  @Test
  fun `percent dimensions`() {
    val args = listOf("1337", "File=width=50%;height=75%;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals(InlineImageCommand.DimensionSpec.Percent(50), cmd.widthSpec)
    assertEquals(InlineImageCommand.DimensionSpec.Percent(75), cmd.heightSpec)
  }

  @Test
  fun `cell dimensions`() {
    val args = listOf("1337", "File=width=10;height=5;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals(InlineImageCommand.DimensionSpec.Cells(10), cmd.widthSpec)
    assertEquals(InlineImageCommand.DimensionSpec.Cells(5), cmd.heightSpec)
  }

  @Test
  fun `auto dimensions`() {
    val args = listOf("1337", "File=width=auto;height=auto;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertNull(cmd.widthSpec)
    assertNull(cmd.heightSpec)
  }

  @Test
  fun `missing dimensions default to null`() {
    val args = listOf("1337", "File=inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertNull(cmd.widthSpec)
    assertNull(cmd.heightSpec)
  }

  @Test
  fun `preserveAspectRatio defaults to true`() {
    val args = listOf("1337", "File=inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertTrue(cmd.preserveAspectRatio)
  }

  @Test
  fun `preserveAspectRatio set to 0`() {
    val args = listOf("1337", "File=preserveAspectRatio=0;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertFalse(cmd.preserveAspectRatio)
  }

  @Test
  fun `inline=0 parses correctly`() {
    val args = listOf("1337", "File=inline=0:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertFalse(cmd.inline)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `missing base64 data throws`() {
    val args = listOf("1337", "File=inline=1:")
    InlineImageCommand.parse(args)
  }

  @Test
  fun `name parameter base64 decoding`() {
    val nameBase64 = Base64.getEncoder().encodeToString("my-image.gif".toByteArray())
    val args = listOf("1337", "File=name=$nameBase64;inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals("my-image.gif", cmd.name)
  }

  @Test
  fun `multiple semicolons handled correctly via SystemCommandSequence split`() {
    // SystemCommandSequence splits on ';', so args may look like:
    // ["1337", "File=name=dGVzdA==", "width=100px", "inline=1:BASE64"]
    val nameBase64 = Base64.getEncoder().encodeToString("test".toByteArray())
    val args = listOf("1337", "File=name=$nameBase64", "width=100px", "inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertEquals("test", cmd.name)
    assertEquals(InlineImageCommand.DimensionSpec.Pixels(100), cmd.widthSpec)
    assertTrue(cmd.inline)
    assertArrayEquals(samplePngBytes, cmd.imageData)
  }

  @Test
  fun `missing name defaults to null`() {
    val args = listOf("1337", "File=inline=1:$sampleBase64")
    val cmd = InlineImageCommand.parse(args)
    assertNull(cmd.name)
  }

  @Test
  fun `parseDimension with various values`() {
    assertNull(InlineImageCommand.parseDimension("auto"))
    assertNull(InlineImageCommand.parseDimension(""))
    assertEquals(InlineImageCommand.DimensionSpec.Cells(42), InlineImageCommand.parseDimension("42"))
    assertEquals(InlineImageCommand.DimensionSpec.Pixels(200), InlineImageCommand.parseDimension("200px"))
    assertEquals(InlineImageCommand.DimensionSpec.Percent(100), InlineImageCommand.parseDimension("100%"))
  }
}
