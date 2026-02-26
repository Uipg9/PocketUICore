# Changelog

All notable changes to PocketUICore are documented in this file.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.10.0] — 2025-02-26

### Added
- **ProceduralRenderer convenience text overloads** — `drawText`, `drawCenteredText`, `drawScaledText`, `drawScaledCenteredText` now have overloads that auto-resolve `TextRenderer` from `MinecraftClient`.
- **API naming aliases** — `ScreenShakeHelper.isShaking()`, `applyShake(ctx)`, `restoreShake(ctx)` aliases; `AnimationTicker.getProgress(id)` alias for `get01`.
- **DarkPanel tooltip pass** — `renderTooltipPass(DrawContext, int, int)` enables tooltip rendering for children inside scrollable panels with correct offset handling.
- **MC 1.21.11 input bridge methods** — `UIComponent.mouseClicked(Click)`, `mouseClicked(Click, boolean)`, `mouseReleased(Click)`, `keyPressed(KeyInput)` bridge the new MC 1.21.11 input record types to the existing method signatures.
- **AnimationTicker sequence API** — Fluent `sequence("name").then(from, to, dur, easing).then(...).onComplete(cb).start()` builder for chained multi-step animations.
- **FocusManager named popContext** — `popContext(String expectedName)` that asserts the top context name matches, preventing wrong-context-pop bugs in nested modals.
- **Economy event bus** — `ClientNetworkHandler.onBalanceChanged(BiConsumer<Integer,Integer>)`, `onBalanceChanged(Consumer<Integer>)`, `onEstateGrowthChanged(Consumer<Float>)` for reactive economy state listening.
- **Custom rumble overload** — `ControllerHandler.rumble(float intensity, int durationMs)` single-parameter rumble that auto-maps low/high frequencies.
- **Javadoc enhancements** — `@param`, `@return`, `@since` tags added across all public API methods.

---

## [1.9.0] — 2025-02-25

### Added
- **ConfirmationDialog** — Modal yes/no dialogs with configurable title, message, and button labels.
- **SelectableList** — Scrollable list component with single/multi-select and keyboard navigation.
- **TabbedPanel** — Tab-switching container for multi-view screens.
- **UIFormatUtils** — Number formatting utilities (compact notation, currency, percentages).
- **InteractiveGrid** — Grid layout with cell click/hover handling and selection.
- **PaginatedContainer** — Automatic pagination for large child sets with page indicators.
- **NotificationManager** — Queued toast-style notifications with auto-dismiss and stacking.
- **KeyShortcutManager** — Register and handle keyboard shortcuts with modifier key support.
- **ScreenTintManager** — Full-screen colour tint overlays with fade in/out animations.
- **UIDataStore** — Client-side key-value store for persisting UI state across screen opens.
- **RichTooltip integration** — Multi-line, formatted tooltip support on all UIComponents.
- **Sound presets** — `UISoundManager` with `playClick()`, `playHover()`, `playSuccess()`, `playError()` presets.
- **Directional shake** — `ScreenShakeHelper.triggerHorizontal()`, `triggerVertical()`, `triggerDirectional()` methods.
- **AnimatedValue** — Lightweight wrapper for animating a single float property with easing.
- **PercentageBar enhancements** — Label support, text colour customisation, easing speed control.

---

## [1.8.0] — 2025-02-25

### Added
- 22 improvements including new components, utilities, and polish across the library.
- `TextLabel` component for standalone text rendering.
- `Slider` / `Toggle` input components.
- `center()` method on `DarkPanel` for auto-centering children.
- Additional colour palette constants.
- Component visibility and enabled state management.

---

## [1.7.0] — 2025-02-25

### Fixed
- DarkPanel `center()` drift on repeat calls.
- Identifier crash in economy payloads (`CustomPayload.id` → `Identifier.of`).
- Click-through in scrollable DarkPanels.

### Added
- Missing getters on HoverButton, PercentageBar, and DarkPanel.
- `setText()` alias on `HoverButton` (delegates to `setLabel()`).
- `mouseDragged` and `mouseScrolled` forwarding through the component tree.

---

## [1.6.0] — 2025-02-24

### Added
- Estate passive income system with `SyncEstatePayload`.
- `ObservableState<T>` reactive binding for client-side economy values.
- `ClientNetworkHandler` — balance and estate growth synchronisation.
- Caravan trade dispatcher utility.

---

## [1.5.0] — 2025-02-24

### Added
- Full native controller support via GLFW gamepad API.
- `ControllerHandler` — joystick polling, D-pad navigation, rumble feedback.
- `FocusManager` — spatial + linear focus traversal for controller/keyboard users.
- Rumble presets: `rumbleTap()`, `rumbleConfirm()`, `rumbleError()`.

---

## [1.4.0] — 2025-02-24

### Added
- Backend economy system with server-side balance management.
- `SyncBalancePayload` for server → client balance synchronisation.
- Currency transaction API.

---

## [1.3.0] — 2025-02-24

### Added
- `PocketMenuScreen` base class for dark-mode menu screens.
- `/pocket` command registration for opening the menu.

---

## [1.2.0] — 2025-02-24

### Added
- Initial public release.
- `ProceduralRenderer` — dark-mode procedural drawing engine with 14-colour palette.
- `UIComponent` — base component with hit-testing, children, tooltips.
- `DarkPanel` — rounded container with shadow and scroll support.
- `HoverButton` — interactive button with normal/hover/pressed colour states.
- `PercentageBar` — animated progress bar with customisable colours.
- `PocketNotifier` — in-game notification popup system.
- `AnimationTicker` — client-side easing engine with 7 easing curves.
- `ScreenShakeHelper` — camera-shake effect for UI screens.
