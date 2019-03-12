package com.example.prequeltest.effects;

import java.util.Date;
import java.util.Random;

public class GrainEffect extends FilterEffect {

    private float mStrength;
    private int mWidth, mHeight;

    public GrainEffect() {
        this(0.2f, 0, 0);
    }

    public GrainEffect(float inputStrength, int width, int height) {
        mName = FILTER_GRAIN_STR;
        mCode = FILTER_GRAIN;
        setSurfaceSize(width, height);

        if (inputStrength < 0.0f)
            mStrength = 0.0f;
        else if (inputStrength > 1.0f)
            mStrength = 1.0f;
        else
            mStrength = inputStrength;
    }

    public void setSurfaceSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public String getShader() {
        Random random = new Random(new Date().getTime());

        float seed[] = {random.nextFloat(), random.nextFloat()};
        String scaleString = "scale = " + mStrength + ";\n";
        String seedString[] = new String[2];
        seedString[0] = "seed[0] = " + seed[0] + ";\n";
        seedString[1] = "seed[1] = " + seed[1] + ";\n";
        String stepX = "stepX = " + 0.5f / mWidth + ";\n";
        String stepY = "stepY = " + 0.5f / mHeight + ";\n";

        // locString[1] = "loc[1] = loc[1]+" + seedString[1] + ";\n";

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + " vec2 seed;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES tex_sampler_0;\n"
                + "uniform samplerExternalOES tex_sampler_1;\n"
                + "float scale;\n"
                + " float stepX;\n"
                + " float stepY;\n"
                + "float rand(vec2 loc) {\n"
                + "  float theta1 = dot(loc, vec2(0.9898, 0.233));\n"
                + "  float theta2 = dot(loc, vec2(12.0, 78.0));\n"
                + "  float value = cos(theta1) * sin(theta2) + sin(theta1) * cos(theta2);\n"
                +
                // keep value of part1 in range: (2^-14 to 2^14).
                "  float temp = mod(197.0 * value, 1.0) + value;\n"
                + "  float part1 = mod(220.0 * temp, 1.0) + temp;\n"
                + "  float part2 = value * 0.5453;\n"
                + "  float part3 = cos(theta1 + theta2) * 0.43758;\n"
                + "  float sum = (part1 + part2 + part3);\n"
                + "  return fract(sum)*scale;\n"
                + "}\n"
                + "void main() {\n"
                // Parameters that were created above
                + seedString[0]
                + seedString[1]
                + scaleString
                + stepX
                + stepY
                + "  float noise = texture2D(tex_sampler_1, vTextureCoord + vec2(-stepX, -stepY)).r * 0.224;\n"
                + "  noise += texture2D(tex_sampler_1, vTextureCoord + vec2(-stepX, stepY)).r * 0.224;\n"
                + "  noise += texture2D(tex_sampler_1, vTextureCoord + vec2(stepX, -stepY)).r * 0.224;\n"
                + "  noise += texture2D(tex_sampler_1, vTextureCoord + vec2(stepX, stepY)).r * 0.224;\n"
                + "  noise += 0.4448;\n"
                + "  noise *= scale;\n"
                + "  vec4 color = texture2D(tex_sampler_0, vTextureCoord);\n"
                + "  float energy = 0.33333 * color.r + 0.33333 * color.g + 0.33333 * color.b;\n"
                + "  float mask = (1.0 - sqrt(energy));\n"
                + "  float weight = 1.0 - 1.333 * mask * noise;\n"
                + "  gl_FragColor = vec4(color.rgb * weight, color.a);\n"
                + "  gl_FragColor = gl_FragColor+vec4(rand(vTextureCoord + seed), rand(vTextureCoord + seed),rand(vTextureCoord + seed),1);\n"
                + "}\n";

        return shader;
    }
}
