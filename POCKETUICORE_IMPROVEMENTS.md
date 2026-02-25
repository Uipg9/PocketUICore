# PocketUICore v1.6.0 — Improvements, Issues, & Usage Guide

> **Author:** Uipg9  
> **Date:** February 25, 2026  
> **Based on:** Extensive real-world testing via TestCoreMod (Virtual Farm v4.2) — a 1,760-line interactive farming GUI exercising the full PocketUICore API surface.  
> **Target:** Minecraft 1.21.11 (Fabric)

---

## Table of Contents

1. [Critical Bugs (Must Fix)](#1-critical-bugs-must-fix)
2. [API Issues & Missing Features](#2-api-issues--missing-features)
3. [Quality-of-Life Improvements](#3-quality-of-life-improvements)
4. [Architectural Concerns](#4-architectural-concerns)
5. [Component-by-Component Review](#5-component-by-component-review)
6. [Usage Guide & Best Practices](#6-usage-guide--best-practices)
7. [Workarounds Reference](#7-workarounds-reference)
8. [Feature Requests (Prioritized)](#8-feature-requests-prioritized)
9. [Full API Surface Reference](#9-full-api-surface-reference)

---

## 1. Critical Bugs (Must Fix)

### 1.1 — Fatal Identifier Bug in Economy Payloads

**Severity:** 🔴 CRASH — Prevents mod from loading at all  
**Status:** Fixed in local copy, but should be fixed upstream  

**Symptom:**
```
java.lang.ExceptionInInitializerError
  at com.pocketuicore.PocketUICore.onInitialize(PocketUICore.java:28)
Caused by: net.minecraft.util.InvalidIdentifierException:
  Non [a-z0-9/._-] character in path of location: minecraft:pocketuicore:sync_balance
```

**Root Cause:**
Both `SyncBalancePayload.java` and `SyncEstatePayload.java` use:
```java
CustomPayload.id("pocketuicore:sync_balance")  // BUG
```
In MC 1.21.11, `CustomPayload.id(String)` calls `Identifier.ofVanilla()`, which forces `minecraft:` as the namespace and treats the **entire argument** as the path. The colon in `pocketuicore:sync_balance` violates the allowed path characters `[a-z0-9/._-]`.

**Fix Applied:**
```java
// SyncBalancePayload.java
public static final CustomPayload.Id<SyncBalancePayload> ID =
        new CustomPayload.Id<>(Identifier.of("pocketuicore", "sync_balance"));

// SyncEstatePayload.java
public static final CustomPayload.Id<SyncEstatePayload> ID =
        new CustomPayload.Id<>(Identifier.of("pocketuicore", "sync_estate"));
```
This creates `pocketuicore:sync_balance` as a proper Identifier with `namespace=pocketuicore`, `path=sync_balance`.

**Files:** `com/pocketuicore/economy/SyncBalancePayload.java`, `com/pocketuicore/economy/SyncEstatePayload.java`

---

### 1.2 — DarkPanel Consumes All Clicks Within Bounds

**Severity:** 🟠 HIGH — Breaks click-to-select on any child detection pattern  

**Problem:** `DarkPanel` has `blockInputInBounds = true` by default. When used as a root panel, `root.mouseClicked()` consumes **all** clicks within the panel boundaries before you can check which specific child element was clicked.

**Real-world impact:** In our Virtual Farm, clicking on plot cells (which are child `DarkPanel`s inside the root) never worked when we called `root.mouseClicked()` first. The root panel ate the click.

**Current workaround:** Check child elements BEFORE delegating to `root.mouseClicked()`:
```java
@Override
public boolean mouseClicked(Click click, boolean fromKeyboard) {
    // Must check plots FIRST before root eats the click
    for (int i = 0; i < NUM_PLOTS; i++) {
        if (plotCells[i].isHovered(click.x(), click.y())) {
            selectPlot(i);
            return true;
        }
    }
    // THEN let buttons handle via root
    if (root != null && root.mouseClicked(click.x(), click.y(), click.button())) {
        return true;
    }
    return super.mouseClicked(click, fromKeyboard);
}
```

**Suggested fix:** Either:
- (a) Add `setOnClick(Runnable)` to `DarkPanel` so child panels can register click handlers that fire through the normal delegation chain, OR
- (b) Change `mouseClicked()` to delegate to children FIRST before consuming at the container level, OR
- (c) Default `blockInputInBounds = false` on child panels (only `true` on root panels)

---

### 1.3 — PrestigeConfirmationPanel.center() Drift Bug

**Severity:** 🟠 HIGH — Calling `center()` multiple times corrupts layout  

**Problem:** `center()` repositions children by computing a delta-offset from their current absolute positions. If called a second time (e.g., on window resize), the delta is calculated from the already-moved positions, causing children to drift further each time.

**Fix:** Calculate delta from the original construction coordinates, not current positions. Store original positions or recalculate from scratch each time.

---

## 2. API Issues & Missing Features

### 2.1 — No `setText()` / `setLabel()` Confusion on HoverButton

**Severity:** 🟡 MEDIUM — Caused build errors during development  

**Problem:** During development, we naturally expected `HoverButton` to have a `setText()` method to update its label text after creation. It does NOT exist — the method is called `setLabel()`.

This is inconsistent with `TextLabel` which uses `setText()`. A developer coming from `TextLabel.setText()` will intuitively try `HoverButton.setText()` and get a compilation error.

**Suggested fix:**
- Add `setText(String)` as an alias for `setLabel(String)` on `HoverButton`, OR
- Rename both to use the same convention (prefer `setText()` since it matches Minecraft's widget patterns)

**Note:** In earlier testing before we discovered `setLabel()`, we thought there was NO way to change button text at all and used workarounds (ternary in constructor, `setEnabled(false)` to gray out). The API docs should clearly document `setLabel()`.

---

### 2.2 — Absolute Positioning Only — No Relative/Parent-Offset System

**Severity:** 🟡 MEDIUM — Most impactful QOL issue  

**Problem:** ALL coordinates in PocketUICore are absolute screen pixels. When you add a child to a panel at `(px, py)`, the child's `(x, y)` specifies where it renders on the actual screen, NOT relative to the parent.

```java
// This renders at screen (0, 0), NOT inside the panel!
panel.addChild(new TextLabel(0, 0, w, h, "text"));

// Must manually compute:
panel.addChild(new TextLabel(px + 10, py + 8, w, h, "text"));
```

**Impact:** Building any non-trivial UI requires computing absolute positions for every single component. This is:
- Error-prone (easy to get offsets wrong)
- Verbose (every coordinate requires `px +` or `py +` prefix)
- Fragile (changing panel position requires updating every child)
- Counter-intuitive (most UI frameworks use relative positioning)

**Suggested fix:** Add `setRelativePositioning(boolean)` to container components:
```java
DarkPanel panel = new DarkPanel(100, 50, 300, 200);
panel.setRelativePositioning(true);  // Enable relative mode

// Now (0, 0) means "top-left of panel content area"
panel.addChild(new TextLabel(0, 0, 280, 14, "Relative!"));
// Renders at screen (100+padding, 50+padding) automatically
```

The container would translate child coordinates by `(parentX + padding, parentY + padding)` during `render()` and input handling.

---

### 2.3 — Missing Getters on Most Components

**Severity:** 🟡 MEDIUM — Prevents reading current state  

**Problem:** Many components have setters without matching getters:

| Component | Has Setter | Missing Getter |
|-----------|-----------|----------------|
| `HoverButton` | `setNormalColor()` | `getNormalColor()` |
| `HoverButton` | `setHoverColor()` | `getHoverColor()` |
| `HoverButton` | `setPressedColor()` | `getPressedColor()` |
| `HoverButton` | `setTextColor()` | `getTextColor()` |
| `HoverButton` | `setCornerRadius()` | `getCornerRadius()` |
| `PercentageBar` | `setBarColor()` | `getBarColor()` |
| `PercentageBar` | `setTrackColor()` | `getTrackColor()` |
| `PercentageBar` | `setTextColor()` | `getTextColor()` |
| `PercentageBar` | `setCornerRadius()` | `getCornerRadius()` |
| `PercentageBar` | `setLabel()` | `getLabel()` |
| `PercentageBar` | `setShowPercentage()` | `isShowPercentage()` |
| `PercentageBar` | `setEasingSpeed()` | `getEasingSpeed()` |
| `DarkPanel` | `setBackgroundColor()` | `getBackgroundColor()` |
| `DarkPanel` | `setBorderColor()` | `getBorderColor()` |
| `DarkPanel` | `setCornerRadius()` | `getCornerRadius()` |
| `DarkPanel` | `setDrawBorder()` | `isDrawBorder()` |
| `DarkPanel` | `setDrawShadow()` | `isDrawShadow()` |

**Impact:** Can't read back component state at runtime — must track it externally. Makes conditional logic harder:
```java
// Can't do this:
if (bar.getBarColor() == BLUE) { ... }
// Must track separately:
boolean isBlue = someExternalFlag;
```

---

### 2.4 — No Input Forwarding Beyond Click/Release/Scroll

**Severity:** 🟡 MEDIUM — Limits component interactivity  

**Problem:** `UIComponent` only forwards these input events through the component tree:
- `mouseClicked(double, double, int)`
- `mouseReleased(double, double, int)`
- `mouseScrolled(double, double, double, double)`

Missing from the component tree:
- `mouseDragged` — needed for sliders, drag-and-drop
- `keyPressed` — needed for text input components, per-component hotkeys
- `charTyped` — needed for text fields
- `mouseMoved` — needed for accurate hover tracking (currently approximated via `isHovered()` during `render()`)

**Impact:** There's no way to build a text input field, slider, or draggable component using PocketUICore's component tree. All keyboard input must be handled at the Screen level.

---

### 2.5 — No Text Input Component

**Severity:** 🟡 MEDIUM — Common UI need  

**Problem:** There is no `TextFieldComponent` or equivalent. Building any form-style UI (search, naming, configuration) requires either:
- Using Minecraft's built-in `TextFieldWidget` (breaks the PocketUICore style/architecture)
- Building one from scratch (significant effort)

**Suggested fix:** Add `TextInputComponent` extending `UIComponent`:
```java
TextInputComponent input = new TextInputComponent(x, y, width, height);
input.setPlaceholder("Enter name...");
input.setMaxLength(32);
input.addChangeListener(text -> { ... });
String value = input.getText();
```

---

### 2.6 — No Disabled Visual State for HoverButton

**Severity:** 🟢 LOW — Cosmetic but confusing  

**Problem:** When `HoverButton.setEnabled(false)` is called, the button becomes non-interactive but the only visual change is reducing the background alpha to ~100. The label text, colors, and layout remain identical to the enabled state. This makes it very unclear to users that the button is disabled.

**Suggested fix:** When disabled:
- Reduce text opacity to ~50%
- Draw background in a grayed-out color (e.g., desaturate existing color)
- Remove hover/press state transitions
- Optional: Add strikethrough or "dimmed" text rendering

---

### 2.7 — No onClick for DarkPanel

**Severity:** 🟢 LOW — Requires workaround  

**Problem:** `DarkPanel` has no `setOnClick(Runnable)`. To make panels clickable (e.g., clickable cards, plot cells), you must manually check `isHovered()` in the Screen's `mouseClicked()`:

```java
// Workaround in mouseClicked():
for (int i = 0; i < panels.length; i++) {
    if (panels[i].isHovered(click.x(), click.y())) {
        handlePanelClick(i);
        return true;
    }
}
```

**Suggested fix:** Add `setOnClick(Runnable)` to `UIComponent` base class, making any component natively clickable:
```java
plotCell.setOnClick(() -> selectPlot(index));
```

---

## 3. Quality-of-Life Improvements

### 3.1 — Sound Manager

**Current pain point:** Every mod must manually play UI sounds:
```java
// Verbose and error-prone
MinecraftClient c = MinecraftClient.getInstance();
if (c.player != null) {
    c.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), volume, pitch);
}
```
You also need to know which `SoundEvent`s are `RegistryEntry<SoundEvent>` (needing `.value()`) and which are plain `SoundEvent` (must NOT call `.value()`). This is a trap:

```java
// SoundEvents.UI_BUTTON_CLICK → RegistryEntry<SoundEvent> → .value() required
SoundEvents.UI_BUTTON_CLICK.value()  // ✅ Works

// SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP → plain SoundEvent → .value() crashes
SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP.value()  // ❌ NoSuchMethodError
SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP  // ✅ Direct usage
```

**Suggestion:** Add `UISoundManager`:
```java
public final class UISoundManager {
    public static void playClick()                    { play(1.0f, 0.4f); }
    public static void playClick(float pitch, float volume) { ... }
    public static void playSuccess()                  { play(1.5f, 0.6f); }
    public static void playError()                    { play(0.3f, 0.5f); }
    public static void playCustom(SoundEvent event, float pitch, float volume) { ... }
    
    // Handles both RegistryEntry<SoundEvent> and plain SoundEvent
    // Null-safe on player reference
}
```

### 3.2 — Controller Rumble/Haptic API

**Current pain point:** `ControllerHandler` exposes only: `enable()`, `disable()`, `setScrollTarget(DarkPanel)`, `isActive()`, `tick()`.

There is NO way to trigger controller vibration/rumble from mod code. We had to implement screen shake as a visual substitute:

```java
private void triggerHaptic(float intensity) {
    AnimationTicker.getInstance().start("shake",
            Math.min(intensity, 6f), 0f, 250, EasingType.EASE_OUT);
}
```

And then apply it in render() via JOML matrix translation — which requires knowledge of `Matrix3x2fStack` and manual mouse coordinate adjustment.

**Suggestion:** Add rumble methods to `ControllerHandler`:
```java
public void rumble(float intensity, int durationMs);
public void rumblePattern(float[] intensities, int[] durations);
public boolean isRumbleSupported();
```
These should delegate to Controlify's `RumbleEffect` API when present and no-op otherwise.

### 3.3 — Built-in Screen Shake Utility

**Current pain point:** Implementing screen shake requires:
1. Starting an animation for intensity
2. Computing sinusoidal offsets from `System.currentTimeMillis()`
3. Using `context.getMatrices().pushMatrix()` / `.translate(float, float)` / `.popMatrix()`
4. Adjusting all mouse coordinates by the shake offset
5. Popping the matrix before tooltips/vanilla rendering

This is ~25 lines of boilerplate that every mod wanting screen shake must reimplement.

**Suggestion:** Add to `AnimationTicker` or as a standalone utility:
```java
AnimationTicker.screenShake(float intensity, int durationMs);  // starts shake
AnimationTicker.getShakeOffset();  // returns {offsetX, offsetY} for render integration

// Or better: automatic integration via a render wrapper
ProceduralRenderer.beginShake(DrawContext context);
// ... render UI ...
ProceduralRenderer.endShake(DrawContext context);
```

### 3.4 — Floating Text / Toast Component

**Current pain point:** Implementing floating "+15g" popups requires:
- Manual `AnimationTicker` for position/alpha
- Manual `ProceduralRenderer.drawScaledCenteredText()` in render
- Manual alpha computation from animation progress
- Manual floating Y offset

We used ~15 lines of rendering code per popup type.

**Suggestion:** Add a `FloatingText` component:
```java
FloatingText.show(parentComponent, x, y, "+15g", GOLD_COLOR, 800, Direction.UP);
```

### 3.5 — Color Utility Expansion

**Current pain point:** We needed to darken colors for button states and had to write our own helper:
```java
private static int darken(int color, float factor) {
    int a = (color >> 24) & 0xFF;
    int r = (int) (((color >> 16) & 0xFF) * factor);
    int g = (int) (((color >>  8) & 0xFF) * factor);
    int b = (int) ((color & 0xFF) * factor);
    return (a << 24) | (r << 16) | (g << 8) | b;
}
```

`ProceduralRenderer` already has `withAlpha()` and `lerpColor()` but is missing:

**Suggestion:** Add to `ProceduralRenderer`:
```java
public static int darken(int argb, float factor);     // 0.0=black, 1.0=original
public static int lighten(int argb, float factor);    // 0.0=original, 1.0=white
public static int saturate(int argb, float factor);   // 0.0=grayscale, 1.0=original
public static int setAlpha(int argb, float alpha01);  // alternative to withAlpha(int)
```

### 3.6 — Built-in State Persistence Utility

**Current pain point:** PocketUICore has no built-in way to persist UI state. We wrote ~130 lines of hand-rolled JSON serialization (`saveToFile()`, `loadFromFile()`, `readInt()`, `readLong()`, `readBool()`, `readIntArray()`, `readFloatArray()`, `readBoolArray()`, `arrToStr()`, `fArrToStr()`, `bArrToStr()`).

Every mod using PocketUICore that needs to save configuration or state must reimplement this from scratch.

**Suggestion:** Add `PocketUIState` utility:
```java
public final class PocketUIState {
    public static void save(String namespace, Map<String, Object> data);
    public static Map<String, Object> load(String namespace);
    public static boolean exists(String namespace);
    public static void delete(String namespace);
}
// Saves to <gameDir>/pocketuicore/<namespace>.json
```

Or for simpler cases:
```java
PocketUIState.saveInt("farm", "gold", 50);
int gold = PocketUIState.loadInt("farm", "gold", 50); // 50 = default
```

### 3.7 — Mode-Switching Helper

**Current pain point:** Building multi-mode UIs (normal mode, picker mode, shop mode) requires toggling visibility AND enabled state AND FocusManager registration for every button in both modes. Our `setCropPickerMode()` and `setShopMode()` methods are ~35 lines each of repetitive toggle logic:

```java
// Must do ALL of these for EVERY button, BOTH modes:
plantBtn.setVisible(!shopping);
plantBtn.setEnabled(!shopping);
// ... repeat for every action button ...
fertShopBtn.setVisible(shopping);
fertShopBtn.setEnabled(shopping);
// ... repeat for every shop button ...
FocusManager fm = FocusManager.getInstance();
fm.clear();
if (shopping) {
    fm.register(fertShopBtn);
    // ... register each shop button ...
} else {
    fm.register(plantBtn);
    // ... register each action button ...
}
fm.focusFirst();
```

**Suggestion:** Add a `ButtonGroup` or `ModeManager` utility:
```java
ButtonGroup normalMode = new ButtonGroup(plantBtn, waterBtn, harvestBtn, closeBtn);
ButtonGroup shopMode = new ButtonGroup(fertBtn, autoWaterBtn, magnetBtn, cancelBtn);

ModeManager modes = new ModeManager(normalMode, shopMode);
modes.activate(shopMode);  // hides normal, shows shop, updates FocusManager
```

---

## 4. Architectural Concerns

### 4.1 — FocusManager Global Singleton

**Problem:** `FocusManager` is a singleton accessed via `FocusManager.getInstance()`. This means:
- Only one focus context can exist at a time
- Opening a nested dialog/overlay requires clearing and rebuilding the focus set
- Closing the inner dialog requires rebuilding the outer dialog's focus set
- Two mods cannot independently manage focus

**Impact:** Multi-screen workflows (confirmation dialogs, nested menus) require manual focus management:
```java
// Opening inner dialog:
fm.clear();
// register only dialog buttons
fm.register(yesBtn); fm.register(noBtn);

// Closing inner dialog — must re-register ALL parent buttons:
fm.clear();
fm.register(plantBtn); fm.register(waterBtn); // etc.
```

**Suggestion:** Support focus contexts:
```java
fm.pushContext("shop");      // saves current, creates new
fm.register(shopBtn1);       // only affects "shop" context
fm.popContext();             // restores parent context
```

### 4.2 — AnimationTicker Global String Keys

**Problem:** `AnimationTicker` uses string keys (`"gold_flash"`, `"shake"`, etc.) in a global namespace backed by `ConcurrentHashMap`. If two mods use the same animation key, they'll overwrite each other.

**Impact:** Key collisions will cause visual glitches with no error message. This is a ticking time bomb as more mods use PocketUICore.

**Suggestion:** Support namespaced animation keys:
```java
// Option A: Manual prefix convention (documented, not enforced)
anim.start("mymod:gold_flash", ...);

// Option B: Scoped animation context
AnimationContext ctx = anim.createContext("mymod");
ctx.start("gold_flash", ...);  // internally stored as "mymod:gold_flash"
float val = ctx.get("gold_flash");
```

### 4.3 — ObservableState Memory Leak via map()

**Problem:** `ObservableState.map()` creates a derived observable that adds a listener to the source. There's no `unbind()` or `dispose()` method, so the listener chain can never be disconnected.

**Impact:** If you create mapped observables in `init()` without cleanup, each screen reopening adds another listener to the source. Over many open/close cycles, this accumulates dead listeners.

**Current workaround:** Call `clearListeners()` on source observables in `removed()`:
```java
@Override
public void removed() {
    goldState.clearListeners();
    seasonState.clearListeners();
    harvestCount.clearListeners();
}
```

**Suggestion:** Add dispose/unbind:
```java
ObservableState<String> derived = source.map(v -> v.toString());
// Later:
derived.dispose();  // removes listener from source
```

### 4.4 — Economy System Hardcoded Configuration

**Problem:** All economy parameters are hardcoded:
- Estate growth rate: `0.05%` per tick (can't change)
- Estate payout: `$50` per cycle (can't change)
- Caravan cost: 16 oak logs + 32 cobblestone + 4 raw iron + 8 bread (can't change)
- Caravan reward: `$200` + 50 XP (can't change)
- Sync interval: every 10 ticks (can't change)

**Impact:** Mods can't customize the economy. The fixed values may not fit every mod's game balance.

**Suggestion:** Add configuration API:
```java
EconomyConfig config = EconomyManager.getConfig();
config.setEstateGrowthRate(0.1f);
config.setEstatePayout(100);
config.setSyncInterval(5);
```
Or better, use a config file that loads on startup.

### 4.5 — Estate Manager Not Persistent

**Problem:** `EstateManager` stores growth data in-memory only. Server restart or player disconnect resets all estate growth progress.

**Suggestion:** Persist estate data alongside `PlayerVaultState` using the same `PersistentState` pattern.

---

## 5. Component-by-Component Review

### UIComponent (Base Class)

| Aspect | Status | Notes |
|--------|--------|-------|
| Positioning | ⚠️ Absolute only | Most impactful improvement would be relative positioning |
| Visibility | ✅ Works well | `setVisible()` / `isVisible()` |
| Enabled | ⚠️ Functional only | No visual disabled state (graying, dimming) |
| Tooltips | ✅ Good | Multi-line, 500ms delay, auto-positioned |
| Children | ✅ Good | `addChild()`, `removeChild()`, `clearChildren()`, `getChildren()` |
| Bounds | ✅ Good | `setPosition()`, `setSize()`, `setBounds()` |
| Input | ⚠️ Limited | Click/release/scroll only — no drag, keyboard, charTyped |

### DarkPanel

| Aspect | Status | Notes |
|--------|--------|-------|
| Rendering | ✅ Excellent | Rounded corners, border, shadow, gradient possible |
| Scrolling | ✅ Good | Vertical scroll with scissor clipping |
| Customization | ✅ Good | Background color, border color, corner radius, shadow |
| Click handling | ⚠️ Issue | `blockInputInBounds=true` eats all clicks (see §1.2) |
| Missing getters | ⚠️ Issue | No getters for bg color, border, radius, etc. (see §2.3) |

### TextLabel

| Aspect | Status | Notes |
|--------|--------|-------|
| Text rendering | ✅ Good | Left/Center/Right align, custom scale |
| Mutable | ✅ Good | `setText()`, `setColor()`, `setScale()`, `setAlign()` |
| Limitations | ⚠️ Minor | Single-line only, plain String only (no Minecraft `Text`/formatting) |

### HoverButton

| Aspect | Status | Notes |
|--------|--------|-------|
| Interaction | ✅ Excellent | Smooth hover lerp, press state, focus ring |
| Customization | ✅ Good | Colors, corner radius, tooltip |
| Mutable label | ⚠️ Confusing | Uses `setLabel()` not `setText()` — inconsistent with TextLabel |
| Disabled state | ⚠️ Poor | Only reduces alpha — no gray-out or visual distinction |
| Sound | ⚠️ Not configurable | Always plays vanilla button click on release |
| Missing getters | ⚠️ Issue | No getters for any color property |

### PercentageBar

| Aspect | Status | Notes |
|--------|--------|-------|
| Animation | ✅ Excellent | Smooth easing, configurable speed |
| Snap | ✅ Good | `snapTo()` bypasses animation for instant updates |
| Health colors | ✅ Useful | `applyHealthColors()` auto-colorizes by progress |
| Missing getters | ⚠️ Issue | Can't read back bar color, label, etc. |

### GridPanel

| Aspect | Status | Notes |
|--------|--------|-------|
| Layout | ✅ Functional | Auto-positions children in grid |
| Manual trigger | ⚠️ Inconvenient | Must call `layout()` after any change |
| Height handling | ⚠️ Quirk | Sets child width but NOT height |
| Auto-scroll | ✅ Good | Enables scroll when content exceeds height |

### VerticalListPanel

| Aspect | Status | Notes |
|--------|--------|-------|
| Layout | ✅ Good | Auto-positions with padding and spacing |
| Stretch | ✅ Good | `stretchWidth` option fills panel width |
| Manual trigger | ⚠️ Inconvenient | Must call `layout()` after any change |

### FocusManager

| Aspect | Status | Notes |
|--------|--------|-------|
| Navigation | ✅ Excellent | Tab, arrows, spatial direction |
| Activation | ✅ Good | `activateFocused()` synthesizes click |
| Listeners | ✅ Good | `FocusChangeListener` for audio cues |
| Architecture | ⚠️ Singleton | No context stacking for nested UIs (see §4.1) |
| Spatial nav | ⚠️ Quirk | Diagonal targets can be unreachable due to half-plane filter |

### AnimationTicker

| Aspect | Status | Notes |
|--------|--------|-------|
| Precision | ✅ Excellent | nanoTime-based, sub-tick smooth |
| Easing types | ✅ Good | 6 types: LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, EASE_OUT_BACK, EASE_IN_OUT_SINE |
| Static helpers | ✅ Useful | `lerp()`, `smoothDamp()`, `applyEasing()` |
| Thread safety | ✅ Good | ConcurrentHashMap internally |
| Key collisions | ⚠️ Risk | Global string namespace, no mod isolation (see §4.2) |

### HudOverlayComponent

| Aspect | Status | Notes |
|--------|--------|-------|
| Anchoring | ✅ Good | 4 anchor points, custom margins |
| Durability display | ✅ Useful | Built-in progress bar |
| Tier display | ✅ Good | Customizable tier label with color |
| Auto-hide | ✅ Smart | Hidden when any Screen is open |
| Cleanup | ⚠️ Manual | Must call `remove()` or overlays leak |

### ProceduralRenderer

| Aspect | Status | Notes |
|--------|--------|-------|
| Shape rendering | ✅ Excellent | Rounded rects, borders, gradients, shadows |
| Text rendering | ✅ Good | Scaled text, centered text, `Text` object support |
| Caching | ✅ Good | `CachedShape` / `bakePanel()` for VBO-style optimization |
| Color utilities | ✅ Good | `withAlpha()`, `lerpColor()`, `hex()` |
| Missing utilities | ⚠️ Minor | No `darken()`, `lighten()`, `saturate()` (see §3.5) |
| Performance | ⚠️ Quirk | `fillGradientH` draws 1px columns — slow for wide areas |

### ObservableState

| Aspect | Status | Notes |
|--------|--------|-------|
| Reactivity | ✅ Good | Listener-based, change detection via `equals()` |
| Binding | ✅ Useful | `bindProgress()`, `bindText()` convenience methods |
| Mapping | ⚠️ Leak risk | `map()` creates non-disposable listener chain (see §4.3) |
| Missing | ⚠️ Minor | No `combine()` / `biMap()` for multi-source derivation |

---

## 6. Usage Guide & Best Practices

### 6.1 — Basic Setup

**Project structure:**
```
MyMod/
├── build.gradle          # Include PocketUICore JAR
├── libs/
│   └── pocketuicore-1.6.0.jar
└── src/main/java/com/mymod/
    ├── MyModClient.java   # ClientModInitializer
    └── gui/
        └── MyScreen.java  # Your custom screen
```

**build.gradle dependency:**
```groovy
dependencies {
    modImplementation files("libs/pocketuicore-1.6.0.jar")
}
```

### 6.2 — Screen Boilerplate (MC 1.21.11)

```java
public class MyScreen extends Screen {
    private DarkPanel root;
    private int px, py; // panel origin

    public MyScreen() {
        super(Text.literal("My Screen"));
    }

    @Override
    protected void init() {
        super.init();
        
        int PW = 300, PH = 200;
        px = (this.width - PW) / 2;
        py = (this.height - PH) / 2;

        FocusManager fm = FocusManager.getInstance();
        fm.clear();
        fm.clearFocusChangeListeners();

        // Root panel
        root = new DarkPanel(px, py, PW, PH);
        root.setBackgroundColor(0xE60D1117);
        root.setBorderColor(0xFF1B2028);
        root.setCornerRadius(8);
        root.setDrawBorder(true);
        root.setDrawShadow(true);

        // Add children using ABSOLUTE coordinates
        int PAD = 10;
        root.addChild(new TextLabel(
            px + PAD, py + 8, PW - 2*PAD, 14,
            "Title", 0xFFE6EDF3, TextLabel.Align.CENTER, 1.0f));

        HoverButton btn = new HoverButton(
            px + PAD, py + 30, 100, 20,
            "Click Me", () -> System.out.println("Clicked!"),
            0xFF238636, 0xFF2EA043, 0xFF196C2E, 0xFFE6EDF3, 4);
        root.addChild(btn);
        fm.register(btn);
        
        fm.focusFirst();
    }

    // CRITICAL: Override renderBackground to prevent blur crash
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ProceduralRenderer.drawFullScreenOverlay(context, this.width, this.height,
                ProceduralRenderer.COL_OVERLAY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if (root != null) root.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        if (root != null) UIComponent.renderTooltip(context, root, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean fromKeyboard) {
        if (root != null && root.mouseClicked(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, fromKeyboard);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (root != null && root.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        if (root != null && root.mouseScrolled(mouseX, mouseY, hAmt, vAmt)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        FocusManager fm = FocusManager.getInstance();
        int key = keyInput.key();
        
        // Tab navigation
        if (key == 258) {
            if ((keyInput.modifiers() & 1) != 0) fm.navigatePrevious();
            else fm.navigateNext();
            return true;
        }
        // Enter/Space activate
        if (key == 257 || key == 32) {
            if (fm.activateFocused()) return true;
        }
        // Escape close
        if (key == 256) { this.close(); return true; }
        
        return super.keyPressed(keyInput);
    }

    @Override
    public void removed() {
        FocusManager fm = FocusManager.getInstance();
        fm.clear();
        fm.clearFocusChangeListeners();
        AnimationTicker.getInstance().cancelAll();
        super.removed();
    }
}
```

### 6.3 — Input Signature Gotchas (MC 1.21.11)

MC 1.21.11 uses wrapper records for input, not raw parameters:

| Method | 1.21.11 Signature | Gets raw values from |
|--------|-------------------|---------------------|
| `mouseClicked` | `(Click click, boolean fromKeyboard)` | `click.x()`, `click.y()`, `click.button()` |
| `mouseReleased` | `(Click click)` | `click.x()`, `click.y()`, `click.button()` |
| `keyPressed` | `(KeyInput keyInput)` | `keyInput.key()`, `keyInput.scancode()`, `keyInput.modifiers()` |
| `mouseScrolled` | `(double, double, double, double)` | Same as before |

PocketUICore's `UIComponent` still uses old-style `(double, double, int)` for mouse methods. You must unwrap the `Click` record:
```java
root.mouseClicked(click.x(), click.y(), click.button());
```

### 6.4 — Matrix API (MC 1.21.11)

`DrawContext.getMatrices()` returns `org.joml.Matrix3x2fStack` (NOT the old `MatrixStack`):

```java
// CORRECT (1.21.11):
context.getMatrices().pushMatrix();
context.getMatrices().translate(offsetX, offsetY);
// ... render ...
context.getMatrices().popMatrix();

// WRONG (old MC):
context.getMatrices().push();
context.getMatrices().translate(offsetX, offsetY, 0f);
context.getMatrices().pop();
```

### 6.5 — FocusManager Lifecycle Rules

1. **Always `clear()` in `init()`** — Screen can be re-initialized (window resize)
2. **Always `clear()` in `removed()`** — Prevent stale references
3. **Always `clearFocusChangeListeners()` in both** — Listeners aren't cleaned by `clear()`
4. **Re-register on mode changes** — When toggling button sets (picker/shop mode), do:
   ```java
   fm.clear();
   // register only the buttons for the new mode
   fm.register(btn1); fm.register(btn2);
   fm.focusFirst();
   ```
5. **Call `focusFirst()` after registration** — Sets initial focus target

### 6.6 — AnimationTicker Best Practices

```java
AnimationTicker anim = AnimationTicker.getInstance();

// Start animation (from, to, duration, easing)
anim.start("my_anim", 0f, 1f, 500, EasingType.EASE_OUT);

// Read in render()
float progress = anim.get("my_anim", 0f);     // linear interpolated value
float clamped = anim.get01("my_anim");          // clamped 0-1
boolean running = anim.isActive("my_anim");

// Cancel
anim.cancel("my_anim");
anim.cancelAll();  // use in removed()

// IMPORTANT: Completed animations survive one extra tick before removal.
// This guarantees the final value (1.0) is readable for at least one render frame.
```

Use descriptive, unique key names. Convention: `"modid_animname"` or `"animname_instanceId"`:
```java
anim.start("farm_cell_flash_" + plotIndex, ...);
```

### 6.7 — Sound Playback Patterns

Different pitch × volume combinations create distinct audio profiles with the same SoundEvent:

| Sound Feel | Pitch | Volume | Usage |
|------------|-------|--------|-------|
| Soft click | 1.0f | 0.25f | Selection, focus change |
| Sprout (double) | 1.4f + 1.8f | 0.5f / 0.3f | Planting |
| Deep thud | 0.5f + 0.7f | 0.5f / 0.3f | Watering |
| Celebration | 1.5f + 1.8f + 2.0f | 0.6f / 0.4f / 0.3f | Harvesting |
| Warning | 0.3f | 0.5f | Error |
| Gong | 0.4f + 0.6f | 0.6f / 0.4f | Season change |
| Bright ding | 1.6f + 2.0f | 0.4f / 0.2f | Crop ready |
| Nearly silent | 1.0f | 0.15f | Focus navigation |
| Boundary thud | 0.3f | 0.2f | Edge of grid |

Playing multiple sounds at slightly different pitches in quick succession creates richer audio:
```java
playClick(1.5f, 0.6f);
playClick(1.8f, 0.4f);
playClick(2.0f, 0.3f);  // 3-note cascade
```

### 6.8 — Tooltip Usage

```java
// Single-line tooltip
button.setTooltip("Click to plant");

// Multi-line tooltip  
plotCell.setTooltip(
    "Plot 3: Melon",
    "Growth: 67%",
    "☂ Watered (2× growth)",
    "Reward: 16-28g"
);

// Render tooltips at the END of render(), AFTER all components
UIComponent.renderTooltip(context, root, mouseX, mouseY);

// Note: Tooltip has a 500ms hover delay (not configurable)
```

### 6.9 — Blur Crash Prevention

MC 1.21.11's `Screen.renderBackground()` calls `applyBlur()` which crashes if called twice per frame. Always override:

```java
@Override
public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    // Replace blur with simple overlay
    ProceduralRenderer.drawFullScreenOverlay(context, this.width, this.height,
            ProceduralRenderer.COL_OVERLAY);
}
```

---

## 7. Workarounds Reference

Quick reference for issues and their workarounds:

| Issue | Workaround |
|-------|-----------|
| Panel eats child clicks | Check children BEFORE `root.mouseClicked()` |
| No `setText()` on HoverButton | Use `setLabel()` instead |
| No controller rumble | Screen shake via `AnimationTicker` + matrix translate |
| No relative positioning | Compute `px + offset` for every component |
| No disabled visual | Use `setEnabled(false)` + set gray tooltip manually |
| No onClick on DarkPanel | Check `isHovered()` manually in `mouseClicked()` |
| `SoundEvent` vs `RegistryEntry<SoundEvent>` | Check type — use `.value()` only on RegistryEntry |
| ObservableState listener leak | Call `clearListeners()` in `removed()` |
| FocusManager stale state | Call `clear()` + `clearFocusChangeListeners()` in `init()` and `removed()` |
| Animation key collisions | Use `"modid_keyname"` convention |
| GridPanel not updating | Call `layout()` after adding/removing children |
| Matrix API changed | Use `pushMatrix()`/`popMatrix()`, NOT `push()`/`pop()` |
| Blur crash | Override `renderBackground()` with `drawFullScreenOverlay()` |
| Economy payload crash | Use `Identifier.of("ns", "path")` not `CustomPayload.id("ns:path")` |
| `PrestigeConfirmationPanel.center()` drift | Only call once, or reset positions before re-centering |
| HUD hidden during Screen | Expected behavior — overlays auto-hide when Screens are open |

---

## 8. Feature Requests (Prioritized)

### 🔴 Priority 1 — Critical / High Impact

| # | Feature | Justification |
|---|---------|---------------|
| 1 | Relative positioning mode | Eliminates most verbose boilerplate in every mod |
| 2 | Fix payload identifier bug | Prevents crash on load |
| 3 | DarkPanel click delegation fix | Breaks any click-to-select pattern |
| 4 | Add missing getters | Can't read component state at runtime |
| 5 | `keyPressed` / `charTyped` forwarding | Needed for text input, per-component hotkeys |

### 🟡 Priority 2 — Quality of Life

| # | Feature | Justification |
|---|---------|---------------|
| 6 | TextInputComponent | Common UI need, currently impossible |
| 7 | `setOnClick()` on UIComponent | Makes any component clickable without workarounds |
| 8 | UISoundManager | Removes 5+ lines of boilerplate per sound, handles type safety |
| 9 | Disabled button visual state | Players can't tell if buttons are disabled |
| 10 | Auto-`layout()` on child changes | `GridPanel`/`VerticalListPanel` require manual calls |
| 11 | Color utilities (darken/lighten) | Common need, currently requires manual bit math |
| 12 | FocusManager context stacking | Needed for dialogs, overlays, nested menus |

### 🟢 Priority 3 — Nice to Have

| # | Feature | Justification |
|---|---------|---------------|
| 13 | Screen shake utility | ~25 lines of boilerplate reduced to 1 call |
| 14 | FloatingText / Toast component | Common UX pattern, currently ~15 lines per popup |
| 15 | Controller rumble API | Proper haptic feedback when using gamepad |
| 16 | State persistence utility | Every mod reimplements JSON save/load |
| 17 | Mode/ButtonGroup manager | Reduces 35-line mode toggles to 1 line |
| 18 | AnimationTicker namespacing | Prevents inter-mod key collisions |
| 19 | Economy configuration API | Hardcoded values limit flexibility |
| 20 | Estate persistence | Growth resets on restart — should be persistent |
| 21 | Horizontal scrolling | Only vertical currently supported |
| 22 | Minecraft `Text` support in TextLabel | Currently plain String only |

---

## 9. Full API Surface Reference

### Component Hierarchy
```
UIComponent (abstract)
├── DarkPanel
│   ├── GridPanel
│   ├── VerticalListPanel
│   └── (PrestigeConfirmationPanel — prefab)
├── TextLabel
├── HoverButton
├── PercentageBar
├── HudOverlayComponent
└── ItemDisplayComponent
```

### Static Utilities
```
ProceduralRenderer    — shape rendering, text, colors, caching
AnimationTicker       — named animations with easing (singleton)
FocusManager          — keyboard/gamepad navigation (singleton)
ControllerHandler     — native gamepad support (singleton)
PocketNotifier        — server-side notifications (static)
EconomyManager        — server-side economy (static)
EstateManager         — server-side estate growth (static)
CaravanManager        — server-side caravan dispatching (static)
PocketCommandRegister — client command registration (static)
```

### Key Constructors

```java
// Panel
new DarkPanel(x, y, width, height)
new DarkPanel(x, y, w, h, bgColor, borderColor, cornerRadius, drawBorder, drawShadow)

// Text
new TextLabel(x, y, width, height, text)
new TextLabel(x, y, width, height, text, color, align, scale)

// Button
new HoverButton(x, y, width, height, label, onClick)
new HoverButton(x, y, w, h, label, onClick, normal, hover, pressed, textColor, cornerRadius)

// Progress bar
new PercentageBar(x, y, width, height, initialProgress)
new PercentageBar(x, y, w, h, progress, trackColor, barColor, textColor, radius, label, showPct)

// Layout panels
new GridPanel(x, y, width, height, columns, padding)
new VerticalListPanel(x, y, width, height, padding, spacing)

// HUD
new HudOverlayComponent(anchor, marginX, marginY)

// Item display
new ItemDisplayComponent(x, y, itemStack)
new ItemDisplayComponent(x, y, width, height, itemStack)
```

### Animation Easing Types
```
LINEAR          — constant speed
EASE_IN         — starts slow, accelerates
EASE_OUT        — starts fast, decelerates
EASE_IN_OUT     — slow-fast-slow
EASE_OUT_BACK   — overshoots then settles (bounce effect)
EASE_IN_OUT_SINE — smooth sine wave
```

### ProceduralRenderer Color Palette
```
COL_BG_PRIMARY   = 0xFF1A1A2E   (very dark blue-purple)
COL_BG_SURFACE   = 0xFF16213E   (deep navy)
COL_BG_ELEVATED  = 0xFF0F3460   (medium navy)
COL_ACCENT       = 0xFFE94560   (red-pink)
COL_ACCENT_TEAL  = 0xFF00D2FF   (teal)
COL_SUCCESS      = 0xFF2ECC71   (green)
COL_WARNING      = 0xFFF39C12   (amber)
COL_ERROR        = 0xFFE74C3C   (red)
COL_TEXT_PRIMARY  = 0xFFEAEAEA   (light grey)
COL_TEXT_MUTED   = 0xFFA0A0B0   (muted)
COL_BORDER       = 0xFF2A2A4A   (subtle purple-ish)
COL_HOVER        = 0xFF1F4068   (hover tint)
COL_OVERLAY      = 0xC0000000   (75% black)
COL_SHADOW_BASE  = 0x00000000   (transparent)
```

---

*End of PocketUICore Improvements Document*
