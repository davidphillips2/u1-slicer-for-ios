#!/bin/bash
# =============================================================================
# Build Script: Cross-compile dependencies for Android ARM64
# =============================================================================
# Prerequisites:
#   - Android NDK r26+ installed (set ANDROID_NDK_HOME)
#   - CMake 3.22+
#   - Git
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
EXTERN_DIR="$PROJECT_ROOT/app/src/main/cpp/extern"
BUILD_DIR="$SCRIPT_DIR/build"

# Configuration
ANDROID_ABI="arm64-v8a"
ANDROID_API=26
ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-/tmp/android-ndk-r26b}"
ANDROID_TOOLCHAIN="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"

NUM_CORES=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

echo "============================================"
echo "  u1-slicer-for-android Dependency Builder"
echo "============================================"
echo "NDK:        $ANDROID_NDK_HOME"
echo "ABI:        $ANDROID_ABI"
echo "API Level:  $ANDROID_API"
echo "Cores:      $NUM_CORES"
echo "============================================"

# Verify NDK exists
if [ ! -f "$ANDROID_TOOLCHAIN" ]; then
    echo "ERROR: Android NDK toolchain not found at: $ANDROID_TOOLCHAIN"
    echo "Set ANDROID_NDK_HOME to your NDK installation path."
    exit 1
fi

mkdir -p "$BUILD_DIR"
mkdir -p "$EXTERN_DIR"

