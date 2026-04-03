# PocketUICore - Master Reference

> **Last Updated:** 2026-04-03  
> **Maintainer:** Uipg9  
> **Purpose:** Single source of truth for architecture, migration status, and future development.

---

## 1. Current Architecture Snapshot

### 1.1 Stonecutter 0.9 Active Version Setup

PocketUICore uses [Stonecutter 0.9](https://github.com/kikugie/stonecutter) with a shared source root and a 26.1 version overlay.

**`settings.gradle` (critical DSL rules):**
```groovy
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins { id 'dev.kikugie.stonecutter' version '0.9' }
}
plugins { id 'dev.kikugie.stonecutter' }
rootProject.name = 'pocketuicore'

stonecutter.create(project(':')) {
    // versions() is a METHOD CALL, not property assignment
    versions '26.1'
    vcsVersion = '26.1'                  // vcsVersion is a Property<String> - use '='
}

// includeBuild MUST be at Settings level, OUTSIDE stonecutter.create()
includeBuild '../PocketCanvasCore'
```

**Stonecutter 0.9 DSL rules (hard-won):**

| Setting | Correct syntax | Wrong syntax |
|---|---|---|
| Declare versions | `versions '26.1'` | `versions = ['26.1']` |
| VCS version | `vcsVersion = '26.1'` | `vcsVersion '26.1'` |
| Active version | Read-only, do NOT set | ~~`stonecutter { active = current }`~~ |
| Composite build | Outside `stonecutter.create()` | Inside the block |

**Directory layout:**
```
Depend/
├── src/main/java/com/pocketuicore/   ← shared source
├── versions/
│   └── 26.1/
│       ├── gradle.properties
│       └── src/main/java/com/pocketuicore/render/GuiGraphicsAdapter.java
└── settings.gradle
```

---

### 1.2 Common Adapter Pattern (`RenderContext`)

**Problem:** Shared UI code (`UIComponent`, `HoverButton`, `DarkPanel`, etc.) should not depend directly on version-specific rendering entry points.

**Solution:** A version-neutral `RenderContext` interface in shared source. The active 26.1 overlay provides `GuiGraphicsAdapter`.

```
RenderContext (interface, shared src)
    └── GuiGraphicsAdapter   (versions/26.1/ - wraps GuiGraphicsExtractor)
```

**`RenderContext` interface (shared):**
```java
public interface RenderContext {
    void fill(int x1, int y1, int x2, int y2, int color);
    void fillGradient(int x, int y, int x2, int y2, int colorTop, int colorBottom);
    void drawTextWithShadow(TextRenderer tr, Text text, int x, int y, int color);
    void drawCenteredTextWithShadow(TextRenderer tr, Text text, int x, int y, int color);
    void matPush();
    void matPop();
    void matTranslate(float x, float y);
    void matScale(float sx, float sy);
    void drawItem(ItemStack stack, int x, int y);
    void drawStackOverlay(TextRenderer tr, ItemStack stack, int x, int y);
    void drawStackOverlay(TextRenderer tr, ItemStack stack, int x, int y, String countOverride);
    void enableScissor(int x1, int y1, int x2, int y2);
    void disableScissor();
    // Temporary bridge - remove after PocketCanvasCore is migrated
    default Object unwrapNative() { return null; }
}
```

**Why matrix ops are on the interface, not `getMatrices()`:**  
Matrix handling and coordinate transforms differ between game versions. Exposing native matrix stacks directly would leak version-specific types into shared code. Abstracting as `matPush/matPop/matTranslate/matScale` keeps the interface stable.

**`unwrapNative()` bridge - WHY it exists:**  
PocketCanvasCore (`PocketCanvas.*` methods) still accepts a native render object directly. Until PocketCanvasCore is refactored (see §4), shared components unwrap and cast:
```java
Object nativeCtx = ctx.unwrapNative();
// cast based on the active version adapter before calling PocketCanvas
```
**This is intentionally temporary.** Remove all `unwrapNative()` calls after PocketCanvasCore migration.

---

## 2. Migration Status - April 2026

| Version | Status | Java | Mappings | Notes |
|---|---|---|---|---|
| **26.1** | ✅ BUILD SUCCESSFUL | 25 | Official Mojang | Primary active release line |

### 26.1 - Active and validated

Build output confirmed for `:26.1:compileJava` and `:26.1:build`.  
Current release artifacts are produced from the 26.1 target.

The migration is no longer staged or disabled; 26.1 is configured as the active Stonecutter version in `settings.gradle`.

---

## 3. 26.1 Technical Specifications

### 3.1 Critical API Changes

**Partial ticks — `DeltaTracker`:**
```java
// 1.21.11
float delta = tickDelta; // passed directly

// 26.1
import net.minecraft.client.render.DeltaTracker;
float delta = deltaTracker.getGameTimeDeltaPartialTick(true);
```

**HUD overlays — `HudElementRegistry`:**
```java
// 1.21.11
HudRenderCallback.EVENT.register((ctx, tickDelta) -> { ... });

// 26.1
HudElementRegistry.register(new HudElement() {
    @Override public void render(GuiGraphics gfx, DeltaTracker dt) { ... }
});
```
Current `HudCompat.java` in `versions/26.1/` wraps this — verify import paths when re-enabling.

**Items — `ItemStack` → `ItemStackTemplate`:**  
26.1 introduces `ItemStackTemplate` for static/template item references. `ItemStack` remains for runtime instances. Audit all `drawItem()` call sites when activating 26.1.

**Removed render type:**  
`core/rendertype_translucent_moving_block` is removed in 26.1. If any component relied on this (unlikely for UI), substitute with `RenderType.translucent()`.

### 3.2 `versions/26.1/gradle.properties` (current)
```properties
mc_version=26.1
minecraft_version=26.1.1
fabric_api_version=0.145.3+26.1.1
loader_version=0.18.6
controlify_version=
```

### 3.3 `GuiGraphicsAdapter` notes
```java
@Override public void drawTextWithShadow(...)   { ctx.drawString(tr, text, x, y, color); }
@Override public void drawCenteredTextWithShadow(...) { ctx.drawCenteredString(tr, text, x, y, color); }
@Override public void matPush()  { ctx.pose().pushPose(); }
@Override public void matPop()   { ctx.pose().popPose(); }
```
These adapter paths were validated as part of the 26.1 compile/build migration.

---

## 4. Roadmap: PocketCanvasCore Refactor

**Goal:** Remove all `unwrapNative()` casts from shared PocketUICore source.

### Step-by-step

**Step 1 — Add `RenderContext` dependency to PocketCanvasCore**  
Either:
- Copy the `RenderContext` interface into PocketCanvasCore, or
- Extract it to a shared `pocketcore-api` module both mods depend on.

**Step 2 — Update `PocketCanvas` static methods**
```java
// Before (PocketCanvasCore)
public static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) { ... }

// After
public static void drawRoundedRect(RenderContext ctx, int x, int y, int w, int h, int r, int color) { ... }
```
Inside these methods, call `ctx.fill(...)`, `ctx.fillGradient(...)`, etc. — not DrawContext directly.

**Step 3 — Remove bridge from PocketUICore**

Once PocketCanvasCore is migrated, remove from each call site:
```java
// Remove these lines:
DrawContext dc = (DrawContext) ctx.unwrapNative();
// Change:
PocketCanvas.drawRoundedRect(dc, ...);
// To:
PocketCanvas.drawRoundedRect(ctx, ...);
```

**Affected files in PocketUICore:**
- `src/main/java/com/pocketuicore/component/DarkPanel.java` — 3 call sites
- `src/main/java/com/pocketuicore/component/HoverButton.java` — 2 call sites
- `src/main/java/com/pocketuicore/component/UIComponent.java` — tooltip rendering

**Step 4 — Remove `unwrapNative()` from `RenderContext`**  
Once no call sites remain, delete the `default Object unwrapNative()` method from the interface and its overrides in adapters.

---

## 5. Future-Proofing for 26.2

### Expected Changes
- **Rendering backend shift**: `GuiGraphics` may evolve toward new backend abstractions.
- **`RenderContext` insulates shared code**: The adapter pattern means only overlay adapters need updates; shared components remain untouched.

### Prep Checklist
- [ ] Keep all rendering calls inside `RenderContext` methods — never bypass to native API from shared code
- [ ] When adding new rendering features, add the abstract method to `RenderContext` first, then implement in both adapters
- [ ] Monitor [FabricMC/fabric](https://github.com/FabricMC/fabric) releases for 26.2 alpha toolchains
- [ ] Monitor [FabricMC/yarn](https://github.com/FabricMC/yarn) for new mapping builds
- [ ] Prepare `versions/26.2/` overlay directory in advance (copy 26.1 as template)

### Java Version Matrix
| MC Version | Java | Toolchain path |
|---|---|---|
| 26.1 | 25 | `C:/Program Files/Java/jdk-25.0.2` |
| 26.2 | 25+ | TBD |

---

## 6. Coding Standards

### 6.1 The 80/150 Efficiency Rule

- **80%** of logic lives in shared `src/main/` - minimize version overlays
- **150-char soft cap** on method bodies - extract helpers if exceeded
- Prefer dense logic over verbosity: adapters are glue code, not business logic
- No defensive null-checks on internal wiring — trust the adapter contract

### 6.2 RenderContext Contract

Always pair `matPush()` / `matPop()` - use `withShake(ctx, runnable)` style lambdas where possible to guarantee cleanup:
```java
ctx.matPush();
try {
    ctx.matTranslate(x, y);
    // ... render ...
} finally {
    ctx.matPop();
}
```

### 6.3 Component Authoring Rules

- All setters return `this` (fluent API)
- Keep data in model class (`component/model/`), rendering in component
- `blockInputInBounds` defaults to `false` on containers - callers opt in
- Tooltip text set via `setTooltip()` - never rendered manually in `renderSelf()`

### 6.4 Mappings Rule

**Use mappings consistent with the active target.** The 26.1 line uses official Mojang mappings in Loom, so shared and overlay code should follow Mojang names used by the active source set.

---

## 7. Build Reference

```bash
# Gradle JVM must be Java 25+ - set explicitly if needed
export JAVA_HOME="C:/Program Files/Java/jdk-25.0.2"

# Build all active versions
./gradlew build

# Build specific version
./gradlew :26.1:build

# Publish to local Maven (for dependent mods)
./gradlew publishToMavenLocal

# Clean
./gradlew clean
```

**PocketCanvasCore** must be present at `../PocketCanvasCore` — it is included as a Gradle composite build via `includeBuild '../PocketCanvasCore'` in `settings.gradle`.
