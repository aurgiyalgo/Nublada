#version 460
out vec4 FragColor;

in vec2 textureCoords;
in vec4 outData;
in vec3 passLight;
in float passFace;

const float border = 0.03125;
const vec2 texelSize = vec2(1 / 2048.0, 1 / 2048.0);

layout (binding = 0) uniform sampler2D textureSampler;
layout (binding = 1) uniform sampler2D shadowSampler;
uniform float dayTime;
uniform vec3 lightDir;
uniform float timeOfDay;
uniform float light;

const int atlasSize = 128;
const int textureSize = 32;
const int texturesPerSide = atlasSize / textureSize;

in VS_OUT {
    vec3 FragPos;
    vec3 Normal;
    vec2 TexCoords;
    vec4 FragPosLightSpace;
} fs_in;

float calculateShadow(vec4 fragPosLightSpace, float dotProduct) {
    if (dotProduct < 0.0025)
        return 1.0;

    // perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0)
        return 0.0;

    // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords)
    float closestDepth = texture(shadowSampler, projCoords.xy).r;
    // get depth of current fragment from light's perspective
    float currentDepth = projCoords.z;
    // check whether current frag pos is in shadow

    float bias = max(0.01 * (1.0 - dotProduct), 0.0025);
    float shadow = 0.0;
    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadowSampler, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return shadow * dotProduct;
}

void main()
{
    if (textureCoords.x > 1 || textureCoords.y < 1) {
        discard;
    }

    float xUv = mod(outData.z, texturesPerSide) / texturesPerSide + mod(outData.x, 1.0) / texturesPerSide;
    float yUv = int(outData.z / texturesPerSide) / float(texturesPerSide) + mod(outData.y, 1.0) / texturesPerSide;
    vec4 color = texture(textureSampler, vec2(xUv, yUv));
    if (color.a < 1.0) {
        discard;
    }

    float dotProduct = dot(fs_in.Normal, lightDir);
    float shadow = calculateShadow(fs_in.FragPosLightSpace, dotProduct);
    float s = max((1 - shadow) * dotProduct * max(timeOfDay, 0.25), 0.375 * sqrt(light) + 0.0625);

    FragColor = vec4((vec3(color.r * max(passLight.r, s), color.g * max(passLight.g, s), color.b * max(passLight.b, s)) * outData.a), 1.0);
}