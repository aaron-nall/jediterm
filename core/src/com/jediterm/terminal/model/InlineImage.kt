package com.jediterm.terminal.model

class InlineImage(
  val imageData: ByteArray,
  val cellWidth: Int,
  val cellHeight: Int
)

class InlineImagePlacement(
  val image: InlineImage,
  val startColumn: Int
)
