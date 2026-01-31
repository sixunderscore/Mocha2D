#version 460
layout(location = 0) in vec2 vertexPos;
layout(location = 1) in vec2 uvCoords;
layout(location = 2) in float textureIndex;
layout(location = 3) in vec2 rotationSinAndCos;
layout(location = 4) in vec2 pivotPos;

layout(push_constant) uniform PushConstants {
    mat4 viewProjection;
} pushConstants;

layout(location = 0) out vec2 fragUvCoords;
layout(location = 1) out float fragTextureIndex;

void main() {
    fragUvCoords = uvCoords;
    fragTextureIndex = textureIndex;

    float sin = rotationSinAndCos.x;
    float cos = rotationSinAndCos.y;

    vec2 translated = vertexPos - pivotPos;
    vec2 rotated = mat2(cos, sin, -sin, cos) * translated;
    vec2 finalPos = rotated + pivotPos;

    gl_Position = pushConstants.viewProjection * vec4(finalPos, 0.0, 1.0);
}