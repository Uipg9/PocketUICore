# Changelog

All notable changes to PocketUICore are documented in this file.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.15.0] — 2025-07-15

### Fixed — Bug Fixes
- **Dropdown overlay z-ordering (Bug 2.3)** — Expanded dropdown lists now render in a dedicated overlay pass, ensuring they always appear above sibling components. Two-pass rendering (normal + overlay) added to `UIComponent.render()` and `DarkPanel.render()`, with `PocketScreen` also rendering root overlays.
- **Dropdown keyboard navigation** — Arrow keys now work when the dropdown is expanded regardless of `FocusManager` focus state. Escape key now closes the dropdown without selecting.
- **SpinnerComponent click at limits** — Clicking +/− buttons at min/max no longer plays a spurious click sound.

### Added — API Enhancements
- **UIComponent.renderOverlay()** — New protected overlay render method for subclasses to draw content above siblings (e.g. expanded dropdowns).
- **UIComponent.addChild() chaining** — `addChild()` now returns `this` for fluent builder patterns.
- **UIComponent.requestFocus()** — Convenience method that registers with `FocusManager` and sets focus in one call.
- **TextInputComponent — Numeric mode** — Built-in numeric input filtering via `setNumericMode(boolean, boolean)`, `setNumericRange(double, double)`, `getNumericValue()`, `setNumericValue(double)`. Supports integer-only and decimal modes with automatic validation.
- **TextInputComponent.setSelectionColor()** — Configurable selection highlight colour with improved default contrast (alpha 120).
- **VerticalListPanel — Scroll bar indicator** — Visual rounded scroll bar rendered when content overflows. Configurable via `setShowScrollBar()`, `setScrollBarWidth()`, `setScrollBarColor()`.
- **DarkPanel.setBackgroundOpacity()** — Control background transparency (0.0–1.0) independently from the background colour alpha.
- **SelectableList — Multi-select mode** — Optional multi-select with Ctrl+click toggle, Shift+click range selection. New API: `setMultiSelect()`, `getSelectedItems()`, `getSelectedIndices()`, `clearSelection()`.
- **ConfirmationDialog.recenter()** — Reposition dialog to centre on new screen dimensions after window resize.
- **PocketScreen — Screen-scoped notifications** — `close()` now calls `NotificationManager.clearAll()` to prevent stale notifications from persisting across screens.

### Changed — Theme Awareness
- **Separator** — Default colour now resolves from `Theme.current().border()` instead of hardcoded `ProceduralRenderer.COL_BORDER`. Explicit `setColor()` overrides theme.
- **SpinnerComponent** — All colours (background, border, buttons, text, focus ring) now resolve from `Theme.current()` at render time. Explicit setters override individual colours.
- **SpinnerComponent — Limit visual feedback** — +/− buttons are greyed out with muted text when the value is at min/max.
- **RadioGroup** — All colours (radio circle, selected dot, text, hover) now resolve from `Theme.current()` at render time.
- **PercentageBar** — Default constructor now uses `Theme.current()` for track, bar, and text colours instead of hardcoded constants.

---

## [1.14.0] — 2025-07-14

### Fixed — Bug Fixes
- **Chunky mouse scrolling** — DarkPanel and SelectableList scroll handling now accumulates fractional scroll deltas instead of truncating to integers. High-resolution / smooth-scroll mice will see much smoother scrolling. Also fixed `scrollBy()` / `scrollByX()` for the same issue.
- **Overlay z-ordering** — PocketScreen now renders layers in strict order: UI tree → notifications → tooltips, ensuring tooltips always appear above notifications and other overlays. Also flushes `TooltipRenderer` queue automatically.
- **GUI left-offset positioning** — Added `centerHorizontally()`, `centerVertically()`, and `centerOnScreen()` helpers to UIComponent so screens can properly centre their root panels instead of positioning at x=0.

