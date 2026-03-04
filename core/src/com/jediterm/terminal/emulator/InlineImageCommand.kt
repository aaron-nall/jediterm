package com.jediterm.terminal.emulator

import java.util.Base64

class InlineImageCommand(
  val name: String?,
  val widthSpec: DimensionSpec?,
  val heightSpec: DimensionSpec?,
  val preserveAspectRatio: Boolean,
  val inline: Boolean,
  val imageData: ByteArray
) {
  sealed class DimensionSpec {
    data class Cells(val value: Int) : DimensionSpec()
    data class Pixels(val value: Int) : DimensionSpec()
    data class Percent(val value: Int) : DimensionSpec()
  }

  companion object {
    /**
     * Parse an OSC 1337 File= command from SystemCommandSequence args.
     *
     * Since SystemCommandSequence splits on ';', the args list looks like:
     * - args[0] = "1337"
     * - args[1] = "File=name=dGVzdA==" (first param prefixed with "File=")
     * - args[2..n] = more key=value pairs, with the last containing ":BASE64DATA" after the colon
     */
    fun parse(args: List<String>): InlineImageCommand {
      require(args.size >= 2) { "Expected at least 2 args, got ${args.size}" }

      // Rejoin args[1:] with ";"
      val payload = args.subList(1, args.size).joinToString(";")

      // Strip "File=" prefix
      require(payload.startsWith("File=")) { "Expected 'File=' prefix, got: ${payload.take(20)}" }
      val afterFile = payload.removePrefix("File=")

      // Split on first ":" → params string + base64 string
      val colonIndex = afterFile.indexOf(':')
      require(colonIndex >= 0) { "Missing ':' separator between params and base64 data" }
      val paramsString = afterFile.substring(0, colonIndex)
      val base64String = afterFile.substring(colonIndex + 1)

      require(base64String.isNotEmpty()) { "Empty base64 data" }

      // Parse params
      val params = mutableMapOf<String, String>()
      if (paramsString.isNotEmpty()) {
        for (param in paramsString.split(";")) {
          val eqIndex = param.indexOf('=')
          if (eqIndex >= 0) {
            params[param.substring(0, eqIndex)] = param.substring(eqIndex + 1)
          }
        }
      }

      val name = params["name"]?.let { decodeBase64String(it) }
      val widthSpec = params["width"]?.let { parseDimension(it) }
      val heightSpec = params["height"]?.let { parseDimension(it) }
      val preserveAspectRatio = params["preserveAspectRatio"]?.let { it != "0" } ?: true
      val inline = params["inline"]?.let { it == "1" } ?: false
      // Use MIME decoder to tolerate line breaks (e.g. from `base64` without -w0)
      val imageData = Base64.getMimeDecoder().decode(base64String)

      return InlineImageCommand(
        name = name,
        widthSpec = widthSpec,
        heightSpec = heightSpec,
        preserveAspectRatio = preserveAspectRatio,
        inline = inline,
        imageData = imageData
      )
    }

    fun parseDimension(value: String): DimensionSpec? {
      if (value.isEmpty() || value == "auto") return null
      return when {
        value.endsWith("px") -> {
          val num = value.removeSuffix("px").toIntOrNull() ?: return null
          DimensionSpec.Pixels(num)
        }
        value.endsWith("%") -> {
          val num = value.removeSuffix("%").toIntOrNull() ?: return null
          DimensionSpec.Percent(num)
        }
        else -> {
          val num = value.toIntOrNull() ?: return null
          DimensionSpec.Cells(num)
        }
      }
    }

    private fun decodeBase64String(encoded: String): String {
      return try {
        String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
      } catch (_: IllegalArgumentException) {
        encoded
      }
    }
  }
}
