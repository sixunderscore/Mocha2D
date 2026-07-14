#version 460
#extension GL_EXT_nonuniform_qualifier : require
#extension GL_EXT_buffer_reference : enable
#extension GL_EXT_scalar_block_layout : require

layout(buffer_reference, scalar) buffer Tints {
    vec4 buff[];
};

layout(location = 0) in vec2 uvCoords;
layout(location = 1) in float textureIndex;
layout(location = 2) in float tintIndex;

layout(set = 0, binding = 0) uniform sampler2D textures[];

layout(push_constant) uniform PushConstants {
    layout(offset = 72) Tints tints;
} pushConstants;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 texColor = texture(textures[nonuniformEXT(uint(textureIndex))], uvCoords);
    vec4 tintColor = pushConstants.tints.buff[uint(tintIndex)];

    vec3 tinted = texColor.rgb * tintColor.rgb;
    vec3 final = mix(texColor.rgb, tinted, tintColor.a);

    outColor = vec4(final, texColor.a);
}
