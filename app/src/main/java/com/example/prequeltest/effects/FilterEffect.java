package com.example.prequeltest.effects;

public abstract class FilterEffect {

    //not using enum case it dramatically increase apk size
    public static final int FILTER_NONE = 0;
    public static final String FILTER_NONE_NAME_STR = "Normal";

    public static final int FILTER_GRAIN = 1;
    public static final String FILTER_GRAIN_STR = "Grain";

    public static final int FILTER_NEGATIVE = 2;
    public static final String FILTER_NEGATIVE_STR = "Negative";

    public static final int FILTER_SEPIA = 3;
    public static final String FILTER_SEPIA_STR = "Sepia";

    protected int mCode;
    protected String mName;

    public abstract String getShader();

    public String getFilterName() {
        return mName;
    }

    public int getFilterCode() {
        return mCode;
    }
}
