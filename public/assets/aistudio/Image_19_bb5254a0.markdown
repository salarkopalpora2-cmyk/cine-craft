---
name: CineCraft AI Studio
colors:
  surface: '#f7f9ff'
  surface-dim: '#d5dae3'
  surface-bright: '#f7f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4fd'
  surface-container: '#e9eef7'
  surface-container-high: '#e3e8f1'
  surface-container-highest: '#dee3ec'
  on-surface: '#161c22'
  on-surface-variant: '#434655'
  inverse-surface: '#2b3138'
  inverse-on-surface: '#ecf1fa'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#4b41e1'
  on-secondary: '#ffffff'
  secondary-container: '#645efb'
  on-secondary-container: '#fffbff'
  tertiary: '#6a1edb'
  on-tertiary: '#ffffff'
  tertiary-container: '#8343f4'
  on-tertiary-container: '#f7edff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#e2dfff'
  secondary-fixed-dim: '#c3c0ff'
  on-secondary-fixed: '#0f0069'
  on-secondary-fixed-variant: '#3323cc'
  tertiary-fixed: '#eaddff'
  tertiary-fixed-dim: '#d2bbff'
  on-tertiary-fixed: '#25005a'
  on-tertiary-fixed-variant: '#5a00c6'
  background: '#f7f9ff'
  on-background: '#161c22'
  surface-variant: '#dee3ec'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers an ultra-clean, compliant Material 3 studio experience calibrated specifically for high-efficiency mobile creative workflows. It balances utilitarian precision with an approachable, modern feel, prioritizing high readability, fluid visual rhythm, and zero visual clutter.

### Personality & Emotional Response
- **Surgical Precision & Trust:** Evoked through strict geometric structure, crisp contrast, and canonical royal blue accents that inspire confidence during complex rendering, audio editing, and generative video tasks.
- **Approachable Sophistication:** The tactile, generously radiused components counter the intimidating nature of generative AI video pipelines, transforming heavy-duty studio tooling into a welcoming, fluid canvas.
- **Studio Polish:** Subtle ambient shadows tinted with indigo undertones and deliberate surface hierarchy emulate high-end creative hardware and premier native Android studio applications.

### Design Movement & Aesthetic Direction
- **Material 3 Expressive Studio:** Adheres to M3 tonal elevation tiers, dynamic surface layering, and canonical bottom navigation paradigms while introducing hyper-focused production studio touches: dedicated deep-midnight preview canvases, high-density tabular metadata displays, and deliberate 0.95x kinetic micro-feedback states.

## Colors

The palette leverages a crisp, cool foundation accented by saturated royal and indigo primaries, establishing an unambiguous hierarchy for media management, real-time capture, and sequence compilation.

### Surface Architecture & Core Roles
- **App Canvas (`#EDF2FB`):** A soft, cool slate blue backdrop serving as the base layer (`surface-dim`), preventing eye fatigue in extended mobile sessions while lifting foreground content naturally.
- **Card & Component Surface (`#FFFFFF`):** High-clarity white for active tool panels, inspector cards, and interactive modal surfaces, banded with subtle structural outlines (`#E0E7FF`).
- **Alternating Tint (`#F9FBFF`):** Micro-tinted neutral applied across data rows, script line trackers, and asset lists to sustain scannability without heavy border dividers.
- **Preview Canvas (`#070E1C`):** Deep Midnight viewport surface isolating active timeline previews, video playback monitors, and AI masking viewports.

### Primary Accents & Semantic Signals
- **Primary Action (`#2563EB`):** Canonical Royal Blue commanding focal attention on floating triggers, key confirmation targets, and primary active states.
- **Secondary Interactive (`#4F46E5`):** Indigo driving segmented selections, tool toggle groups, and active navigation indicators.
- **Status - Export & Complete (`#059669`):** Emerald indicating finished renders, active audio levels, and validated exports.
- **Status - Terminate & Danger (`#E11D48`):** Vivid Rose signalling recording stops, layer deletion, and timeline clipping limits.
- **Status - Warning & Trim (`#D97706`):** Amber for frame-drop notifications, clip trimming warnings, and storage alerts.
- **Feature & Schedule (`#7C3AED`):** Violet identifying AI model selection, scheduled posts, and voice synthesis parameters.

## Typography

The typographic hierarchy accommodates technical density, rapid asset scanning, and seamless dual-script rendering across Latin and Bengali orthographies.

### Dual-Script & Monospaced Stack
- **Primary Latin & Bengali Display:** `Plus Jakarta Sans` pairs with fallback `Hind Siliguri` across headline, body, and label roles. Typographic line heights incorporate extra vertical clearance to accommodate Bengali conjunct ascenders and descenders without clipping.
- **Precision Data Stack:** `JetBrains Mono` handles all numerical metrics, frame counters (SMPTE timecodes), render percentages, and timeline millisecond labels using `font-variant-numeric: tabular-nums` to eliminate jitter during active updates.

### Scaling & Legibility Directives
- **Micro-Hierarchy:** Dynamic video timestamps and clip badges leverage `label-sm` and `code-sm` with strict uppercase tracking (+0.5px) for instant recognition over complex layered frames.
- **Reading Rhythm:** Paragraph body text maintains a strictly controlled line-height ratio (1.45–1.5x) to prevent density fatigue during script reading and prompt curation.

## Layout & Spacing

This layout adheres strictly to an 8dp baseline grid (with a 4dp micro-step), tuned for one-handed mobile ergonomics and quick thumbnail browsing.

