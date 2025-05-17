package com.lukastomoszek.idea.codemetricsvisualization

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PluginIcons {

    @JvmField
    val LOCKED: Icon = IconLoader.getIcon("/icons/locked.svg", PluginIcons::class.java)

    @JvmField
    val UNLOCKED: Icon = IconLoader.getIcon("/icons/unlocked.svg", PluginIcons::class.java)
}
