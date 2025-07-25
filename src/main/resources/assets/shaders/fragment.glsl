#version 460
#extension GL_EXT_nonuniform_qualifier : require
layout(location = 0) in vec2 uvCoords;
layout(location = 1) in float textureIndex;

layout(constant_id = 0) const int MAX_TEXTURES = 16;
layout(set = 0, binding = 0) uniform sampler2D textures[MAX_TEXTURES];

layout(location = 0) out vec4 outColor;

void main() {
    outColor = texture(textures[nonuniformEXT(int(textureIndex))], uvCoords);
}
