# Layout Engine & Coordinate System

This document explains the technical implementation of `GodotLayout`, the OpenGL bottom-left orthographic coordinate system, and container slot allocation algorithms (`RowNode`, `ColumnNode`, `GridContainerNode`).

---

## 1. OpenGL Bottom-Left Coordinate System

Mindustry and Arc follow standard OpenGL screen conventions:
* **Origin $(0, 0)$:** Located at the **Bottom-Left** corner of the screen.
* **$X$-Axis:** Increases from Left to Right ($0 \rightarrow \text{Width}$).
* **$Y$-Axis:** Increases from Bottom to Top ($0 \rightarrow \text{Height}$).

### Top-to-Bottom Stacking in `ColumnNode`:
Because users expect vertical UI containers to stack from **Top to Bottom**:
* Stacking begins at the top: `currentTopY = parentY + parentH - padT`.
* The bottom of the first child slot: `slotY = currentTopY - slotH`.
* Progressing downward: `currentTopY -= slotH + gap`.

---

## 2. Godot 2-Pass Container Algorithm

When a container (`RowNode` or `ColumnNode`) calculates `layout()`, it runs a 2-pass measurement and allocation algorithm:

### 🔹 Pass 1: Measurement Pass
Iterates through all visible children (`child.visible == true`):
1. Calculates minimum required bounds including outward margin:
   * Width: `getChildMinWidth(child) = child.getPrefWidth() + child.marginL + child.marginR`
   * Height: `getChildMinHeight(child) = child.getPrefHeight() + child.marginT + child.marginB`
2. Accumulates total minimum space: $\text{totalMinMain} = \sum \text{childMinSize}$.
3. Computes total stretch ratio for children with the `EXPAND` flag: $\text{totalStretchRatio} = \sum \text{stretchRatio}$.

### 🔹 Pass 2: Slot Allocation Pass
1. **Calculates available free space:**
   $$\text{freeSpace} = \max(0, \text{availableSpace} - \text{totalMinMain} - \text{totalGaps})$$
2. **When expanding children exist:**
   * Excess space is proportionally distributed:
     $$\text{extraSlot} = \text{freeSpace} \times \left(\frac{\text{child.stretchRatio}}{\text{totalStretchRatio}}\right)$$
   * $\text{slotSize} = \text{childMinSize} + \text{extraSlot}$.
3. **When no expanding children exist:**
   * Applies [`Arrangement`](#3-arrangement-modes) distribution modes:
     * **`Start`:** Packed at the beginning.
     * **`Center`:** Centered together within available free space.
     * **`End`:** Packed at the end.
     * **`SpaceBetween`:** Evenly distributed with first and last children touching edges.
     * **`SpaceAround`:** Evenly distributed with half-size space at edges.
     * **`SpaceEvenly`:** Uniform spacing between all items and container edges.

---

## 3. Size Flags & `fitChildInRect`

Each child possesses `sizeFlagsHorizontal` and `sizeFlagsVertical`.
When a slot $(rx, ry, rw, rh)$ is allocated to a child:

1. **Margin Inset:**
   * $\text{slotInnerX} = rx + \text{marginL}$
   * $\text{slotInnerY} = ry + \text{marginB}$
   * $\text{slotInnerW} = rw - \text{marginL} - \text{marginR}$
   * $\text{slotInnerH} = rh - \text{marginT} - \text{marginB}$

2. **Placement & Sizing:**
   * **`FILL`:** Stretches node to consume full $\text{slotInnerW} \times \text{slotInnerH}$.
   * **`SHRINK_BEGIN`:** Preserves minimum size, placed at Left (horizontal) or Top (vertical).
   * **`SHRINK_CENTER`:** Preserves minimum size, centered within the slot.
   * **`SHRINK_END`:** Preserves minimum size, placed at Right (horizontal) or Bottom (vertical).