# ---- BOOST ----
build_boost() {
    echo ""
    echo ">>> Building Boost for Android..."
    local NATIVE_BOOST_DIR="/tmp/u1-deps-boost-build"
    mkdir -p "$NATIVE_BOOST_DIR"
    
    if [ ! -d "$NATIVE_BOOST_DIR/.git" ]; then
        git clone https://github.com/moritz-wundke/Boost-for-Android.git "$NATIVE_BOOST_DIR"
    fi
    
    cd "$NATIVE_BOOST_DIR"
    
    # Sanitize PATH to remove Windows paths with spaces which break libiconv/boost build
    local OLD_PATH=$PATH
    export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
    
    ./build-android.sh \
        --boost=1.84.0 \
        --arch=arm64-v8a \
        --with-libraries=system,filesystem,thread,log,regex,iostreams,nowide \
        "$ANDROID_NDK_HOME"
        
    export PATH=$OLD_PATH
    
    # Copy outputs
    mkdir -p "$EXTERN_DIR/boost/include"
    mkdir -p "$EXTERN_DIR/boost/lib/$ANDROID_ABI"
    cp -rL build/out/arm64-v8a/include/boost-1_84/boost "$EXTERN_DIR/boost/include/"
    cp -L build/out/arm64-v8a/lib/*.a "$EXTERN_DIR/boost/lib/$ANDROID_ABI/"
    
    echo ">>> Boost build complete."
}

# ---- EIGEN ----
build_eigen() {
    echo ""
    echo ">>> Fetching Eigen (header-only)..."
    local EIGEN_DIR="$BUILD_DIR/eigen"
    
    if [ ! -d "$EIGEN_DIR" ]; then
        git clone --depth 1 --branch 3.4.0 https://gitlab.com/libeigen/eigen.git "$EIGEN_DIR"
    fi
    
    mkdir -p "$EXTERN_DIR/eigen/include"
    cp -r "$EIGEN_DIR/Eigen" "$EXTERN_DIR/eigen/include/"
    cp -r "$EIGEN_DIR/unsupported" "$EXTERN_DIR/eigen/include/"
    
    echo ">>> Eigen fetch complete."
}

# ---- TBB (oneTBB) ----
build_tbb() {
    echo ""
    echo ">>> Building oneTBB for Android..."
    local TBB_DIR="$BUILD_DIR/oneTBB"
    local TBB_BUILD="$TBB_DIR/build-android"
    
    if [ ! -d "$TBB_DIR" ]; then
        git clone --depth 1 --branch v2021.11.0 https://github.com/oneapi-src/oneTBB.git "$TBB_DIR"
    fi
    
    mkdir -p "$TBB_BUILD"
    cd "$TBB_BUILD"
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
        -DANDROID_ABI="$ANDROID_ABI" \
        -DANDROID_PLATFORM=android-$ANDROID_API \
        -DCMAKE_BUILD_TYPE=Release \
        -DTBB_TEST=OFF \
        -DTBB_EXAMPLES=OFF \
        -DBUILD_SHARED_LIBS=OFF
    cmake --build . -j "$NUM_CORES"
    
    mkdir -p "$EXTERN_DIR/tbb/include"
    mkdir -p "$EXTERN_DIR/tbb/lib/$ANDROID_ABI"
    cp -r "$TBB_DIR/include/tbb" "$EXTERN_DIR/tbb/include/"
    cp -r "$TBB_DIR/include/oneapi" "$EXTERN_DIR/tbb/include/"
    find "$TBB_BUILD" -name "*.a" -exec cp {} "$EXTERN_DIR/tbb/lib/$ANDROID_ABI/" \;
    
    echo ">>> TBB build complete."
}

# ---- CGAL ----
build_cgal() {
    echo ">>> Fetching CGAL (header-only)..."
    cd "$BUILD_DIR"
    if [ ! -d "CGAL-5.6" ]; then
        wget -q https://github.com/CGAL/cgal/releases/download/v5.6/CGAL-5.6.tar.xz
        tar xf CGAL-5.6.tar.xz
    fi
    mkdir -p "$EXTERN_DIR/cgal/include"
    cp -r "$BUILD_DIR/CGAL-5.6/include/CGAL" "$EXTERN_DIR/cgal/include/"
    
    echo ">>> CGAL fetch complete."
}

build_gmp_mpfr() {
    echo ""
    echo ">>> Building GMP & MPFR for Android..."
    local GMP_VER="6.3.0"
    local MPFR_VER="4.2.1"
    
    local NATIVE_BUILD_DIR="/tmp/u1-deps-gmp-build"
    mkdir -p "$NATIVE_BUILD_DIR"
    cd "$NATIVE_BUILD_DIR"
    
    if [ ! -d "gmp-${GMP_VER}" ]; then
        wget -q "https://gmplib.org/download/gmp/gmp-${GMP_VER}.tar.xz"
        tar xf "gmp-${GMP_VER}.tar.xz"
    fi
    
    if [ ! -d "mpfr-${MPFR_VER}" ]; then
        wget -q "https://www.mpfr.org/mpfr-${MPFR_VER}/mpfr-${MPFR_VER}.tar.xz"
        tar xf "mpfr-${MPFR_VER}.tar.xz"
    fi
    
    local TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
    local TARGET="aarch64-linux-android"
    
    export AR="$TOOLCHAIN/bin/llvm-ar"
    export AS="$TOOLCHAIN/bin/${TARGET}${ANDROID_API}-clang"
    export CC="$TOOLCHAIN/bin/${TARGET}${ANDROID_API}-clang -fPIC"
    export CXX="$TOOLCHAIN/bin/${TARGET}${ANDROID_API}-clang++ -fPIC"
    export LD="$TOOLCHAIN/bin/ld"
    export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
    export STRIP="$TOOLCHAIN/bin/llvm-strip"
    
    export CFLAGS="-fPIC"
    export CXXFLAGS="-fPIC"
    
    echo ">>> Configuring and building GMP (Clean Build)..."
    cd "$NATIVE_BUILD_DIR/gmp-${GMP_VER}"
    make distclean || true
    ./configure --host=$TARGET --disable-shared --enable-static --enable-cxx --with-pic --prefix="$EXTERN_DIR/gmp"
    make -j"$NUM_CORES"
    make install
    
    echo ">>> Configuring and building MPFR (Clean Build)..."
    cd "$NATIVE_BUILD_DIR/mpfr-${MPFR_VER}"
    make distclean || true
    ./configure --host=$TARGET --disable-shared --enable-static --with-pic --with-gmp="$EXTERN_DIR/gmp" --prefix="$EXTERN_DIR/mpfr"
    make -j"$NUM_CORES"
    make install
    
    mkdir -p "$EXTERN_DIR/gmp/lib/$ANDROID_ABI"
    mkdir -p "$EXTERN_DIR/mpfr/lib/$ANDROID_ABI"
    cp "$EXTERN_DIR/gmp/lib/libgmp.a" "$EXTERN_DIR/gmp/lib/libgmpxx.a" "$EXTERN_DIR/gmp/lib/$ANDROID_ABI/" 2>/dev/null || true
    cp "$EXTERN_DIR/mpfr/lib/libmpfr.a" "$EXTERN_DIR/mpfr/lib/$ANDROID_ABI/" 2>/dev/null || true
    
    echo ">>> GMP & MPFR build complete."
}

# ---- OCCT (Open Cascade Technology) ----
build_occt() {
    echo ""
    echo ">>> Building OCCT for Android..."
    local NATIVE_OCCT_DIR="/tmp/u1-deps-occt-build"
    local OCCT_BUILD="$NATIVE_OCCT_DIR/build-android"
    
    mkdir -p "$NATIVE_OCCT_DIR"
    if [ ! -d "$NATIVE_OCCT_DIR/.git" ]; then
        git clone --depth 1 --branch V7_8_1 https://github.com/Open-Cascade-SAS/OCCT.git "$NATIVE_OCCT_DIR"
    fi
    
    mkdir -p "$OCCT_BUILD"
    cd "$OCCT_BUILD"
    cmake "$NATIVE_OCCT_DIR" \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
        -DANDROID_ABI="$ANDROID_ABI" \
        -DANDROID_PLATFORM=android-$ANDROID_API \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_LIBRARY_TYPE=Static \
        -DUSE_FREETYPE=OFF \
        -DUSE_FREEIMAGE=OFF \
        -DUSE_OPENVR=OFF \
        -DUSE_RAPIDJSON=OFF \
        -DUSE_TBB=OFF \
        -DUSE_VTK=OFF \
        -DBUILD_MODULE_Draw=OFF \
        -DBUILD_MODULE_Visualization=OFF \
        -DBUILD_DOC_Overview=OFF
    cmake --build . -j "$NUM_CORES"
    
    mkdir -p "$EXTERN_DIR/occt/include"
    mkdir -p "$EXTERN_DIR/occt/lib/$ANDROID_ABI"
    cp -r "$NATIVE_OCCT_DIR/src"/*/*.hxx "$EXTERN_DIR/occt/include/" 2>/dev/null || true
    cp -r "$NATIVE_OCCT_DIR/src"/*/*.h "$EXTERN_DIR/occt/include/" 2>/dev/null || true
    find "$OCCT_BUILD" -name "*.a" -exec cp {} "$EXTERN_DIR/occt/lib/$ANDROID_ABI/" \;
    
    echo ">>> OCCT build complete."
}

# ---- CEREAL (header-only) ----
build_cereal() {
    echo ""
    echo ">>> Fetching Cereal (header-only)..."
    local CEREAL_DIR="$BUILD_DIR/cereal"
    
    if [ ! -d "$CEREAL_DIR" ]; then
        git clone --depth 1 --branch v1.3.2 https://github.com/USCiLab/cereal.git "$CEREAL_DIR"
    fi
    
    mkdir -p "$EXTERN_DIR/cereal/include"
    cp -r "$CEREAL_DIR/include/cereal" "$EXTERN_DIR/cereal/include/"
    
    echo ">>> Cereal fetch complete."
}

# ---- NLOHMANN JSON (header-only) ----
build_nlohmann_json() {
    echo ""
    echo ">>> Fetching nlohmann/json (header-only)..."
    local JSON_DIR="$BUILD_DIR/json"
    
    if [ ! -d "$JSON_DIR" ]; then
        git clone --depth 1 --branch v3.11.3 https://github.com/nlohmann/json.git "$JSON_DIR"
    fi
    
    mkdir -p "$EXTERN_DIR/nlohmann/include"
    cp -r "$JSON_DIR/include/nlohmann" "$EXTERN_DIR/nlohmann/include/"
    
    echo ">>> nlohmann/json fetch complete."
}

# ---- CLIPPER2 ----
build_clipper2() {
    echo ""
    echo ">>> Building Clipper2 for Android..."
    local CLIPPER_DIR="$BUILD_DIR/Clipper2"
    local CLIPPER_BUILD="$CLIPPER_DIR/build-android"
    
    if [ ! -d "$CLIPPER_DIR" ]; then
        git clone --depth 1 https://github.com/AngusJohnson/Clipper2.git "$CLIPPER_DIR"
    fi
    
    mkdir -p "$CLIPPER_BUILD"
    cd "$CLIPPER_BUILD"
    cmake "$CLIPPER_DIR/CPP" \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
        -DANDROID_ABI="$ANDROID_ABI" \
        -DANDROID_PLATFORM=android-$ANDROID_API \
        -DCMAKE_BUILD_TYPE=Release \
        -DCLIPPER2_EXAMPLES=OFF \
        -DCLIPPER2_TESTS=OFF \
        -DCLIPPER2_UTILS=OFF \
        -DBUILD_SHARED_LIBS=OFF
    cmake --build . -j "$NUM_CORES"
    
    mkdir -p "$EXTERN_DIR/clipper2/include"
    mkdir -p "$EXTERN_DIR/clipper2/lib/$ANDROID_ABI"
    cp -r "$CLIPPER_DIR/CPP/Clipper2Lib/include"/* "$EXTERN_DIR/clipper2/include/"
    find "$CLIPPER_BUILD" -name "*.a" -exec cp {} "$EXTERN_DIR/clipper2/lib/$ANDROID_ABI/" \;
    
    echo ">>> Clipper2 build complete."
}

# ---- ZLIB ----
build_zlib() {
    echo ""
    echo ">>> Building zlib for Android..."
    local ZLIB_DIR="$BUILD_DIR/zlib"
    local ZLIB_BUILD="$ZLIB_DIR/build-android"
    
    if [ ! -d "$ZLIB_DIR" ]; then
        git clone --depth 1 --branch v1.3.1 https://github.com/madler/zlib.git "$ZLIB_DIR"
    fi
    
    mkdir -p "$ZLIB_BUILD"
    cd "$ZLIB_BUILD"
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
        -DANDROID_ABI="$ANDROID_ABI" \
        -DANDROID_PLATFORM=android-$ANDROID_API \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF
    cmake --build . -j "$NUM_CORES"
    
    mkdir -p "$EXTERN_DIR/zlib/include"
    mkdir -p "$EXTERN_DIR/zlib/lib/$ANDROID_ABI"
    cp "$ZLIB_DIR/zlib.h" "$EXTERN_DIR/zlib/include/"
    cp "$ZLIB_BUILD/zconf.h" "$EXTERN_DIR/zlib/include/" 2>/dev/null || cp "$ZLIB_DIR/zconf.h" "$EXTERN_DIR/zlib/include/"
    find "$ZLIB_BUILD" -name "*.a" -exec cp {} "$EXTERN_DIR/zlib/lib/$ANDROID_ABI/" \;
    
    echo ">>> zlib build complete."
}

# ---- EXPAT ----
build_expat() {
    echo ""
    echo ">>> Building Expat for Android..."
    local EXPAT_DIR="$BUILD_DIR/libexpat"
    local EXPAT_BUILD="$EXPAT_DIR/build-android"
    
    if [ ! -d "$EXPAT_DIR" ]; then
        git clone --depth 1 --branch R_2_6_2 https://github.com/libexpat/libexpat.git "$EXPAT_DIR"
    fi
    
    mkdir -p "$EXPAT_BUILD"
    cd "$EXPAT_BUILD"
    cmake "$EXPAT_DIR/expat" \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
        -DANDROID_ABI="$ANDROID_ABI" \
        -DANDROID_PLATFORM=android-$ANDROID_API \
        -DCMAKE_BUILD_TYPE=Release \
        -DEXPAT_BUILD_EXAMPLES=OFF \
        -DEXPAT_BUILD_FUZZERS=OFF \
        -DEXPAT_BUILD_TESTS=OFF \
        -DEXPAT_BUILD_TOOLS=OFF \
        -DEXPAT_SHARED_LIBS=OFF
    cmake --build . -j "$NUM_CORES"
    
    mkdir -p "$EXTERN_DIR/expat/include"
    mkdir -p "$EXTERN_DIR/expat/lib/$ANDROID_ABI"
    cp "$EXPAT_DIR/expat/lib/expat.h" "$EXPAT_DIR/expat/lib/expat_external.h" "$EXTERN_DIR/expat/include/"
    find "$EXPAT_BUILD" -name "*.a" -exec cp {} "$EXTERN_DIR/expat/lib/$ANDROID_ABI/" \;
    
    echo ">>> Expat build complete."
}

# ---- Main ----
echo ""
echo "Starting dependency builds..."
echo ""

if [ "$#" -gt 0 ]; then
    "$@"
else
    # Header-only (fast)
    build_eigen
    build_cgal
    build_cereal
    build_nlohmann_json

    # Compiled dependencies
    build_zlib
    build_expat
    build_clipper2
    build_tbb
    build_gmp_mpfr
    build_boost
    build_occt
fi

echo ""
echo "============================================"
echo "  All dependencies built successfully!"
echo "  Output: $EXTERN_DIR"
echo "============================================"
