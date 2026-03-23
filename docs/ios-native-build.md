# iOS Native Library Build Guide

## Overview

This guide covers building the Snapmaker Orca 2.2.4 C++ slicing engine as an iOS static library/framework for use with Kotlin/Native.

## Current Status

- ✅ Platform-agnostic API defined (`SapilWrapper` interface)
- ✅ Android JNI implementation complete (uses prebuilt `libprusaslicer-jni.so`)
- ⚠️ iOS implementation is stub - needs native library build

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Shared Kotlin Code                       │
│  com.u1.slicer.shared.native.SapilWrapper (expect/actual)   │
└─────────────────────────────────────────────────────────────┘
         │                                    │
         │ Android                            │ iOS
         ▼                                    ▼
┌──────────────────────┐          ┌──────────────────────────┐
│   Android (JNI)      │          │   iOS (Kotlin/Native)    │
│                      │          │                          │
│ libprusaslicer-jni.so│          │ sapil_ios.xcframework    │
│ (prebuilt .so)       │          │ (to be built)            │
└──────────────────────┘          └──────────────────────────┘
         │                                    │
         └────────────┬───────────────────────┘
                      ▼
         ┌────────────────────────────┐
         │   OrcaSlicer C++ Core      │
         │   (Snapmaker fork 2.2.4)   │
         └────────────────────────────┘
```

## iOS Build Strategy

Since the OrcaSlicer source is ~20MB+ and has many dependencies, we have two options:

### Option A: Use Official OrcaSlicer iOS Build (Recommended)

Check if Snapmaker/OrcaSlicer provides an iOS framework:

1. Contact Snapmaker for iOS SDK
2. Check OrcaSlicer GitHub releases for iOS binaries
3. Use existing Xcode project if available

### Option B: Build from Source

If no official iOS build exists, build it yourself:

## Dependencies to Build for iOS

The following libraries need iOS builds:

### 1. Boost (required)
```bash
# Download Boost 1.76+ or later
wget https://boostorg.jfrog.io/artifactory/main/release/1.76.0/source/boost_1_76_0.tar.gz

# Build for iOS (arm64 + simulator)
./bootstrap.sh
./toolchain/build_ios.sh
```

### 2. Intel TBB (Threading Building Blocks)
```bash
git clone https://github.com/oneapi-src/oneTBB.git
cd oneTBB
mkdir build
cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=../cmake/ios.toolchain.cmake \
  -DIOS_ARCH=arm64 \
  -DCMAKE_BUILD_TYPE=Release
make -j4
```

### 3. OpenCascade (OCCT) - Geometry kernel
This is the largest dependency (~500MB source)

```bash
git clone https://github.com/Open-Cascade-SAS/OCCT.git
cd OCCT
mkdir build
cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=../cmake/ios.toolchain.cmake \
  -DBUILD_MODULE_DE=OFF \
  -DBUILD_MODULE_Draw=OFF \
  -DBUILD_MODULE_Visualization=OFF \
  -DCMAKE_BUILD_TYPE=Release
make -j4
```

### 4. CGAL - Computational Geometry Algorithms Library
```bash
# Header-only, easier - just include headers
wget https://github.com/CGAL/cgal/releases/download/v5.3/CGAL-5.3.zip
unzip CGAL-5.3.zip
```

### 5. Header-only libraries (no build needed):
- nlohmann/json (JSON parsing)
- cereal (serialization)
- semver (semantic versioning)
- miniz (ZIP compression)

## Building OrcaSlicer for iOS

### 1. Get OrcaSlicer Source
```bash
git clone https://github.com/SoftFever/OrcaSlicer.git
cd OrcaSlicer
git checkout v2.2.4  # Or appropriate Snapmaker fork
```

### 2. Create iOS Build Configuration

Create `CMakeLists_iOS.txt`:

```cmake
cmake_minimum_required(VERSION 3.22)
project(sapil_ios CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_OSX_DEPLOYMENT_TARGET "14.0")

# iOS-specific flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fembed-bitcode")

# Include paths (similar to Android CMakeLists.txt)
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/deps_src
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_CURRENT_SOURCE_DIR}/deps_src/clipper
    # ... (see Android CMakeLists.txt for full list)
)

# Source files (from Android build)
add_library(sapil_ios STATIC
    src/sapil_print.cpp
    src/sapil_model.cpp
    src/sapil_config.cpp
    src/sapil_arrange.cpp
    src/sapil_gcode.cpp
    src/sapil_progress.cpp
    src/sapil_diagnostics.cpp
    src/slicer_wrapper.cpp
)