### Mobile Grid & Canvas Constraints
- **Grid Architecture:** 4 fluid columns on mobile devices (`<600dp`) with `16px` (`1rem`) gutters and `16px` margins. Expand to 8 fluid columns on foldable/tablet landscape modes with `24px` margins.
- **Top Bar System:** Fixed 64dp sticky top bar housing the studio brand anchor alongside 4 horizontal action slots (Undo/Redo, AI Inspector, Canvas Options, Export Profile).
- **Bottom Navigation Dock:** Fixed 80dp M3 navigation dock containing 6 dedicated studio workspaces (Agent, Voice, Capture, Video, Gallery, Tailor). Content containers maintain an explicit bottom padding of `96px` to avoid navigation collision.
- **Preview Monitor Ratio:** Dark preview viewports enforce strict 16:9, 9:16, or 1:1 aspect ratios pinned to screen margins, maintaining edge bleed when entering full-canvas production mode.

## Elevation & Depth

Visual depth follows a tactile, light-drenched studio model relying on soft blue-tinted ambient attenuation rather than high-contrast, dirty grey drop shadows.

### Elevation Hierarchy
- **Level 0 (Flat / Canvas):** Applied to the `#EDF2FB` workspace surface and inactive timeline tracks. No shadow; structural definition provided by element contrast.
- **Level 1 (Card & Module Resting):** Primary content cards and module blocks utilize a crisp 1dp border (`#E0E7FF`) paired with an ambient shadow: `0px 4px 20px -2px rgba(37, 99, 235, 0.06)`.
- **Level 2 (Active Studio Cards & Floating Triggers):** Draggable timeline items, focused script cards, and floating primary action triggers step up: `0px 8px 24px -4px rgba(37, 99, 235, 0.12)`.
- **Level 3 (Sticky Headers & Dock Navigation):** Sticky upper controls and the 6-tab bottom navigation dock float over canvas content using `0px -4px 24px 0px rgba(7, 14, 28, 0.04)` combined with a background surface opacity of 96% and a 12px backdrop blur.
- **Level 4 (Modals & Sheet Drawers):** Bottom asset sheets and render status overlays utilize `0px 16px 36px -6px rgba(7, 14, 28, 0.16)`.

## Shapes

The design system employs an expressive, ultra-rounded aesthetic characterized by generous structural radii and pill-shaped interactive anchors that soften technical UI density.

### Shape Tiers & Curvature
- **Pill Radius (`9999px`):** Used universally across interactive buttons, active indicator capsules in the bottom navigation bar, floating status badges, segmented control selections, and text input fields.
- **Large Cards & Drawers (`24px / rounded-3xl`):** Primary structural surface wrappers, tool inspector panels, and media timeline cards consistently adhere to 24px corner radii.
- **Small Components (`12px`):** Timeline track thumbnails, secondary tag chips, and dropdown flyouts maintain a 12px inner corner radius, guaranteeing harmonious concentric curves within 24px parent cards.

## Components

### Buttons & Interactive Touchpoints
- **Primary Action Buttons:** Height: 48dp. Fully pill-rounded (`9999px`), rendered in Primary Royal Blue (`#2563EB`) with high-contrast white text. Upon active press, triggers an immediate, hardware-accelerated scale transform to `0.95x` with an ease-out timing curve (150ms).
- **Secondary Studio Buttons:** Outlined with a 1.5dp border in `#E0E7FF`, white background, and `#2563EB` text. Active state adopts `#F9FBFF` tint with matching `0.95x` scaling.
- **Icon Action Buttons (Top Bar):** 40dp circular touch targets housing a 20dp icon. Ambient background in `#FFFFFF` with Level 1 elevation to ensure immediate tap accuracy.

### Navigation Systems
- **Sticky Top Bar:** Compact 64dp bar with solid `#FFFFFF` backing and a bottom border in `#E0E7FF`. Houses the brand identity on the left and 4 quick-action utility icons aligned horizontally to the right with 8dp spacing.
- **6-Tab Fixed Bottom Dock:** Height: 80dp. Sits flush against the bottom edge. Tabs (Agent, Voice, Capture, Video, Gallery, Tailor) display vertical stacks: 24dp icon above a `label-sm` text string. The active tab displays an elongated pill indicator capsule (`#4F46E5` at 12% opacity) enveloping the icon, with active text and icon colored in `#2563EB`. Badges appear as 8dp red dots (`#E11D48`) or pill counters anchored to the icon's top-right corner.

### Cards & Media Panels
- **Standard Studio Cards:** White background (`#FFFFFF`), 24px corner radius, framed by a continuous 1dp border (`#E0E7FF`) and soft blue-tinted elevation. Internal padding defaults to `16px`.
- **Preview Monitor Frame:** Dark preview surface (`#070E1C`) with a 24px border radius, nested timecode badge (`code-sm` over `#000000` at 60% opacity), and inset clipping controls.

### Segmented Controls & Chips
- **Studio Segmented Control:** Integrated track container in `#EDF2FB` with a 24px radius and 4dp padding. Active segment transitions via a floating white pill card carrying Level 1 elevation and crisp `#2563EB` text.
- **Status & Parameter Chips:** Pill-shaped capsules (32dp height) containing inline semantic icons. Inactive chips use a transparent fill with a 1dp `#E0E7FF` border; active chips switch to a solid light wash (e.g., `#059669` at 12% fill with `#059669` text).

### Form Controls & Inputs
- **Text & Prompt Fields:** 52dp height, fully pill-rounded, `#FFFFFF` fill, bounded by a 1.5dp `#E0E7FF` outline. On focus, outline transitions to `#2563EB` accompanied by a 3dp exterior ring (`rgba(37, 99, 235, 0.15)`).
- **Selection Controls:** Checkboxes and radio targets display 20dp dimensions with a 2dp stroke in `#2563EB` when selected, providing instant tactile haptic confirmation.