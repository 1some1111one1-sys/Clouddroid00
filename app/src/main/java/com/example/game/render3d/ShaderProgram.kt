package com.example.game.render3d

import android.opengl.GLES30

class ShaderProgram {
    val programId: Int

    val uMvpMatrixLoc: Int
    val uModelMatrixLoc: Int
    val uColorLoc: Int
    val uUseTextureLoc: Int
    val uTextureLoc: Int
    val uFogColorLoc: Int
    val uFogNearLoc: Int
    val uFogFarLoc: Int
    val uSunDirLoc: Int
    val uEmissiveLoc: Int
    val uCameraPosLoc: Int
    val uSpecularLoc: Int
    val uShininessLoc: Int
    val uUvScaleLoc: Int
    val uUvOffsetLoc: Int
    val uIsSkyLoc: Int
    val uSkyTopColorLoc: Int
    val uSkyHorizonColorLoc: Int
    val uSunColorLoc: Int
    val uAmbientFactorLoc: Int

    init {
        val vertSrc = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aTexCoord;

            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            uniform vec2 uUvScale;
            uniform vec2 uUvOffset;

            out vec3 vNormal;
            out vec3 vWorldPos;
            out vec2 vTexCoord;
            out float vViewDist;
            out float vLocalY;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec4 clipPos = uMvpMatrix * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vWorldPos = worldPos.xyz;
                vNormal = mat3(uModelMatrix) * aNormal;
                vTexCoord = aTexCoord * uUvScale + uUvOffset;
                vViewDist = clipPos.w;
                vLocalY = aPosition.y;
            }
        """.trimIndent()

        val fragSrc = """
            #version 300 es
            precision mediump float;

            in vec3 vNormal;
            in vec3 vWorldPos;
            in vec2 vTexCoord;
            in float vViewDist;
            in float vLocalY;

            uniform vec4 uColor;
            uniform int uUseTexture;
            uniform sampler2D uTexture;
            uniform vec3 uFogColor;
            uniform float uFogNear;
            uniform float uFogFar;
            uniform vec3 uSunDir;
            uniform float uEmissive;
            uniform vec3 uCameraPos;
            uniform float uSpecular;
            uniform float uShininess;
            uniform int uIsSky;
            uniform vec3 uSkyTopColor;
            uniform vec3 uSkyHorizonColor;
            uniform vec3 uSunColor;
            uniform float uAmbientFactor;

            out vec4 fragColor;

