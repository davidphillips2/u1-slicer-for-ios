#!/bin/bash
set -e

BUILD_DIR="/tmp/u1-deps-build"
EXTERN_DIR="/mnt/c/Users/kevin/projects/u1-slicer-for-android/app/src/main/cpp/extern"

mkdir -p "$BUILD_DIR"

echo "=== Fetching Eigen 3.4 (header-only) ==="
cd "$BUILD_DIR"
if [ ! -d "eigen" ]; then
    git clone --depth 1 --branch 3.4.0 https://gitlab.com/libeigen/eigen.git eigen
fi
mkdir -p "$EXTERN_DIR/eigen/include"
cp -r "$BUILD_DIR/eigen/Eigen" "$EXTERN_DIR/eigen/include/"
cp -r "$BUILD_DIR/eigen/unsupported" "$EXTERN_DIR/eigen/include/"
echo "Eigen DONE"

echo "=== Fetching Cereal 1.3.2 (header-only) ==="
cd "$BUILD_DIR"
if [ ! -d "cereal" ]; then
    git clone --depth 1 --branch v1.3.2 https://github.com/USCiLab/cereal.git cereal
fi
mkdir -p "$EXTERN_DIR/cereal/include"
cp -r "$BUILD_DIR/cereal/include/cereal" "$EXTERN_DIR/cereal/include/"
echo "Cereal DONE"

echo "=== Fetching nlohmann/json 3.11.3 (header-only) ==="
cd "$BUILD_DIR"
if [ ! -d "json" ]; then
    git clone --depth 1 --branch v3.11.3 https://github.com/nlohmann/json.git json
fi
mkdir -p "$EXTERN_DIR/nlohmann/include"
cp -r "$BUILD_DIR/json/include/nlohmann" "$EXTERN_DIR/nlohmann/include/"
echo "nlohmann/json DONE"

echo "=== Building zlib 1.3.1 ==="
NDK_HOME="/tmp/android-ndk-r26b"
TOOLCHAIN="$NDK_HOME/build/cmake/android.toolchain.cmake"
cd "$BUILD_DIR"
if [ ! -d "zlib" ]; then
    git clone --depth 1 --branch v1.3.1 https://github.com/madler/zlib.git zlib
fi
mkdir -p zlib/build-android
cd zlib/build-android
cmake .. \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF 2>&1 | tail -5
cmake --build . -j$(nproc) 2>&1 | tail -3
mkdir -p "$EXTERN_DIR/zlib/include"
mkdir -p "$EXTERN_DIR/zlib/lib/arm64-v8a"
cp ../zlib.h ../zconf.h "$EXTERN_DIR/zlib/include/" 2>/dev/null || cp zconf.h "$EXTERN_DIR/zlib/include/"
find . -name "*.a" -exec cp {} "$EXTERN_DIR/zlib/lib/arm64-v8a/" \;
echo "zlib DONE"

echo "=== Building Expat 2.6.2 ==="
cd "$BUILD_DIR"
if [ ! -d "libexpat" ]; then
    git clone --depth 1 --branch R_2_6_2 https://github.com/libexpat/libexpat.git libexpat
fi
mkdir -p libexpat/build-android
cd libexpat/build-android
cmake ../expat \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release \
    -DEXPAT_BUILD_EXAMPLES=OFF \
    -DEXPAT_BUILD_TESTS=OFF \
    -DEXPAT_BUILD_TOOLS=OFF \
    -DEXPAT_SHARED_LIBS=OFF 2>&1 | tail -5
cmake --build . -j$(nproc) 2>&1 | tail -3
mkdir -p "$EXTERN_DIR/expat/include"
mkdir -p "$EXTERN_DIR/expat/lib/arm64-v8a"
cp ../expat/lib/expat.h ../expat/lib/expat_external.h "$EXTERN_DIR/expat/include/"
find . -name "*.a" -exec cp {} "$EXTERN_DIR/expat/lib/arm64-v8a/" \;
echo "Expat DONE"

echo ""
echo "=== All fetched dependencies ==="
ls "$EXTERN_DIR"
echo "COMPLETE"
