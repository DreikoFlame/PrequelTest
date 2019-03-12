package com.example.prequeltest.effects;

public class NoFilter extends FilterEffect {

    public NoFilter() {
        mName = FILTER_NONE_NAME_STR;
        mCode = FILTER_NONE;
    }

    @Override
    public String getShader() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";
    }
}
