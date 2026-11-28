package com.example.render

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(
    vertexSrc: String,
    fragmentSrc: String,
    private val label: String = "Shader",
    fallbackVert: String? = null,
    fallbackFrag: String? = null
) {
    val programId: Int
    val isValid: Boolean
    val errorLog: String

    init {
        val primary = buildProgram(vertexSrc, fragmentSrc)
        if (primary.second) {
            programId = primary.first
            isValid = true
            errorLog = ""
        } else if (fallbackVert != null && fallbackFrag != null) {
            val fb = buildProgram(fallbackVert, fallbackFrag)
            programId = fb.first
            isValid = fb.second
            errorLog = "${primary.third} | fallback=${fb.second}"
            Log.e("ShaderProgram", "[$label] Primary shader failed, fallback=${fb.second}: ${primary.third}")
        } else {
            programId = primary.first
            isValid = false
            errorLog = primary.third
            Log.e("ShaderProgram", "[$label] Shader build failed: ${primary.third}")
        }
    }

    private fun buildProgram(vertexSrc: String, fragmentSrc: String): Triple<Int, Boolean, String> {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        if (vs.first == 0 || fs.first == 0) {
            if (vs.first != 0) GLES30.glDeleteShader(vs.first)
            if (fs.first != 0) GLES30.glDeleteShader(fs.first)
            return Triple(0, false, "VS:${vs.second} FS:${fs.second}".trim())
        }
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs.first)
        GLES30.glAttachShader(prog, fs.first)
        GLES30.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
        GLES30.glDeleteShader(vs.first)
        GLES30.glDeleteShader(fs.first)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            GLES30.glDeleteProgram(prog)
            return Triple(0, false, "Link error: $log")
        }
        return Triple(prog, true, "")
    }

    fun use() {
        if (programId != 0) {
            GLES30.glUseProgram(programId)
        }
    }

    fun uniformLocation(name: String): Int = if (programId != 0) GLES30.glGetUniformLocation(programId, name) else -1

    fun destroy() {
        if (programId != 0) GLES30.glDeleteProgram(programId)
    }

    private fun compileShader(type: Int, src: String): Pair<Int, String> {
        val id = GLES30.glCreateShader(type)
        if (id == 0) return 0 to "glCreateShader failed"
        GLES30.glShaderSource(id, src)
        GLES30.glCompileShader(id)
        val status = IntArray(1)
        GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(id)
            Log.e("ShaderProgram", "[$label] Compile error: $log")
            GLES30.glDeleteShader(id)
            return 0 to log
        }
        return id to ""
    }
}

object Shaders {

    const val SHADOW_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
uniform mat4 uLightMVP;
void main() {
    gl_Position = uLightMVP * vec4(aPos, 1.0);
}
"""

    const val SKINNED_SHADOW_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 4) in vec4 aJoints;
layout(location = 5) in vec4 aWeights;

layout(std140) uniform BoneBlock {
    mat4 uBones[72];
};

uniform mat4 uLightMVP;

void main() {
    ivec4 j = ivec4(aJoints + 0.5);
    float wSum = max(aWeights.x + aWeights.y + aWeights.z + aWeights.w, 0.0001);
    vec4 w = aWeights / wSum;
    mat4 skin = w.x * uBones[clamp(j.x, 0, 71)]
              + w.y * uBones[clamp(j.y, 0, 71)]
              + w.z * uBones[clamp(j.z, 0, 71)]
              + w.w * uBones[clamp(j.w, 0, 71)];
    gl_Position = uLightMVP * (skin * vec4(aPos, 1.0));
}
"""

    const val SHADOW_FRAGMENT = """#version 300 es
precision mediump float;
void main() {
}
"""

