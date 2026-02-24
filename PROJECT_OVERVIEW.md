# 🎨 PocketUICore 1.0.0 — Complete Project Overview

> **Minecraft 1.21.11** · Fabric API 0.141.3 · Java 21 · Gradle 9.3.1 + Loom 1.15  
> **Output:** `pocketuicore-1.0.0.jar` (24.7 KB) + sources JAR (19.9 KB)

---

## 📂 Project Structure

```
Depend/
├── build.gradle                          # Fabric Loom, deps, publishing
├── gradle.properties                     # MC 1.21.11 versions & mod metadata
├── settings.gradle                       # Plugin repos (Fabric Maven, Central)
├── LICENSE                               # MIT License
│
└── src/main/
    ├── resources/
    │   ├── fabric.mod.json               # Mod descriptor & entrypoints
    │   └── assets/pocketuicore/
    │       └── lang/en_us.json           # English translations
    │
    └── java/com/pocketuicore/
        ├── PocketUICore.java             # Server entrypoint
        ├── PocketUICoreClient.java       # Client entrypoint
        │
        ├── render/
        │   └── ProceduralRenderer.java   # Module 1 — Render Engine
        │
        ├── component/
        │   ├── UIComponent.java          # Module 2 — Base component
        │   ├── DarkPanel.java            # Module 2 — Container panel
        │   ├── HoverButton.java          # Module 2 — Interactive button
        │   └── PercentageBar.java        # Module 2 — Progress bar
        │
        ├── notification/
        │   └── PocketNotifier.java       # Module 3 — Notification Manager
        │
        └── animation/
            └── AnimationTicker.java      # Module 4 — Animation Engine
```

---

## 🔧 Build Scaffold

### `gradle.properties`
| Property | Value |
|----------|-------|
| `mod_version` | `1.0.0` |
| `maven_group` | `com.pocketuicore` |
| `minecraft_version` | `1.21.11` |
| `yarn_mappings` | `1.21.11+build.4` |
| `loader_version` | `0.18.4` |
| `loom_version` | `1.15-SNAPSHOT` |
| `fabric_api_version` | `0.141.3+1.21.11` |

### `build.gradle`
- Fabric Loom plugin + Maven Publish
- Dependencies: MC, Yarn mappings, Fabric Loader, Fabric API
- Java 21 source/target, UTF-8 encoding
- Bundles LICENSE into JAR
- `publishToMavenLocal` for dependent mods

### `fabric.mod.json`
- ID: `pocketuicore`
- Entrypoints: `main` → `PocketUICore`, `client` → `PocketUICoreClient`
- Depends: Fabric Loader ≥0.18, MC ≥1.21.11, Java ≥21, Fabric API

---

## 🚪 Entrypoints

### `PocketUICore.java` — Server Entrypoint (23 lines)
```
Implements: ModInitializer
Exposes:    MOD_ID ("pocketuicore"), LOGGER (SLF4J)
Action:     Logs initialization message
```

### `PocketUICoreClient.java` — Client Entrypoint (29 lines)
```
Implements: ClientModInitializer
Action:     Registers END_CLIENT_TICK callback
            → Pumps AnimationTicker.getInstance().tick()
            → Runs unconditionally (menus + in-world)
```

---

## 🖌️ Module 1 — Procedural Render Engine

> **File:** `render/ProceduralRenderer.java` · **360 lines**  
> Static utility class — all rendering via `DrawContext`, zero textures.

### Dark-Mode Palette (14 Constants)

```
COL_BG_PRIMARY .... #1A1A2E   Very dark blue-purple background
COL_BG_SURFACE .... #16213E   Deep navy panel fill
COL_BG_ELEVATED ... #0F3460   Medium navy elevated surfaces
COL_ACCENT ........ #E94560   Vibrant red-pink accent
COL_ACCENT_TEAL ... #00D2FF   Teal for positive actions
COL_SUCCESS ....... #2ECC71   Green
COL_WARNING ....... #F39C12   Amber
COL_ERROR ......... #E74C3C   Red
COL_TEXT_PRIMARY .. #EAEAEA   Primary text
COL_TEXT_MUTED .... #A0A0B0   Secondary/muted text
COL_BORDER ........ #2A2A4A   Subtle borders/dividers
COL_HOVER ......... #1F4068   Hover highlight tint
COL_OVERLAY ....... C0000000  75% black overlay
COL_SHADOW_BASE ... 00000000  Shadow base (alpha varies)
```

### Colour Helpers (3 methods)

