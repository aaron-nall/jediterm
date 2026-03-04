package com.jediterm.terminal.model

// Intentionally not a data class: identity-based equals/hashCode is required
// because instances are used as WeakHashMap keys in the decoded image cache.
class InlineImage(
  val imageData: ByteArray,
  val cellWidth: Int,
  val cellHeight: Int
)

class InlineImagePlacement(
  val image: InlineImage,
  val startColumn: Int
)
