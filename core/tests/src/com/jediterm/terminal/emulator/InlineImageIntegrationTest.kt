package com.jediterm.terminal.emulator

import com.jediterm.terminal.InlineImageSize
import com.jediterm.util.TestSession
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class InlineImageIntegrationTest {

  private val sampleImageData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
  private val sampleBase64 = Base64.getEncoder().encodeToString(sampleImageData)

  private fun oscInlineImage(base64: String = sampleBase64, inline: Int = 1, extraParams: String = ""): String {
    val params = buildString {
      if (extraParams.isNotEmpty()) {
        append(extraParams)
        append(";")
      }
      append("inline=$inline")
    }
    return "\u001b]1337;File=$params:$base64\u0007"
  }

  @Test
  fun `inline=1 stores image in text buffer`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 3))
    session.process(oscInlineImage())

    val line = session.terminalTextBuffer.getLine(0)
    val placements = session.terminalTextBuffer.getInlineImages(line)
    assertEquals(1, placements.size)
    assertEquals(0, placements[0].startColumn)
    assertEquals(5, placements[0].image.cellWidth)
    assertEquals(3, placements[0].image.cellHeight)
    assertArrayEquals(sampleImageData, placements[0].image.imageData)
  }

  @Test
  fun `inline=0 does not store image`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 3))
    session.process(oscInlineImage(inline = 0))

    val line = session.terminalTextBuffer.getLine(0)
    val placements = session.terminalTextBuffer.getInlineImages(line)
    assertTrue(placements.isEmpty())
  }

  @Test
  fun `cursor advances past image`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 3))
    session.process(oscInlineImage())

    // Image is 3 rows high, cursor should be at the start of the line after the image
    // Initial cursor at (1,1), image occupies rows 1-3, cursor moves to (1,4)
    session.assertCursorPosition(1, 4)
  }

  @Test
  fun `malformed image data does not crash`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 3))
    // Send a malformed OSC sequence - should not throw
    session.process("\u001b]1337;File=inline=1:not-valid-base64!!!\u0007")
    // If we get here without exception, the test passes
  }

  @Test
  fun `multiple images on different lines`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 2))
    session.process(oscInlineImage())
    session.process(oscInlineImage())

    val line0 = session.terminalTextBuffer.getLine(0)
    val line0Placements = session.terminalTextBuffer.getInlineImages(line0)
    assertEquals(1, line0Placements.size)

    // Second image starts after cursor advanced past the first (2 rows + 1 for next line)
    val line3 = session.terminalTextBuffer.getLine(2)
    val line3Placements = session.terminalTextBuffer.getInlineImages(line3)
    assertEquals(1, line3Placements.size)
  }

  @Test
  fun `null resolve size silently skips`() {
    val session = TestSession(40, 10)
    // Don't set override - resolveInlineImageSize returns null by default
    session.process(oscInlineImage())

    // No images stored
    val line = session.terminalTextBuffer.getLine(0)
    val placements = session.terminalTextBuffer.getInlineImages(line)
    assertTrue(placements.isEmpty())
    // Cursor should not move from initial position (1,1)
    session.assertCursorPosition(1, 1)
  }

  @Test
  fun `text before and after image`() {
    val session = TestSession(40, 10)
    session.display.setInlineImageSizeOverride(InlineImageSize(5, 2))
    session.process("Hello\r\n")
    session.process(oscInlineImage())
    session.process("World")

    // "Hello" on line 0, image on line 1, cursor after image, "World" on the line after
    val line1 = session.terminalTextBuffer.getLine(1)
    val placements = session.terminalTextBuffer.getInlineImages(line1)
    assertEquals(1, placements.size)
  }
}
