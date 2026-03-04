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

🟢 **Phase 2 Complete** — The PrusaSlicer 2.8.x core and JNI bridge (SAPIL) are fully integrated, compiled, and linked. `libprusaslicer-jni.so` is built and deployed. The project is ready for Phase 3 (Android application verification).

## Credits
- [PrusaSlicer](https://github.com/prusa3d/PrusaSlicer) — Core slicing engine
- [SliceBeam](https://github.com/utkabobr/SliceBeam) — Architectural inspiration for Android JNI approach
- [u1-slicer-bridge](https://github.com/taylormadearmy/u1-slicer-bridge) — Original server-side slicer
