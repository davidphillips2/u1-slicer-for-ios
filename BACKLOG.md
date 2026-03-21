# Backlog

Open bugs, features, and investigations. Everything else is done — see git log.

## Open Bugs

### B33: Print status stuck at 0% (GitHub #8)
- Reporter: kabakakao — printer status stays at 0% during a print
- Investigate the Moonraker polling loop in `PrinterRepository`/`MoonrakerClient`: is `print_stats.progress` being parsed and surfaced correctly?
- Check whether `PrinterStatus.printProgress` is wired to the UI and whether the polling interval is appropriate

### B34: Printer light button icon confused for app theme toggle (GitHub #7) — FIXED
- The button used `Icons.Default.LightMode` (sun) / `Icons.Default.DarkMode` (moon) — identical to Android's theme-switch icons
- Fixed: changed to `Icons.Default.Lightbulb` (yellow when on, dimmed when off) — clearly a physical light, not a UI theme toggle
- There is no app-level light/dark theme switch; the app is dark-only

### B35: "Upload" button confusingly does not start a print (GitHub #6)
- Reporter: ThorinOkenshield — expects the Upload button to start printing; the Print button works
- The Upload-only path is intentional (just deposits the file), but the UX distinction is unclear
- Options: rename "Upload" to "Upload Only", add a tooltip/subtitle, or prompt the user after upload asking if they want to start the print now

### B18: OOM on large/complex 3MF files
- Reproduce with: `C:\Users\kevin\Downloads\test-data\2026+F1+CALENDAR+-+DATES+&+TRACK+NAMES+(P_X+SERIES).3mf`
- This file is ~103 MB compressed and contains a `3D/Objects/object_9.model` entry that expands to ~680 MB
- Investigate memory usage across import, sanitize, embed, and native load for giant component-model files

### B31: First slip/slide slice can hit a native Clipper range crash
- Repro from `G:/My Drive/tes-data/clipper_investigation_bundle (1).txt`
- First slice attempt on `slip slide spin fidget.3mf` failed with `Coordinate outside allowed range`
- The failure path shows `hasCustomPlacement=true` and a wipe tower near the edge, so the placement/wipe-tower interaction needs another pass

### B32: Printer still shows the generic printer image after upload
- Repro from `G:/My Drive/tes-data/output (2).gcode`
- After upload, the printer falls back to its default/generic image instead of the file thumbnail
- Suspected cause: Moonraker/Klipper thumbnail handling or upload metadata caching still needs investigation

## Open Features

### F36: Plate type selector with bed-temp presets (GitHub #1, partial)
- Reporter: ThorinOkenshield — wants a plate type picker (Textured PEI, Smooth PEI, Cool Plate, Engineering Plate) that auto-adjusts bed temperature
- Textured PEI should be the default (current behavior)
- Changing plate type should update `bedTemp` in `SliceConfig` to the recommended value for that surface + filament combination
- The "slice settings default open" part of #1 is now done (v1.4.15)

### F35: Post-upgrade Clipper coordinate error — still occurring
- Related to B31 / old I2 fix: users still hit "Coordinate outside allowed range" crashes after an app upgrade
- Needs a fresh repro trace post-v1.4.x to see if the upgrade detector / stale-config clearing is firing correctly
- Check whether `UpgradeDetector` is clearing all relevant cached slice state on version bump

### F14: Mixed-colour / pseudo-extruder support (FullSpectrum fork)
- Source: ratdoux/OrcaSlicer-FullSpectrum — fork of Snapmaker Orca 2.2.4
- Produces optically-blended colours via layer-cycle alternation (e.g. Blue+Yellow→Green)
- **Blocked**: upstream was v0.9.4 alpha as of 2026-03-12, untested on real hardware
- Wait for v1.0 / hardware-verified release before porting
- Requires native .so rebuild

### F27: Cancel slicing mid-operation
- No way to abort a slice once started — user must wait or force-kill
- Requires native cancellation support (check OrcaSlicer for a cancel flag in the slicing loop)

## Closed (recent)
See git log for full history. Most recent fixes:
- **Upload-only bug**: "Print started!" was shown after upload-only; now shows "Uploaded successfully!" — FIXED
- **F28**: Prime tower waste in Slice Summary — amber "Prime Tower Waste" row in SliceCompleteSummaryCard — DONE
- **F33**: Feature-type color mode in 3D G-code viewer — Palette toggle in toolbar, 12-color feature palette — DONE
- **F29**: Prompt to slice when navigating to Preview with no result — "Slice Now" button on empty Preview when model is loaded — DONE
- **F30**: More support settings — XY distance, interface pattern/spacing, support speed, tree branch angle/distance/diameter — DONE
- **F31**: More infill/shell settings — top/bottom shell layers, top/bottom surface pattern, infill speed, expanded infill pattern list — DONE
- **F32**: Settings accordion opens on "Layer & Infill" by default on Prepare screen — DONE
- **F34**: Remote screen probe + button — auto-detects paxx12 firmware via HEAD `/screen/`, shows button in Printer screen when available — DONE
- **B32**: Thumbnail format switched to vanilla Klipper format (no `THUMBNAIL_BLOCK_START/END` wrappers, 76-char base64 lines) — matches u1-slicer-bridge which is confirmed working on Snapmaker hardware; the THUMBNAIL_BLOCK_START format added in v1.4.10 requires newer Moonraker and was causing the printer to show its default icon instead of the job preview — FIXED v1.4.11
- **B30**: Uploaded printer thumbnails now mirror Orca's native `THUMBNAIL_BLOCK_START/END` wrapping and line length so Moonraker/Klipper recognizes the embedded preview instead of showing the default icon
- **I2**: First post-update Clipper "Coordinate outside allowed range" failure hardened again — FIXED v1.4.1
- **Native Prepare Preview**: Prepare preview now uses native/Orca-backed mesh export instead of the old Kotlin-only path — DONE v1.4.0
- **B24**: Stale slice config (skirt/prime tower not updating on re-slice) — FIXED v1.3.42
- **B22/B23**: Multi-color preview race + extruder map mismatch — FIXED v1.3.37
- **F3/F25**: Per-vertex multi-color preview + extruder picker — DONE v1.3.36