### Added — API Enhancements
- **UIComponent.centerHorizontally(int)** / **centerVertically(int)** / **centerOnScreen(int, int)** — Centering helpers for any component.
- **UIComponent.resize(int, int)** — Set size and cascade relative child resolution.
- **UIComponent.setId(String)** / **getId()** — Assign string IDs to components for tree queries.
- **UIComponent.findById(String)** — Depth-first tree search by component ID.
- **UIComponent.findByType(Class)** / **findAllByType(Class)** — Type-based tree queries.
- **UIComponent.withTooltip()** / **withEnabled()** / **withVisible()** / **withOpacity()** / **withOnClick()** — Fluent builder aliases.
- **Dropdown.setOnChange(Consumer)** — Standardised onChange alias for setOnSelect.
- **RadioGroup.setOnChange(Consumer)** — Standardised onChange alias.
- **SelectableList.setOnChange(Consumer)** — Standardised onChange alias.
- **SliderComponent.setOnChange(Consumer)** — Standardised onChange alias for setOnValueChanged.
- **ButtonModel()** — No-arg constructor (defaults to empty label).
- **ToggleModel()** — No-arg constructor (defaults to empty label, off).
- **Separator()** — No-arg constructor for use in layout panels.
- **RadioGroup(List\<String\>)** — Simplified constructor with just options.
- **PocketScreen.onBuildComplete(UIComponent)** — Lifecycle hook after buildUI completes.

---

## [1.13.0] — 2025-07-14

### Added — New Components & Systems
- **InputHelper** — Click/key decomposition, `InputMode` tracking (keyboard-mouse vs controller), modifier checks (`isShiftDown`, `isCtrlDown`, `isAltDown`).
- **ControllerGlyphs** — Xbox/PlayStation/Nintendo/Generic button prompt labels. `getActionLabel()`, `renderPrompt()`, `renderPromptWithLabel()`, `renderActionBar()` for 12 controller actions.
- **AnchorLayout** — 9-point anchor positioning (`TOP_LEFT` through `BOTTOM_RIGHT` + `CENTER`), plus `fill()`, `center()`, `distributeHorizontally()`, `distributeVertically()`.
- **TooltipRenderer** — Global tooltip render queue with `queue(Runnable)`, `flush(DrawContext)` for correct z-ordering.
- **VersionHelper** — Runtime Minecraft version detection via `SharedConstants`. `isAtLeast()`, `isExactly()`, `major()`/`minor()`/`patch()` accessors.
- **Model/View Architecture** — Five model classes for data/UI separation:
  - `ButtonModel` — label, enabled, pressed states + action binding
  - `SliderModel` — value/min/max + normalized get/set
  - `ToggleModel` — toggled/enabled states + toggle()
  - `ListModel` — item list management + selected index tracking
  - `TextModel` — text/placeholder + max length + append

### Added — API Enhancements
- **NotificationManager.showToast()** — Toast API replacing deprecated `FloatingText`. Two overloads with anchor and duration.
- **NotificationManager.showPersistent()** — Persistent notifications that stay on screen until explicitly dismissed. Includes pulsing accent stripe rendering.
- **NotificationManager.dismissPersistent()** / **getPersistent()** — Manage persistent notification lifecycle.
- **PocketNotifier.showClient()** — Client-side bridge methods that delegate to `NotificationManager.show()`.
- **VerticalListPanel.setSuppressLayout()** — Suppress auto-layout during batch child additions for performance.
- **GridPanel.setSuppressLayout()** — Same batch layout suppression for grid panels.
- **UISoundManager.schedulePlay()** — Schedule sounds with delay for multi-note sequences. Auto-ticked via client tick.
- **ScreenShakeHelper.triggerAdditive()** — Additive shake stacking that builds on existing shake intensity.
- **ProceduralRenderer.fillGradientH(int step)** — Quality-parameterized gradient with configurable pixel step.

### Changed
- **ProceduralRenderer SCANLINE_CACHE** — Converted from unbounded `HashMap` to LRU `LinkedHashMap` capped at 64 entries.
- **AnimationTicker** — Internal map changed from `ConcurrentHashMap` to `HashMap` (only accessed from client thread).

### Deprecated
- **FloatingText** — Deprecated in favour of `NotificationManager.showToast()`. Marked for removal.

---

## [1.12.0] — 2025-07-14

### Added — New Components
- **HorizontalListPanel** — Auto-layout panel that arranges children left-to-right with configurable padding and spacing.
- **FlowPanel** — Wrapping flow layout: children fill left-to-right, wrapping to the next row automatically.
- **ToggleSwitch** — Animated binary toggle with sliding knob, colour lerp, focus ring pulse, and sound feedback.
- **Dropdown\<T\>** — Generic typed dropdown with collapsed/expanded states, custom label extractor, scrollable list, and keyboard navigation.
- **Separator** — Horizontal or vertical line divider with configurable colour and thickness.
- **SpinnerComponent** — Numeric ─/+ input with step quantization, range clamping, keyboard/scroll support, and integer or float modes.
- **RadioGroup** — Single-selection radio button group with circular indicators, keyboard (Up/Down) navigation, and sound feedback.

