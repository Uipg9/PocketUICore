# PocketUICore — Developer Guide

> **Version 1.11.0** · Fabric 1.21.11 · Java 21
>
> A dark-mode procedural UI framework for Minecraft mod developers.
> Build polished, controller-friendly GUIs without any textures or JSON.

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Architecture Overview](#2-architecture-overview)
3. [Rendering — ProceduralRenderer](#3-rendering--proceduralrenderer)
4. [Base Component — UIComponent](#4-base-component--uicomponent)
5. [Containers & Layout](#5-containers--layout)
6. [Interactive Components](#6-interactive-components)
7. [Overlay & HUD Components](#7-overlay--hud-components)
8. [Screens](#8-screens)
9. [Navigation & Focus](#9-navigation--focus)
10. [Animation System](#10-animation-system)
11. [Reactive Data Binding](#11-reactive-data-binding)
12. [Notifications & Toasts](#12-notifications--toasts)
13. [Keyboard Shortcuts](#13-keyboard-shortcuts)
14. [Controller / Gamepad Support](#14-controller--gamepad-support)
15. [Sound System](#15-sound-system)
16. [Screen Effects](#16-screen-effects)
17. [Client Data Persistence](#17-client-data-persistence)
18. [Server-Side Economy System](#18-server-side-economy-system)
19. [Networking & Sync](#19-networking--sync)
20. [Formatting Utilities](#20-formatting-utilities)
21. [API Quick Reference](#21-api-quick-reference)

---

## 1. Getting Started

### Dependency Setup (build.gradle)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    modImplementation 'com.github.Uipg9:PocketUICore:v1.11.0'
}
```

### Minimum Versions

| Component          | Version                |
|--------------------|------------------------|
| Minecraft          | 1.21.11               |
| Fabric Loader      | 0.18.4                |
| Fabric API         | 0.141.3+1.21.11       |
| Java               | 21                    |

---

## 2. Architecture Overview

```
com.pocketuicore
├── render/           ProceduralRenderer, UIFormatUtils
├── component/        All UI components (UIComponent base + 20+ widgets)
│   └── prefab/       Prefabricated panels (PrestigeConfirmationPanel)
├── focus/            FocusManager (keyboard/controller navigation)
├── controller/       ControllerHandler, Controlify compat
├── screen/           PocketMenuScreen, ScreenShakeHelper, ScreenTintManager
├── data/             ObservableState, PocketDataStore, UIDataStore
├── sound/            UISoundManager
├── economy/          EconomyManager, EstateManager, CaravanManager, configs
├── command/          PocketCommandRegister (/pocket command)
└── notification/     PocketNotifier (server→client durability alerts)
```

**Key Design Principles:**
- **No textures** — everything is drawn procedurally (rounded rects, gradients, borders)
- **Dark-mode palette** — 14 predefined color constants
- **Component tree** — parent/child hierarchy with automatic input forwarding
- **Controller-first** — full gamepad support with focus navigation
- **Reactive** — `ObservableState<T>` binds server data to UI automatically

---

## 3. Rendering — ProceduralRenderer

The core rendering engine. All drawing is done via static methods — no instances needed.

### Color Palette Constants

```java
// Backgrounds
ProceduralRenderer.COL_BG_PRIMARY    // 0xFF1A1A2E — darkest background
ProceduralRenderer.COL_BG_SURFACE    // 0xFF16213E — panels, cards
ProceduralRenderer.COL_BG_ELEVATED   // 0xFF0F3460 — elevated elements
ProceduralRenderer.COL_OVERLAY       // 0xBB000000 — screen overlay dim

// Text
ProceduralRenderer.COL_TEXT_PRIMARY  // 0xFFE0E0E0 — main text
ProceduralRenderer.COL_TEXT_MUTED    // 0xFF888888 — secondary text

// Accents
ProceduralRenderer.COL_ACCENT       // 0xFFE94560 — primary accent (red-pink)
ProceduralRenderer.COL_ACCENT_TEAL  // 0xFF00B4D8 — teal accent
ProceduralRenderer.COL_HOVER        // 0x33FFFFFF — hover highlight
ProceduralRenderer.COL_BORDER       // 0xFF2A2A4A — border lines

// Semantic
ProceduralRenderer.COL_SUCCESS      // 0xFF4CAF50 — green
ProceduralRenderer.COL_WARNING      // 0xFFFF9800 — orange
ProceduralRenderer.COL_ERROR        // 0xFFF44336 — red
ProceduralRenderer.COL_GOLD         // 0xFFFFD700 — gold
```

### Shape Drawing

```java
// Solid fill
ProceduralRenderer.fillRect(ctx, x, y, w, h, color);

// Rounded rectangle
ProceduralRenderer.fillRoundedRect(ctx, x, y, w, h, radius, color);

// Border only
ProceduralRenderer.drawBorder(ctx, x, y, w, h, color);
ProceduralRenderer.drawRoundedBorder(ctx, x, y, w, h, radius, color);

// Rounded rect + border in one call
ProceduralRenderer.fillRoundedRectWithBorder(ctx, x, y, w, h, radius, fillColor, borderColor);

// Gradients
ProceduralRenderer.fillVerticalGradient(ctx, x, y, w, h, topColor, bottomColor);
ProceduralRenderer.fillHorizontalGradient(ctx, x, y, w, h, leftColor, rightColor);

// Drop shadow behind a rect
ProceduralRenderer.drawDropShadow(ctx, x, y, w, h, radius, layers, shadowColor);

// Full-screen overlay (dim background behind menus)
ProceduralRenderer.drawFullScreenOverlay(ctx, screenW, screenH, color);
```

### Text Drawing

```java
// Standard text with shadow
ProceduralRenderer.drawText(ctx, textRenderer, "Hello", x, y, color);

// Centered horizontally within a width
ProceduralRenderer.drawCenteredText(ctx, textRenderer, "Hello", x, y, width, color);

// Right-aligned
ProceduralRenderer.drawRightAlignedText(ctx, textRenderer, "Hello", x, y, width, color);

// Scaled text — NOTE: color comes BEFORE scale!
ProceduralRenderer.drawScaledText(ctx, textRenderer, "Big Title", x, y, color, 1.5f);
ProceduralRenderer.drawCenteredScaledText(ctx, textRenderer, "Title", x, y, width, color, 1.5f);

// v1.10.0 convenience overloads (auto-get TextRenderer from MinecraftClient):
ProceduralRenderer.drawText(ctx, "Hello", x, y, color);
ProceduralRenderer.drawCenteredText(ctx, "Hello", x, y, width, color);
ProceduralRenderer.drawScaledText(ctx, "Big", x, y, color, 2.0f);
ProceduralRenderer.drawCenteredScaledText(ctx, "Title", x, y, width, color, 1.5f);
```

### Color Utilities

```java
int color = ProceduralRenderer.hexColor(0xRRGGBB);          // Add 0xFF alpha
int transparent = ProceduralRenderer.withAlpha(color, 128);  // Set alpha 0–255
float lerpedColor = ProceduralRenderer.setAlpha(color, 0.5f);// Set alpha 0.0–1.0
int blended = ProceduralRenderer.lerpColor(colorA, colorB, 0.5f); // Blend
int darker = ProceduralRenderer.darken(color, 0.3f);        // Darken by factor
int lighter = ProceduralRenderer.lighten(color, 0.3f);      // Lighten by factor
int vivid = ProceduralRenderer.saturate(color, 0.5f);       // Saturate by factor
```

### CachedShape / ShapeBuilder (Performance)

For shapes drawn every frame unchanged, bake them into a `CachedShape`:

```java
CachedShape card = ShapeBuilder.create()
    .fill(0, 0, 200, 100, COL_BG_SURFACE)
    .roundedBorder(0, 0, 200, 100, 6, COL_BORDER)
    .shadow(0, 0, 200, 100, 4, 3, 0x40000000)
    .build();

// In render():
card.render(ctx, x, y); // Replays all operations offset by (x, y)
```

---

## 4. Base Component — UIComponent

All widgets extend `UIComponent`. It provides:

- **Geometry**: `x`, `y`, `width`, `height` + offset system
- **Visibility/Enabled**: `setVisible(bool)`, `setEnabled(bool)`
- **Children**: `addChild()`, `removeChild()`, `clearChildren()`
- **Input pipeline**: `mouseClicked()`, `mouseReleased()`, `mouseScrolled()`, `mouseDragged()`, `keyPressed()`, `charTyped()`, `mouseMoved()`
- **Tooltips**: plain text or `RichTooltip`
- **Click handler**: `setOnClick(Runnable)`

### Creating a Custom Component

```java
public class MyWidget extends UIComponent {
    public MyWidget(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, 4,
                ProceduralRenderer.COL_BG_SURFACE);
        ProceduralRenderer.drawCenteredText(ctx, "My Widget",
                x, y + 4, width, ProceduralRenderer.COL_TEXT_PRIMARY);
    }
}
```

### Tooltip System

```java
// Simple tooltip
component.setTooltip("Title", "Description line 1", "Line 2");

// Rich tooltip
component.setRichTooltip(RichTooltip.builder()
    .title("Upgrade Token", ProceduralRenderer.COL_GOLD)
    .separator()
    .line("Cost: $100", ProceduralRenderer.COL_TEXT_MUTED)
    .line("+10% growth rate", ProceduralRenderer.COL_SUCCESS)
    .build());

// Render tooltips (call last in your screen's render method):
UIComponent.renderTooltip(ctx, rootPanel, mouseX, mouseY);
```

### Relative Positioning

```java
component.setRelativePosition(parentComponent, 10, 20);
// Sets position to (parent.x + 10, parent.y + 20)
// cascadePosition() propagates offsets to all children
```

---

## 5. Containers & Layout

### DarkPanel — Scrollable Container

The base container with dark-mode styling, drop shadow, and scrolling.

```java
DarkPanel panel = new DarkPanel(x, y, 300, 200);
panel.setCornerRadius(6);
panel.setBackgroundColor(ProceduralRenderer.COL_BG_PRIMARY);
panel.setBorderColor(ProceduralRenderer.COL_ACCENT);

// Scrolling (vertical)
panel.setScrollable(true, contentHeight);
panel.scrollBy(deltaY);        // Analog scroll (gamepad)

// Horizontal scrolling
panel.setHorizontalScrollable(true, contentWidth);
panel.scrollByX(deltaX);

panel.addChild(someWidget);
```

### VerticalListPanel — Auto-Stacking Layout

Stacks children vertically with padding and spacing. Auto-scrolls if content overflows.

```java
VerticalListPanel list = new VerticalListPanel(x, y, 250, 400, 10, 6);
//                                              padding=10, spacing=6
list.addChild(label1);   // auto-positioned
list.addChild(label2);   // stacked below label1
list.addChild(button1);  // stacked below label2
// layout() is called automatically on addChild/removeChild

// Options
list.setStretchWidth(true);   // default — children fill panel width
list.setPadding(12);
list.setSpacing(8);
```

### GridPanel — Grid Layout

Arranges children in a uniform grid.

```java
GridPanel grid = new GridPanel(x, y, 300, 200, 3, 8);
//                                         columns=3, padding=8
grid.addChild(btn1);  // row 0, col 0
grid.addChild(btn2);  // row 0, col 1
grid.addChild(btn3);  // row 0, col 2
grid.addChild(btn4);  // row 1, col 0
// layout() auto-called on addChild

grid.setColumns(4);
grid.setCellSpacingX(6);
grid.setCellSpacingY(6);
grid.layout(); // re-layout after changing settings
```

### TabbedPanel — Tab Switching

```java
TabbedPanel tabs = new TabbedPanel(x, y, 300, 200);
tabs.addTab("Stats", statsPanel);
tabs.addTab("Crops", cropsPanel);
tabs.addTab("Config", configPanel);

tabs.setOnTabChange(index -> refreshData(index));
tabs.setNumberHotkeys(true);   // Ctrl+1, Ctrl+2, Ctrl+3

// Programmatic control
tabs.setActiveTab(1);
tabs.nextTab();
tabs.prevTab();

// Query
int active = tabs.getActiveIndex();        // 0-based
int count = tabs.getTabCount();
UIComponent content = tabs.getActiveContent();
```

### PaginatedContainer — Multi-Page with Dots

```java
PaginatedContainer pages = new PaginatedContainer(x, y, 300, 200);
pages.addPage(pageOnePanel);
pages.addPage(pageTwoPanel);
pages.addPage(pageThreePanel);

pages.setOnPageChange(idx -> loadPageData(idx));
pages.setAnimateSlide(true);       // smooth slide transition
pages.setSlideDuration(250);       // milliseconds
pages.setShowDots(true);           // page indicator dots
pages.setShowArrows(true);         // ← → arrow buttons

// Navigation
pages.nextPage();                  // wraps around
pages.prevPage();
pages.setPage(2, true);            // animate=true

// Query
int current = pages.getCurrentPage();
int total = pages.getPageCount();
```

---

## 6. Interactive Components

### HoverButton

Animated button with hover transition, focus ring, and disabled state.

```java
HoverButton btn = new HoverButton(x, y, 120, 26, "Click Me", () -> {
    System.out.println("Clicked!");
});

btn.setTooltip("Do something", "This button does a thing");
btn.setEnabled(false);   // grayed out, no interaction
btn.setText("New Label"); // update text

// Colors (all return `this` for chaining)
btn.setBaseColor(ProceduralRenderer.COL_BG_SURFACE);
btn.setHoverColor(ProceduralRenderer.COL_BG_ELEVATED);
btn.setTextColor(ProceduralRenderer.COL_TEXT_PRIMARY);
btn.setBorderColor(ProceduralRenderer.COL_BORDER);
```

### TextInputComponent

Full-featured text input with cursor, selection, clipboard, and character filter.

```java
TextInputComponent input = new TextInputComponent(x, y, 200, 20);

input.setPlaceholder("Enter name...");
input.setMaxLength(32);
input.setCharFilter(c -> Character.isLetterOrDigit(c) || c == '_');
input.setOnSubmit(text -> processName(text));

// Read state
String value = input.getText();
input.setText("default");

// Supports: Ctrl+A, Ctrl+C, Ctrl+X, Ctrl+V, Home, End,
//           Ctrl+arrows (word jump), Shift+arrows (selection)
```

### SliderComponent

Horizontal slider with drag + keyboard support.

```java
SliderComponent slider = new SliderComponent(x, y, 180, 16, 0, 100, 50);
//                                                         min max value
slider.setStep(5);   // snap to multiples of 5
slider.setOnChange(value -> setVolume(value));

float current = slider.getValue();
slider.setValue(75);
```

### SelectableList&lt;T&gt;

Generic typed list with scroll, hover, and selection.

```java
SelectableList<String> list = new SelectableList<>(x, y, 200, 150);
list.setItems(List.of("Apple", "Banana", "Cherry"));
list.setItemRenderer((ctx, item, ix, iy, iw, ih, hovered, selected) -> {
    int bg = selected ? COL_ACCENT : (hovered ? COL_HOVER : 0);
    if (bg != 0) ctx.fill(ix, iy, ix+iw, iy+ih, bg);
    ProceduralRenderer.drawText(ctx, item, ix+4, iy+2, COL_TEXT_PRIMARY);
});
list.setOnSelect(item -> System.out.println("Selected: " + item));

String sel = list.getSelected();  // currently selected item
int idx = list.getSelectedIndex();
```

### InteractiveGrid&lt;T&gt;

2D grid with cell-level interaction, keyboard nav, and multi-select.

```java
InteractiveGrid<Item> grid = new InteractiveGrid<>(x, y, 200, 200, 4, 4);
//                                                              cols rows

grid.setData(flatItemList);
grid.setCellRenderer((ctx, item, cx, cy, cw, ch, hov, sel) -> {
    int bg = sel ? COL_ACCENT : (hov ? COL_HOVER : COL_BG_SURFACE);
    ProceduralRenderer.fillRoundedRect(ctx, cx+1, cy+1, cw-2, ch-2, 2, bg);
});
grid.setOnCellClick((row, col) -> useItem(row, col));

// Multi-select mode
grid.setMultiSelect(true);
grid.setCellGap(2);

// Query
T selected = grid.getSelected();
boolean isSel = grid.isSelected(1, 2);
```

### ConfirmationDialog

Modal dialog with Confirm/Cancel, focus context, and optional timeout.

```java
ConfirmationDialog dialog = new ConfirmationDialog(
    screenWidth, screenHeight,
    "Confirm Purchase",
    "Spend $100 to upgrade your estate?",
    () -> doPurchase(),      // onConfirm
    () -> {}                 // onCancel
);

dialog.setTimeout(10000); // auto-cancel after 10 seconds
dialog.show(parentScreen);
// dialog.dismiss(); — called automatically when user picks an option
```

### PercentageBar

Progress bar with easing, gradient colors, and pulse at 100%.

```java
PercentageBar bar = new PercentageBar(x, y, 180, 12, 0.0f);

bar.setProgress(0.75f); // 0.0–1.0

// Label formats
bar.setLabelFormat(PercentageBar.LabelFormat.PERCENT);   // "75%"
bar.setLabelFormat(PercentageBar.LabelFormat.FRACTION);  // "75 / 100"
bar.setLabelFormat(PercentageBar.LabelFormat.CUSTOM);
bar.setCustomLabel("Level 3");
bar.setLabelFormat(PercentageBar.LabelFormat.NONE);      // no label

bar.setBarColor(ProceduralRenderer.COL_ACCENT_TEAL);
bar.setUseGradientColors(true); // red → yellow → green based on progress
```

### TextLabel

Text component with alignment, scaling, and word wrapping.

```java
TextLabel label = new TextLabel(x, y, 200, 14, "Hello World");

label.setAlign(TextLabel.Align.CENTER);  // LEFT, CENTER, RIGHT
label.setColor(ProceduralRenderer.COL_ACCENT);
label.setScale(1.5f);

// Word wrapping
label.setWrap(true);
// Height auto-adjusts based on wrapped content
```

### ButtonGroup — Mutually Exclusive

```java
ButtonGroup group = new ButtonGroup();
group.add(option1Button);
group.add(option2Button);
group.add(option3Button);
group.setSelection(0); // select first

group.setOnSelect(index -> switchMode(index));

int selected = group.getSelectedIndex();
group.clear(); // restore all button colors
```

### ItemDisplayComponent

Renders a Minecraft `ItemStack` with optional background and tooltip.

```java
ItemDisplayComponent item = new ItemDisplayComponent(x, y, new ItemStack(Items.DIAMOND));
item.setDrawBackground(true);
item.setBackgroundColor(ProceduralRenderer.COL_BG_ELEVATED);
item.setBackgroundRadius(4);
item.setShowCount(true);
item.setScale(1.5f);
item.setTooltip("Diamond", "A precious gem");
```

---

## 7. Overlay & HUD Components

### FloatingText (Toasts)

Temporary floating messages that fade in/out and drift upward.

```java
// Simple toast
FloatingText.show("+50 Coins", FloatingText.Anchor.TOP_CENTER, 2000);

// Custom color
FloatingText.show("Level Up!", FloatingText.Anchor.CENTER,
        ProceduralRenderer.COL_SUCCESS, 3000);

FloatingText.clearAll();

// Render in your screen:
FloatingText.renderAll(ctx, screenWidth, screenHeight, delta);
```

**Anchors:** `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER`, `BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`

### HudOverlayComponent

Persistent HUD overlay rendered on the game screen (not in a menu).

```java
HudOverlayComponent overlay = new HudOverlayComponent(10, 10, 120, 24);
overlay.setLabel("HP");
overlay.setItem(new ItemStack(Items.GOLDEN_APPLE));
overlay.setDurabilityPercent(0.8f);  // shows a progress bar
overlay.setDurabilityColor(ProceduralRenderer.COL_SUCCESS);
// Auto-registered on creation, call overlay.remove() to unregister

// Render all overlays (done automatically if registered in client entrypoint):
HudOverlayComponent.renderAll(ctx, delta);
```

---

## 8. Screens

### PocketMenuScreen

The built-in example screen opened via `/pocket` or `/p`. Demonstrates:
- `VerticalListPanel` for auto-layout
- `ObservableState` binding to server-synced wallet balance
- `FocusManager` for keyboard/controller navigation
- `ControllerHandler` for gamepad support

### Creating Your Own Screen

```java
public class MyScreen extends Screen {
    private DarkPanel mainPanel;

    public MyScreen() {
        super(Text.literal("My Screen"));
    }

    @Override
    protected void init() {
        super.init();
        FocusManager fm = FocusManager.getInstance();
        fm.clear();

        mainPanel = new DarkPanel((width - 300) / 2, (height - 200) / 2, 300, 200);
        mainPanel.setCornerRadius(8);
        mainPanel.setBackgroundColor(ProceduralRenderer.COL_BG_PRIMARY);

        HoverButton btn = new HoverButton(20, 20, 100, 26, "Click", () -> { });
        mainPanel.addChild(btn);
        fm.register(btn);
        fm.focusFirst();

        ControllerHandler.getInstance().setScrollTarget(mainPanel);
        ControllerHandler.getInstance().enable();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ProceduralRenderer.drawFullScreenOverlay(ctx, width, height,
                ProceduralRenderer.COL_OVERLAY);
        mainPanel.render(ctx, mouseX, mouseY, delta);
        UIComponent.renderTooltip(ctx, mainPanel, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (mainPanel.mouseClicked(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (mainPanel.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (mainPanel.mouseScrolled(mx, my, hAmt, vAmt)) return true;
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (mainPanel.mouseDragged(click.x(), click.y(), click.button(), dx, dy)) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        FocusManager fm = FocusManager.getInstance();
        switch (keyInput.key()) {
            case GLFW.GLFW_KEY_TAB -> {
                if ((keyInput.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) fm.navigatePrevious();
                else fm.navigateNext();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                fm.activateFocused();
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void removed() {
        ControllerHandler.getInstance().disable();
        FocusManager.getInstance().clear();
        super.removed();
    }

    @Override
    public boolean shouldPause() { return false; }
}
```

---

## 9. Navigation & Focus

### FocusManager

Singleton that tracks focusable components and enables keyboard/controller navigation.

```java
FocusManager fm = FocusManager.getInstance();

// Register focusable components
fm.register(button1);
fm.register(button2);
fm.register(slider1);

// Navigation
fm.focusFirst();
fm.navigateNext();                  // Tab
fm.navigatePrevious();              // Shift+Tab
fm.navigateDirection(Direction.UP); // Spatial navigation (arrow keys)
fm.navigateDirection(Direction.DOWN);
fm.navigateDirection(Direction.LEFT);
fm.navigateDirection(Direction.RIGHT);

// Activate (Enter key / gamepad A button)
fm.activateFocused();

// Context stacking (e.g., push when dialog opens)
fm.pushContext("dialog");
fm.register(confirmBtn);
fm.register(cancelBtn);
fm.focusFirst();
// ... later:
fm.popContext();                    // restores previous focus set
fm.popContext("dialog");            // pop by name with assertion

// Listeners
fm.addFocusChangeListener((oldComp, newComp) -> {
    // react to focus changes
});

// Cleanup
fm.clear();
```

---

## 10. Animation System

### AnimationTicker

Singleton easing engine for smooth value transitions.

```java
AnimationTicker anim = AnimationTicker.getInstance();

// Start an animation
anim.start("myAnim", 500, EasingType.EASE_OUT);   // 500ms, ease-out
anim.start("fadeIn", 300, EasingType.EASE_IN_OUT);

// Read progress in render (0.0 → 1.0)
float progress = anim.getProgress("myAnim");  // 0.0–1.0
// Or the original method name:
float t = anim.get("myAnim");

// Check if running
boolean running = anim.isRunning("myAnim");

// Cancel
anim.cancel("myAnim");
anim.cancelAll();

// Static helpers
float lerped = AnimationTicker.lerp(start, end, t);
float smooth = AnimationTicker.smoothDamp(current, target, speed, delta);
```

**Available Easing Types:**
- `LINEAR` — constant speed
- `EASE_IN` — slow start
- `EASE_OUT` — slow end (most common for UI)
- `EASE_IN_OUT` — slow start and end
- `EASE_OUT_BACK` — slight overshoot
- `EASE_IN_OUT_SINE` — sinusoidal

### Animation Sequences

Chain multiple animations:

```java
AnimationTicker.sequence("openMenu")
    .then("slideIn", 300, EasingType.EASE_OUT)
    .then("fadeContent", 200, EasingType.EASE_IN)
    .onComplete(() -> System.out.println("Menu ready!"))
    .start();

// In render:
float slideT = AnimationTicker.sequence("openMenu").get("slideIn");
float fadeT = AnimationTicker.sequence("openMenu").get("fadeContent");
```

### AnimationContext (Namespaced)

Prevent animation key collisions between components:

```java
AnimationContext ctx = new AnimationContext("inventory");
ctx.start("hover", 200, EasingType.EASE_OUT);
float t = ctx.get("hover");
// Internal key: "inventory:hover" — won't conflict with other components
```

### AnimatedValue

Single-value animation wrapper that handles rendering automatically.

```java
AnimatedValue balance = new AnimatedValue(x, y, 100, 20, 0);
balance.setTarget(500);       // animates from 0 → 500
balance.setFormatter(v -> "$" + (int) v);
balance.setFlashOnChange(true);    // brief gold flash
balance.setBounceOnChange(true);   // scale bounce effect
balance.setCenterText(true);
balance.setTextScale(1.2f);

// In render:
balance.render(ctx, mouseX, mouseY, delta);
```

---

## 11. Reactive Data Binding

### ObservableState&lt;T&gt;

Reactive value container that notifies listeners on change.

```java
// Create
ObservableState<Integer> health = new ObservableState<>(100);

// Listen
health.addListener((oldVal, newVal) -> updateHealthBar(newVal));

// Update
health.set(80);   // triggers all listeners
int val = health.get();

// Derived state (auto-updates when source changes)
ObservableState<String> healthText = health.map(hp -> "HP: " + hp);

// Combined state
ObservableState<String> display = ObservableState.combine(
    health, mana,
    (hp, mp) -> "HP: " + hp + " | MP: " + mp
);

// Bind to UI components
ObservableState.bindText(healthText, textLabel);        // auto-update label
ObservableState.bindProgress(health.map(h -> h / 100f), percentageBar); // auto-update bar
```

### MappedState Disposal

When a mapped state is no longer needed, dispose to prevent memory leaks:

```java
ObservableState<String> mapped = source.map(v -> "Value: " + v);
// ... use it ...
mapped.dispose(); // removes listener from source
```

---

## 12. Notifications & Toasts

### NotificationManager (Typed)

Typed notification system with queue and positioning.

```java
// Simple notifications
NotificationManager.show(NotificationType.SUCCESS, "Harvest complete!");
NotificationManager.show(NotificationType.ERROR, "Not enough gold!", 3000);
NotificationManager.show(NotificationType.WARNING, "Crops need water");
NotificationManager.show(NotificationType.INFO, "New quest available");
NotificationManager.show(NotificationType.MILESTONE, "Prestige Level 5!", true); // with sound

// Configuration
NotificationManager.setMaxVisible(5);      // max on screen at once
NotificationManager.setPosition(Position.TOP_CENTER);

// Render in your screen
NotificationManager.renderAll(ctx, screenWidth, screenHeight, delta);

// Clear all
NotificationManager.clearAll();
```

**Notification Types:** `SUCCESS` (green ✔), `ERROR` (red ✖), `WARNING` (yellow ⚠), `INFO` (teal ℹ), `MILESTONE` (gold ★)

**Positions:** `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`

---

## 13. Keyboard Shortcuts

### KeyShortcutManager

Context-stacked keyboard shortcut system with conflict detection and help overlay.

```java
KeyShortcutManager keys = KeyShortcutManager.getInstance();

keys.pushContext("main");
keys.register("main", GLFW.GLFW_KEY_E, KeyShortcutManager.NONE,
              "Open Inventory", () -> openInventory());
keys.register("main", GLFW.GLFW_KEY_H, KeyShortcutManager.CTRL,
              "Toggle Help", () -> keys.toggleHelp());

// Modifier combinations
keys.register("main", GLFW.GLFW_KEY_S, KeyShortcutManager.CTRL | KeyShortcutManager.SHIFT,
              "Save All", () -> saveAll());

// In keyPressed():
if (keys.handleKey(keyCode, modifiers)) return true;

// Help overlay — shows all registered shortcuts
keys.renderHelp(ctx, screenWidth, screenHeight);
keys.toggleHelp();
keys.showHelp();
keys.hideHelp();

// Context stacking (e.g., dialog opens with its own shortcuts)
keys.pushContext("dialog");
keys.register("dialog", GLFW.GLFW_KEY_ENTER, "Confirm", () -> confirm());
// ... later:
keys.popContext(); // removes dialog shortcuts, restores main

keys.clearAll();
```

---

## 14. Controller / Gamepad Support

### ControllerHandler

Full GLFW gamepad polling with D-pad navigation, analog stick, rumble, and virtual cursor.

```java
ControllerHandler ch = ControllerHandler.getInstance();

// Enable/disable (e.g., when screen opens/closes)
ch.enable();
ch.disable();

// Set scroll target for right stick Y-axis
ch.setScrollTarget(myPanel);

// Rumble feedback
ch.rumble(0.5f, 200);  // intensity 0.0–1.0, duration ms

// Auto-handled by the system:
// - D-pad / left stick → FocusManager navigation
// - A button → activate focused
// - B button → close screen
// - LB/RB → tab cycling (TabbedPanel)
// - Right stick Y → scroll
```

### Controlify Compatibility

PocketUICore includes optional Controlify integration via `PocketControlifyCompat`. If the Controlify mod is present, its screen processor is used instead of the built-in GLFW polling.

---

## 15. Sound System

### UISoundManager

Pre-configured sound presets for UI feedback.

```java
UISoundManager.playClick();         // button click
UISoundManager.playSuccess();       // positive action
UISoundManager.playError();         // error / failure
UISoundManager.playWarning();       // warning
UISoundManager.playNotification();  // info notification
UISoundManager.playMilestone();     // achievement
UISoundManager.playOpen();          // menu open
UISoundManager.playClose();         // menu close
```

---

## 16. Screen Effects

### ScreenShakeHelper

Camera shake with exponential decay.

```java
// Omnidirectional shake
ScreenShakeHelper.shake(0.5f);    // intensity 0.0–1.0

// Directional variants
ScreenShakeHelper.shakeHorizontal(0.3f);
ScreenShakeHelper.shakeVertical(0.3f);
ScreenShakeHelper.shakeDirectional(0.4f, 45.0f); // angle in degrees

// Apply in render (uses matrix push/translate/pop):
ScreenShakeHelper.applyShake(ctx);   // call before rendering
// ... render your content ...
ScreenShakeHelper.restoreShake(ctx); // call after rendering

// Query
boolean shaking = ScreenShakeHelper.isShaking();
```

### ScreenTintManager

Full-screen tint overlays (fade, flash, pulse).

```java
ScreenTintManager tint = ScreenTintManager.getInstance();

tint.fadeIn(0xFF000000, 500);          // fade to black over 500ms
tint.fadeOut(500);                      // fade back to transparent
tint.flash(0x80FF0000, 200);           // brief red flash
tint.pulse(ProceduralRenderer.COL_ACCENT, 1000); // pulsing tint

// Render (call in your screen):
tint.render(ctx, screenWidth, screenHeight, delta);
```

---

## 17. Client Data Persistence

### UIDataStore

Client-side key-value JSON storage in `.minecraft/config/`.

```java
UIDataStore store = new UIDataStore("mymod_ui");
// File: .minecraft/config/mymod_ui.json

// Write (auto-saves immediately)
store.set("lastTab", 2);
store.set("showTooltips", true);
store.set("username", "Player1");

// Read (with defaults)
int tab = store.getInt("lastTab", 0);
boolean tips = store.getBoolean("showTooltips", true);
String name = store.getString("username", "");
float vol = store.getFloat("volume", 1.0f);

// Check existence
if (store.has("lastTab")) { ... }

// Remove
store.remove("oldKey");
store.clear();

// Versioned migration
store.setDataVersion(2);
store.onMigrate(s -> {
    // migrate from v1 to v2
    if (s.has("old_key")) {
        s.set("new_key", s.getString("old_key", ""));
        s.remove("old_key");
    }
});
```

### PocketDataStore (Server-Side)

Generic codec-based `PersistentState` wrapper for server-side data.

```java
// Define your state with a Codec
public class MyState extends PersistentState {
    public static final Codec<MyState> CODEC = ...;
    public static final PersistentStateType<MyState> TYPE = ...;
}

// Access via server:
MyState state = PocketDataStore.getOrCreate(server, MyState.TYPE);
```

---

## 18. Server-Side Economy System

### EconomyManager

Server-side balance CRUD operations.

```java
// Get balance
int balance = EconomyManager.getBalance(player);

// Add/remove money
EconomyManager.addMoney(player, 50);
boolean success = EconomyManager.removeMoney(player, 100); // false if insufficient

// Set balance directly
EconomyManager.setBalance(player, 500);
```

### EconomyConfig

Tuning constants — modify before first server tick:

```java
EconomyConfig.ESTATE_GROWTH_PER_TICK = 0.10f;  // faster estates (default: 0.05)
EconomyConfig.ESTATE_PAYOUT_AMOUNT = 100;       // bigger payouts (default: 50)
EconomyConfig.ESTATE_SYNC_INTERVAL = 10;        // ticks between client sync (default: 10)
EconomyConfig.STARTING_BALANCE = 100;           // new player starting gold (default: 0)
EconomyConfig.MAX_BALANCE = 1_000_000;          // balance cap (default: Integer.MAX_VALUE)
```

### EstateManager

Passive income system — automatically ticks for all online players.

```java
// Query a player's estate growth
float pct = EstateManager.getGrowth(player.getUuid()); // 0.0–100.0

// Reset growth (e.g., on prestige)
EstateManager.resetGrowth(player.getUuid(), server); // saves first

// Registered in server tick:
ServerTickEvents.END_SERVER_TICK.register(EstateManager::tick);
```

### CaravanManager

Trade dispatcher for NPC-to-NPC caravans.

```java
// Dispatch a trade
CaravanManager.dispatch(player, cost, reward, durationTicks, server);

// Check if player has an active caravan
boolean active = CaravanManager.isActive(player.getUuid());
```

---

## 19. Networking & Sync

### Client-Side Observables

The economy state is automatically synced to clients via custom payloads:

```java
// These are pre-bound ObservableState instances:
ClientNetworkHandler.CLIENT_BALANCE         // ObservableState<Integer>
ClientNetworkHandler.CLIENT_ESTATE_GROWTH   // ObservableState<Float>

// Listen for balance changes:
ClientNetworkHandler.onBalanceChanged((oldBal, newBal) -> {
    FloatingText.show("+" + (newBal - oldBal) + " coins",
            FloatingText.Anchor.TOP_CENTER, 2000);
});

// Single-arg listener:
ClientNetworkHandler.onBalanceChanged(newBal -> updateUI(newBal));

// Estate growth changes:
ClientNetworkHandler.onEstateGrowthChanged((oldPct, newPct) -> { });
```

### Custom Payloads

```java
// S2C payloads (server → client)
SyncBalancePayload   // record(int balance)
SyncEstatePayload    // record(float percentage)
```

---

## 20. Formatting Utilities

### UIFormatUtils

Pure formatting functions — no side effects.

```java
UIFormatUtils.formatGold(1500)          // "$1,500"
UIFormatUtils.formatNumber(1234567)     // "1,234,567"
UIFormatUtils.formatCompact(1500000)    // "1.5M"
UIFormatUtils.formatTime(3661)          // "1h 1m 1s"
UIFormatUtils.formatTicks(200)          // "10.0s"
UIFormatUtils.formatCountdown(90)       // "1:30"
UIFormatUtils.formatPercent(0.756f)     // "75%"
UIFormatUtils.formatPercentDecimal(0.756f) // "75.6%"
UIFormatUtils.formatFraction(7, 10)     // "7 / 10"
UIFormatUtils.pluralise(5, "item")      // "5 items"
UIFormatUtils.pluralise(1, "item")      // "1 item"
```

---

## 21. API Quick Reference

### Component Hierarchy

```
UIComponent (abstract base)
├── HoverButton
├── TextLabel
├── TextInputComponent
├── SliderComponent
├── PercentageBar
├── SelectableList<T>
├── InteractiveGrid<T>
├── AnimatedValue
├── ItemDisplayComponent
├── FloatingText (static)
├── HudOverlayComponent
├── ConfirmationDialog
├── RichTooltip (builder)
├── ButtonGroup (wrapper)
├── DarkPanel (scrollable container)
│   ├── VerticalListPanel
│   └── GridPanel
├── TabbedPanel
└── PaginatedContainer
```

### Singletons

| Class                    | Access                                  |
|--------------------------|-----------------------------------------|
| `AnimationTicker`        | `AnimationTicker.getInstance()`         |
| `FocusManager`           | `FocusManager.getInstance()`            |
| `ControllerHandler`      | `ControllerHandler.getInstance()`       |
| `KeyShortcutManager`     | `KeyShortcutManager.getInstance()`      |
| `ScreenTintManager`      | `ScreenTintManager.getInstance()`       |

### Static Managers

| Class                    | Usage                                   |
|--------------------------|-----------------------------------------|
| `NotificationManager`    | `NotificationManager.show(...)`         |
| `FloatingText`           | `FloatingText.show(...)`                |
| `UISoundManager`         | `UISoundManager.playClick()`            |
| `ScreenShakeHelper`      | `ScreenShakeHelper.shake(...)`          |
| `EconomyManager`         | `EconomyManager.addMoney(...)`          |
| `EstateManager`          | `EstateManager.tick(server)`            |
| `CaravanManager`         | `CaravanManager.dispatch(...)`          |

### MC 1.21.11 Input Signatures

```java
// Screen input methods use Click and KeyInput records:
boolean mouseClicked(Click click, boolean bl)     // click.x(), click.y(), click.button()
boolean mouseReleased(Click click)                // click.x(), click.y(), click.button()
boolean mouseDragged(Click click, double dx, double dy)
boolean keyPressed(KeyInput keyInput)             // keyInput.key(), keyInput.scancode(), keyInput.modifiers()

// UIComponent methods still use raw types:
boolean mouseClicked(double mouseX, double mouseY, int button)
boolean mouseReleased(double mouseX, double mouseY, int button)
boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
boolean keyPressed(int keyCode, int scanCode, int modifiers)
```

---

## Changelog (v1.11.0)

### Bug Fixes
- **InteractiveGrid**: Fixed division-by-zero crash when columns/rows set to 0 (now clamped to ≥ 1)
- **InteractiveGrid**: Fixed asymmetric arrow-key navigation from unselected state (now initializes to (0,0))
- **NotificationManager**: Fixed thread-unsafe static collections (now synchronized)
- **FloatingText**: Fixed thread-unsafe static list + stale stacking indices causing visual gaps
- **EstateManager**: Fixed `ArithmeticException` if `EconomyConfig.ESTATE_SYNC_INTERVAL` is 0
- **PaginatedContainer**: Fixed page position not restored to exact coords after slide animation
- **PaginatedContainer**: Removed unused `slideFromX` field
- **VerticalListPanel**: Fixed missing empty-children guard causing negative scroll height
- **GridPanel / VerticalListPanel**: Fixed scrollable flag never being cleared when content shrinks
- **PocketMenuScreen**: Fixed TOCTOU race condition on wallet balance check
- **PocketMenuScreen**: Added missing `mouseDragged` forwarding (sliders/drag now work)
- **TabbedPanel**: Fixed spurious `onTabChange` callback when removing tabs before the active tab
- **PlayerVaultState**: Now enforces `EconomyConfig.MAX_BALANCE` instead of hardcoded `Integer.MAX_VALUE`
- **UIDataStore**: Removed dead `oldVersion` variable
- **RichTooltip**: Removed unused `Text` import
