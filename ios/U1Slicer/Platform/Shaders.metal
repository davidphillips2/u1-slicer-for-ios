//
//  Shaders.metal
//  U1Slicer
//
//  Metal shaders for 3D mesh rendering
//

#include <metal_stdlib>
using namespace metal;

// ============================================================================
// MARK: - Vertex Shader
// ============================================================================

struct VertexInput {
    float3 position [[attribute(0)]];
    float3 normal [[attribute(1)]];
    float4 color [[attribute(2)]];
};

struct VertexOutput {
    float4 position [[position]];
    float3 normal;
    float4 color;
    float3 worldPos;
};

struct Uniforms {
    float4x4 modelViewProjectionMatrix;
    float4x4 modelMatrix;
    float3x3 normalMatrix;
    float3 lightDirection;
    float ambientIntensity;
    float directionalIntensity;
    uint useVertexColor;
    int highlightIndex;
};

vertex VertexOutput vertex_main(VertexInput in [[stage_in]],
                                constant Uniforms &uniforms [[buffer(1)]]) {
    VertexOutput out;

    // Transform position to clip space
    out.position = uniforms.modelViewProjectionMatrix * float4(in.position, 1.0);

    // Transform normal to world space
    out.normal = uniforms.normalMatrix * in.normal;

    // Pass color through
    out.color = in.color;

    // World position for lighting
    out.worldPos = (uniforms.modelMatrix * float4(in.position, 1.0)).xyz;

    return out;
}

// ============================================================================
// MARK: - Fragment Shader
// ============================================================================

fragment float4 fragment_main(VertexOutput in [[stage_in]],
                              constant Uniforms &uniforms [[buffer(1)]]) {
    // Normalize interpolated normal
    float3 normal = normalize(in.normal);

    // Ambient lighting
    float3 ambient = uniforms.ambientIntensity * in.color.rgb;

    // Directional lighting
    float3 lightDir = normalize(uniforms.lightDirection);
    float diff = max(dot(normal, -lightDir), 0.0);
    float3 directional = uniforms.directionalIntensity * diff * in.color.rgb;

    // Combined lighting
    float3 finalColor = ambient + directional;

    // Apply alpha
    return float4(finalColor, in.color.a);
}
