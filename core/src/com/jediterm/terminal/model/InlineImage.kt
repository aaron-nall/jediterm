package com.jediterm.terminal.model

// Intentionally not a data class: identity-based equals/hashCode is required because
// ByteArray lacks value-based equals, and instances are used as LinkedHashMap keys
// in the TerminalPanel decoded-image cache.
class InlineImage(
  imageData: ByteArray,
  val cellWidth: Int,
  val cellHeight: Int
) {
  val imageData: ByteArray = imageData.copyOf()
}

class InlineImagePlacement(
  val image: InlineImage,
  val startColumn: Int
)
