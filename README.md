# Smooth Quest Canvas

Give your FTB Quests book a smoother feel when exploring a large quest tree.
Drag empty canvas space with the left mouse button: the view follows smoothly
and gently slows down after release. Press again to catch the moving canvas.

The mod also preserves fractional canvas offsets while rendering, reducing
the visible stepping that can occur near the end of a slow movement.

## Features

- Smooth dragging and configurable release inertia.
- Small pointer movements do not launch the canvas.
- Keeps normal task clicks, navigation, and zoom behavior.
- Editing mode retains FTB's immediate movement and node editing.
- Does not add quests, items, progression requirements, or network packets.

## Installation

Install on the **client** with **Minecraft 1.21.1**, **NeoForge 21.1.x**, and
**FTB Quests 2101.x** plus FTB's usual dependencies. The server does not need
this add-on. Remove older Smooth Quest Canvas JARs when upgrading.

Eclosion is not required. Certain Questing Additions is optional; its smooth
zoom has been tested alongside this mod.

Tested with NeoForge 21.1.259, FTB Quests 2101.1.36, and FTB Library 2101.1.37.
The accepted version ranges are wider than the versions tested. If a required
canvas interface is found to be incompatible, the add-on skips its canvas
mixins, logs the reason, and keeps native FTB behavior.

## Configuration

After the first client launch, edit `config/smooth_quest_canvas-client.toml`
while the game is closed:

```toml
smoothQuestPanning = true
questPanFollow = 18.0
questPanFriction = 7.0
```

Higher follow values make the view follow your pointer faster. Higher friction
values make it stop sooner. Set `smoothQuestPanning` to `false` to restore the
original dragging and rendering behavior.

MIT licensed. An independent add-on, not affiliated with or endorsed by FTB.

Development disclosure: this project's code and this page draft were produced
with substantial AI coding and writing assistance. The supplied icon draft is
also AI-assisted, programmatically drawn artwork. This is a disclosure, not a
claim of eligibility under any platform's publication rules.
