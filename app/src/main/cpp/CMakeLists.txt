cmake_minimum_required(VERSION 3.22.1)

project("prusaslicer_ndk" VERSION 1.0.0 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# =============================================================================
# Path Setup
# =============================================================================
set(EXTERN_DIR "${CMAKE_CURRENT_SOURCE_DIR}/extern")
set(ABI_DIR "${ANDROID_ABI}")
set(PRUSA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/prusaslicer")
set(LIBSLIC3R_DIR "${PRUSA_DIR}/src/libslic3r")
set(BUNDLED_DIR "${PRUSA_DIR}/bundled_deps")
set(CLIPPER_DIR "${PRUSA_DIR}/src/clipper")

# Consolidate all bundled include paths for core targets
set(BUNDLED_INCLUDES
    ${BUNDLED_DIR}
    ${BUNDLED_DIR}/admesh
    ${BUNDLED_DIR}/admesh/admesh
    ${BUNDLED_DIR}/semver
    ${BUNDLED_DIR}/miniz
    ${BUNDLED_DIR}/glu-libtess
    ${BUNDLED_DIR}/glu-libtess/include
    ${BUNDLED_DIR}/localesutils
    ${BUNDLED_DIR}/fast_float
    ${BUNDLED_DIR}/int128
    ${BUNDLED_DIR}/ankerl
    ${BUNDLED_DIR}/tcbspan
    ${BUNDLED_DIR}/agg
    ${BUNDLED_DIR}/libigl
    ${BUNDLED_DIR}/nanosvg
    ${BUNDLED_DIR}/qoi
    ${BUNDLED_DIR}/libnest2d/include
    ${CLIPPER_DIR}

    ${CMAKE_CURRENT_SOURCE_DIR}/extern/libbgcode_stub/include
    ${CMAKE_CURRENT_SOURCE_DIR}/extern/jpeg_stub/include
    ${CMAKE_CURRENT_SOURCE_DIR}/extern/nlopt_stub/include
    ${CMAKE_CURRENT_SOURCE_DIR}/extern/libpng_stub/include
    ${CMAKE_CURRENT_SOURCE_DIR}/extern/qhull_stub/include
)



# =============================================================================
# Android-specific definitions
# =============================================================================
add_compile_definitions(
    SLIC3R_ANDROID=1
    NDEBUG
    SLIC3R_VERSION="2.8.1"
    SLIC3R_VERSION_FULL="2.8.1-android"
    SLIC3R_BUILD_ID="android-arm64"
    # Disable GUI and desktop features
    SLIC3R_GUI=0
)

# =============================================================================
# External Dependencies (built by scripts/build_deps.sh)
# =============================================================================

# --- Boost ---
set(BOOST_ROOT "${EXTERN_DIR}/boost")
include_directories("${BOOST_ROOT}/include")
link_directories("${BOOST_ROOT}/lib/${ABI_DIR}")
# Note: Boost-for-Android uses custom suffixes for the .a files
set(BOOST_SUFFIX "-clang-mt-a64-1_84")
set(BOOST_LIBS 
    "boost_log_setup${BOOST_SUFFIX}"
    "boost_log${BOOST_SUFFIX}"
    "boost_filesystem${BOOST_SUFFIX}"
    "boost_thread${BOOST_SUFFIX}"
    "boost_regex${BOOST_SUFFIX}"
    "boost_iostreams${BOOST_SUFFIX}"
    "boost_nowide${BOOST_SUFFIX}"
    "boost_chrono${BOOST_SUFFIX}"
    "boost_atomic${BOOST_SUFFIX}"
    "boost_system${BOOST_SUFFIX}"
)

# --- Eigen (header-only) ---
include_directories("${EXTERN_DIR}/eigen/include")

# --- TBB ---
include_directories("${EXTERN_DIR}/tbb/include")
link_directories("${EXTERN_DIR}/tbb/lib/${ABI_DIR}")

# --- CGAL (header-only) ---
include_directories("${EXTERN_DIR}/cgal/include")

# --- GMP/MPFR (for CGAL) ---
include_directories("${EXTERN_DIR}/gmp/include")
link_directories("${EXTERN_DIR}/gmp/lib/${ABI_DIR}")
include_directories("${EXTERN_DIR}/mpfr/include")
link_directories("${EXTERN_DIR}/mpfr/lib/${ABI_DIR}")

# --- OCCT ---
include_directories("${EXTERN_DIR}/occt/include")
link_directories("${EXTERN_DIR}/occt/lib/${ABI_DIR}")
set(OCCT_LIBS
    TKDESTEP TKDEIGES TKDEOBJ TKDEPLY TKDEGLTF TKDEVRML TKDESTL
    TKMesh TKGeomAlgo TKTopAlgo TKGeomBase TKBRep TKMath TKernel
    TKService TKV3d TKCAF TKCDF TKXSBase TKBO TKBool TKFillet
    TKOffset TKPrim TKHLR TKXCAF TKBinXCAF TKXmlXCAF TKBin TKXml
    TKShHealing TKExpress TKRWMesh TKXMesh TKStd TKBinL TKXmlL
)

# --- Clipper2 ---
include_directories("${EXTERN_DIR}/clipper2/include")
link_directories("${EXTERN_DIR}/clipper2/lib/${ABI_DIR}")

# --- Cereal (header-only) ---
include_directories("${EXTERN_DIR}/cereal/include")

# --- nlohmann/json (header-only) ---
include_directories("${EXTERN_DIR}/nlohmann/include")

# --- zlib ---
include_directories("${EXTERN_DIR}/zlib/include")
link_directories("${EXTERN_DIR}/zlib/lib/${ABI_DIR}")

# --- Expat ---
include_directories("${EXTERN_DIR}/expat/include")
link_directories("${EXTERN_DIR}/expat/lib/${ABI_DIR}")

# =============================================================================
# Bundled Dependencies (from prusaslicer/bundled_deps/)
# =============================================================================

# --- admesh ---
add_library(admesh STATIC
    ${BUNDLED_DIR}/admesh/admesh/connect.cpp
    ${BUNDLED_DIR}/admesh/admesh/normals.cpp
    ${BUNDLED_DIR}/admesh/admesh/shared.cpp
    ${BUNDLED_DIR}/admesh/admesh/stl_io.cpp
    ${BUNDLED_DIR}/admesh/admesh/stlinit.cpp
    ${BUNDLED_DIR}/admesh/admesh/util.cpp
)
target_include_directories(admesh PUBLIC ${BUNDLED_DIR}/admesh ${BUNDLED_DIR}/admesh/admesh ${BUNDLED_INCLUDES})

# --- semver ---
add_library(semver STATIC ${BUNDLED_DIR}/semver/semver.c)
target_include_directories(semver PUBLIC ${BUNDLED_DIR}/semver ${BUNDLED_INCLUDES})

# --- miniz ---
add_library(miniz_static STATIC ${BUNDLED_DIR}/miniz/miniz.c)
target_include_directories(miniz_static PUBLIC ${BUNDLED_DIR}/miniz ${BUNDLED_INCLUDES})
add_library(miniz INTERFACE)
target_link_libraries(miniz INTERFACE miniz_static)

# --- glu-libtess ---
add_library(glu-libtess STATIC
    ${BUNDLED_DIR}/glu-libtess/src/dict.c
    ${BUNDLED_DIR}/glu-libtess/src/geom.c
    ${BUNDLED_DIR}/glu-libtess/src/memalloc.c
    ${BUNDLED_DIR}/glu-libtess/src/mesh.c
    ${BUNDLED_DIR}/glu-libtess/src/normal.c
    ${BUNDLED_DIR}/glu-libtess/src/priorityq.c
    ${BUNDLED_DIR}/glu-libtess/src/render.c
    ${BUNDLED_DIR}/glu-libtess/src/sweep.c
    ${BUNDLED_DIR}/glu-libtess/src/tess.c
    ${BUNDLED_DIR}/glu-libtess/src/tessmono.c
)
target_include_directories(glu-libtess
    PRIVATE ${BUNDLED_DIR}/glu-libtess
    PUBLIC ${BUNDLED_DIR}/glu-libtess/include
)
target_link_libraries(glu-libtess m)

# --- qoi ---
add_library(qoi STATIC ${BUNDLED_DIR}/qoi/qoilib.c)
target_include_directories(qoi PUBLIC ${BUNDLED_DIR}/qoi ${BUNDLED_INCLUDES})

# --- localesutils ---
add_library(localesutils STATIC
    ${BUNDLED_DIR}/localesutils/LocalesUtils.cpp
)
target_include_directories(localesutils PUBLIC ${BUNDLED_DIR}/localesutils ${BUNDLED_INCLUDES})

add_library(fastfloat INTERFACE)
target_include_directories(fastfloat INTERFACE ${BUNDLED_DIR}/fast_float ${BUNDLED_INCLUDES})

add_library(int128 INTERFACE)
target_include_directories(int128 INTERFACE ${BUNDLED_DIR}/int128 ${BUNDLED_INCLUDES})

add_library(ankerl INTERFACE)
target_include_directories(ankerl INTERFACE ${BUNDLED_DIR}/ankerl ${BUNDLED_INCLUDES})

add_library(tcbspan INTERFACE)
target_include_directories(tcbspan INTERFACE ${BUNDLED_DIR}/tcbspan ${BUNDLED_INCLUDES})

add_library(agg INTERFACE)
target_include_directories(agg INTERFACE ${BUNDLED_DIR}/agg ${BUNDLED_INCLUDES})

# --- libigl (header-only) ---
add_library(libigl INTERFACE)
target_include_directories(libigl INTERFACE ${BUNDLED_DIR}/libigl ${BUNDLED_INCLUDES})



# --- clipper (PrusaSlicer's bundled version) ---
add_library(clipper STATIC
    ${CLIPPER_DIR}/clipper.cpp
    ${CLIPPER_DIR}/clipper_z.cpp
)
target_include_directories(clipper PUBLIC ${CLIPPER_DIR} ${BUNDLED_INCLUDES})

# Link admesh to its dependencies
target_link_libraries(admesh PRIVATE localesutils)
target_include_directories(admesh PRIVATE ${BUNDLED_INCLUDES})

# =============================================================================
# Fake/Stub targets for deps we skip on Android
# =============================================================================

# LibBGCode — stub (binary G-code not needed for initial Android build)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/extern/libbgcode_stub/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/extern/jpeg_stub/include)
add_library(bgcode_convert INTERFACE)
add_library(LibBGCode::bgcode_convert ALIAS bgcode_convert)

# NLopt — stub (optimization library, optional)
# libnest2d needs it but we can stub for now
add_library(NLopt INTERFACE)

# Qhull — stub for now
add_library(qhull INTERFACE)

# PNG/JPEG — stubs (not needed for headless slicing)
add_library(png_stub INTERFACE)
add_library(jpeg_stub INTERFACE)

# =============================================================================
# Generate version header
# =============================================================================
set(SLIC3R_VERSION "2.8.1")
set(SLIC3R_VERSION_FULL "2.8.1-android")
set(SLIC3R_BUILD_ID "android-arm64")
configure_file(
    ${LIBSLIC3R_DIR}/libslic3r_version.h.in
    ${CMAKE_CURRENT_BINARY_DIR}/libslic3r_version.h
    @ONLY
)

# =============================================================================
# libslic3r — Core FFF slicing sources
# We include only the files needed for FFF slicing, excluding SLA, GUI, etc.
# =============================================================================
set(LIBSLIC3R_CORE_SOURCES
    # Core geometry
    ${LIBSLIC3R_DIR}/AABBMesh.cpp
    ${LIBSLIC3R_DIR}/Algorithm/RegionExpansion.cpp
    ${LIBSLIC3R_DIR}/BoundingBox.cpp
    ${LIBSLIC3R_DIR}/BridgeDetector.cpp
    ${LIBSLIC3R_DIR}/Brim.cpp
    ${LIBSLIC3R_DIR}/BuildVolume.cpp
    ${LIBSLIC3R_DIR}/clipper.cpp
    ${LIBSLIC3R_DIR}/ClipperUtils.cpp
    ${LIBSLIC3R_DIR}/Color.cpp
    ${LIBSLIC3R_DIR}/Config.cpp
    ${LIBSLIC3R_DIR}/EdgeGrid.cpp
    ${LIBSLIC3R_DIR}/ElephantFootCompensation.cpp
    ${LIBSLIC3R_DIR}/Emboss.cpp
    ${LIBSLIC3R_DIR}/ExPolygon.cpp
    ${LIBSLIC3R_DIR}/ExPolygonsIndex.cpp
    ${LIBSLIC3R_DIR}/Extruder.cpp
    ${LIBSLIC3R_DIR}/ExtrusionEntity.cpp
    ${LIBSLIC3R_DIR}/ExtrusionEntityCollection.cpp
    ${LIBSLIC3R_DIR}/ExtrusionRole.cpp
    ${LIBSLIC3R_DIR}/ExtrusionSimulator.cpp

    # Fill patterns
    ${LIBSLIC3R_DIR}/Fill/Fill.cpp
    ${LIBSLIC3R_DIR}/Fill/Fill3DHoneycomb.cpp
    ${LIBSLIC3R_DIR}/Fill/FillAdaptive.cpp
    ${LIBSLIC3R_DIR}/Fill/FillBase.cpp
    ${LIBSLIC3R_DIR}/Fill/FillConcentric.cpp
    ${LIBSLIC3R_DIR}/Fill/FillEnsuring.cpp
    ${LIBSLIC3R_DIR}/Fill/FillHoneycomb.cpp
    ${LIBSLIC3R_DIR}/Fill/FillGyroid.cpp
    ${LIBSLIC3R_DIR}/Fill/FillPlanePath.cpp
    ${LIBSLIC3R_DIR}/Fill/FillLine.cpp
    ${LIBSLIC3R_DIR}/Fill/FillLightning.cpp
    ${LIBSLIC3R_DIR}/Fill/Lightning/DistanceField.cpp
    ${LIBSLIC3R_DIR}/Fill/Lightning/Generator.cpp
    ${LIBSLIC3R_DIR}/Fill/Lightning/Layer.cpp
    ${LIBSLIC3R_DIR}/Fill/Lightning/TreeNode.cpp
    ${LIBSLIC3R_DIR}/Fill/FillRectilinear.cpp
    ${LIBSLIC3R_DIR}/Flow.cpp

    # Format loaders
    ${LIBSLIC3R_DIR}/Format/3mf.cpp
    ${LIBSLIC3R_DIR}/Format/AMF.cpp
    ${LIBSLIC3R_DIR}/Format/OBJ.cpp
    ${LIBSLIC3R_DIR}/Format/objparser.cpp
    ${LIBSLIC3R_DIR}/Format/STL.cpp
    ${LIBSLIC3R_DIR}/Format/STEP.cpp
    ${LIBSLIC3R_DIR}/Format/SVG.cpp
    ${LIBSLIC3R_DIR}/Format/SLAArchiveWriter.cpp
    ${LIBSLIC3R_DIR}/Format/SLAArchiveReader.cpp
    ${LIBSLIC3R_DIR}/Format/ZipperArchiveImport.cpp
    ${LIBSLIC3R_DIR}/Format/SL1.cpp
    ${LIBSLIC3R_DIR}/Format/SL1_SVG.cpp
    ${LIBSLIC3R_DIR}/Format/AnycubicSLA.cpp
    ${LIBSLIC3R_DIR}/Format/SLAArchiveFormatRegistry.cpp
    ${LIBSLIC3R_DIR}/Format/PrintRequest.cpp

    # GCode generation
    ${LIBSLIC3R_DIR}/GCode/ThumbnailData.cpp
    ${LIBSLIC3R_DIR}/GCode/Thumbnails.cpp
    ${LIBSLIC3R_DIR}/GCode/ConflictChecker.cpp
    ${LIBSLIC3R_DIR}/GCode/CoolingBuffer.cpp
    ${LIBSLIC3R_DIR}/GCode/ExtrusionProcessor.cpp
    ${LIBSLIC3R_DIR}/GCode/FindReplace.cpp
    ${LIBSLIC3R_DIR}/GCode/LabelObjects.cpp
    ${LIBSLIC3R_DIR}/GCode/GCodeWriter.cpp
    ${LIBSLIC3R_DIR}/GCode/PostProcessor.cpp
    ${LIBSLIC3R_DIR}/GCode/PressureEqualizer.cpp
    ${LIBSLIC3R_DIR}/GCode/PrintExtents.cpp
    ${LIBSLIC3R_DIR}/GCode/RetractWhenCrossingPerimeters.cpp
    ${LIBSLIC3R_DIR}/GCode/SpiralVase.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamPlacer.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamChoice.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamPerimeters.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamShells.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamGeometry.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamAligned.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamRear.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamRandom.cpp
    ${LIBSLIC3R_DIR}/GCode/SeamPainting.cpp
    ${LIBSLIC3R_DIR}/GCode/ModelVisibility.cpp
    ${LIBSLIC3R_DIR}/GCode/SmoothPath.cpp
    ${LIBSLIC3R_DIR}/GCode/ToolOrdering.cpp
    ${LIBSLIC3R_DIR}/GCode/Wipe.cpp
    ${LIBSLIC3R_DIR}/GCode/WipeTower.cpp
    ${LIBSLIC3R_DIR}/GCode/WipeTowerIntegration.cpp
    ${LIBSLIC3R_DIR}/GCode/GCodeProcessor.cpp
    ${LIBSLIC3R_DIR}/GCode/AvoidCrossingPerimeters.cpp
    ${LIBSLIC3R_DIR}/GCode/Travels.cpp
    ${LIBSLIC3R_DIR}/GCode/ExtrusionOrder.cpp
    ${LIBSLIC3R_DIR}/GCode.cpp
    ${LIBSLIC3R_DIR}/GCodeReader.cpp

    # Geometry
    ${LIBSLIC3R_DIR}/Geometry.cpp
    ${LIBSLIC3R_DIR}/Geometry/ArcWelder.cpp
    ${LIBSLIC3R_DIR}/Geometry/Circle.cpp
    ${LIBSLIC3R_DIR}/Geometry/ConvexHull.cpp
    ${LIBSLIC3R_DIR}/Geometry/MedialAxis.cpp
    ${LIBSLIC3R_DIR}/Geometry/VoronoiOffset.cpp
    ${LIBSLIC3R_DIR}/Geometry/VoronoiUtils.cpp
    ${LIBSLIC3R_DIR}/Geometry/Voronoi.cpp

    # Core classes
    ${LIBSLIC3R_DIR}/JumpPointSearch.cpp
    ${LIBSLIC3R_DIR}/Layer.cpp
    ${LIBSLIC3R_DIR}/LayerRegion.cpp
    ${LIBSLIC3R_DIR}/Line.cpp
    ${LIBSLIC3R_DIR}/BlacklistedLibraryCheck.cpp
    ${LIBSLIC3R_DIR}/CutUtils.cpp
    ${LIBSLIC3R_DIR}/Model.cpp
    ${LIBSLIC3R_DIR}/ModelArrange.cpp
    ${LIBSLIC3R_DIR}/MultiMaterialSegmentation.cpp
    ${LIBSLIC3R_DIR}/MeshNormals.cpp
    ${LIBSLIC3R_DIR}/Measure.cpp
    ${LIBSLIC3R_DIR}/CustomGCode.cpp

    # Arrange
    ${LIBSLIC3R_DIR}/Arrange.cpp
    ${LIBSLIC3R_DIR}/Arrange/Items/ArrangeItem.cpp
    ${LIBSLIC3R_DIR}/Arrange/Items/SimpleArrangeItem.cpp
    ${LIBSLIC3R_DIR}/Arrange/ArrangeSettingsDb_AppCfg.cpp
    ${LIBSLIC3R_DIR}/Arrange/Scene.cpp
    ${LIBSLIC3R_DIR}/Arrange/SceneBuilder.cpp
    ${LIBSLIC3R_DIR}/Arrange/Core/Beds.cpp
    ${LIBSLIC3R_DIR}/Arrange/Core/NFP/NFP.cpp
    ${LIBSLIC3R_DIR}/Arrange/Core/NFP/NFPConcave_CGAL.cpp
    ${LIBSLIC3R_DIR}/Arrange/Core/NFP/NFPConcave_Tesselate.cpp
    ${LIBSLIC3R_DIR}/Arrange/Core/NFP/EdgeCache.cpp

    # Print & config
    ${LIBSLIC3R_DIR}/MultiPoint.cpp
    ${LIBSLIC3R_DIR}/NormalUtils.cpp
    ${LIBSLIC3R_DIR}/NSVGUtils.cpp
    ${LIBSLIC3R_DIR}/ObjectID.cpp
    ${LIBSLIC3R_DIR}/PerimeterGenerator.cpp
    ${LIBSLIC3R_DIR}/PlaceholderParser.cpp
    ${LIBSLIC3R_DIR}/Platform.cpp
    ${LIBSLIC3R_DIR}/Point.cpp
    ${LIBSLIC3R_DIR}/Polygon.cpp
    ${LIBSLIC3R_DIR}/MutablePolygon.cpp
    ${LIBSLIC3R_DIR}/PolygonTrimmer.cpp
    ${LIBSLIC3R_DIR}/Polyline.cpp
    ${LIBSLIC3R_DIR}/Preset.cpp
    ${LIBSLIC3R_DIR}/PresetBundle.cpp
    ${LIBSLIC3R_DIR}/PrincipalComponents2D.cpp
    ${LIBSLIC3R_DIR}/AppConfig.cpp
    ${LIBSLIC3R_DIR}/Print.cpp
    ${LIBSLIC3R_DIR}/PrintApply.cpp
    ${LIBSLIC3R_DIR}/PrintBase.cpp
    ${LIBSLIC3R_DIR}/PrintConfig.cpp
    ${LIBSLIC3R_DIR}/PrintObject.cpp
    ${LIBSLIC3R_DIR}/PrintObjectSlice.cpp
    ${LIBSLIC3R_DIR}/PrintRegion.cpp
    ${LIBSLIC3R_DIR}/PNGReadWrite.cpp
    ${LIBSLIC3R_DIR}/QuadricEdgeCollapse.cpp
    ${LIBSLIC3R_DIR}/Semver.cpp
    ${LIBSLIC3R_DIR}/ShortEdgeCollapse.cpp
    ${LIBSLIC3R_DIR}/ShortestPath.cpp
    ${LIBSLIC3R_DIR}/Slicing.cpp
    ${LIBSLIC3R_DIR}/SlicesToTriangleMesh.cpp
    ${LIBSLIC3R_DIR}/SlicingAdaptive.cpp
    ${LIBSLIC3R_DIR}/Subdivide.cpp

    # Support
    ${LIBSLIC3R_DIR}/Support/SupportCommon.cpp
    ${LIBSLIC3R_DIR}/Support/SupportDebug.cpp
    ${LIBSLIC3R_DIR}/Support/SupportMaterial.cpp
    ${LIBSLIC3R_DIR}/Support/SupportParameters.cpp
    ${LIBSLIC3R_DIR}/Support/OrganicSupport.cpp
    ${LIBSLIC3R_DIR}/Support/TreeSupport.cpp
    ${LIBSLIC3R_DIR}/Support/TreeSupportCommon.cpp
    ${LIBSLIC3R_DIR}/Support/TreeModelVolumes.cpp
    ${LIBSLIC3R_DIR}/SupportSpotsGenerator.cpp
    ${LIBSLIC3R_DIR}/Surface.cpp
    ${LIBSLIC3R_DIR}/SurfaceCollection.cpp
    ${LIBSLIC3R_DIR}/SVG.cpp

    # Mesh
    ${LIBSLIC3R_DIR}/Tesselate.cpp
    ${LIBSLIC3R_DIR}/TriangleMesh.cpp
    ${LIBSLIC3R_DIR}/TriangleMeshSlicer.cpp
    ${LIBSLIC3R_DIR}/utils.cpp
    ${LIBSLIC3R_DIR}/Time.cpp
    ${LIBSLIC3R_DIR}/Timer.cpp
    ${LIBSLIC3R_DIR}/Thread.cpp
    ${LIBSLIC3R_DIR}/TriangleSelector.cpp
    ${LIBSLIC3R_DIR}/TriangleSetSampling.cpp
    ${LIBSLIC3R_DIR}/TriangleSelectorWrapper.cpp
    ${LIBSLIC3R_DIR}/Zipper.cpp
    ${LIBSLIC3R_DIR}/MinAreaBoundingBox.cpp
    ${LIBSLIC3R_DIR}/miniz_extension.cpp
    ${LIBSLIC3R_DIR}/ProfilesSharingUtils.cpp
    ${LIBSLIC3R_DIR}/Utils/DirectoriesUtils.cpp
    ${LIBSLIC3R_DIR}/Utils/JsonUtils.cpp

    # SLA (minimal — needed for some shared code)
    ${LIBSLIC3R_DIR}/SLAPrint.cpp
    ${LIBSLIC3R_DIR}/SLAPrintSteps.cpp
    ${LIBSLIC3R_DIR}/SLA/Pad.cpp
    ${LIBSLIC3R_DIR}/SLA/SupportTreeMesher.cpp
    ${LIBSLIC3R_DIR}/SLA/SupportTreeBuilder.cpp
    ${LIBSLIC3R_DIR}/SLA/SupportTree.cpp
    ${LIBSLIC3R_DIR}/SLA/Rotfinder.cpp
    ${LIBSLIC3R_DIR}/SLA/SpatIndex.cpp
    ${LIBSLIC3R_DIR}/SLA/RasterBase.cpp
    ${LIBSLIC3R_DIR}/SLA/RasterToPolygons.cpp
    ${LIBSLIC3R_DIR}/SLA/ConcaveHull.cpp
    ${LIBSLIC3R_DIR}/SLA/Hollowing.cpp
    ${LIBSLIC3R_DIR}/SLA/SupportPointGenerator.cpp
    ${LIBSLIC3R_DIR}/SLA/Clustering.cpp
    ${LIBSLIC3R_DIR}/SLA/DefaultSupportTree.cpp
    ${LIBSLIC3R_DIR}/SLA/BranchingTreeSLA.cpp
    ${LIBSLIC3R_DIR}/SLA/ZCorrection.cpp
    ${LIBSLIC3R_DIR}/BranchingTree/BranchingTree.cpp
    ${LIBSLIC3R_DIR}/BranchingTree/PointCloud.cpp

    # Arachne (variable-width perimeters)
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/BeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/BeadingStrategyFactory.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/DistributedBeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/LimitedBeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/OuterWallInsetBeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/RedistributeBeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/BeadingStrategy/WideningBeadingStrategy.cpp
    ${LIBSLIC3R_DIR}/Arachne/utils/ExtrusionLine.cpp
    ${LIBSLIC3R_DIR}/Arachne/utils/SquareGrid.cpp
    ${LIBSLIC3R_DIR}/Arachne/utils/PolylineStitcher.cpp
    ${LIBSLIC3R_DIR}/Arachne/PerimeterOrder.cpp
    ${LIBSLIC3R_DIR}/Arachne/SkeletalTrapezoidation.cpp
    ${LIBSLIC3R_DIR}/Arachne/SkeletalTrapezoidationGraph.cpp
    ${LIBSLIC3R_DIR}/Arachne/WallToolPaths.cpp
)

# CGAL-dependent sources (separate lib)
add_library(libslic3r_cgal STATIC
    ${LIBSLIC3R_DIR}/CutSurface.cpp
    ${LIBSLIC3R_DIR}/Geometry/VoronoiUtilsCgal.cpp
    ${LIBSLIC3R_DIR}/IntersectionPoints.cpp
    ${LIBSLIC3R_DIR}/MeshBoolean.cpp
    ${LIBSLIC3R_DIR}/TryCatchSignal.cpp
    ${LIBSLIC3R_DIR}/Triangulation.cpp
)
target_include_directories(libslic3r_cgal PRIVATE
    ${CMAKE_CURRENT_BINARY_DIR}
    ${PRUSA_DIR}/src
    ${LIBSLIC3R_DIR}
    ${BUNDLED_INCLUDES}
)

# =============================================================================
# Main libslic3r library
# =============================================================================
add_library(libslic3r STATIC ${LIBSLIC3R_CORE_SOURCES})

target_compile_definitions(libslic3r PUBLIC
    -DUSE_TBB
    -DTBB_USE_CAPTURED_EXCEPTION=0
)

target_include_directories(libslic3r PUBLIC
    ${PRUSA_DIR}/src
    ${LIBSLIC3R_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
    ${BUNDLED_INCLUDES}
)

target_link_libraries(libslic3r PRIVATE
    admesh
    clipper
    glu-libtess
    qoi
    fastfloat
    int128
    localesutils
    libslic3r_cgal
    miniz
)

target_link_libraries(libslic3r PUBLIC
    semver
    ankerl
    tcbspan
    libigl
    agg
    # External libs
    ${BOOST_LIBS}
    ${OCCT_LIBS}
    tbb
    tbbmalloc
    gmpxx
    gmp
    mpfr
    Clipper2
    z
    expat
)

# =============================================================================
# SAPIL (Slicer API Layer) — JNI Bridge
# =============================================================================
add_library(prusaslicer-jni SHARED
    src/slicer_wrapper.cpp
    src/sapil_config.cpp
    src/sapil_model.cpp
    src/sapil_print.cpp
    src/sapil_gcode.cpp
    src/sapil_progress.cpp
    src/sapil_arrange.cpp
)

target_include_directories(prusaslicer-jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${PRUSA_DIR}/src
    ${LIBSLIC3R_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
    ${BUNDLED_INCLUDES}
)

find_library(log-lib log)

target_link_libraries(prusaslicer-jni
    ${log-lib}
    libslic3r
)
