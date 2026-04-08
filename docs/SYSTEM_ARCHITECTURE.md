# PaletteBoard System Architecture

## Recommended Architecture

Use a pragmatic clean architecture:

- `ui` for Compose screens, navigation, and view models
- `ime` for `InputMethodService`, keyboard rendering, touch handling, and toolbar/suggestion strip views
- `engine` for keyboard layout generation, theme resolution, gesture decoding, and suggestions
- `data` for Room entities/DAO, DataStore settings, repositories, and import/export
- `domain` for serializable models and business-facing types
- `app` for DI container and application bootstrap

The scaffold keeps everything in a single Android app module for faster iteration. Once the product stabilizes, split into Gradle modules such as `core-model`, `core-data`, `ime-engine`, `feature-settings`, and `feature-theme-builder`.

## Module Breakdown

- `app`: application wiring, container, startup seeding
- `domain`: `Theme`, `KeyStyle`, `KeyboardLayout`, `UserSettings`, `GestureSettings`, `SoundPack`, `HapticProfile`, `ThemeExportFormat`
- `data`: Room database for theme library and future clipboard/snippets, DataStore for user settings, JSON import/export
- `engine/theme`: active theme resolution, per-key override lookup, default presets
- `engine/layout`: QWERTY/symbols/emoji layout factories
- `engine/gesture`: touch-path simplification, candidate scoring, glide decoding
- `engine/suggestion`: local lexicon access and prefix suggestions
- `ime/controller`: input pipeline, shift/mode state, commit behavior, auto-spacing, toolbar actions
- `ime/view`: custom canvas key rendering, trail overlay, toolbar, suggestion strip
- `ui/screen`: dashboard, settings, theme library, theme builder, import/export, gesture and haptic controls

## IME Service Structure

- `PaletteInputMethodService` owns the keyboard session
- it observes `UserSettings` and the active `Theme`
- it creates `KeyboardRootView` in `onCreateInputView()`
- it asks `KeyboardController` for the active layout and action handling
- it uses `currentInputConnection` to commit text, delete text, and query context for suggestions
- it updates the root view when theme, settings, suggestions, or layout mode change

Recommended session pipeline:

1. `onStartInput()` inspects `EditorInfo`
2. `KeyboardController` selects initial mode and shift behavior
3. `KeyboardRootView` renders toolbar, suggestions, and key grid
4. taps or gesture samples are sent back to the service
5. controller commits actions through `InputConnection`
6. suggestion strip refreshes from surrounding text context

## State Management

- `MainViewModel` owns app-side dashboard/theme-builder state
- `ThemeManager` exposes the active resolved theme as a flow
- `SettingsRepository` exposes a `Flow<UserSettings>`
- `ThemeRepository` exposes a `Flow<List<Theme>>`
- IME service keeps a service-local render state and only redraws when meaningful inputs change

## Theme Engine Design

### Model Strategy

Theme data is serializable JSON with layers:

- keyboard background
- default key style
- function-key style set
- popup style
- toolbar style
- gesture trail style
- row overrides
- per-key overrides

Each key render asks `ThemeManager.resolveKeyStyle(theme, key)` so the drawing layer never hardcodes visual rules.

### Resolution Order

1. per-key override
2. row override
3. special-key bucket override
4. default key style

### Theme Builder Strategy

- live-edit a draft `Theme`
- preview against a fixed sample layout
- persist drafts to Room as full JSON
- export via `ThemeExportFormat`
- validate import format version before save

## Gesture Typing Engine Design

Use a geometry-driven local decoder:

1. capture raw finger points
2. simplify points with distance and angle thresholds
3. convert touched key geometry into a de-duplicated character signature
4. build a centroid path for candidate dictionary words
5. score candidate words by:
   - first/last letter match
   - condensed letter signature similarity
   - average distance between resampled input path and candidate centroid path
   - language frequency prior
6. commit the top candidate and expose alternates in the suggestion strip

This is the best practical MVP approach because it avoids needing a full ML model while still behaving like a real swipe keyboard. The decoder can later be upgraded to a weighted finite-state decoder or on-device neural ranking model without changing the touch pipeline.

## Storage Approach

- Room:
  - custom themes
  - preset metadata
  - future clipboard history
  - future snippets/macros
- DataStore:
  - active theme id
  - keyboard feature toggles
  - gesture/sound/haptic settings
  - privacy settings
  - toolbar configuration
- JSON file exchange:
  - theme import/export and future sharing payloads

## Performance Considerations

- keep the IME rendering path in Views/Canvas, not Compose
- avoid inflating per-key child views for the main typing surface
- cache text measurement and paints in the keyboard canvas
- redraw only the keyboard layer or trail overlay that changed
- keep gesture decoding off the main thread once the MVP path-capture flow is stable
- cap blur/translucency and animated theme effects on low-end devices
- debounce expensive suggestion refresh work after rapid deletions

## Privacy and Security

- default to local-only suggestions and glide decoding
- never send typed text off-device without explicit opt-in
- mark sensitive-field sessions and disable clipboard/suggestion learning in them
- keep import/export payloads human-readable JSON so users can inspect them
- avoid logging input text in debug statements
- isolate analytics from raw typed content

## Hard Parts and Tradeoffs

- real autocorrect quality is harder than theming or layout work
- gesture typing quality depends heavily on lexicon quality and language-frequency data
- split layout and tablet ergonomics add significant geometry complexity
- blur and animated backgrounds can easily hurt latency on low-end devices
- per-key advanced editing must stay discoverable or it becomes overwhelming

## Roadmap

### MVP

- reliable layouts, theme persistence, theme builder, onboarding, local gesture typing starter

### V2

- better autocorrect, clipboard/snippets, richer toolbar customization, multilingual refinement

### V3

- split/tablet layouts, AI theme generation, animation packs, marketplace, advanced productivity tools

## Suggested Folder Structure

```text
app/
  src/main/java/com/paletteboard/
    MainActivity.kt
    app/
      AppContainer.kt
      PaletteBoardApp.kt
    data/
      local/
      mapper/
      repository/
    domain/
      model/
    engine/
      gesture/
      layout/
      suggestion/
      theme/
    ime/
      PaletteInputMethodService.kt
      controller/
      view/
    ui/
      designsystem/
      main/
      navigation/
      screen/
      state/
    util/
  src/main/res/
    values/
    xml/
docs/
  PRODUCT_SPEC.md
  SYSTEM_ARCHITECTURE.md
  FEATURE_BACKLOG.md
```
