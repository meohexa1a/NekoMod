# Layout Engine & Coordinate System

This document details the inner workings of `GodotLayout`, the OpenGL bottom-left orthographic coordinate system in Mindustry/Arc, and slot allocation across containers via the `MeasurePolicy` architecture.

---

## 1. OpenGL Bottom-Left Coordinate System

Mindustry and Arc operate in standard OpenGL coordinates:
* **Origin $(0, 0)$:** Bottom-Left corner of the screen.
* **$X$-Axis:** Increases Left to Right ($0 \rightarrow \text{Width}$).
* **$Y$-Axis:** Increases Bottom to Top ($0 \rightarrow \text{Height}$).

### Vertical Direction Conversion (`ColumnMeasurePolicy`):
Because UI layouts naturally flow **Top to Bottom**:
* Top slot anchor: `currentTopY = parentY + parentH - padT`.
* First element bottom baseline: `slotY = currentTopY - slotH`.
* Downward step after placement: `currentTopY -= slotH + gap`.

---

## 2. 2-Pass Godot Container Layout Algorithm

When a `LayoutNode` executes `layout()`, its active `MeasurePolicy` performs 2 sequential passes:

### 🔹 Pass 1: Minimum Measurement Pass
The container iterates through all visible children (`child.visible == true`):
1. Computes child minimum bounds:
   * Horizontal: `getChildMinWidth(child) = child.getPrefWidth() + child.marginL + child.marginR`
   * Vertical: `getChildMinHeight(child) = child.getPrefHeight() + child.marginT + child.marginB`
2. Aggregates total minimum size: $\text{totalMinMain} = \sum \text{childMinSize}$.
3. Sums stretch ratios of children with `EXPAND`: $\text{totalStretchRatio} = \sum \text{stretchRatio}$.

### 🔹 Pass 2: Slot Allocation & Margin Placement Pass
1. **Compute Available Free Space:**
   $$\text{freeSpace} = \max(0, \text{availableSpace} - \text{totalMinMain} - \text{totalGaps})$$
2. **If Children Have `EXPAND`:**
   * Extra space is distributed proportionally by weight:
     $$\text{extraSlot} = \text{freeSpace} \times \left(\frac{\text{child.stretchRatio}}{\text{totalStretchRatio}}\right)$$
   * $\text{slotSize} = \text{childMinSize} + \text{extraSlot}$.
3. **If No Children Have `EXPAND`:**
   * Aligns slots using [`Arrangement`](#3-arrangement-modes) (`Start`, `Center`, `End`, `SpaceBetween`, `SpaceAround`, `SpaceEvenly`).

---

## 3. Size Flags (`SizeFlags`) & `fitChildInRect`

Each child node maintains `sizeFlagsHorizontal` and `sizeFlagsVertical`.
When a slot $(rx, ry, rw, rh)$ is allocated to a child:

1. **Margin Inset Application:**
   * $\text{slotInnerX} = rx + \text{marginL}$
   * $\text{slotInnerY} = ry + \text{marginB}$
   * $\text{slotInnerW} = rw - \text{marginL} - \text{marginR}$
   * $\text{slotInnerH} = rh - \text{marginT} - \text{marginB}$
2. **Shrink vs Fill Execution:**
   * With `FILL`: Child expands to fill the entire `slotInnerW` / `slotInnerH`.
   * With `SHRINK_CENTER`: Child retains its preferred size and is centered precisely within the slot.
