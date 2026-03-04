#!/bin/bash
# =============================================================================
# Fetch PrusaSlicer 2.8.x source as a submodule
# =============================================================================
# This script clones PrusaSlicer and sets up the libslic3r source files
# that will be compiled into the SAPIL JNI library.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRUSA_DIR="$PROJECT_ROOT/app/src/main/cpp/prusaslicer"

PRUSA_REPO="https://github.com/prusa3d/PrusaSlicer.git"
PRUSA_BRANCH="version_2.8.1"

echo "============================================"
echo "  Fetching PrusaSlicer 2.8.x Source"
echo "============================================"

if [ -d "$PRUSA_DIR" ]; then
    echo "PrusaSlicer directory already exists at: $PRUSA_DIR"
    echo "Updating..."
    cd "$PRUSA_DIR"
    git pull origin "$PRUSA_BRANCH" || true
else
    echo "Cloning PrusaSlicer ($PRUSA_BRANCH)..."
    git clone --depth 1 --branch "$PRUSA_BRANCH" "$PRUSA_REPO" "$PRUSA_DIR"
fi

echo ""
echo "PrusaSlicer source is at: $PRUSA_DIR"
echo ""
echo "Key directories for SAPIL integration:"
echo "  Core library:  $PRUSA_DIR/src/libslic3r/"
echo "  G-code:        $PRUSA_DIR/src/libslic3r/GCode/"
echo "  Print:         $PRUSA_DIR/src/libslic3r/Print*.cpp"
echo "  Config:        $PRUSA_DIR/src/libslic3r/PrintConfig.cpp"
echo "  Model:         $PRUSA_DIR/src/libslic3r/Model.cpp"
echo "  Format:        $PRUSA_DIR/src/libslic3r/Format/"
echo ""

# List the main source files that need to be compiled
echo "Core source files to integrate:"
find "$PRUSA_DIR/src/libslic3r" -maxdepth 1 -name "*.cpp" | head -30
echo "..."
echo ""

CORE_COUNT=$(find "$PRUSA_DIR/src/libslic3r" -name "*.cpp" | wc -l)
HEADER_COUNT=$(find "$PRUSA_DIR/src/libslic3r" -name "*.hpp" -o -name "*.h" | wc -l)

echo "Total source files: $CORE_COUNT .cpp, $HEADER_COUNT headers"
echo ""
echo "============================================"
echo "  PrusaSlicer source ready!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  1. Run scripts/build_deps.sh to build native dependencies"
echo "  2. Update CMakeLists.txt to include libslic3r sources"
echo "  3. Build the project with 'gradlew assembleDebug'"
