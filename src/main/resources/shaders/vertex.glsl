#version 460
#extension GL_EXT_buffer_reference2 : require
#extension GL_EXT_scalar_block_layout : require

struct TransformData {
    mat2 transform;
    vec2 origin;
};

layout(buffer_reference, scalar) buffer Transforms {
    TransformData buff[];
};

layout(location = 0) in vec2 vertexPos;
layout(location = 1) in vec2 uvCoords;
layout(location = 2) in float textureIndex;
layout(location = 3) in float transformIndex;

layout(push_constant) uniform PushConstants {
    mat4 viewProjection;
    Transforms transforms;
} pushConstants;

layout(location = 0) out vec2 fragUvCoords;
layout(location = 1) out float fragTextureIndex;

void main() {
    fragUvCoords = uvCoords;
    fragTextureIndex = textureIndex;

    TransformData data = pushConstants.transforms.buff[uint(transformIndex)];

    vec2 transformed = data.transform * (vertexPos - data.origin);
    vec2 finalPos = transformed + data.origin;

    gl_Position = pushConstants.viewProjection * vec4(finalPos, 0.0, 1.0);
}