    const val PBR_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTangent;
layout(location = 3) in vec2 aUv;

uniform mat4 uModel;
uniform mat4 uMVP;
uniform mat4 uLightSpaceMatrix;

out vec3 vWorldPos;
out vec3 vNormal;
out vec3 vTangent;
out vec2 vUv;
out vec4 vLightSpacePos;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;
    mat3 normalMat = transpose(inverse(mat3(uModel)));
    vec3 rawN = normalMat * aNormal;
    vNormal = dot(rawN, rawN) > 1e-8 ? normalize(rawN) : vec3(0.0, 1.0, 0.0);
    vec3 rawT = normalMat * aTangent;
    vTangent = dot(rawT, rawT) > 1e-8 ? normalize(rawT) : vec3(1.0, 0.0, 0.0);
    vUv = aUv;
    vLightSpacePos = uLightSpaceMatrix * worldPos;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

    const val SKINNED_PBR_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTangent;
layout(location = 3) in vec2 aUv;
layout(location = 4) in vec4 aJoints;
layout(location = 5) in vec4 aWeights;

layout(std140) uniform BoneBlock {
    mat4 uBones[72];
};

uniform mat4 uModel;
uniform mat4 uMVP;
uniform mat4 uLightSpaceMatrix;

out vec3 vWorldPos;
out vec3 vNormal;
out vec3 vTangent;
out vec2 vUv;
out vec4 vLightSpacePos;

void main() {
    ivec4 j = ivec4(aJoints + 0.5);
    float wSum = max(aWeights.x + aWeights.y + aWeights.z + aWeights.w, 0.0001);
    vec4 w = aWeights / wSum;
    mat4 skin = w.x * uBones[clamp(j.x, 0, 71)]
              + w.y * uBones[clamp(j.y, 0, 71)]
              + w.z * uBones[clamp(j.z, 0, 71)]
              + w.w * uBones[clamp(j.w, 0, 71)];
    vec4 skinnedPos = skin * vec4(aPos, 1.0);
    mat3 skinRot = mat3(skin);
    vec3 rawSN = skinRot * aNormal;
    vec3 skinnedNormal = dot(rawSN, rawSN) > 1e-8 ? normalize(rawSN) : vec3(0.0, 1.0, 0.0);
    vec3 rawST = skinRot * aTangent;
    vec3 skinnedTangent = dot(rawST, rawST) > 1e-8 ? normalize(rawST) : vec3(1.0, 0.0, 0.0);

    vec4 worldPos = uModel * skinnedPos;
    vWorldPos = worldPos.xyz;
    mat3 normalMat = transpose(inverse(mat3(uModel)));
    vec3 rawWN = normalMat * skinnedNormal;
    vNormal = dot(rawWN, rawWN) > 1e-8 ? normalize(rawWN) : vec3(0.0, 1.0, 0.0);
    vec3 rawWT = normalMat * skinnedTangent;
    vTangent = dot(rawWT, rawWT) > 1e-8 ? normalize(rawWT) : vec3(1.0, 0.0, 0.0);
    vUv = aUv;
    vLightSpacePos = uLightSpaceMatrix * worldPos;
    gl_Position = uMVP * skinnedPos;
}
"""

    const val PBR_FRAGMENT = """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec3 vTangent;
in vec2 vUv;
in vec4 vLightSpacePos;

uniform sampler2D uAlbedoMap;
uniform sampler2D uNormalMap;
uniform sampler2D uMraMap;
uniform sampler2D uShadowMap;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyColor;
uniform vec3 uGroundAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;

uniform vec3 uColorTint;
uniform float uAlpha;
uniform float uUseOverrideTint;
uniform float uMetallicOverride;
uniform float uRoughnessOverride;
uniform float uUseMaterialOverrides;
uniform float uCrackLevel;
uniform vec3 uEmissiveColor;
uniform float uEmissiveIntensity;
uniform float uHighlight;
uniform float uFrozen;
uniform float uShadowsEnabled;
uniform vec3 uPointLightPos;
uniform vec3 uPointLightColor;
uniform vec3 uFillLightPos;
uniform vec3 uFillLightColor;

uniform vec3 uFlashlightPos;
uniform vec3 uFlashlightDir;
uniform float uFlashlightActive;
uniform float uAoStrength;
uniform float uReflectionStrength;
uniform float uIsSkin;
uniform float uIsHair;
uniform vec3 uSssColor;

out vec4 fragColor;

const float PI = 3.14159265;

vec3 ACESFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    return a2 / max(PI * denom * denom, 0.0001);
}

