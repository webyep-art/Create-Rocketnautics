layout(location = 0) in vec3 QuadPosition;
layout(location = 1) in vec3 SableNormal;
layout(location = 2) in uvec2 SableData;

layout(std140) uniform SableSprites {
    vec4 sableSprites[2 * SABLE_TEXTURE_CACHE_SIZE];
};

uniform mat4 SableTransform;

vec3 Position;
vec3 Normal;
vec4 Color;
vec2 UV0;
ivec2 UV2;

void _sable_unpack() {
    uint vertexIndex = uint(gl_VertexID) & 0x3u;

    // Packed data format:
    // TTTTTTTTTTTTLLLLLLLLZZZZYYYYXXXX
    // T = Texture ID
    // L = Packed Light
    // Z = Relative Z position
    // Y = Relative Y position
    // X = Relative X position
    uint posX = SableData.x & 15u;
    uint posY = (SableData.x >> 4) & 15u;
    uint posZ = (SableData.x >> 8) & 15u;
    uint packedLight = (SableData.x >> 12) & 255u;
    uint textureId = SableData.x >> 20u;

    // Packed data format:
    // AAAAAAAAYYYYYYYYZZZZZZZZXXXXXXXX
    // A = Ambient Occlusion
    // Y = Section Y
    // Z = Section Z
    // X = Section X
    uint xOffset = (SableData.y) & 0xFFu;
    uint yOffset = (SableData.y >> 8) & 0xFFu;
    uint zOffset = (SableData.y >> 16) & 0xFFu;
    uint ambientOcclusion = (SableData.y >> (24u + (vertexIndex << 1u))) & 0x3u;

    // 0,0 == 0b00
    // 0,1 == 0b01
    // 1,1 == 0b10
    // 1,0 == 0b11
    uint lower = uint(gl_VertexID) & 1u;
    uint upper = (uint(gl_VertexID) >> 1) & 1u;
    vec2 uv = vec2(float(upper), float(lower ^ upper));
    uint textureOffset = vertexIndex << 3u;

    vec4 textureU = sableSprites[(textureId << 1u)];
    vec4 textureV = sableSprites[(textureId << 1u) + 1u];

    Position = (SableTransform * vec4(QuadPosition + vec3(float((xOffset << 4u) + posX), float((yOffset << 4u) + posY), float((zOffset << 4u) + posZ)), 1.0)).xyz;
    Normal = (SableTransform * vec4(SableNormal, 0.0)).xyz;
    Color = vec4(1.0, 1.0, 1.0, 1.0) * vec4(vec3(1.0 - 0.2 * float(ambientOcclusion)), 1.0);
    UV0 = vec2(textureU[vertexIndex], textureV[vertexIndex]);
    UV2 = ivec2(packedLight & 0xF0u, (packedLight << 4) & 0xF0u);
}