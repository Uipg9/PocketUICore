# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PocketUICore** is a dark-mode procedural UI framework for Minecraft 1.21.11 (Fabric). It depends on **PocketCanvasCore** (a sibling project) as a local composite build.

Directory layout:
- `../PocketCanvasCore` — sibling project included as a Gradle composite build (must exist on disk)
- `src/main/java/com/pocketuicore/` — all source code

## Build Commands

```bash
# Build the mod JAR
./gradlew build
# Output: build/libs/

# Publish to local Maven (for dependent mods)
./gradlew publishToMavenLocal

# Clean
./gradlew clean
```

Java 21 required. PocketCanvasCore must be present at `../PocketCanvasCore` (composite build via `settings.gradle`).

## Architecture

### Rendering Layer
`ProceduralRenderer` wraps `PocketCanvas` (from PocketCanvasCore) to draw all shapes — rounded rects, drop shadows, gradients, scanline arcs. `RenderUtils` provides static helpers. `Theme` holds the 14-colour dark/light palettes with `Theme.current()` / `Theme.setCurrent()`.

### Component System
`UIComponent` is the abstract base for all UI elements. Coordinates are **absolute screen pixels**. The tree is parent → children. Key responsibilities:
- Hit-testing, visibility/enabled state, opacity
- Tooltip management (plain and `RichTooltip`)
- `blockInputInBounds` flag for strict Z-order blocking
- Component ID for tree queries (`findById`)

All concrete components use a **fluent API** (setters return `this`).

Layout containers: `VerticalListPanel`, `HorizontalListPanel`, `FlowPanel`, `GridPanel`, `AnchorLayout`, `PaginatedContainer`.

Interactive components: `HoverButton`, `SliderComponent`, `ToggleSwitch`, `Dropdown<T>`, `SpinnerComponent`, `RadioGroup`, `SelectableList`, `TextInputComponent`, `ConfirmationDialog`, `TabbedPanel`, `ButtonGroup`.

### Model/View Split
Each interactive component has a corresponding model in `component/model/` (`ButtonModel`, `SliderModel`, `ToggleModel`, `ListModel`, `TextModel`). Keep data in the model, rendering in the component.

### Screen Base
Extend `PocketScreen` for any new screen — it wires `FocusManager`, `ControllerHandler`, input forwarding, and tooltip rendering automatically.

### Animation
`AnimationTicker` (singleton) manages all animations. Use `AnimationHandle` to control running animations. `AnimatedValue<T>` wraps a value with easing. 7 easing curves available as constants on `AnimationTicker`.

### Controller Support
`ControllerHandler` polls native GLFW gamepad input. `ControllerGlyphs` renders button prompts. `PocketControlifyCompat` provides optional Controlify mod integration (compile-only dep, guarded by class-existence checks).

### Economy System
Opt-in server-side subsystem. Disabled via `PocketUICore.setEconomyEnabled(false)` before mod initialization. `EconomyManager` + `EstateManager` sync data to clients using `SyncBalancePayload` / `SyncEstatePayload` (S2C packets). Client receives updates in `ClientNetworkHandler` which writes to `ObservableState<T>` reactive bindings.

### Data & State
`ObservableState<T>` — reactive wrapper supporting `bindBidirectional()`. `UIDataStore` / `PocketDataStore` — client-side persistence (NBT-backed, stored in `.minecraft/pocketuicore/`).

### Other Systems
- `PocketNotifier` / `NotificationManager` — queued toast notifications
- `UISoundManager` — preset click/hover/success/error sounds + scheduling
- `ScreenShakeHelper` — camera shake with `withShake(ctx, runnable)` lambda API
- `ScreenTintManager` — screen transition tints
- `KeyShortcutManager` — keyboard shortcut registration
- `InputHelper` — input-mode tracking, modifier checks
- `VersionHelper` — runtime Minecraft version detection
- `DebugOverlay` — F3+P to visualize component bounds and focus state
- `TooltipRenderer` — global tooltip render queue (correct z-ordering)
- `UIFormatUtils` — number/text formatting utilities