# Link libraries
target_link_libraries(sapil_ios
    ${Boost_LIBRARIES}
    ${TBB_LIBRARIES}
    ${OCCT_LIBRARIES}
)
```

### 3. Build for Multiple Architectures

```bash
# Build for arm64 (devices)
cmake -DCMAKE_TOOLCHAIN_FILE=cmake/ios.toolchain.cmake \
      -DIOS_ARCH=arm64 \
      -DCMAKE_BUILD_TYPE=Release \
      -S . -B build_arm64

cmake --build build_arm64 -- -j4

# Build for simulator (x86_64 or arm64-sim)
cmake -DCMAKE_TOOLCHAIN_FILE=cmake/ios.toolchain.cmake \
      -DIOS_ARCH=arm64-sim \
      -DCMAKE_BUILD_TYPE=Release \
      -S . -B build_sim

cmake --build build_sim -- -j4

# Create universal binary (fat library)
lipo -create -output libsapil_ios.a \
    build_arm64/libsapil_ios.a \
    build_sim/libsapil_ios.a
```

### 4. Create XCFramework

```bash
xcodeframework -create-xcframework \
    -library build_arm64/libsapil_ios.a \
    -headers include \
    -library build_sim/libsapil_ios.a \
    -headers include \
    -output sapil_ios.xcframework
```

### 5. Create C Header for Kotlin/Native

Create `sapil_ios.h` (see documentation in `SapilWrapper.ios.kt`):

```c
#ifndef SAPIL_IOS_H
#define SAPIL_IOS_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void* sapil_handle_t;

typedef struct {
    char* filename;
    char* format;
    float size_x, size_y, size_z;
    int triangle_count;
    int volume_count;
    bool is_manifold;
} sapil_model_info_t;

typedef struct {
    float* triangle_positions;
    int position_count;
    uint8_t* extruder_indices;
    int extruder_count;
} sapil_preview_mesh_t;

typedef struct {
    bool success;
    char* error_message;
    char* gcode_path;
    int total_layers;
    float estimated_time_seconds;
    float estimated_filament_mm;
    float estimated_filament_grams;
} sapil_slice_result_t;

// Core API
sapil_handle_t sapil_create(void);
void sapil_destroy(sapil_handle_t handle);
const char* sapil_get_core_version(sapil_handle_t handle);
bool sapil_load_model(sapil_handle_t handle, const char* path);
bool sapil_get_model_info(sapil_handle_t handle, sapil_model_info_t* info);
void sapil_clear_model(sapil_handle_t handle);

// Memory management
void sapil_free_model_info(sapil_model_info_t* info);
void sapil_free_preview_mesh(sapil_preview_mesh_t* mesh);
void sapil_free_slice_result(sapil_slice_result_t* result);

#ifdef __cplusplus
}
#endif

#endif // SAPIL_IOS_H
```

### 6. Integrate with Kotlin/Native

Create `shared/src/iosMain/cinterop/sapil_ios.def`:

```def
headers = sapil_ios.h
headerFilter = sapil_ios.h
package = com.u1.slicer.shared.native

compilerOpts = -F<path_to_xcframework>
linkerOpts = -framework Foundation
linkerOpts = -F<path_to_xcframework> -lsapil_ios
```

### 7. Place Framework in iOS Project

```bash
# Copy framework to iOS app
cp -r sapil_ios.xcframework ios/U1Slicer/Frameworks/

# Add to Xcode project
# In Xcode: Add Files to "U1Slicer" → select sapil_ios.xcframework
```

## Alternative: Use Prebuilt iOS Framework

If OrcaSlicer releases an iOS framework:

1. Download from Snapmaker/Orca releases
2. Place in `ios/U1Slicer/Frameworks/`
3. Update Kotlin/Native cinterop def to point to framework
4. Implement iOS SapilWrapper using cinterop bindings

## Testing the iOS Build

Once built, test with:

```kotlin
// In iOS app
val wrapper = SapilWrapper()
val version = wrapper.getCoreVersion()
println("OrcaSlicer version: $version")

val loaded = wrapper.loadModel("/path/to/model.stl")
if (loaded) {
    val info = wrapper.getModelInfo()
    println("Model: ${info?.filename}")
}
```

## Known Issues

1. **Size**: Full OrcaSlicer build is 20MB+ - consider lazy loading or dynamic framework
2. **Dependencies**: OCCT is large and complex - may need to subset for iOS
3. **Memory**: C++ slicer can use 500MB+ RAM - monitor iOS memory limits
4. **Time**: First build can take 1-2 hours

## Resources

- OrcaSlicer: https://github.com/SoftFever/OrcaSlicer
- Snapmaker: https://github.com/Snapmaker
- Kotlin/Native C Interop: https://kotlinlang.org/docs/native-c-interop.html
- iOS CMake Toolchain: https://github.com/leetal/ios-cmake
