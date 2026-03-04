package com.jediterm.app

import com.jediterm.pty.PtyProcessTtyConnector
import com.jediterm.terminal.CursorShape
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.pty4j.PtyProcessBuilder
import java.awt.Color
import java.awt.Font
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.io.path.pathString

fun main(args: Array<String>) {
  val config = parseArgs(args)
  SwingUtilities.invokeLater { createAndShowGUI(config) }
}

private data class Config(
  val fontFamily: String? = null,
  val fontSize: Float? = null,
  val bg: Color? = null,
  val fg: Color? = null,
  val command: String? = null,
)

private fun parseArgs(args: Array<String>): Config {
  var fontFamily: String? = null
  var fontSize: Float? = null
  var bg: Color? = null
  var fg: Color? = null
  var command: String? = null

  var i = 0
  while (i < args.size) {
    when (args[i]) {
      "--font-family" -> fontFamily = args[++i]
      "--font-size" -> fontSize = args[++i].toFloat()
      "--bg" -> bg = parseColor(args[++i])
      "--fg" -> fg = parseColor(args[++i])
      "--command" -> command = args[++i]
      else -> System.err.println("Unknown option: ${args[i]}")
    }
    i++
  }
  return Config(fontFamily, fontSize, bg, fg, command)
}

private fun parseColor(hex: String): Color {
  val h = hex.removePrefix("#")
  require(h.length == 6) { "Color must be in #RRGGBB format: $hex" }
  return Color(h.substring(0, 2).toInt(16), h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16))
}

private fun createSettingsProvider(config: Config): DefaultSettingsProvider {
  return object : DefaultSettingsProvider() {
    override fun getTerminalFont(): Font {
      val family = config.fontFamily
      if (family != null) {
        return Font(family, Font.PLAIN, getTerminalFontSize().toInt())
      }
      return super.getTerminalFont()
    }

    override fun getTerminalFontSize(): Float {
      return config.fontSize ?: super.getTerminalFontSize()
    }

    override fun getDefaultForeground(): TerminalColor {
      val c = config.fg ?: return super.getDefaultForeground()
      return TerminalColor.rgb(c.red, c.green, c.blue)
    }

    override fun getDefaultBackground(): TerminalColor {
      val c = config.bg ?: return super.getDefaultBackground()
      return TerminalColor.rgb(c.red, c.green, c.blue)
    }
  }
}

private fun createTtyConnector(config: Config): PtyProcessTtyConnector {
  val envs = HashMap(System.getenv())
  if (isMacOS()) {
    envs["LC_CTYPE"] = Charsets.UTF_8.name()
  }
  if (!isWindows()) {
    envs["TERM"] = "xterm-256color"
  }

  val command: Array<String> = if (config.command != null) {
    if (isWindows()) arrayOf("cmd.exe", "/c", config.command)
    else arrayOf("/bin/sh", "-c", config.command)
  } else if (isWindows()) {
    arrayOf("powershell.exe")
  } else {
    val shell = envs["SHELL"] ?: "/bin/bash"
    if (isMacOS()) arrayOf(shell, "--login") else arrayOf(shell)
  }

  val workingDirectory = Path.of(".").toAbsolutePath().normalize().pathString
  val process = PtyProcessBuilder()
    .setDirectory(workingDirectory)
    .setInitialColumns(120)
    .setInitialRows(24)
    .setCommand(command)
    .setEnvironment(envs)
    .setConsole(false)
    .setUseWinConPty(true)
    .start()

  return PtyProcessTtyConnector(process, StandardCharsets.UTF_8, command.toList())
}

private fun createAndShowGUI(config: Config) {
  val settingsProvider = createSettingsProvider(config)
  val widget = JediTermWidget(settingsProvider)
  widget.terminalPanel.setDefaultCursorShape(CursorShape.BLINK_VERTICAL_BAR)
  widget.setTtyConnector(createTtyConnector(config))
  widget.start()

  val frame = JFrame("JediTerm Test")
  frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
  widget.addListener {
    widget.close()
    SwingUtilities.invokeLater {
      if (frame.isVisible) frame.dispose()
    }
  }
  frame.addWindowListener(object : WindowAdapter() {
    override fun windowClosing(e: WindowEvent) {
      frame.isVisible = false
      widget.ttyConnector.close()
    }
  })
  frame.contentPane = widget
  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.isVisible = true
}
