# PaletteBoard Product Spec

## Vision

PaletteBoard is an Android keyboard that competes on personalization rather than generic prediction alone. The goal is to deliver a high-quality daily-driver keyboard with a visual theme system deep enough for hobbyists, creators, accessibility-focused users, and power users who want more control than Gboard or SwiftKey typically expose.

## Target Users

- users who care about keyboard aesthetics and want to build their own visual style
- gamers, creators, streamers, and fandom communities who want identity-driven themes
- accessibility-conscious users who need higher contrast, larger keys, or dyslexia-friendly typography
- multilingual users who want language-aware layouts without giving up customization
- power users who want toolbar macros, snippets, gesture controls, clipboard history, and layout tuning

## Key Differentiators

- deep theme builder with per-key, per-row, and special-key styling
- live keyboard preview while editing themes
- import/export/shareable theme JSON format
- first-class gesture typing with themeable trail styling and sensitivity controls
- privacy-first local processing for typing, suggestions, theme storage, and gesture decoding
- ergonomic controls including one-handed mode, keyboard height, spacing, row sizing, and toolbar customization

## Major Feature Sets

### Core Keyboard

- full QWERTY layout with symbols/numbers and emoji mode
- number row toggle, keyboard height adjustment, key spacing, and popup previews
- shift, caps, enter, delete, spacebar gestures, and toolbar actions
- auto-capitalization and auto-spacing
- suggestion strip and auto-correction framework
- clipboard panel and text snippets

### Theme System

- preset library plus local custom theme library
- editable colors, gradients, borders, shadows, translucency, label sizing, and fonts
- separate styles for normal keys, function keys, spacebar, enter, shift, and backspace
- theme import/export via JSON and share sheet
- advanced per-row and per-key override mode

### Gesture Typing

- glide typing path capture over the key geometry model
- visual trail overlay with configurable color, width, and fade behavior
- candidate scoring against a local lexicon
- tap typing fallback without mode switching
- future gesture delete, gesture cursor control, and backspace swipe behaviors

### User Control

- customizable top toolbar
- sound packs and haptic profiles
- multilingual layout preferences
- incognito and local-only modes
- accessibility presets, high contrast mode, and dyslexia-friendly typography

## MVP Scope

- stable alphabet, symbols, and emoji keyboard layouts
- enable/select keyboard onboarding flow
- theme library with presets and save/delete/duplicate
- visual theme builder with live preview and JSON import/export
- Room theme persistence and DataStore user settings
- gesture trail rendering and local glide-decoder starter
- toolbar customization, height/spacing/number-row controls
- suggestion framework with local dictionary stub

## Future Scope

- robust on-device autocorrect and language models
- split keyboard and full tablet ergonomics
- pinned clipboard, snippets, and macro keys
- AI-assisted theme generation from prompt or wallpaper
- animated/RGB themes and marketplace distribution
- richer emoji/sticker search and content packs
- advanced handwriting, voice, translation, and rewrite tools

## Product Principles

- typing latency beats flashy effects
- customization must never make the core keyboard unstable
- privacy-sensitive features default to local processing
- every advanced option needs a clear reset path back to safe defaults
- theme sharing should be portable, inspectable, and backward-compatible

## MVP Success Criteria

- users can enable the keyboard, switch to it, and type reliably in daily apps
- users can create and save a theme in under two minutes
- glide typing works for common English words with believable candidate ranking
- no obvious frame drops during typing, gesture rendering, or theme preview updates
- import/export round-trips preserve theme fidelity
