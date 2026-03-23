#!/bin/bash

# Complete iOS build and setup script
# This script builds the KMP framework and opens the Xcode project

set -e

echo "🚀 U1 Slicer iOS - Complete Build Setup"
echo "======================================"
echo ""

# Navigate to project root
cd "$(dirname "$0")/.."

# Step 1: Clean previous builds
echo "🧹 Step 1/4: Cleaning previous builds..."
./gradlew clean --quiet
echo "✅ Clean complete!"
echo ""

# Step 2: Build shared module for all iOS targets
echo "📦 Step 2/4: Building Kotlin Multiplatform framework..."
echo "Building for iOS arm64 (device)..."
./gradlew :shared:linkReleaseFrameworkIosArm64 --quiet

echo "Building for iOS x64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosX64 --quiet

echo "Building for iOS arm64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64 --quiet

echo "✅ Frameworks built successfully!"
echo ""

# Step 3: Show framework locations
echo "📋 Step 3/4: Framework locations:"
echo "  Device (arm64):"
echo "    shared/build/bin/iosArm64/releaseFramework/SharedModule.framework"
echo ""
echo "  Simulator (x64):"
echo "    shared/build/bin/iosX64/releaseFramework/SharedModule.framework"
echo ""
echo "  Simulator (arm64):"
echo "    shared/build/bin/iosSimulatorArm64/releaseFramework/SharedModule.framework"
echo ""

# Step 4: Open in Xcode
echo "🎯 Step 4/4: Opening project in Xcode..."
if [ -f "ios/U1Slicer/U1Slicer.xcodeproj/project.pbxproj" ]; then
    open ios/U1Slicer/U1Slicer.xcodeproj
    echo "✅ Opened Xcode project"
else
    echo "❌ Xcode project not found!"
    exit 1
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "Next steps in Xcode:"
echo "  1. Select your target device or simulator"
echo "  2. Add the framework:"
echo "     • Select U1Slicer target"
echo "     • Go to General → Frameworks, Libraries, and Embedded Content"
echo "     • Click '+' and add the framework from the paths above"
echo "  3. Press ⌘R to build and run"
echo ""
echo "Note: The framework needs to be added once per configuration (Debug/Release)"
