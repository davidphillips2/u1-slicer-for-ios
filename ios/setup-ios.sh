#!/bin/bash

# Complete iOS build and setup script
# This script builds the KMP framework and sets up the Xcode project

set -e

echo "🚀 U1 Slicer iOS - Complete Build Setup"
echo "======================================"
echo ""

# Navigate to project root
cd "$(dirname "$0")/.."

# Step 1: Build shared module for all iOS targets
echo "📦 Step 1/4: Building Kotlin Multiplatform shared module..."
echo "Building for iOS arm64 (device)..."
./gradlew :shared:linkReleaseFrameworkIosArm64 --quiet

echo "Building for iOS x64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosX64 --quiet

echo "Building for iOS arm64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64 --quiet

echo "✅ Shared module built successfully!"
echo ""

# Step 2: Install CocoaPods dependencies
echo "📱 Step 2/4: Installing CocoaPods dependencies..."
cd ios
if command -v pod &> /dev/null; then
    pod install --silent
    echo "✅ CocoaPods dependencies installed!"
else
    echo "⚠️  CocoaPods not found. Install with: sudo gem install cocoapods"
    echo "Continuing without CocoaPods..."
fi
cd ..
echo ""

# Step 3: Verify Xcode project
echo "🔍 Step 3/4: Verifying Xcode project..."
if [ -f "ios/U1Slicer/U1Slicer.xcodeproj/project.pbxproj" ]; then
    echo "✅ Xcode project structure verified!"
else
    echo "❌ Xcode project not found!"
    exit 1
fi
echo ""

# Step 4: Open in Xcode
echo "🎯 Step 4/4: Opening project in Xcode..."
if [ -f "ios/U1Slicer/U1Slicer.xcworkspace/contents.xcworkspacedata" ]; then
    open ios/U1Slicer/U1Slicer.xcworkspace
    echo "✅ Opened Xcode workspace (CocoaPods)"
elif [ -f "ios/U1Slicer/U1Slicer.xcodeproj/project.pbxproj" ]; then
    open ios/U1Slicer/U1Slicer.xcodeproj
    echo "✅ Opened Xcode project"
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Select your target device or simulator in Xcode"
echo "  2. Press ⌘R to build and run"
echo "  3. If you see import errors, run: ./gradlew clean && ./ios/setup-ios.sh"
echo ""
echo "Framework locations:"
echo "  - Device: shared/build/xcodegen/Release/SharedModule/Release-iphonearm/"
echo "  - Simulator (x64): shared/build/xcodegen/Release/SharedModule/Release-iphonesimulator/"
echo "  - Simulator (arm64): shared/build/xcodegen/Release/SharedModule/Release-iphonesimulatorarm64/"
