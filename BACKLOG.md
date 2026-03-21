# Backlog

Open bugs, features, and investigations. Everything else is done — see git log.

## Open Bugs

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

### F29: Prompt to slice when navigating to Preview with no current slice
- If the user taps Preview but no slice result exists (idle bed), show a dialog or snackbar asking if they want to slice now
- Should respect current model/settings and trigger the slice pipeline on confirmation

### F30: Support settings expansion
- Expose more support-structure settings in the UI: support type (normal/tree), support interface layers, interface spacing, support expansion, XY distance, Z distance, threshold angle, support-on-build-plate-only toggle
- Currently most of these fall back to OrcaSlicer defaults; add to `buildProfileOverrides()` and the settings panel

### F31: More infill settings
- Expose infill pattern (grid, gyroid, honeycomb, etc.), infill anchor length, infill overlap, and sparse infill speed
- Add keys to `buildProfileOverrides()` and the SlicingOverrides system

### F32: Settings panel open / expanded by default on Prepare screen
- Settings accordion is currently collapsed on entry; make it default-open so users see the key knobs immediately
- Should persist the expanded/collapsed state in DataStore so the user's preference is remembered

### F33: G-code preview visual improvements — DONE (feature-type colors)
- `GcodeParser` now tags each move with a `featureType` byte (OUTER_WALL, INNER_WALL, SPARSE_INFILL, etc.)
- `GcodeRenderer` has a `useFeatureColors` mode that swaps extruder-based coloring for a 12-color feature-type palette
- Toggle via the Palette icon button in the 3D viewer toolbar (`GcodeViewer3DScreen`); re-uploads VBO on change while preserving camera position

### F34: Remote screen view (paxx12 extended firmware)
- Source: https://github.com/paxx12/SnapmakerU1-Extended-Firmware — custom Snapmaker U1 firmware that exposes the printer touchscreen remotely
- The firmware runs a Python framebuffer capture server (`fb-http-server.py`) behind nginx; the screen is available at `http://<printer-ip>/screen/`
- Protocol: plain HTTP (not WebSocket); returns framebuffer snapshots; also accepts touch input
- Auth is inherited from Fluidd/Mainsail (same credentials as the printer web UI)
- Integration idea: add a "Remote Screen" tile or button in the app's Printer screen that opens a WebView (or custom renderer) pointed at `http://<printer-ip>/screen/` when the printer is reachable
- Must support regular unmodified U1 printers gracefully — the feature should be invisible/disabled when the firmware is not present
- Detection approach: either auto-detect by probing `http://<printer-ip>/screen/` (show the button only if it responds 200) or add a manual toggle in Printer settings ("Extended firmware remote screen")
- Prefer auto-detection so unmodified U1 users see no difference; fall back to manual opt-in if probing adds noticeable latency

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

### F28: Show prime tower filament usage on Preview page — DONE
- `GcodeParser` now tracks `;TYPE:Prime tower` E-value boundaries and accumulates `wipeTowerFilamentMm` in `ParsedGcode`
- `SliceCompleteSummaryCard` shows "Prime Tower Waste" row (mm + grams) in amber when > 0.5mm

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
