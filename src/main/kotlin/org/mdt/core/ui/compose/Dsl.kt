@file:Suppress("FunctionName")

package org.mdt.core.ui.compose

// Top-level modifier factory shortcuts
fun Modifier(): UIModifier = UIModifier

val Modifier: UIModifier get() = UIModifier

// Common UIModifier extensions
fun Modifier(block: UIModifier.() -> UIModifier): UIModifier = UIModifier.block()
