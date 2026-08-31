// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.input

import arc.input.KeyCode

/**
 * ## CursorIcon
 *
 * Platform-agnostic mouse cursor types rendered during hover and interaction gestures.
 *
 * @see org.mdt.core.ui.node.UINode
 */
enum class CursorIcon {
    DEFAULT,
    ARROW,
    IBEAM,
    CROSSHAIR,
    HAND,
    RESIZE_HORIZONTAL,
    RESIZE_VERTICAL
}

/**
 * ## Key
 *
 * Platform-agnostic keyboard key identifiers for navigation, text editing, and hotkeys.
 *
 * @property keyCode Numeric code representation.
 *
 * @see KeyCode
 * @see org.mdt.core.ui.node.UINode
 */
enum class Key(val keyCode: Int) {
    UNKNOWN(0),
    BACKSPACE(8),
    TAB(9),
    ENTER(13),
    ESCAPE(27),
    SPACE(32),
    DELETE(127),

    LEFT(1001),
    RIGHT(1002),
    UP(1003),
    DOWN(1004),
    HOME(1005),
    END(1006),
    PAGE_UP(1007),
    PAGE_DOWN(1008),

    SHIFT_LEFT(2001),
    SHIFT_RIGHT(2002),
    CTRL_LEFT(2003),
    CTRL_RIGHT(2004),
    ALT_LEFT(2005),
    ALT_RIGHT(2006),

    A(65), B(66), C(67), D(68), E(69), F(70), G(71), H(72), I(73), J(74),
    K(75), L(76), M(77), N(78), O(79), P(80), Q(81), R(82), S(83), T(84),
    U(85), V(86), W(87), X(88), Y(89), Z(90),

    NUM_0(48), NUM_1(49), NUM_2(50), NUM_3(51), NUM_4(52),
    NUM_5(53), NUM_6(54), NUM_7(55), NUM_8(56), NUM_9(57);

    companion object {
        /** Maps Arc engine [KeyCode] to platform-agnostic [Key]. */
        fun fromArcKeyCode(code: KeyCode): Key = when (code) {
            KeyCode.backspace -> BACKSPACE
            KeyCode.tab -> TAB
            KeyCode.enter -> ENTER
            KeyCode.escape -> ESCAPE
            KeyCode.space -> SPACE
            KeyCode.del -> DELETE
            KeyCode.left -> LEFT
            KeyCode.right -> RIGHT
            KeyCode.up -> UP
            KeyCode.down -> DOWN
            KeyCode.home -> HOME
            KeyCode.end -> END
            KeyCode.pageUp -> PAGE_UP
            KeyCode.pageDown -> PAGE_DOWN
            KeyCode.shiftLeft -> SHIFT_LEFT
            KeyCode.shiftRight -> SHIFT_RIGHT
            KeyCode.controlLeft -> CTRL_LEFT
            KeyCode.controlRight -> CTRL_RIGHT
            KeyCode.altLeft -> ALT_LEFT
            KeyCode.altRight -> ALT_RIGHT
            KeyCode.a -> A; KeyCode.b -> B; KeyCode.c -> C; KeyCode.d -> D; KeyCode.e -> E
            KeyCode.f -> F; KeyCode.g -> G; KeyCode.h -> H; KeyCode.i -> I; KeyCode.j -> J
            KeyCode.k -> K; KeyCode.l -> L; KeyCode.m -> M; KeyCode.n -> N; KeyCode.o -> O
            KeyCode.p -> P; KeyCode.q -> Q; KeyCode.r -> R; KeyCode.s -> S; KeyCode.t -> T
            KeyCode.u -> U; KeyCode.v -> V; KeyCode.w -> W; KeyCode.x -> X; KeyCode.y -> Y; KeyCode.z -> Z
            KeyCode.num0 -> NUM_0; KeyCode.num1 -> NUM_1; KeyCode.num2 -> NUM_2; KeyCode.num3 -> NUM_3
            KeyCode.num4 -> NUM_4; KeyCode.num5 -> NUM_5; KeyCode.num6 -> NUM_6; KeyCode.num7 -> NUM_7
            KeyCode.num8 -> NUM_8; KeyCode.num9 -> NUM_9
            else -> UNKNOWN
        }
    }
}
