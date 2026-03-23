#!/bin/bash

# Build Kotlin Multiplatform framework for iOS
# This script builds the shared module and generates the Xcode framework

set -e

echo "🔨 Building Kotlin Multiplatform framework for iOS..."

# Navigate to project root
cd "$(dirname "$0")/.."

# Build all iOS targets
echo "📱 Building for iOS arm64 (device)..."
./gradlew :shared:linkReleaseFrameworkIosArm64

echo "📱 Building for iOS x64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosX64

echo "📱 Building for iOS arm64 (simulator)..."
./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64

echo "✅ Frameworks built successfully!"
echo ""
echo "Framework locations:"
echo "  - Device: shared/build/bin/iosArm64/releaseFramework/SharedModule.framework"
echo "  - Simulator (x64): shared/build/bin/iosX64/releaseFramework/SharedModule.framework"
echo "  - Simulator (arm64): shared/build/bin/iosSimulatorArm64/releaseFramework/SharedModule.framework"
echo ""
echo "Next steps:"
echo "  1. Open ios/U1Slicer/U1Slicer.xcodeproj in Xcode"
echo "  2. Run 'pod install' in the ios/ directory"
echo "  3. Build and run the app"
