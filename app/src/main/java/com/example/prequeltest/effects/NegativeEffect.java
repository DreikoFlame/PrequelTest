package com.example.prequeltest.effects;

public class NegativeEffect extends FilterEffect {

    public NegativeEffect() {
        mName = FILTER_NEGATIVE_STR;
        mCode = FILTER_NEGATIVE;
    }

    @Override
    public String getShader() {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "void main() {\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  float colorR = (1.0 - color.r) / 1.0;\n"
                + "  float colorG = (1.0 - color.g) / 1.0;\n"
                + "  float colorB = (1.0 - color.b) / 1.0;\n"
                + "  gl_FragColor = vec4(colorR, colorG, colorB, color.a);\n"
                + "}\n";
    }
}
