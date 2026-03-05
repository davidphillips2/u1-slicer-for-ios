# U1 Slicer for Android

Native Android 3D model slicer powered by **PrusaSlicer 2.8.x** core, using the JNI approach pioneered by [SliceBeam](https://github.com/utkabobr/SliceBeam).

## Architecture

```
┌──────────────────────────────────────────┐
│           Android UI (Compose)           │
│     MainActivity / SlicerViewModel       │
├──────────────────┬───────────────────────┤
│  NativeLibrary   │   Data Classes        │
│   (JNI Bridge)   │  SliceConfig/Result   │
├──────────────────┴───────────────────────┤
│          SAPIL (C++ JNI Layer)           │
│  slicer_wrapper / sapil_config/model/... │
├──────────────────────────────────────────┤
│       PrusaSlicer 2.8.x Core            │
│     libslic3r (Print/GCode/Model)       │
├──────────────────────────────────────────┤
│  Native Deps (Boost/TBB/CGAL/OCCT/...)  │
│        Cross-compiled for ARM64          │
└──────────────────────────────────────────┘
```

## Building

### Prerequisites
- Android SDK & NDK r26+
- CMake 3.22+
- Git, Bash

### Steps

```bash
# 1. Fetch PrusaSlicer source
bash scripts/fetch_prusaslicer.sh

# 2. Build native dependencies (requires NDK)
export ANDROID_NDK_HOME=/path/to/ndk
bash scripts/build_deps.sh

# 3. Build the app
./gradlew assembleDebug
```

## Project Structure

| Directory | Description |
|-----------|-------------|
| `app/src/main/cpp/` | Native C++ code (SAPIL + PrusaSlicer) |
| `app/src/main/cpp/include/` | SAPIL header (`sapil.h`) |
| `app/src/main/cpp/src/` | SAPIL implementation files |
| `app/src/main/cpp/extern/` | Pre-built native dependencies |
| `app/src/main/java/` | Kotlin source (UI + JNI bridge) |
| `scripts/` | Build and setup scripts |

## Status

Phases 1-2, 4-5, 7-10 complete. See [native-so-build.md](native-so-build.md) for native .so build details.

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | Done | Foundation: navigation, settings, G-code export/share |
| 2 | Done | Bambu 3MF sanitization + multi-plate support |
| 3 | Next | Multi-extruder/multi-color (needs native rebuild) |
| 4 | Done | Moonraker printer connectivity |
| 5 | Done | Filament library (Room DB), enhanced settings, job history |
| 6 | Partial | MakerWorld import (download only); copies need native rebuild |
| 7 | Done | 2D G-code layer viewer |
| 8 | Done | 3D model viewer (OpenGL ES 3.0) |
| 9 | Done | 3D G-code viewer (OpenGL ES 3.0) |
| 10 | Done | Plate preview thumbnails |

## Credits
- [PrusaSlicer](https://github.com/prusa3d/PrusaSlicer) — Core slicing engine
- [SliceBeam](https://github.com/utkabobr/SliceBeam) — Architectural inspiration for Android JNI approach
- [u1-slicer-bridge](https://github.com/taylormadearmy/u1-slicer-bridge) — Original server-side slicer
