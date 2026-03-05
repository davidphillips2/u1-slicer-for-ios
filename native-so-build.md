# Native .so Build Guide

How to build `libprusaslicer-jni.so` from source for ARM64 Android.

## Prerequisites

- **Android NDK r26** (26.1.10909125) — installed via Android Studio SDK Manager
- **CMake 3.22.1** — installed via Android Studio SDK Manager
- **Git + Bash** — for fetching sources and running scripts

## Directory Layout

```
app/src/main/cpp/
  CMakeLists.txt          # Main build file (~580 lines)
  include/sapil.h         # JNI bridge header
  src/                    # SAPIL implementation (slicer_wrapper, sapil_config, sapil_model, sapil_print)
  prusaslicer/            # PrusaSlicer 2.8.x source (fetched by script)
    src/libslic3r/        # Core slicer library
    bundled_deps/         # Bundled deps (admesh, semver, miniz, etc.)
  extern/                 # Pre-built ARM64 libraries
    boost/                # Boost 1.84
    tbb/                  # TBB 2021.11
    cgal/                 # CGAL 5.6 (headers)
    eigen/                # Eigen 3.4 (headers)
    gmp/                  # GMP 6.3
    mpfr/                 # MPFR 4.2.1
    occt/                 # OpenCASCADE 7.8.1
    clipper2/             # Clipper2
    zlib/                 # zlib 1.3.1
    expat/                # expat 2.6.2
    cereal/               # cereal (headers)
    nlohmann/             # nlohmann-json (headers)
```

## Build Steps

### 1. Fetch PrusaSlicer source (one-time)

```bash
bash scripts/fetch_prusaslicer.sh
```

### 2. Build native dependencies (one-time)

```bash
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125
bash scripts/build_deps.sh
```

This cross-compiles Boost, TBB, GMP, MPFR, OCCT, Clipper2, zlib, and expat for `arm64-v8a`. Results go into `app/src/main/cpp/extern/`.

### 3. Build the .so via Gradle

```bash
# Build just the native library
./gradlew :app:externalNativeBuildDebug

# Or build the full APK (includes native build)
./gradlew assembleDebug
```

The `externalNativeBuild` block in `app/build.gradle` tells Gradle to invoke CMake:

```groovy
externalNativeBuild {
    cmake {
        cppFlags "-std=c++17 -O2 -DNDEBUG"
        arguments "-DANDROID_STL=c++_shared"
        abiFilters 'arm64-v8a'
    }
}
```

Output: `app/build/intermediates/cmake/debug/obj/arm64-v8a/libprusaslicer-jni.so`

Build takes ~5 minutes on a modern machine.

## Adding New JNI Methods or Config Fields

When modifying the native/Kotlin interface (e.g., adding multi-extruder config fields):

1. **C++ header**: Add fields to structs in `include/sapil.h`
2. **C++ implementation**: Update the relevant `src/sapil_*.cpp` file
3. **JNI bridge**: Update `src/slicer_wrapper.cpp` — match JNI method signatures
4. **Kotlin data class**: Update the matching `data/` class — field order and types must match the JNI `NewObject()` call exactly
5. **Rebuild**: `./gradlew :app:externalNativeBuildDebug` then `./gradlew installDebug`

### JNI Signature Reference

| Kotlin Type | JNI Signature |
|-------------|---------------|
| `String`    | `Ljava/lang/String;` |
| `Float`     | `F` |
| `Int`       | `I` |
| `Boolean`   | `Z` |
| `FloatArray`| `[F` |
| `IntArray`  | `[I` |

Current `ModelInfo` signature: `(Ljava/lang/String;Ljava/lang/String;FFFIIZ)V`
- String filename, String format, float sizeX/Y/Z, int triangleCount, int volumeCount, boolean isManifold

## Troubleshooting

- **JNI NoSuchMethodError**: Kotlin data class fields don't match the C++ `NewObject()` constructor signature. Check field count, order, and types.
- **OOM during Gradle build**: Use `./gradlew --no-daemon` or increase heap in `gradle.properties`
- **CMake not found**: Install CMake 3.22.1 via Android Studio > SDK Manager > SDK Tools