            void main() {
                vec3 sunDir = normalize(uSunDir);

                if (uIsSky == 1) {
                    vec3 viewRay = normalize(vWorldPos - uCameraPos);
                    float h = clamp(viewRay.y, 0.0, 1.0);
                    vec3 midSky = mix(uSkyHorizonColor, uSkyTopColor, 0.55);
                    vec3 sky = mix(uSkyHorizonColor, midSky, smoothstep(0.0, 0.30, h));
                    sky = mix(sky, uSkyTopColor, smoothstep(0.24, 0.92, h));
                    float sunGlow = pow(max(dot(viewRay, sunDir), 0.0), 42.0);
                    float sunDisc = smoothstep(0.9960, 0.9991, dot(viewRay, sunDir));
                    sky += uSunColor * (sunGlow * 0.52 + sunDisc * 1.45);
                    fragColor = vec4(sky, 1.0);
                    return;
                }

                vec4 baseColor = uColor;
                if (uUseTexture == 1) {
                    vec4 texColor = texture(uTexture, vTexCoord);
                    baseColor = baseColor * texColor;
                }
                if (baseColor.a < 0.02) discard;

                vec3 norm = normalize(vNormal);
                vec3 viewDir = normalize(uCameraPos - vWorldPos);
                vec3 reflDir = reflect(-viewDir, norm);

                // Dynamic Time-of-Day sunlight + sky hemisphere bounce + ground-contact AO
                float diff = max(dot(norm, sunDir), 0.0);
                float hemi = 0.5 + 0.5 * norm.y;
                float contactAo = mix(0.74, 1.0, smoothstep(0.0, 3.2, vWorldPos.y));
                vec3 groundBounce = vec3(0.18, 0.20, 0.25) * uAmbientFactor;
                vec3 skyBounce = mix(uSkyTopColor, uSkyHorizonColor, 0.45) * (0.62 * uAmbientFactor);
                vec3 skyAmbient = mix(groundBounce, skyBounce, hemi) * contactAo;
                vec3 sunLight = uSunColor * (diff * 0.78 * uAmbientFactor);
                vec3 lighting = skyAmbient + sunLight;
                lighting = mix(lighting, vec3(1.12), uEmissive);

                // Blinn-Phong specular highlight + Schlick Fresnel clearcoat sky/horizon reflection
                vec3 halfDir = normalize(sunDir + viewDir);
                float spec = pow(max(dot(norm, halfDir), 0.0), max(uShininess, 4.0)) * uSpecular * uAmbientFactor;
                float fresnel = pow(1.0 - max(dot(norm, viewDir), 0.0), 3.4);
                vec3 envGround = vec3(0.16, 0.17, 0.20) * uAmbientFactor;
                vec3 envColor = mix(envGround, uSkyHorizonColor, smoothstep(-0.18, 0.06, reflDir.y));
                envColor = mix(envColor, uSkyTopColor, smoothstep(0.06, 0.65, reflDir.y));
                float reflStrength = uSpecular * (0.14 + 0.72 * fresnel);

                vec3 litColor = baseColor.rgb * lighting
                    + uSunColor * spec
                    + envColor * reflStrength;

                float fogFactor = clamp((vViewDist - uFogNear) / (uFogFar - uFogNear), 0.0, 1.0);
                fogFactor = fogFactor * fogFactor * (3.0 - 2.0 * fogFactor);
                vec3 finalRgb = mix(litColor, uFogColor, fogFactor);
                fragColor = vec4(finalRgb, baseColor.a);
            }
        """.trimIndent()

        val vertShader = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val fragShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        programId = GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vertShader)
            GLES30.glAttachShader(prog, fragShader)
            GLES30.glLinkProgram(prog)
        }
        GLES30.glDeleteShader(vertShader)
        GLES30.glDeleteShader(fragShader)

        uMvpMatrixLoc = GLES30.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "uModelMatrix")
        uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
        uUseTextureLoc = GLES30.glGetUniformLocation(programId, "uUseTexture")
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
        uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
        uFogNearLoc = GLES30.glGetUniformLocation(programId, "uFogNear")
        uFogFarLoc = GLES30.glGetUniformLocation(programId, "uFogFar")
        uSunDirLoc = GLES30.glGetUniformLocation(programId, "uSunDir")
        uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
        uCameraPosLoc = GLES30.glGetUniformLocation(programId, "uCameraPos")
        uSpecularLoc = GLES30.glGetUniformLocation(programId, "uSpecular")
        uShininessLoc = GLES30.glGetUniformLocation(programId, "uShininess")
        uUvScaleLoc = GLES30.glGetUniformLocation(programId, "uUvScale")
        uUvOffsetLoc = GLES30.glGetUniformLocation(programId, "uUvOffset")
        uIsSkyLoc = GLES30.glGetUniformLocation(programId, "uIsSky")
        uSkyTopColorLoc = GLES30.glGetUniformLocation(programId, "uSkyTopColor")
        uSkyHorizonColorLoc = GLES30.glGetUniformLocation(programId, "uSkyHorizonColor")
        uSunColorLoc = GLES30.glGetUniformLocation(programId, "uSunColor")
        uAmbientFactorLoc = GLES30.glGetUniformLocation(programId, "uAmbientFactor")
    }

    private fun compileShader(type: Int, src: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, src)
            GLES30.glCompileShader(shader)
        }
    }
}
