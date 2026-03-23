# U1 Slicer for iOS

iOS app for the Snapmaker U1 3D printer, built with SwiftUI and Kotlin Multiplatform.

## Overview

This is the iOS companion to the U1 Slicer Android app, sharing ~70% of code through Kotlin Multiplatform (KMP). The app provides full slicing capabilities with multi-extruder support for the Snapmaker U1 (270×270×270mm, 4 extruders).

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    iOS App (SwiftUI)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Prepare     │  │   Preview    │  │    Jobs      │    │
│  │  Screen      │  │   Screen     │  │   Screen      │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Shared Kotlin Module (KMP)                     │
│  • Data models (ModelInfo, SliceConfig, etc.)              │
│  • Parsers (STL, 3MF, G-code)                               │
│  • Database (SQLDelight)                                    │
│  • Native wrapper (SapilWrapper)                            │
│  • 3D graphics (Metal renderer)                              │
└─────────────────────────────────────────────────────────────┘
```

## Features

### Implemented
- ✅ **Tab-based navigation**: Prepare, Preview, Jobs, Printer, Settings
- ✅ **Model loading**: Support for STL, 3MF, OBJ, STEP files
- ✅ **3D preview**: Metal-based model viewer
- ✅ **Slicing config**: Layer height, infill, temperature, support
- ✅ **Job history**: View and manage slice jobs
- ✅ **Printer connection**: Moonraker API integration
- ✅ **Multi-extruder**: 4-extruder support with per-color visualization

### In Progress
- 🔄 **G-code preview**: Layer-by-layer visualization
- 🔄 **File export**: Share G-code via Files app
- 🔄 **Printer control**: Home, extrude, preheat commands
- 🔄 **Real-time updates**: Printer status and progress

### Planned
- 📋 **Filament profiles**: Manage custom filament settings
- 📋 **Printer profiles**: Multiple printer configurations
- 📋 **Cloud integration**: MakerWorld browser
- 📋 **Camera capture**: Scan QR codes from printer

## Project Structure

```
ios/U1Slicer/
├── App/
│   └── U1SlicerApp.swift          # Main app entry point
├── UI/
│   ├── Prepare/
│   │   ├── PrepareScreen.swift    # Load models, configure slicing
│   │   └── PrepareViewModel.swift
│   ├── Preview/
│   │   └── PreviewScreen.swift    # G-code preview, layer viewer
│   ├── Jobs/
│   │   └── JobsScreen.swift       # Slice job history
│   ├── Printer/
│   │   └── PrinterScreen.swift    # Printer connection and control
│   ├── Settings/
│   │   └── SettingsScreen.swift   # App settings
│   └── Components/
│       └── Model3DView.swift      # Metal 3D viewer wrapper
├── Platform/
│   ├── MetalMeshRenderer.swift   # Metal rendering engine
│   └── Shaders.metal             # Metal shaders
└── Resources/
    └── Info.plist                # App configuration
```

## Building

### Requirements

- macOS 14.0+
- Xcode 15.0+
- iOS 14.0+ deployment target
- CocoaPods (install with `sudo gem install cocoapods`)
- Kotlin Multiplatform shared module

### Build Steps

#### Option 1: Using CocoaPods (Recommended)

1. **Build the shared Kotlin framework**:
   ```bash
   ./ios/build-shared-framework.sh
   ```
   Or manually:
   ```bash
   ./gradlew :shared:linkReleaseFrameworkIosArm64
   ./gradlew :shared:linkReleaseFrameworkIosX64
   ./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64
   ```

2. **Install CocoaPods dependencies**:
   ```bash
   cd ios
   pod install
   ```

3. **Open the workspace** (not the project!):
   ```bash
   open U1Slicer.xcworkspace
   ```
   Or use Xcode:
   ```bash
   open U1Slicer/U1Slicer.xcworkspace
   ```

4. **Build and run**:
   - Select a target device or simulator
   - Product → Run (⌘R)

#### Option 2: Direct Framework Integration

1. **Build all iOS frameworks**:
   ```bash
   ./ios/build-shared-framework.sh
   ```

2. **Open the project**:
   ```bash
   open ios/U1Slicer/U1Slicer.xcodeproj
   ```

3. **Add framework reference** (one-time setup):
   - Select U1Slicer target
   - General → Frameworks, Libraries, and Embedded Content
   - Add the built framework from `shared/build/xcodegen/...`

4. **Build and run**:
   - Select a target device or simulator
   - Product → Run (⌘R)

### Troubleshooting

**Pod install fails**:
```bash
pod repo update
cd ios && pod install
```

**Framework not found**:
```bash
./gradlew clean :shared:linkReleaseFrameworkIosArm64
./ios/build-shared-framework.sh
```

**Swift import errors**:
- Ensure you're using `.xcworkspace` (CocoaPods) or have added the framework
- Clean build folder: Shift + Command + K
- Rebuild the shared framework

## Dependencies

### iOS (SwiftUI)
- **SwiftUI**: UI framework
- **MetalKit**: 3D graphics
- **UniformTypeIdentifiers**: File type handling

### Shared (KMP)
- **Kotlin 1.9.22**: Language
- **SQLDelight 2.0.2**: Database
- **Ktor 2.3.7**: Networking
- **kotlinx.serialization**: JSON parsing

## Shared Code

The iOS app shares the following with Android:

- **Data models**: `ModelInfo`, `SliceConfig`, `SliceResult`, etc.
- **Parsers**: STL, 3MF, G-code parsers
- **Database**: SQLDelight-based filament and job storage
- **Native interface**: SapilWrapper for C++ slicing engine
- **3D graphics**: MeshRenderer interface

See `/shared/src/commonMain/` for implementation.

## 3D Rendering

The iOS app uses Metal for hardware-accelerated 3D rendering:

- **Vertex format**: 10 floats per vertex (position + normal + color)
- **Per-triangle coloring**: Multi-extruder support
- **Camera controls**: Orbit, pan, zoom
- **Lighting**: Ambient + directional

See `Platform/MetalMeshRenderer.swift` and `docs/metal-3d-rendering.md`.

## Slicing Engine

The app wraps the Snapmaker Orca 2.2.4 C++ slicing engine:

- **Integration**: Via Kotlin/Native interop
- **Features**: Multi-extruder, wipe tower, supports
- **Performance**: Native C++ speed
- **Status**: Stub implementation (needs iOS native library build)

See `docs/ios-native-build.md` for building instructions.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on both Android and iOS (if applicable)
5. Submit a pull request

## License

This project is licensed under the same terms as the Snapmaker OrcaSlicer project.

## Acknowledgments

- **Snapmaker**: For the U1 3D printer and OrcaSlicer
- **Prusa Research**: For the original PrusaSlicer
- **Kotlin Multiplatform**: For enabling code sharing
- **Metal**: For high-performance graphics

## Support

- **Issues**: https://github.com/davidphillips2/u1-slicer-for-ios/issues
- **Snapmaker U1**: https://snapmaker.com
- **OrcaSlicer**: https://github.com/SoftFever/OrcaSlicer