### Added — New Systems
- **Theme** — Runtime theme system with `DARK` and `LIGHT` built-in palettes, builder pattern, and `Theme.current()`/`setCurrent()`/`resetToDefault()`.
- **PocketScreen** — Abstract base screen class with automatic FocusManager and ControllerHandler lifecycle management, MC 1.21.11 input forwarding, and tooltip rendering. Subclasses implement `buildUI()`.
- **DebugOverlay** — F3+P toggle overlay showing component bounds (colour-coded by hierarchy depth), class name/position/size labels, focused component highlight, and hovered component info at cursor.

### Added — API Enhancements
- **Fluent setters** — All setters across 9 existing components now return `this` for method chaining.
- **UIComponent.opacity** — New `setOpacity(float)`/`getOpacity()` field on the base component class.
- **UIComponent.disabledTooltipLines** — Separate tooltip shown when a component is disabled.
- **ProceduralRenderer** — `fillCircle()`, `drawCircle()`, `drawLine()` (Bresenham), `drawLine()` with thickness.
- **AnimationTicker.LoopMode** — `ONCE`, `LOOP`, `PING_PONG` loop modes for animations.
- **AnimationTicker.AnimationHandle** — Type-safe animation reference with `get()`, `get01()`, `isActive()`, `cancel()`.
- **AnimationTicker** — `startHandle()`, `startLooping()`, `startPingPong()` convenience methods.
- **FocusManager.registerTree()** — Recursively walks a component tree and registers all interactive components.
- **ObservableState.bindBidirectional()** — Two-way binding between two `ObservableState` instances with infinite-loop guard.
- **TabbedPanel tab transitions** — Configurable fade-in animation when switching tabs, with `setAnimateTransitions()` and `setTransitionDurationMs()`.
- **ScreenShakeHelper.withShake()** — Lambda API: `shake.withShake(ctx, () -> { ... })` wraps apply/restore in try/finally.
- **UISoundManager** — `setMasterVolume(float)`, `setMuted(boolean)`, `getMasterVolume()`, `isMuted()` global controls.
- **ScreenTintManager** — `transitionIn(durationMs)`, `transitionOut(durationMs)` screen transition helpers with optional custom colour.
- **HoverButton.setText()** / **PercentageBar.setText()** — Primary label setter; `setLabel()` is now `@Deprecated`.

### Changed
- **Economy subsystem decoupled** — Economy registration (payload types, join/disconnect handlers, tick events) is now controlled by `PocketUICore.setEconomyEnabled(boolean)`. Default is `true` for backward compatibility. Set to `false` before initialization if your mod only needs the UI framework.
- **settings.gradle** — Added `rootProject.name = 'pocketuicore'` for consistent artifact naming.

### Deprecated
- `HoverButton.setLabel(String)` — Use `setText(String)` instead.
- `PercentageBar.setLabel(String)` — Use `setText(String)` instead.

---

## [1.11.0] — 2025-07-08

### Fixed
- **InteractiveGrid** — Division-by-zero crash when columns/rows set to 0 (now clamped to ≥ 1)
- **InteractiveGrid** — Arrow-key navigation from unselected state now initializes to (0,0) instead of crashing
- **NotificationManager** — Thread-unsafe static collections replaced with synchronized/concurrent variants
- **FloatingText** — Thread-unsafe static list + stale stacking indices causing visual gaps after dismissal
- **EstateManager** — `ArithmeticException` when `EconomyConfig.ESTATE_SYNC_INTERVAL` is 0
- **PaginatedContainer** — Page position not restored to exact coordinates after slide animation completes
- **VerticalListPanel** — Missing empty-children guard causing negative scroll height
- **GridPanel / VerticalListPanel** — Scrollable flag never cleared when content shrinks below panel height
- **PocketMenuScreen** — TOCTOU race condition on wallet balance check-and-deduct
- **PocketMenuScreen** — Missing `mouseDragged` forwarding (sliders and drags inside the menu now work)
- **TabbedPanel** — Active tab index not adjusted when removing a tab before the active tab
- **PlayerVaultState** — Balance cap now enforces `EconomyConfig.MAX_BALANCE` instead of `Integer.MAX_VALUE`

### Changed
- Removed unused `slideFromX` field from `PaginatedContainer`
- Removed dead `oldVersion` variable from `UIDataStore`
- Removed unused `Text` import from `RichTooltip`

### Added
- **DEVELOPER_GUIDE.md** — Comprehensive developer documentation with usage examples for all components

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
