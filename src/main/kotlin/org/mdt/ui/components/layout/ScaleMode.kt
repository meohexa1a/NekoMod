package org.mdt.ui.components.layout

/**
 * ## ScaleMode
 *
 * Scaling behavior for texture regions inside UI containers and image widgets.
 *
 * - [FIT]: Scales the image proportionally so it fits completely within container bounds.
 * - [CROP]: Scales the image proportionally to completely fill container bounds, cropping overflow.
 * - [STRETCH]: Non-uniformly stretches the image to exactly match container dimensions.
 * - [CENTER]: Displays the image at its natural 1:1 pixel size centered in the container.
 */
enum class ScaleMode {
    FIT,
    CROP,
    STRETCH,
    CENTER
}