| Method | Description |
|--------|-------------|
| `hex(String)` | Parse `"#RRGGBB"` or `"#AARRGGBB"` → ARGB int |
| `withAlpha(int color, int alpha)` | Replace alpha channel (0–255) |
| `lerpColor(int from, int to, float t)` | Per-channel ARGB linear interpolation |

### Shape Drawing (5 methods)

| Method | Description |
|--------|-------------|
| `fillRect(ctx, x, y, w, h, color)` | Solid filled rectangle |
| `drawBorder(ctx, x, y, w, h, color)` | 1px outline rectangle |
| `fillRoundedRect(ctx, x, y, w, h, radius, color)` | Rounded rect via scanline arcs (3 body rects + 4×radius scanlines) |
| `fillRoundedRectWithBorder(ctx, ...)` | Rounded fill + border combo |
| `drawRoundedBorder(ctx, x, y, w, h, radius, color)` | 1px rounded border only |

### Gradients (2 methods)

| Method | Description |
|--------|-------------|
| `fillGradientV(ctx, x, y, w, h, top, bottom)` | Vertical gradient (native `fillGradient`) |
| `fillGradientH(ctx, x, y, w, h, left, right)` | Horizontal gradient (column-by-column lerp) |

### Drop Shadow (2 methods)

| Method | Description |
|--------|-------------|
| `drawDropShadow(ctx, ..., layers, maxAlpha)` | Configurable — concentric rounded rects with quadratic alpha falloff |
| `drawDropShadow(ctx, ..., radius)` | Convenience — 6 layers, 40% peak alpha |

### Text Rendering (5 methods)

| Method | Description |
|--------|-------------|
| `drawText(ctx, tr, string, x, y, color)` | Left-aligned with shadow |
| `drawCenteredText(ctx, tr, string, cx, y, color)` | Centre-aligned with shadow |
| `drawScaledText(ctx, tr, string, x, y, color, scale)` | Scaled via Matrix3x2fStack push/translate/scale/pop |
| `drawScaledCenteredText(ctx, tr, string, cx, y, color, scale)` | Scaled + centred |
| `drawScaledText(ctx, tr, Text, x, y, color, scale)` | Scaled text accepting `Text` objects |

### Utility (2 methods)

| Method | Description |
|--------|-------------|
| `drawFullScreenOverlay(ctx, w, h, color)` | Full-screen fill for modal backdrops |
| `drawDivider(ctx, x, y, width, color)` | 1px horizontal line |

---

## 🧩 Module 2 — Component Tree API

### `UIComponent.java` — Abstract Base (170 lines)

```
Fields:
  x, y, width, height       Absolute screen-pixel geometry
  visible, enabled           State toggles
  parent, children           Tree structure (ArrayList-backed)

Rendering:
  render(ctx, mouseX, mouseY, delta)     Draws self → then children
  renderSelf(ctx, ...)                   Abstract — subclass override

Input Forwarding (back-to-front child iteration):
  mouseClicked(mouseX, mouseY, button)   → returns true if consumed
  mouseReleased(mouseX, mouseY, button)
  mouseScrolled(mouseX, mouseY, hAmount, vAmount)

Hit Testing:
  isHovered(int mouseX, int mouseY)      Point-in-rect check
  isHovered(double, double)              Overload for double coords

Tree Manipulation:
  addChild(child)            Sets parent, appends to list
  removeChild(child)         Removes from list, clears parent
  clearChildren()            Removes all, clears parent refs
  getChildren()              Unmodifiable List view

Accessors:
  get/setPosition, get/setSize, setBounds
  isVisible / setVisible
  isEnabled / setEnabled
```

---

### `DarkPanel.java` — Container Panel (154 lines)

```
Extends: UIComponent

Rendering Layers (bottom → top):
  1. Drop shadow     (optional, configurable layers + alpha)
  2. Background      (fillRoundedRect, configurable colour)
  3. Border          (optional, 1px rounded)
  4. Children        (with optional scroll support)

Scroll Support:
  When scrollable == true:
    → enableScissor clips to panel bounds
    → Matrix3x2fStack.translate applies scroll offset
    → mouseScrolled: 20px per notch, clamped to [0, contentHeight - height]

Defaults:
  Background:    COL_BG_SURFACE (#16213E)
  Border:        COL_BORDER (#2A2A4A)
  Corner radius: 6px
  Shadow:        ON, 6 layers, 40% alpha
  Border:        ON

Setters:
  setBackgroundColor(c)         setBorderColor(c)
  setCornerRadius(r)            setDrawBorder(bool)
  setDrawShadow(bool)           setShadow(layers, alpha)
  setScrollable(bool, height)   setScrollOffset(offset)
```

