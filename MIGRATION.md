# Migration Guide

## Upgrading to 1.16.0

PocketUICore 1.16.0 moves the library target to **Minecraft 26.1.1** and Java 25.

### Breaking environment changes

- Minimum Minecraft version is now `26.1`.
- Minimum Java version is now `25`.
- Build toolchain and runtime dependencies are aligned to Fabric 26.1.1 artifacts.

### Internal migration highlights

- Rendering now routes through a shared `RenderContext` boundary with a 26.1 `GuiGraphicsAdapter` implementation.
- Payload and sync networking code was migrated to modern payload registration/type + codec APIs.
- Saved-data persistence moved to current `SavedData`/`SavedDataType` patterns used by 26.1.
- Screen, keyboard, mouse, and window call sites were updated to 26.1 signatures/accessors.

### Upgrade steps for dependent mods

1. Update your dependency to `com.pocketuicore:pocketuicore:1.16.0`.
2. Update your dev/runtime Java to 25.
3. Ensure your mod targets Minecraft/Fabric versions compatible with 26.1.1.
4. Rebuild and test all custom screens/components that integrate with PocketUICore.

## Upgrading to 1.10.0

PocketUICore 1.10.0 is **fully backward-compatible** with 1.9.0. All new APIs are additive — no existing method signatures were changed or removed.

### New APIs available

| Class | New Method(s) | Purpose |
|-------|--------------|---------|
| `ProceduralRenderer` | `drawText(ctx, text, x, y, color)` | Text overloads that auto-resolve `TextRenderer` |
| `ProceduralRenderer` | `drawCenteredText(ctx, text, cx, y, color)` | |
| `ProceduralRenderer` | `drawScaledText(ctx, text, x, y, color, scale)` | |
| `ProceduralRenderer` | `drawScaledCenteredText(ctx, text, cx, y, color, scale)` | |
| `ScreenShakeHelper` | `isShaking()`, `applyShake(ctx)`, `restoreShake(ctx)` | Naming aliases |
| `AnimationTicker` | `getProgress(id)` | Alias for `get01()` |
| `AnimationTicker` | `sequence(baseName)` | Fluent chained animation builder |
| `DarkPanel` | `renderTooltipPass(ctx, mouseX, mouseY)` | Second-pass tooltip rendering for scrollable panels |
| `UIComponent` | `mouseClicked(Click)`, `mouseReleased(Click)`, `keyPressed(KeyInput)` | MC 1.21.11 input record bridges |
| `FocusManager` | `popContext(String expectedName)` | Named context pop with assertion |
| `ClientNetworkHandler` | `onBalanceChanged(...)`, `onEstateGrowthChanged(...)` | Economy event listeners |
| `ControllerHandler` | `rumble(float intensity, int durationMs)` | Single-intensity rumble |

### Migration steps

1. Update your `build.gradle` dependency version to `1.10.0`.
2. (Optional) Replace verbose `TextRenderer` lookups with the new convenience overloads.
3. (Optional) Replace `get01()` calls with `getProgress()` for readability.
4. (Optional) Use `popContext("name")` instead of `popContext()` for safer context management.

No breaking changes — your existing code will compile and run without modification.

---

## Upgrading to 1.9.0

Backward-compatible with 1.8.0. Adds 15 new features (see CHANGELOG.md).

## Upgrading to 1.8.0

Backward-compatible with 1.7.0.

## Upgrading to 1.7.0

**Contains bug fixes for critical issues in 1.6.0:**
- Economy payload identifiers now use `Identifier.of("pocketuicore", "path")` instead of `CustomPayload.id("pocketuicore:path")`.
- If you had workarounds for the crash, you can remove them after upgrading.
