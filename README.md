# U1 Slicer - Kotlin Multiplatform

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Android](https://img.shields.io/badge/Android-v1.4.27-green.svg)](https://play.google.com/store/apps/details)
[![iOS](https://img.shields.io/badge/iOS-1.0.0-lightgrey.svg)](https://apps.apple.com/app/)

**Kotlin Multiplatform slicer** for the **Snapmaker U1** 3D printer (270×270×270mm, 4 extruders), powered by [Snapmaker Orca 2.2.4](https://github.com/Snapmaker/OrcaSlicer) (OrcaSlicer fork).

Native Android slicer + iOS app (in development) sharing ~70% of code via Kotlin Multiplatform — no server required, everything runs on-device.

**Current Release**: `v1.4.27` (Android) | iOS app in development

## Security

Security reports should be handled privately. See [SECURITY.md](SECURITY.md) for the preferred reporting flow.

## Features

- **STL and 3MF slicing** — single-color, multi-color (up to 4 extruders), and paint-based (SEMM)
- **Bambu 3MF support** — multi-plate extraction, profile embedding, sanitization pipeline
- **3D model viewer** — OpenGL ES 3.0 (Android) / Metal (iOS)
- **3D G-code viewer** — per-layer toolpath rendering with Gouraud shading
- **Wipe tower auto-positioning** — evaluates 8 candidates, picks spot with most clearance
- **Moonraker connectivity** — send G-code directly to your printer
- **Slicer overrides** — per-job control over layer height, infill, support, and more
- **MakerWorld integration** — share models from Bambu Handy to slice locally
- **Filament library** — manage profiles with temps, speeds, retraction settings
- **Settings backup/restore** — export and import all app settings as JSON

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Multiplatform                      │
│                      (Shared Module)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Data Models │  │    Parsers   │  │   Database   │    │
│  │  (Common)    │  │   (Common)   │  │ (SQLDelight) │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Native     │  │   Graphics   │  │   Network    │    │
│  │   Wrapper    │  │  (expect/actual) │  │   (Ktor)     │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
         │                                  │
         ▼                                  ▼
┌──────────────────────┐      ┌──────────────────────────┐
│   Android (Compose)   │      │     iOS (SwiftUI)       │
│  • OpenGL ES 3.0       │      │  • Metal                │
│  • Room/DataStore      │      │  • Core Data/UserDefaults │
│  • OkHttp              │      │  • Ktor                  │
│  • JNI to C++          │      │  • Kotlin/Native        │
└──────────────────────┘      └──────────────────────────┘
         │                                  │
         ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│              Snapmaker Orca 2.2.4 Core                       │
│     libslic3r (Print/GCode/Model) + Native Deps              │
└─────────────────────────────────────────────────────────────┘
```

## Project Structure

```
u1-slicer-for-android-main/
├── app/                          # Android app
│   └── src/main/
│       ├── java/com/u1/slicer/    # Compose UI, ViewModels
│       ├── cpp/                   # Native C++ (JNI)
│       └── jniLibs/               # Pre-built .so (ARM64)
├── ios/                          # iOS app (SwiftUI)
│   └── U1Slicer/
│       ├── App/                   # App entry point
│       ├── UI/                    # SwiftUI screens
│       │   ├── Prepare/           # Model loading, slicing
│       │   ├── Preview/           # G-code preview
│       │   ├── Jobs/              # Job history
│       │   ├── Printer/           # Printer control
│       │   └── Settings/          # App settings
│       ├── Platform/              # Metal renderer
│       └── Resources/             # Info.plist
└── shared/                       # Kotlin Multiplatform module
    └── src/
        ├── commonMain/            # Shared code (70%)
        │   ├── data/              # Data models
        │   ├── gcode/             # G-code parser
        │   ├── viewer/            # STL/3MF parsers
        │   ├── database/          # SQLDelight schema
        │   ├── graphics/          # 3D graphics API
        │   ├── native/            # C++ wrapper API
        │   └── platform/          # Platform abstractions
        ├── androidMain/           # Android-specific (15%)
        │   └── kotlin/.../database/    # Room, DataStore impl
        └── iosMain/               # iOS-specific (15%)
            └── kotlin/.../database/    # Native, UserDefaults impl
```

## Building

### Android

The Android app builds normally without the NDK (native `.so` is pre-built):

```bash
./gradlew installDebug    # Build and install on connected device
./gradlew assembleDebug   # Build APK only
```

**Requirements**: Android SDK 34, JDK 17, Kotlin 1.9.22

### iOS

The iOS app requires Xcode and the shared KMP module:

```bash
# 1. Build shared module for iOS
./gradlew :shared:compileKotlinIosArm64

# 2. Open in Xcode
open ios/U1Slicer/U1Slicer.xcodeproj

# 3. Build and run in Xcode
```

**Requirements**: macOS 14.0+, Xcode 15.0+, iOS 14.0+ target

**Status**: iOS app foundation is complete. Native library build required for full functionality.

See [ios/README.md](ios/README.md) for details.

## Testing

### Android

```bash
./gradlew testDebugUnitTest              # 517 JVM unit tests
./gradlew connectedDebugAndroidTest      # 125 instrumented tests
```

**642 total tests** covering:
- G-code parsing/validation
- 3MF sanitization and profile embedding
- STL parsing and mesh generation
- Slicing integration
- Room database operations
- Placement layout algorithms

### Shared Module

```bash
./gradlew :shared:testDebugUnitTest      # Shared unit tests (parsers, data models)
```

## Platform Comparison

| Feature | Android | iOS |
|---------|---------|-----|
| **UI Framework** | Jetpack Compose | SwiftUI |
| **3D Graphics** | OpenGL ES 3.0 | Metal |
| **Database** | Room + DataStore | SQLDelight |
| **Networking** | OkHttp | Ktor |
| **Native Interop** | JNI | Kotlin/Native |
| **Status** | ✅ Production | 🔄 Development |

## Shared Code

**~70% of code is shared** between Android and iOS:

- **Data Models**: `SliceConfig`, `ModelInfo`, `SliceResult`, `FilamentProfile`, etc.
- **Parsers**: STL, 3MF, G-code parsers with full feature parity
- **Database**: SQLDelight-based storage for filaments and slice jobs
- **Network**: Ktor-based Moonraker client
- **3D Graphics**: `MeshRenderer` interface with platform-specific implementations
- **Native Wrapper**: `SapilWrapper` for C++ slicing engine integration

**Platform-Specific (30%)**:
- UI layer (Compose vs SwiftUI)
- Graphics implementation (OpenGL ES vs Metal)
- Storage APIs (Room/DataStore vs SQLDelight)
- Native interop (JNI vs Kotlin/Native)

## Dependencies

### Kotlin Multiplatform
- **Kotlin**: 1.9.22
- **Coroutines**: 1.7.3
- **Serialization**: 1.6.2
- **SQLDelight**: 2.0.2
- **Ktor**: 2.3.7

### Android
- **Jetpack Compose**: UI framework
- **Room**: Database
- **DataStore**: Preferences storage
- **OkHttp**: HTTP client
- **OrcaSlicer**: C++ slicing engine (pre-built)

### iOS
- **SwiftUI**: UI framework
- **Metal**: 3D graphics
- **SQLDelight Native**: iOS driver
- **Ktor Client**: iOS client
- **OrcaSlicer**: C++ slicing engine (to be built)

## Documentation

- [CLAUDE.md](CLAUDE.md) - Project instructions and conventions
- [BACKLOG.md](BACKLOG.md) - Open bugs and features
- [SECURITY.md](SECURITY.md) - Security policy
- [docs/ios-native-build.md](docs/ios-native-build.md) - iOS native library build guide
- [docs/metal-3d-rendering.md](docs/metal-3d-rendering.md) - Metal rendering implementation
- [docs/ios-swiftui-app-foundation.md](docs/ios-swiftui-app-foundation.md) - iOS app documentation
- [ios/README.md](ios/README.md) - iOS app specifics

## Contributing

Contributions welcome! The project uses Kotlin Multiplatform to maintain both Android and iOS apps in parallel.

**Development Focus**:
1. Shared business logic in `shared/`
2. Platform-specific UI in `app/` (Android) and `ios/` (iOS)
3. Test on both platforms before submitting

## Roadmap

### ✅ Completed
- [x] KMP project structure
- [x] Shared data models
- [x] Shared parsers (STL, 3MF, G-code)
- [x] Database layer (SQLDelight)
- [x] 3D graphics API
- [x] Native wrapper interface
- [x] iOS SwiftUI app foundation

### 🔄 In Progress
- [ ] Build OrcaSlicer C++ library for iOS
- [ ] Complete iOS shared module integration
- [ ] Test iOS app on device

### 📋 Planned
- [ ] Advanced iOS features (camera, QR scan)
- [ ] Cloud integration
- [ ] Performance optimization
- [ ] Additional testing

## Native Rebuild

### Android

To rebuild the native library (requires NDK 25.1+):

1. Uncomment `externalNativeBuild` blocks in `app/build.gradle`
2. Run `./gradlew assembleDebug` to configure CMake
3. Re-comment CMake blocks, then `ninja -j1` in `app/.cxx/Debug/<hash>/arm64-v8a/`
4. Strip: `llvm-strip --strip-unneeded libprusaslicer-jni.so`
5. Copy to `app/src/main/jniLibs/arm64-v8a/`
6. `./gradlew clean installDebug`

Use `-j1` — higher parallelism OOMs on most machines.

### iOS

See [docs/ios-native-build.md](docs/ios-native-build.md) for complete build instructions. The OrcaSlicer C++ engine requires:
- Boost 1.76+
- Intel TBB
- OpenCascade (OCCT)
- CGAL
- nlohmann/json, cereal

## Credits

- [Snapmaker Orca / OrcaSlicer](https://github.com/SoftFever/OrcaSlicer) — Core slicing engine (AGPL-3.0)
- [PrusaSlicer](https://github.com/prusa3d/PrusaSlicer) — Upstream slicer (AGPL-3.0)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) — Cross-platform framework

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0-or-later).

See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for third-party dependency licenses.