---

### `HoverButton.java` — Interactive Button (163 lines)

```
Extends: UIComponent

States: Normal → Hovered → Pressed
  Smooth colour blend via exponential lerp (0.25 decay factor)
  Frame-rate independent — works at any FPS

Rendering:
  Rounded rect with blended bg colour
  Centred label text (uses tr.fontHeight for vertical centering)

Input:
  mouseClicked  → sets pressed = true
  mouseReleased → fires onClick callback
                → plays PositionedSoundInstance.ui(UI_BUTTON_CLICK)

Disabled State:
  Alpha reduced to 100/255
  Text colour → COL_TEXT_MUTED

Defaults:
  Normal:   COL_BG_ELEVATED (#0F3460)
  Hover:    COL_HOVER (#1F4068)
  Pressed:  COL_ACCENT (#E94560)
  Text:     COL_TEXT_PRIMARY (#EAEAEA)
  Radius:   4px

Setters:
  setLabel(str)              setOnClick(Runnable)
  setNormalColor(c)          setHoverColor(c)
  setPressedColor(c)         setTextColor(c)
  setCornerRadius(r)
  isHoveredState()           isPressedState()
```

---

### `PercentageBar.java` — Progress Bar (163 lines)

```
Extends: UIComponent

Rendering Layers (bottom → top):
  1. Track         (rounded rect, dark background)
  2. Fill bar      (rounded rect, coloured, scissor-clipped)
  3. Label/text    (optional, centred — "73%" or custom label)

Animation:
  displayProgress eases toward targetProgress each frame
  easingSpeed configurable (default 6, higher = faster)
  snapTo(float) jumps immediately — useful for init

Health Colouring:
  applyHealthColors() auto-sets bar colour:
    > 50%  → COL_SUCCESS (green)
    > 25%  → COL_WARNING (amber)
    ≤ 25%  → COL_ERROR (red)

Defaults:
  Track:    COL_BG_PRIMARY (#1A1A2E)
  Bar:      COL_ACCENT_TEAL (#00D2FF)
  Text:     COL_TEXT_PRIMARY (#EAEAEA)
  Radius:   height/2 (pill shape)
  Shows:    Percentage text ON

Setters:
  setProgress(float)           snapTo(float)
  setBarColor(c)               setTrackColor(c)
  setTextColor(c)              setCornerRadius(r)
  setLabel(str)                setShowPercentage(bool)
  setEasingSpeed(float)
  getTargetProgress()          getDisplayProgress()
```

---

## 📢 Module 3 — Notification Manager

> **File:** `notification/PocketNotifier.java` · **157 lines**  
> Static server-side utility — all methods null-safe on player.

### Action-Bar Messages (4 methods)

| Method | Description |
|--------|-------------|
| `sendActionBar(player, Text)` | Raw Text overlay above hotbar |
| `sendActionBar(player, String)` | Plain string convenience |
| `sendActionBar(player, String, Formatting)` | Coloured string |
| `sendActionBarProgress(player, prefix, float)` | Auto-formats to `"Growing… 73%"` in aqua |

### Chat Reminders (3 methods)

| Method | Description |
|--------|-------------|
| `sendChatReminder(player, Text)` | Branded **[Pocket]** gold prefix + message |
| `sendChatReminder(player, String)` | Plain string → yellow text |
| `sendChatReminder(player, String, Formatting...)` | Custom formatting |

### Specialised Helpers (3 methods)

| Method | Channel | Description |
|--------|---------|-------------|
| `sendDurabilityAlert(player, remaining, max, itemName)` | Chat | `"⚠ Pickaxe durability: 12/1561"` — red ≤10%, yellow otherwise |
| `sendTierUpgrade(player, itemName, newTier)` | Both | Action bar flash + chat with green bold tier |
| `sendMilestone(player, milestone)` | Both | `"✦ First Diamond!"` — purple bold chat + action bar |

---

## 🎬 Module 4 — Animation Engine

> **File:** `animation/AnimationTicker.java` · **227 lines**  
> Singleton, `System.nanoTime()`-based, `ConcurrentHashMap`-backed.

### 6 Easing Curves

