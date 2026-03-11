# PocketUICore

A dark-mode procedural UI framework for Minecraft 1.21.11 (Fabric). Zero textures — every shape, shadow, gradient, and button is drawn with code.

## Features

- **Procedural Render Engine** — Rounded rectangles, drop shadows, gradients, scanline arcs, circles, lines, and cached geometry. 14-colour dark-mode palette + runtime theme switching.
- **Component System** — `UIComponent` base class with hit-testing, children, tooltips, opacity, visibility/enabled states. Components: `DarkPanel`, `HoverButton`, `PercentageBar`, `TextLabel`, `SliderComponent`, `ToggleSwitch`, `SelectableList`, `TabbedPanel`, `GridPanel`, `Dropdown<T>`, `SpinnerComponent`, `RadioGroup`, `Separator`, `VerticalListPanel`, `HorizontalListPanel`, `FlowPanel`, `ConfirmationDialog`, `PaginatedContainer`.
- **Fluent API** — All setters return `this` for chaining: `btn.setText("Go").setTextColor(0xFFFFFF).setEnabled(true)`.
- **Theme System** — `Theme` with DARK/LIGHT built-in palettes, builder pattern, and `Theme.current()`/`setCurrent()` runtime switching.
- **PocketScreen Base** — Abstract base screen with automatic FocusManager + ControllerHandler lifecycle, input forwarding, and tooltip rendering.
- **Animation Engine** — `AnimationTicker` with 7 easing curves, `AnimatedValue` wrapper, fluent sequence/chain API, `LoopMode` (ONCE/LOOP/PING_PONG), and typed `AnimationHandle`.
- **Screen Shake** — Omnidirectional, horizontal, vertical, and custom-angle camera shake with `withShake(ctx, runnable)` lambda API.
- **Controller Support** — Native GLFW gamepad polling, D-pad spatial navigation, thumbstick scroll, rumble feedback presets.
- **Focus Management** — Spatial + linear focus traversal, context stacking, `registerTree()` for automatic discovery.
- **Economy System** — Server → client balance & estate-growth synchronisation via `ObservableState<T>` reactive bindings. Opt-in via `PocketUICore.setEconomyEnabled()`.
- **Notifications** — Queued toast-style popups with auto-dismiss and stacking.
- **Sound Manager** — Click, hover, success, error presets + master volume and mute control.
- **Debug Overlay** — F3+P to visualise component bounds, hierarchy depth, and focus state.
- **Utilities** — `UIFormatUtils`, `KeyShortcutManager`, `ScreenTintManager` (with screen transitions), `UIDataStore`, `ObservableState.bindBidirectional()`.

## Requirements

| Dependency | Version |
|-----------|---------|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.4 |
| Fabric API | 0.141.3+1.21.11 |
| Java | 21 |

## Installation

Add PocketUICore as a dependency in your mod's `build.gradle`:

```groovy
dependencies {
    modImplementation "com.pocketuicore:pocketuicore:1.12.0"
}
```

Or place the JAR in your development environment and add it as a local dependency.

## Quick Start

```java
// Draw a dark panel with a button (fluent API)
DarkPanel panel = new DarkPanel(50, 50, 200, 100, 8);
HoverButton btn = new HoverButton(60, 70, 80, 20, "Click Me", () -> {})
    .setText("Go!")
    .setTextColor(0xFFFFFFFF);
panel.addChild(btn);

// Auto-layout with HorizontalListPanel
HorizontalListPanel toolbar = new HorizontalListPanel(0, 0, 300, 30)
    .setSpacing(4).setPadding(4);
toolbar.addChild(new HoverButton(0, 0, 60, 22, "Save", () -> {}));
toolbar.addChild(new HoverButton(0, 0, 60, 22, "Load", () -> {}));

// Dropdown
Dropdown<String> dropdown = new Dropdown<>(10, 10, 120, 20,
    List.of("Option A", "Option B"), s -> s);

// Animate with loop
AnimationTicker.getInstance().startLooping("pulse", 0f, 1f, 1000,
    AnimationTicker.EASE_IN_OUT);

// Screen shake — lambda style
ScreenShakeHelper shake = new ScreenShakeHelper();
shake.triggerMedium();
shake.withShake(ctx, () -> { /* render here */ });

// Theme switching
Theme.setCurrent(Theme.LIGHT);
```

## Economy Opt-Out

By default the economy subsystem (balance sync, estate growth) is enabled. To disable it:

```java
// In your mod initializer, before PocketUICore loads:
PocketUICore.setEconomyEnabled(false);
```

## Building

```bash
./gradlew build
```

Output JAR is in `build/libs/`.

## License

[MIT](LICENSE)