float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / max(NdotV * (1.0 - k) + k, 0.0001);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    return GeometrySchlickGGX(max(dot(N, V), 0.0), roughness) *
           GeometrySchlickGGX(max(dot(N, L), 0.0), roughness);
}

vec3 FresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

float ComputeShadow(vec4 lightSpacePos, vec3 N, vec3 L) {
    if (uShadowsEnabled < 0.5) return 0.0;
    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0) {
        return 0.0;
    }
    float bias = max(0.0035 * (1.0 - dot(N, L)), 0.0010);
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(uShadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (projCoords.z - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

vec3 EvalLight(vec3 L, vec3 radiance, vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, vec3 F0) {
    vec3 H = normalize(V + L);
    float NDF = DistributionGGX(N, H, roughness);
    float G = GeometrySmith(N, V, L, roughness);
    vec3 F = FresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 kS = F;
    vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);
    float NdotL = max(dot(N, L), 0.0);
    vec3 specular = (NDF * G * F) / max(4.0 * max(dot(N, V), 0.0) * NdotL, 0.001);
    return (kD * albedo / PI + specular) * radiance * NdotL;
}

void main() {
    vec4 texAlbedo = texture(uAlbedoMap, vUv);
    vec3 baseAlbedo = mix(texAlbedo.rgb, uColorTint * (0.76 + 0.40 * texAlbedo.rgb), uUseOverrideTint);
    vec3 mra = texture(uMraMap, vUv).rgb;
    float metallic = mix(mra.r, clamp(uMetallicOverride, 0.0, 1.0), uUseMaterialOverrides);
    float roughness = clamp(mix(mra.g, uRoughnessOverride * (0.85 + 0.30 * mra.g), uUseMaterialOverrides), 0.05, 0.98);
    float ao = mix(1.0, mra.b, clamp(uAoStrength, 0.0, 1.4));

    // Procedural fracture cracks when damaged
    float crackMask = 0.0;
    if (uCrackLevel > 0.02) {
        vec2 p = vUv * 12.0;
        float lineA = abs(fract(p.x + sin(p.y * 2.3) * 0.45) - 0.5);
        float lineB = abs(fract(p.y * 1.15 - cos(p.x * 1.9) * 0.40) - 0.5);
        float web = min(lineA, lineB);
        crackMask = smoothstep(0.06 * uCrackLevel + 0.005, 0.0, web) * uCrackLevel;
        baseAlbedo = mix(baseAlbedo, vec3(0.86, 0.91, 0.96), crackMask * 0.72);
        roughness = mix(roughness, 0.85, crackMask);
    }

    vec3 N_geom = dot(vNormal, vNormal) > 1e-8 ? normalize(vNormal) : vec3(0.0, 1.0, 0.0);
    vec3 tangentOrtho = vTangent - N_geom * dot(N_geom, vTangent);
    if (dot(tangentOrtho, tangentOrtho) < 1e-6) {
        vec3 upRef = abs(N_geom.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        tangentOrtho = cross(upRef, N_geom);
    }
    vec3 T = normalize(tangentOrtho);
    vec3 B = cross(N_geom, T);
    mat3 TBN = mat3(T, B, N_geom);
    vec3 mapN = texture(uNormalMap, vUv).xyz * 2.0 - 1.0;
    vec3 blendedN = TBN * mix(vec3(0.0, 0.0, 1.0), mapN, 0.68);
    vec3 N = dot(blendedN, blendedN) > 1e-8 ? normalize(blendedN) : N_geom;

    vec3 toCam = uCameraPos - vWorldPos;
    vec3 V = dot(toCam, toCam) > 1e-8 ? normalize(toCam) : vec3(0.0, 0.0, 1.0);
    vec3 F0 = mix(vec3(0.04), baseAlbedo, metallic);

    // Calibrated Primary Key Light (prevents washed-out interior blowouts)
    vec3 Lsun = dot(uSunDir, uSunDir) > 1e-8 ? normalize(uSunDir) : vec3(0.3, 0.8, 0.4);
    float shadow = ComputeShadow(vLightSpacePos, N_geom, Lsun);
    vec3 Lo = EvalLight(Lsun, uSunColor * 1.50 * (1.0 - shadow * 0.74), N, V, baseAlbedo, metallic, roughness, F0);

    // Subsurface wrap glow on organic skin
    if (uIsSkin > 0.5) {
        float wrap = clamp((dot(N, Lsun) + 0.42) / 1.42, 0.0, 1.0);
        Lo += uSssColor * (wrap * 0.22 * (1.0 - shadow * 0.5));
    }
    // Anisotropic highlight sheen on sculpted hair strands
    if (uIsHair > 0.5) {
        vec3 Hhair = normalize(V + Lsun);
        float sheen = pow(max(0.0, 1.0 - abs(dot(T, Hhair))), 10.0) * max(dot(N, Lsun), 0.15);
        Lo += baseAlbedo * sheen * 0.24;
    }

    // Overhead Interior / High-Bay Key Point Light
    vec3 toPt = uPointLightPos - vWorldPos;
    float distPt = length(toPt);
    vec3 Lpt = toPt / max(distPt, 0.001);
    float atten = 1.0 / (1.0 + 0.036 * distPt + 0.009 * distPt * distPt);
    Lo += EvalLight(Lpt, uPointLightColor * 9.5 * atten, N, V, baseAlbedo, metallic, roughness, F0);

    // Architectural Soft Fill Light for Shadowed Bay Depth
    vec3 toFill = uFillLightPos - vWorldPos;
    float distFill = length(toFill);
    vec3 Lfill = toFill / max(distFill, 0.001);
    float attFill = 1.0 / (1.0 + 0.030 * distFill + 0.006 * distFill * distFill);
    Lo += EvalLight(Lfill, uFillLightColor * 5.8 * attFill, N, V, baseAlbedo, metallic, roughness, F0);

    // Flashlight / Worklight dynamic spot beam
    if (uFlashlightActive > 0.5) {
        vec3 toFlash = uFlashlightPos - vWorldPos;
        float dFlash = length(toFlash);
        vec3 Lf = toFlash / max(dFlash, 0.001);
        float spotCos = dot(-Lf, normalize(uFlashlightDir));
        float spot = smoothstep(0.82, 0.95, spotCos);
        float attFlash = 1.0 / (1.0 + 0.05 * dFlash + 0.018 * dFlash * dFlash);
        Lo += EvalLight(Lf, vec3(1.0, 0.96, 0.88) * 20.0 * spot * attFlash, N, V, baseAlbedo, metallic, roughness, F0);
    }

    // Hemisphere IBL + Reflection approximation
    float hemi = N.y * 0.5 + 0.5;
    vec3 envColor = mix(uGroundAmbient, uSkyColor, hemi);
    vec3 R = reflect(-V, N);
    float reflHemi = R.y * 0.5 + 0.5;
    vec3 specEnv = mix(uGroundAmbient, uSkyColor, reflHemi) * (1.0 - roughness * 0.78);
    vec3 F_ibl = FresnelSchlick(max(dot(N, V), 0.0), F0);
    vec3 ambient = (baseAlbedo * (1.0 - metallic * 0.65) * envColor * 0.48 + specEnv * F_ibl * 0.58 * clamp(uReflectionStrength, 0.0, 1.4)) * ao;

    vec3 color = ambient + Lo + uEmissiveColor * uEmissiveIntensity;

    // Subtle frozen / target rim indicators
    float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0);
    if (uFrozen > 0.5) {
        color = mix(color, vec3(0.38, 0.68, 0.88), 0.18 + rim * 0.28);
    }
    if (uHighlight > 0.5) {
        color += vec3(0.86, 0.56, 0.22) * (0.10 + rim * 0.36);
    }

    // Distance exponential fog
    float camDist = length(uCameraPos - vWorldPos);
    float fogFactor = clamp(1.0 - exp(-camDist * uFogDensity), 0.0, 0.76);
    color = mix(color, uFogColor, fogFactor);

    // ACES Filmic tone-mapping + gamma (guarded against negative/NaN radiance)
    color = max(color, vec3(0.0));
    color = ACESFilmic(color);
    color = pow(clamp(color, 0.0, 1.0), vec3(1.0 / 2.2));

    fragColor = vec4(color, clamp(uAlpha + crackMask * 0.35, 0.0, 1.0));
}
"""

    const val FALLBACK_LIT_FRAGMENT = """#version 300 es
precision mediump float;
in vec3 vNormal;
in vec2 vUv;
uniform sampler2D uAlbedoMap;
uniform vec3 uColorTint;
uniform float uUseOverrideTint;
uniform float uAlpha;
out vec4 fragColor;
void main() {
    vec4 tex = texture(uAlbedoMap, vUv);
    vec3 col = mix(tex.rgb, uColorTint * (0.76 + 0.40 * tex.rgb), uUseOverrideTint);
    vec3 n = normalize(vNormal + vec3(0.0, 0.001, 0.0));
    float l = clamp(dot(n, normalize(vec3(0.4, 0.85, 0.3))) * 0.6 + 0.45, 0.25, 1.0);
    fragColor = vec4(col * l, clamp(uAlpha, 0.0, 1.0));
}
"""

    const val LINE_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
uniform mat4 uVP;
void main() {
    gl_Position = uVP * vec4(aPos, 1.0);
}
"""

    const val LINE_FRAGMENT = """#version 300 es
precision mediump float;
uniform vec4 uColor;
out vec4 fragColor;
void main() {
    fragColor = uColor;
}
"""

    const val SKY_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec2 aQuad;
out vec2 vNdc;
void main() {
    vNdc = aQuad;
    gl_Position = vec4(aQuad, 0.9999, 1.0);
}
"""

    const val SKY_FRAGMENT = """#version 300 es
precision mediump float;
in vec2 vNdc;
uniform vec3 uSkyTop;
uniform vec3 uSkyHorizon;
uniform float uPitch;
out vec4 fragColor;
void main() {
    float t = clamp(vNdc.y * 0.5 + 0.5 + uPitch * 0.45, 0.0, 1.0);
    vec3 col = mix(uSkyHorizon, uSkyTop, pow(t, 0.75));
    fragColor = vec4(col, 1.0);
}
"""

    const val PARTICLE_VERTEX = """#version 300 es
precision highp float;
layout(location = 0) in vec4 aPosSize;
layout(location = 1) in vec4 aColor;
uniform mat4 uVP;
uniform float uPointScale;
out vec4 vColor;
void main() {
    vec4 clip = uVP * vec4(aPosSize.xyz, 1.0);
    gl_Position = clip;
    float dist = max(clip.w, 0.2);
    gl_PointSize = clamp(aPosSize.w * uPointScale / dist, 2.0, 44.0);
    vColor = aColor;
}
"""

    const val PARTICLE_FRAGMENT = """#version 300 es
precision mediump float;
in vec4 vColor;
out vec4 fragColor;
void main() {
    vec2 d = gl_PointCoord - vec2(0.5);
    float r2 = dot(d, d);
    if (r2 > 0.25) discard;
    float soft = smoothstep(0.25, 0.04, r2);
    fragColor = vec4(vColor.rgb, vColor.a * soft);
}
"""
}
