---
name: LumiNa Academic
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf6'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e3ff'
  on-surface: '#0b1c30'
  on-surface-variant: '#434654'
  inverse-surface: '#213145'
  inverse-on-surface: '#ebf1ff'
  outline: '#737685'
  outline-variant: '#c3c6d6'
  surface-tint: '#1155d0'
  primary: '#003b9a'
  on-primary: '#ffffff'
  primary-container: '#0050cb'
  on-primary-container: '#c1cfff'
  inverse-primary: '#b3c5ff'
  secondary: '#565e74'
  on-secondary: '#ffffff'
  secondary-container: '#d7dff9'
  on-secondary-container: '#5a6278'
  tertiary: '#004d33'
  on-tertiary: '#ffffff'
  tertiary-container: '#026746'
  on-tertiary-container: '#8fe2b8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#001849'
  on-primary-fixed-variant: '#003fa4'
  secondary-fixed: '#dae2fc'
  secondary-fixed-dim: '#bec6e0'
  on-secondary-fixed: '#131b2e'
  on-secondary-fixed-variant: '#3e465b'
  tertiary-fixed: '#a0f4c9'
  tertiary-fixed-dim: '#85d7ae'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e3ff'
  surface-bg: '#f8f9ff'
  outline-muted: '#c2c6d8'
  tertiary-accent: '#10b981'
typography:
  display:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  3xl: 64px
  margin-desktop: 40px
  margin-mobile: 16px
  gutter: 24px
  max-width: 1440px
---

## Brand & Style
LumiNa is a high-fidelity academic e-learning platform that bridges the gap between technical rigor and modern productivity. The brand personality is **Elite, Precise, and Intellectual**, targeting researchers, advanced students, and institutional administrators who demand technical excellence.

The visual style is **Corporate Modern with a Glassmorphic edge**. It utilizes a "Bento Box" layout logic to organize complex information into digestible, high-contrast modules. The aesthetic balances a "Light & Airy" interface with deep "Midnight" sections to establish authority and focus during intense study sessions. The use of italics in headlines adds a touch of academic sophistication and motion to the otherwise structured grid.

## Colors
The palette is rooted in **Academic Blues and Slate**. 
- **Primary Blue (#0050cb):** Used for primary actions, active navigation states, and brand identity.
- **Surface Tiers:** The system uses a multi-layered light blue background (#f8f9ff) to distinguish between the canvas and interactive containers.
- **Inverse Surface:** A deep navy (#213145) is used for "Deep Focus" areas like course catalogs or call-to-action sections to provide a high-contrast break in the visual flow.
- **Success/Tertiary:** A refined emerald green is used for certification badges and progress indicators, signifying achievement without being jarring.

## Typography
The typography system uses a tri-font approach to categorize information types:
- **Hanken Grotesk (Headlines):** A sharp, contemporary grotesque for all display and heading roles. It should occasionally use *italic* weights for emphasis in hero sections.
- **Inter (Body):** A highly legible, utilitarian sans-serif for reading-heavy content and UI labels.
- **JetBrains Mono (Technical/Metadata):** Used for "code-sm" roles, module counts, and technical identifiers to reinforce the platform's precision and "developer-ready" feel.

## Layout & Spacing
LumiNa utilizes a **12-column Fixed Grid** with a maximum content width of 1440px. 
- **The Bento Grid:** For feature sections, a grid with 24px gutters is used. Elements should span increments of 4 columns (e.g., a 1/3 to 2/3 ratio).
- **Asymmetry:** Hero sections should use a 50/50 or 60/40 split to create visual interest.
- **Padding:** Vertical section rhythm follows a 64px (3xl) or 48px (2xl) scale to ensure breathing room between dense information blocks.
- **Responsibility:** On mobile, margins reduce to 16px and bento columns collapse to a single-column stack.

## Elevation & Depth
LumiNa creates depth through **Tonal Layering** and **Subtle Shadows**:
- **Glassmorphism:** Secondary floating widgets use a white semi-transparent background (70% opacity) with a 12px backdrop-blur.
- **Shadows:** A soft "btn-shadow" (0px 4px 12px rgba(15, 23, 42, 0.08)) is applied to primary buttons and cards to make them feel "tapped" onto the surface.
- **Borders:** Every container (cards, inputs, nav) must have a 1px border (#c2c6d8 at low opacity) to maintain structure in the light-themed environment.

## Shapes
The shape language is **Soft & Geometric**. 
- Standard buttons and inputs use a 0.25rem (4px) or 0.5rem (8px) radius.
- Cards and main containers use a 0.75rem (12px) radius (rounded-xl) to feel modern and approachable.
- Badges and "New" tags use a fully rounded "pill" shape to contrast against the more structured rectangular grid of the content cards.

## Components
- **Buttons:** Primary buttons are solid Blue (#0050cb) with white text and a soft shadow. Secondary buttons are outlined with 1px borders.
- **Cards:** Use "Bento" style cards with internal padding of 32px (xl). They should feature a hover state that lightens the border color or slightly scales background images.
- **Inputs:** Search bars should be integrated into the surface (surface-container) with no background, defined only by a subtle border and 20px icons.
- **Course Cards:** Dark-themed cards should use semi-transparent overlays on images (20% black to transparent) to ensure text legibility.
- **Badges:** Small, uppercase labels with increased letter spacing (0.05em) using the JetBrains Mono font for a technical metadata appearance.