```
LINEAR ............. t                          Constant speed
EASE_IN ............ t³                         Slow start
EASE_OUT ........... 1 - (1-t)³                 Slow end
EASE_IN_OUT ........ cubic S-curve              Smooth both ends
EASE_OUT_BACK ...... overshoot + settle         Bouncy feel (c=1.70158)
EASE_IN_OUT_SINE ... -cos(πt)/2 + 0.5          Gentle sine wave
```

### Animation Lifecycle API (7 methods)

| Method | Description |
|--------|-------------|
| `start(id, from, to, durationMs, easing)` | Start/restart by key (replaces existing) |
| `start(id, from, to, durationMs)` | Convenience — defaults to EASE_IN_OUT |
| `get(id, defaultValue)` | Current eased value (real-time nanoTime eval) |
| `get(id)` | Default = 0 |
| `get01(id)` | Clamped to [0, 1] — great for alpha/progress |
| `isActive(id)` | True if still running |
| `exists(id)` | True if in map (running or completed) |

### Control (2 methods)

| Method | Description |
|--------|-------------|
| `cancel(id)` | Remove animation immediately |
| `cancelAll()` | Clear entire map |

### Tick Cleanup

```
tick() — called every client tick (~20 Hz)
  Two-phase cleanup:
    Phase 1: Animation completes → marked "readyToRemove"
    Phase 2: Next tick → actually removed from map
  This guarantees the final value is readable for at least
  one full render cycle, preventing 1-frame visual pops.
```

### Static Helpers (usable without animations)

| Method | Description |
|--------|-------------|
| `lerp(a, b, t)` | Standard linear interpolation |
| `smoothDamp(current, target, speed, delta)` | Exponential decay lerp (frame-rate independent) |
| `applyEasing(t, EasingType)` | Apply any easing curve to linear t |

---

## 📊 Project Stats

```
┌─────────────────────────┬────────┐
│  Java source files      │      9 │
│  Resource files         │      2 │
│  Build config files     │      3 │
│  Other (LICENSE)        │      1 │
├─────────────────────────┼────────┤
│  Total files            │     15 │
│  Total Java LOC         │ ~1,447 │
│  Public API methods     │    60+ │
│  External dependencies  │      0 │
│  Compiled JAR size      │ 24.7KB │
└─────────────────────────┴────────┘
```

---

## ✅ Bugs Fixed for 1.0.0 Stable

| # | Issue | Fix |
|---|-------|-----|
| 1 | AnimationTicker removed completed animations before final value could be rendered (1-frame pop) | Two-phase cleanup — animations survive one extra tick |
| 2 | AnimationTicker only ticked when `client.world != null` — menu animations leaked | Ticks unconditionally now |
| 3 | DarkPanel `setScrollable()` didn't clamp existing scroll offset | Added clamp on content height change |
| 4 | HoverButton used `delta` (partial ticks sawtooth) as frame time — jerky fade | Fixed decay factor `0.25f` (frame-rate independent) |
| 5 | HoverButton had unused `animId` field | Removed dead code |
| 6 | HoverButton & PercentageBar hardcoded font height as 8 (actual = 9) | Now uses `tr.fontHeight` |
| 7 | `fabric.mod.json` referenced nonexistent `icon.png` | Removed icon field |
| 8 | No LICENSE file despite `build.gradle` trying to bundle it | Created MIT LICENSE |

---

## 💡 How Dependent Mods Use This

```java
// In your mod's build.gradle:
repositories {
    mavenLocal()
}
dependencies {
    modImplementation "com.pocketuicore:pocketuicore:1.0.0"
}

// In your fabric.mod.json:
"depends": {
    "pocketuicore": ">=1.0.0"
}
```

### Quick Usage Examples

```java
// Render a dark panel with a button inside
DarkPanel panel = new DarkPanel(50, 50, 200, 150);
HoverButton btn = new HoverButton(70, 80, 160, 20, "Click Me", () -> {
    System.out.println("Clicked!");
});
panel.addChild(btn);
panel.render(drawContext, mouseX, mouseY, delta);

// Animate a slide-in
AnimationTicker.getInstance().start("slideIn", -200f, 0f, 300,
    AnimationTicker.EasingType.EASE_OUT);
float panelX = AnimationTicker.getInstance().get("slideIn");

// Send a notification
PocketNotifier.sendMilestone(player, "First Diamond!");

// Draw procedural shapes
ProceduralRenderer.fillRoundedRect(ctx, 10, 10, 100, 40, 6,
    ProceduralRenderer.COL_BG_ELEVATED);
```

---

*PocketUICore 1.0.0 — Zero external libraries. Procedural rendering only. Performance first.*
