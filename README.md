# PocketUICore

A dark-mode procedural UI framework for Minecraft 1.21.11 (Fabric). Zero textures — every shape, shadow, gradient, and button is drawn with code.

## Features

- **Procedural Render Engine** — Rounded rectangles, drop shadows, gradients, scanline arcs, and cached geometry. 14-colour dark-mode palette.
- **Component System** — `UIComponent` base class with hit-testing, children, tooltips, visibility/enabled states. `DarkPanel`, `HoverButton`, `PercentageBar`, `TextLabel`, `Slider`, `Toggle`, `SelectableList`, `TabbedPanel`, `InteractiveGrid`, `PaginatedContainer`, `ConfirmationDialog`.
- **Animation Engine** — `AnimationTicker` with 7 easing curves, `AnimatedValue` wrapper, and fluent sequence/chain API.
- **Screen Shake** — Omnidirectional, horizontal, vertical, and custom-angle camera shake with exponential decay.
- **Controller Support** — Native GLFW gamepad polling, D-pad spatial navigation, thumbstick scroll, rumble feedback presets.
- **Focus Management** — Spatial + linear focus traversal, context stacking for modals/dialogs.
- **Economy System** — Server → client balance & estate-growth synchronisation via `ObservableState<T>` reactive bindings.
- **Notifications** — Queued toast-style popups with auto-dismiss and stacking.
- **Sound Manager** — Click, hover, success, and error sound presets.
- **Utilities** — `UIFormatUtils`, `KeyShortcutManager`, `ScreenTintManager`, `UIDataStore`.

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
    modImplementation "com.pocketuicore:pocketuicore:1.10.0"
}
```

Or place the JAR in your development environment and add it as a local dependency.

## Quick Start

```java
// Draw a dark panel with a button
DarkPanel panel = new DarkPanel(50, 50, 200, 100, 8);
HoverButton btn = new HoverButton(60, 70, 80, 20, "Click Me");
btn.setOnClick(() -> System.out.println("Clicked!"));
panel.addChild(btn);

// In your screen's render method:
panel.render(drawContext, mouseX, mouseY, delta);

// Animate something
AnimationTicker.getInstance().start("fade", 0f, 1f, 500);
float alpha = AnimationTicker.getInstance().getProgress("fade");

// Screen shake on error
ScreenShakeHelper shake = new ScreenShakeHelper();
shake.triggerMedium();
// In render: shake.applyShake(ctx); ... shake.restoreShake(ctx);
```

## Building

```bash
./gradlew build
```

Output JAR is in `build/libs/`.

## License

[MIT](LICENSE)
