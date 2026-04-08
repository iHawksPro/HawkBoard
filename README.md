# HawkBoard

HawkBoard is a production-minded Android custom keyboard starter focused on deep personalization, theme authoring, and first-class glide typing.

This scaffold includes:

- a modern Android app shell in Kotlin
- a real `InputMethodService` entry point
- a custom keyboard rendering layer for the IME window
- Room-backed theme persistence and DataStore-backed settings
- a flexible theme model with per-key and per-row overrides
- Compose management screens for dashboard, settings, theme library, and theme builder
- starter gesture trail rendering and swipe-decoding infrastructure

Reference docs:

- [Product Spec](docs/PRODUCT_SPEC.md)
- [System Architecture](docs/SYSTEM_ARCHITECTURE.md)
- [Feature Backlog](docs/FEATURE_BACKLOG.md)

The code is intentionally scaffolded for iteration rather than claiming full production completeness on day one. The IME, layout engine, suggestion engine, and gesture decoder are structured so they can be hardened without rewriting the app shell.
