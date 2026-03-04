package com.jediterm.terminal

data class InlineImageSize(val cellWidth: Int, val cellHeight: Int) {
  init {
    require(cellWidth > 0) { "cellWidth must be positive: $cellWidth" }
    require(cellHeight > 0) { "cellHeight must be positive: $cellHeight" }
  }
}
