# PocketUICore — Dependency Usage Guide

> **Version:** 1.13.0  
> **Minecraft:** 1.21.11 (Fabric)  
> **Java:** 21+  

This guide explains how to use PocketUICore as a dependency in your own Fabric mod. It covers setup, core concepts, and practical examples for every major system.

---

## Table of Contents

1. [Setting Up the Dependency](#1-setting-up-the-dependency)
2. [Project Structure Overview](#2-project-structure-overview)
3. [Creating a Screen](#3-creating-a-screen)
4. [Components](#4-components)
5. [Layout Panels](#5-layout-panels)
6. [Procedural Rendering](#6-procedural-rendering)
7. [Animations](#7-animations)
8. [Focus & Controller Navigation](#8-focus--controller-navigation)
9. [Controller Glyphs](#9-controller-glyphs)
10. [Notifications & Toasts](#10-notifications--toasts)
11. [Sound Effects](#11-sound-effects)
12. [Screen Shake](#12-screen-shake)
13. [Reactive State (ObservableState)](#13-reactive-state-observablestate)
14. [Model/View Architecture](#14-modelview-architecture)
15. [Input Handling](#15-input-handling)
16. [Theming](#16-theming)
17. [Anchor Layout](#17-anchor-layout)
18. [Tooltip System](#18-tooltip-system)
19. [Server-Side Notifications](#19-server-side-notifications)
20. [Economy System (Optional)](#20-economy-system-optional)
21. [Version Helper](#21-version-helper)
22. [Debug Overlay](#22-debug-overlay)
23. [Tips & Best Practices](#23-tips--best-practices)

---

## 1. Setting Up the Dependency

### Option A: Local Maven (recommended for development)

1. Clone or download PocketUICore and build it:
   ```bash
   cd PocketUICore
   ./gradlew publishToMavenLocal
   ```

2. In your mod's `build.gradle`, add the local maven repo and dependency:
   ```groovy
   repositories {
       mavenLocal()
   }
   
   dependencies {
       modImplementation "com.pocketuicore:pocketuicore:1.13.0"
   }
   ```

### Option B: JitPack (for published releases)

1. Add JitPack to your `build.gradle`:
   ```groovy
   repositories {
       maven { url 'https://jitpack.io' }
   }
   
   dependencies {
       modImplementation "com.github.Uipg9:PocketUICore:v1.13.0"
   }
   ```

### Option C: Jar-in-Jar (bundle with your mod)

Place the PocketUICore jar in your project's `libs/` folder:
```groovy
dependencies {
    modImplementation files("libs/pocketuicore-1.13.0.jar")
    include files("libs/pocketuicore-1.13.0.jar")
}
```

### fabric.mod.json

Add PocketUICore as a dependency in your `fabric.mod.json`:
```json
{
  "depends": {
    "pocketuicore": ">=1.13.0"
  }
}
```

---

## 2. Project Structure Overview

PocketUICore is organised into these packages:

| Package | Purpose |
|---------|---------|
| `com.pocketuicore.animation` | Animation engine (easing, sequences, looping) |
| `com.pocketuicore.component` | UI components (buttons, panels, lists, etc.) |
| `com.pocketuicore.component.model` | Model classes for Model/View separation |
| `com.pocketuicore.component.prefab` | Pre-built component configurations |
| `com.pocketuicore.controller` | Controller/gamepad support + glyphs |
| `com.pocketuicore.data` | Reactive state, data stores |
| `com.pocketuicore.economy` | Optional economy module |
| `com.pocketuicore.input` | Input helpers, version detection |
| `com.pocketuicore.notification` | Server-side notification utilities |
| `com.pocketuicore.render` | Procedural renderer, theme, tooltip queue |
| `com.pocketuicore.screen` | Screen base classes, shake, tint |
| `com.pocketuicore.sound` | Sound manager with presets |

---

## 3. Creating a Screen

Extend `PocketScreen` for a basic dark-themed screen:

```java
import com.pocketuicore.screen.PocketScreen;
import com.pocketuicore.component.*;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class MyScreen extends PocketScreen {

    public MyScreen() {
        super(Text.literal("My Screen"));
    }

    @Override
    protected void init() {
        super.init();
        // Add components here
        HoverButton btn = new HoverButton(
            width / 2 - 50, height / 2, 100, 20,
            "Click Me!", () -> System.out.println("Clicked!")
        );
        addChild(btn);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dark background is drawn by PocketScreen
        super.render(ctx, mouseX, mouseY, delta);
    }
}
```

Open it from anywhere:
```java
MinecraftClient.getInstance().setScreen(new MyScreen());
```

---

## 4. Components

### HoverButton
```java
HoverButton btn = new HoverButton(x, y, width, height, "Label", () -> {
    // on click
});
btn.setEnabled(false);   // grey out
btn.setVisible(false);   // hide
```

### TextLabel
```java
TextLabel label = new TextLabel(x, y, "Hello World", ProceduralRenderer.COL_TEXT_PRIMARY);
```

### TextInputComponent
```java
TextInputComponent input = new TextInputComponent(x, y, 200, 20);
input.setPlaceholder("Type here...");
input.setMaxLength(50);
String value = input.getText();
```

### SliderComponent
```java
SliderComponent slider = new SliderComponent(x, y, 150, 20, 0f, 100f, 50f);
float value = slider.getValue();
```

### ToggleSwitch
```java
ToggleSwitch toggle = new ToggleSwitch(x, y, 40, 20, false);
boolean on = toggle.isToggled();
```

### Dropdown
```java
Dropdown dd = new Dropdown(x, y, 120, 20);
dd.setOptions(List.of("Option A", "Option B", "Option C"));
dd.setSelectedIndex(0);
```

### SelectableList
```java
SelectableList list = new SelectableList(x, y, 200, 150);
list.addItem("Item 1");
list.addItem("Item 2");
int selected = list.getSelectedIndex();
```

### SpinnerComponent
```java
SpinnerComponent spinner = new SpinnerComponent(x, y, 100, 20, 1, 10, 5);
int value = spinner.getValue();
```

### RadioGroup
```java
RadioGroup radio = new RadioGroup(x, y, List.of("Small", "Medium", "Large"), 0);
int selected = radio.getSelectedIndex();
```

### PercentageBar
```java
PercentageBar bar = new PercentageBar(x, y, 200, 16, 0.75f);
bar.setProgress(0.9f);
```

### Separator
```java
Separator sep = new Separator(x, y, 200);
```

### TabbedPanel
```java
TabbedPanel tabs = new TabbedPanel(x, y, 300, 200);
tabs.addTab("Tab 1", panelA);
tabs.addTab("Tab 2", panelB);
```

### RichTooltip
```java
RichTooltip tooltip = new RichTooltip("Title", "Body text goes here");
// Attach to a component:
myButton.setRichTooltip(tooltip);
```

---

## 5. Layout Panels

### DarkPanel (scrollable container)
```java
DarkPanel panel = new DarkPanel(x, y, 250, 300);
panel.addChild(button1);
panel.addChild(button2);
panel.setScrollable(true, totalContentHeight);
```

### VerticalListPanel (auto-stacking)
```java
VerticalListPanel list = new VerticalListPanel(x, y, 200, 400, 6, 4);
// 6 = padding, 4 = spacing between items

// Suppress layout during bulk adds for performance:
list.setSuppressLayout(true);
for (int i = 0; i < 100; i++) {
    list.addChild(new TextLabel(0, 0, "Item " + i, 0xFFFFFFFF));
}
list.setSuppressLayout(false);
list.layout();  // single layout pass
```

### HorizontalListPanel
```java
HorizontalListPanel row = new HorizontalListPanel(x, y, 400, 40, 4, 4);
row.addChild(btnA);
row.addChild(btnB);
row.addChild(btnC);
```

### GridPanel
```java
GridPanel grid = new GridPanel(x, y, 300, 200, 3, 6);
// 3 columns, 6px padding

grid.setSuppressLayout(true);
grid.addChild(card1);
grid.addChild(card2);
grid.addChild(card3);
grid.addChild(card4);
grid.setSuppressLayout(false);
grid.layout();
```

### FlowPanel (wrapping)
```java
FlowPanel flow = new FlowPanel(x, y, 300, 200, 4, 4);
// Children wrap to next line when row is full
flow.addChild(tag1);
flow.addChild(tag2);
```

---

## 6. Procedural Rendering

The `ProceduralRenderer` provides GPU-free drawing primitives:

```java
import com.pocketuicore.render.ProceduralRenderer;

// Filled rectangle
ProceduralRenderer.fillRect(ctx, x, y, w, h, ProceduralRenderer.COL_BG_PANEL);

// Rounded rectangle with border
ProceduralRenderer.fillRoundedRectWithBorder(ctx, x, y, w, h, 6,
    ProceduralRenderer.COL_BG_ELEVATED,
    ProceduralRenderer.COL_BORDER);

// Gradients
ProceduralRenderer.fillGradientV(ctx, x, y, w, h, colorTop, colorBottom);
ProceduralRenderer.fillGradientH(ctx, x, y, w, h, colorLeft, colorRight);
// High-quality gradient (step=1 for per-pixel):
ProceduralRenderer.fillGradientH(ctx, x, y, w, h, colorLeft, colorRight, 1);

// Drop shadow
ProceduralRenderer.drawDropShadow(ctx, x, y, w, h, 6);

// Circles and lines
ProceduralRenderer.fillCircle(ctx, cx, cy, radius, color);
ProceduralRenderer.drawLine(ctx, x1, y1, x2, y2, color, thickness);

// Text rendering (auto-shadow, centred, etc.)
ProceduralRenderer.drawText(ctx, tr, "Hello", x, y, 0xFFFFFFFF);
ProceduralRenderer.drawCenteredText(ctx, tr, "Centred", cx, y, 0xFFFFFFFF);

// Colour helpers
int blended = ProceduralRenderer.lerpColor(colorA, colorB, 0.5f);
int darker  = ProceduralRenderer.darken(color, 0.2f);
int lighter = ProceduralRenderer.lighten(color, 0.2f);
int faded   = ProceduralRenderer.withAlpha(color, 128);
```

### Palette Constants
```
COL_BG_BASE, COL_BG_PANEL, COL_BG_ELEVATED, COL_BG_SUNKEN
COL_BORDER, COL_BORDER_LIGHT
COL_TEXT_PRIMARY, COL_TEXT_SECONDARY, COL_TEXT_DISABLED
COL_ACCENT_TEAL, COL_ACCENT_WARM
COL_SUCCESS, COL_ERROR, COL_WARNING
```

### CachedShape / ShapeBuilder
For complex shapes rendered every frame, bake them into a `CachedShape`:
```java
CachedShape shape = ProceduralRenderer.shapeBuilder()
    .fillRoundedRect(0, 0, 100, 50, 6, ProceduralRenderer.COL_BG_PANEL)
    .drawRoundedBorder(0, 0, 100, 50, 6, ProceduralRenderer.COL_BORDER)
    .bake();

// In render():
shape.draw(ctx, offsetX, offsetY);
```

---

## 7. Animations

```java
import com.pocketuicore.animation.AnimationTicker;
import com.pocketuicore.animation.AnimationTicker.EasingType;

AnimationTicker anim = AnimationTicker.getInstance();

// Start a simple animation
anim.start("fadeIn", 0f, 1f, 300, EasingType.EASE_IN_OUT);

// In render():
float alpha = anim.get("fadeIn");  // 0.0 → 1.0 over 300ms

// Check completion
if (anim.isComplete("fadeIn")) { /* done */ }

// Looping animations
anim.startLooping("pulse", 0f, 1f, 1000,
    EasingType.EASE_IN_OUT, AnimationTicker.LoopMode.PING_PONG);

// Animation sequences (chain multiple animations)
anim.sequence("intro")
    .then("step1", 0f, 1f, 200, EasingType.EASE_OUT)
    .then("step2", 0f, 1f, 300, EasingType.EASE_IN_OUT)
    .start();

// AnimationHandle for fluent control
AnimationTicker.AnimationHandle handle = anim.startAndGetHandle(
    "slide", -200f, 0f, 400, EasingType.EASE_OUT);
handle.cancel();

// Namespaced contexts (auto-cleanup)
anim.withContext("my-screen", ctx -> {
    ctx.start("local-anim", 0f, 1f, 200, EasingType.LINEAR);
});
anim.cancelContext("my-screen");  // cancels all anims in this context
```

---

## 8. Focus & Controller Navigation

```java
import com.pocketuicore.component.FocusManager;

FocusManager fm = FocusManager.getInstance();

// Register individual components
fm.register(button1);
fm.register(button2);
fm.register(slider1);

// Or register an entire component tree (auto-detects interactive components)
fm.registerTree(myPanel);

// Focus the first component
fm.focusFirst();

// Navigate (called automatically by ControllerHandler)
fm.navigateDirection(FocusManager.Direction.DOWN);
fm.navigateNext();     // Tab
fm.navigatePrevious(); // Shift+Tab

// Activate (simulate click on focused component)
fm.activateFocused();

// Listen for focus changes
fm.addFocusChangeListener((prev, current) -> {
    System.out.println("Focus moved to: " + current);
});

// Context stacking for dialogs
fm.pushContext("my-dialog");
fm.register(dialogButton1);
fm.register(dialogButton2);
fm.focusFirst();
// ... dialog interaction ...
fm.popContext("my-dialog");  // restores previous focus state
```

---

## 9. Controller Glyphs

Display button prompts that match the player's controller:

```java
import com.pocketuicore.controller.ControllerGlyphs;
import com.pocketuicore.controller.ControllerGlyphs.GlyphStyle;
import com.pocketuicore.controller.ControllerGlyphs.Action;

// Get the label for a button (adapts to controller type)
String label = ControllerGlyphs.getActionLabel(Action.CONFIRM, GlyphStyle.XBOX);
// → "A"

// Render a prompt with icon + label
ControllerGlyphs.renderPrompt(ctx, tr, Action.CONFIRM, GlyphStyle.XBOX,
    x, y, ProceduralRenderer.COL_TEXT_PRIMARY);
// Renders: "[A]"

// Render with descriptive text
ControllerGlyphs.renderPromptWithLabel(ctx, tr, Action.CONFIRM,
    GlyphStyle.PLAYSTATION, "Select", x, y, 0xFFFFFFFF);
// Renders: "[X] Select"

// Render an action bar (multiple prompts in a row)
ControllerGlyphs.renderActionBar(ctx, tr,
    new Action[]{Action.CONFIRM, Action.BACK, Action.MENU},
    new String[]{"Select", "Back", "Menu"},
    GlyphStyle.XBOX, x, y, 8, 0xFFCCCCCC);
```

Available styles: `XBOX`, `PLAYSTATION`, `NINTENDO`, `GENERIC`

Available actions: `CONFIRM`, `BACK`, `MENU`, `TAB_LEFT`, `TAB_RIGHT`, `NAV_UP`, `NAV_DOWN`, `NAV_LEFT`, `NAV_RIGHT`, `SCROLL_UP`, `SCROLL_DOWN`, `ACTION`

---

## 10. Notifications & Toasts

### On-Screen Notifications (client-side)
```java
import com.pocketuicore.component.NotificationManager;
import com.pocketuicore.component.NotificationManager.NotificationType;

// Simple notification
NotificationManager.show(NotificationType.SUCCESS, "Harvest complete!");

// With duration
NotificationManager.show(NotificationType.ERROR, "Not enough gold!", 3000);

// With sound
NotificationManager.show(NotificationType.MILESTONE, "Level 10!", true);

// Full options
NotificationManager.show(NotificationType.WARNING, "Low durability!", 4000, true);

// Position
NotificationManager.setPosition(NotificationManager.Position.BOTTOM_RIGHT);
NotificationManager.setMaxVisible(3);

// Persistent notification (stays until dismissed)
int id = NotificationManager.showPersistent(NotificationType.INFO,
    "Download in progress...");
// Later:
NotificationManager.dismissPersistent(id);

// Persistent with callback
int id2 = NotificationManager.showPersistent(NotificationType.WARNING,
    "Server restarting soon", () -> {
        System.out.println("User acknowledged");
    });

// Toast (FloatingText replacement)
NotificationManager.showToast("Picked up 5 diamonds",
    FloatingText.Anchor.TOP_CENTER, 2000);

// Render in your screen:
NotificationManager.renderAll(ctx, width, height, delta);
```

### Notification Types
| Type | Colour | Icon |
|------|--------|------|
| `SUCCESS` | Green | ✔ |
| `ERROR` | Red | ✖ |
| `WARNING` | Yellow | ⚠ |
| `INFO` | Teal | ℹ |
| `MILESTONE` | Gold | ★ |

---

## 11. Sound Effects

```java
import com.pocketuicore.sound.UISoundManager;

// Presets
UISoundManager.playClick();
UISoundManager.playSuccess();
UISoundManager.playError();
UISoundManager.playWarning();
UISoundManager.playSelect();
UISoundManager.playCreate();
UISoundManager.playCelebration();
UISoundManager.playGong();
UISoundManager.playReady();
UISoundManager.playBoundary();
UISoundManager.playHarvest();
UISoundManager.playPlant();
UISoundManager.playUpgrade();
UISoundManager.playMilestone();
UISoundManager.playNotification();
UISoundManager.playTransition();

// Custom
UISoundManager.playCustom(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);

// Volume control
UISoundManager.setMasterVolume(0.8f);
UISoundManager.setMuted(true);

// Scheduled playback (for multi-note sequences with timing)
UISoundManager.schedulePlay(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.4f, 0);
UISoundManager.schedulePlay(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f, 0.4f, 100);
UISoundManager.schedulePlay(SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 0.4f, 200);
// tickScheduled() is called automatically by PocketUICore's client tick
```

---

## 12. Screen Shake

```java
import com.pocketuicore.screen.ScreenShakeHelper;

ScreenShakeHelper shake = new ScreenShakeHelper();

// Trigger
shake.trigger(6f, 300);       // 6px intensity, 300ms
shake.triggerLight();           // 3px, 200ms
shake.triggerMedium();          // 6px, 350ms
shake.triggerHeavy();           // 10px, 500ms

// Directional
shake.triggerHorizontal(4f, 200);
shake.triggerVertical(4f, 200);
shake.triggerDirectional(5f, 300, 45f);  // 45 degrees

// Additive stacking (impacts build up)
shake.triggerAdditive(2f, 150);  // adds to existing shake

// Apply in render (option A: manual)
shake.apply(ctx);
// ... draw your UI ...
shake.restore(ctx);

// Apply in render (option B: lambda)
shake.withShake(ctx, () -> {
    // draw your UI
});
```

---

## 13. Reactive State (ObservableState)

```java
import com.pocketuicore.data.ObservableState;

// Create
ObservableState<Integer> coins = new ObservableState<>(100);

// Listen
coins.addListener((oldVal, newVal) -> {
    System.out.println("Coins: " + oldVal + " → " + newVal);
});

// Update
coins.set(150);  // fires listener

// Derived state
ObservableState<String> display = coins.map(c -> c + " coins");

// Combine two states
ObservableState<Integer> price = new ObservableState<>(50);
ObservableState<Boolean> canBuy = ObservableState.combine(
    coins, price, (c, p) -> c >= p
);

// Bidirectional binding
ObservableState<Float> sliderA = new ObservableState<>(0.5f);
ObservableState<Float> sliderB = new ObservableState<>(0.5f);
sliderA.bindBidirectional(sliderB);
// Now updating either updates the other
```

---

## 14. Model/View Architecture

Separate data from rendering using model classes:

```java
import com.pocketuicore.component.model.*;

// Button Model
ButtonModel btnModel = new ButtonModel("Buy Item");
btnModel.setAction(() -> purchaseItem());
btnModel.enabledState().addListener((old, enabled) -> {
    System.out.println("Button " + (enabled ? "enabled" : "disabled"));
});
// Bind to view: monitor model changes and update your HoverButton

// Slider Model
SliderModel sliderModel = new SliderModel(0f, 100f, 50f);
sliderModel.valueState().addListener((old, val) -> {
    System.out.println("Volume: " + val);
});
sliderModel.setNormalized(0.75f);  // sets to 75

// Toggle Model
ToggleModel toggleModel = new ToggleModel("Dark Mode", true);
toggleModel.toggle();  // false

// List Model
ListModel listModel = new ListModel();
listModel.addItem("Sword");
listModel.addItem("Shield");
listModel.addItem("Potion");
listModel.setSelectedIndex(1);
listModel.selectedIndexState().addListener((old, idx) -> {
    System.out.println("Selected: " + listModel.getSelectedItem());
});

// Text Model
TextModel textModel = new TextModel("", "Search...", 100);
textModel.textState().addListener((old, text) -> {
    filterResults(text);
});
```

---

## 15. Input Handling

```java
import com.pocketuicore.input.InputHelper;
import com.pocketuicore.input.InputHelper.InputMode;

// In your mouseClicked override:
@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (InputHelper.isLeftClick(button)) { /* left click */ }
    if (InputHelper.isRightClick(button)) { /* right click */ }
    if (InputHelper.isMiddleClick(button)) { /* middle click */ }
    return super.mouseClicked(mouseX, mouseY, button);
}

// From a Click record (MC 1.21.11):
int button = InputHelper.getButton(click);
boolean isLeft = InputHelper.isLeftClick(click);

// From a KeyInput record:
int keyCode = InputHelper.getKey(keyInput);
boolean isEscape = InputHelper.isKey(keyInput, org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);

// Modifier checks (convenience wrappers)
boolean shift = InputHelper.isShiftDown();
boolean ctrl  = InputHelper.isCtrlDown();
boolean alt   = InputHelper.isAltDown();

// Input mode tracking
InputMode mode = InputHelper.getInputMode();
boolean usingController = InputHelper.isControllerActive();
```

---

## 16. Theming

```java
import com.pocketuicore.render.Theme;

// Theme provides named colour constants for consistent styling
int bg      = Theme.background();
int surface = Theme.surface();
int primary = Theme.primary();
int text    = Theme.textPrimary();
```

---

## 17. Anchor Layout

Position components relative to screen bounds:

```java
import com.pocketuicore.component.AnchorLayout;
import com.pocketuicore.component.AnchorLayout.Anchor;

// Position a component at the bottom-right with margin
AnchorLayout.anchor(myButton, screenW, screenH, Anchor.BOTTOM_RIGHT, 10);

// Centre a component
AnchorLayout.center(myPanel, screenW, screenH);

// Centre horizontally only
AnchorLayout.centerHorizontally(myButton, screenW);

// Fill the screen with margins
AnchorLayout.fill(myPanel, screenW, screenH, 20);

// Distribute components horizontally with equal spacing
AnchorLayout.distributeHorizontally(buttons, screenW, 10);
```

---

## 18. Tooltip System

```java
// Simple tooltip on any component
myButton.setTooltipText("Click to confirm purchase");

// Rich tooltip
myButton.setRichTooltip(new RichTooltip("Diamond Sword", "Deals 7 damage\n§aDurability: 1561"));

// Disabled tooltip (shown when component is disabled)
myButton.setDisabledTooltipText("Requires 50 coins");

// Global tooltip queue (deferred rendering for correct z-order)
import com.pocketuicore.render.TooltipRenderer;

// In component render:
if (hovered) {
    TooltipRenderer.queue(() -> {
        UIComponent.renderTooltip(ctx, "My tooltip", mouseX, mouseY, screenW, screenH);
    });
}

// At end of screen render:
TooltipRenderer.flush(ctx);
```

---

## 19. Server-Side Notifications

```java
import com.pocketuicore.notification.PocketNotifier;

// Action bar (transient overlay above hotbar)
PocketNotifier.sendActionBar(player, "Growing… 73%");
PocketNotifier.sendActionBar(player, "Ready!", Formatting.GREEN);
PocketNotifier.sendActionBarProgress(player, "Loading", 0.5f);

// Chat reminders (persistent in chat)
PocketNotifier.sendChatReminder(player, "Your crops are ready!");
PocketNotifier.sendDurabilityAlert(player, 10, 100, "Diamond Pickaxe");
PocketNotifier.sendTierUpgrade(player, "Hoe", "Netherite");
PocketNotifier.sendMilestone(player, "1000 blocks mined!");

// Client-side bridge (v1.13.0)
PocketNotifier.showClient(NotificationManager.NotificationType.SUCCESS, "Item crafted!");
```

---

## 20. Economy System (Optional)

The economy module is disabled by default. Enable it in your server-side mod initializer:

```java
PocketUICore.setEconomyEnabled(true);
```

This activates coin storage, transactions, and client-server sync. See the economy package for full API details.

---

## 21. Version Helper

```java
import com.pocketuicore.input.VersionHelper;

// Check Minecraft version at runtime
String ver = VersionHelper.MC_VERSION;  // "1.21.11"

if (VersionHelper.isAtLeast(1, 21, 11)) {
    // Use Matrix3x2fStack API
}

if (VersionHelper.isExactly(1, 21, 11)) {
    // Version-specific code
}
```

---

## 22. Debug Overlay

```java
import com.pocketuicore.render.DebugOverlay;

// Toggle debug overlay (shows component bounds, focus state, etc.)
// Usually bound to a debug key or /pocket debug command
DebugOverlay.setEnabled(true);
DebugOverlay.render(ctx, rootComponent, screenW, screenH);
```

---

## 23. Tips & Best Practices

### Performance
- Use `setSuppressLayout(true)` on `VerticalListPanel` / `GridPanel` when adding many children at once, then call `layout()` once after.
- Use `CachedShape` / `ShapeBuilder` for complex procedural shapes rendered every frame.
- The scanline cache is LRU-bounded (64 entries) — no need for manual cleanup unless using extreme radius values.

### Controller Support
- Always call `FocusManager.registerTree(rootPanel)` so controller users can navigate.
- Test with a gamepad! PocketUICore auto-detects controllers via GLFW polling.
- Use `ControllerGlyphs` to show context-appropriate button labels.

### Architecture
- Prefer `Model` classes (`ButtonModel`, `SliderModel`, etc.) to separate data from views.
- Use `ObservableState` for reactive bindings — avoids manual state synchronisation.
- Use `AnimationTicker` contexts (`withContext`) for automatic cleanup when screens close.

### Notifications
- Use `NotificationManager` (client-side) for visual notifications.
- Use `PocketNotifier` (server-side) for action-bar and chat messages.
- `FloatingText` is deprecated — use `NotificationManager.showToast()` instead.

### Sound
- Use `UISoundManager` presets for consistent feedback.
- Use `schedulePlay()` for timed multi-note sequences.
- Respect the master volume — all sounds go through `UISoundManager`.

### Compatibility
- PocketUICore targets MC 1.21.11 with Fabric.
- Controller features work without Controlify (GLFW polling). Controlify is optional.
- Use `VersionHelper` if your mod needs to support multiple MC versions.

---

## Quick Reference: Minimal Screen Example

```java
public class MyModScreen extends PocketScreen {
    private ScreenShakeHelper shake = new ScreenShakeHelper();

    public MyModScreen() { super(Text.literal("My Mod")); }

    @Override
    protected void init() {
        super.init();

        VerticalListPanel list = new VerticalListPanel(
            width / 2 - 100, 40, 200, height - 80, 8, 4);

        HoverButton btn = new HoverButton(0, 0, 180, 20, "Celebrate!", () -> {
            UISoundManager.playCelebration();
            shake.triggerMedium();
            NotificationManager.show(NotificationType.SUCCESS, "Hooray!");
        });

        list.addChild(btn);
        list.addChild(new PercentageBar(0, 0, 180, 14, 0.65f));
        list.addChild(new ToggleSwitch(0, 0, 40, 20, true));

        addChild(list);

        FocusManager.getInstance().registerTree(list);
        FocusManager.getInstance().focusFirst();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        shake.withShake(ctx, () -> {
            super.render(ctx, mouseX, mouseY, delta);
        });
        NotificationManager.renderAll(ctx, width, height, delta);
        TooltipRenderer.flush(ctx);
    }
}
```

---

*PocketUICore v1.13.0 — Made with care for Minecraft modders.*
