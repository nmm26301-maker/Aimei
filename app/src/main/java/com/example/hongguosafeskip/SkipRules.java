package com.example.hongguosafeskip;

import java.util.Locale;

/** 用于确保只操作明确可跳过的广告按钮。 */
public final class SkipRules {
    private SkipRules() { }

    /** @return 规范化的控件文本。 */
    public static String normalize(String input) {
        if (input == null) return "";
        return input.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    /** @return 文字是否明确表示可以跳过广告。 */
    public static boolean isExplicitAdSkip(String label) {
        String value = normalize(label);
        return value.equals("跳过广告")
                || value.equals("跳过此广告")
                || value.equals("跳过视频广告")
                || value.equals("关闭广告")
                || value.equals("关闭此广告")
                || value.equals("skipad")
                || value.equals("skipadvertisement");
    }

    /** @return 是否仅为通用跳过按钮。 */
    public static boolean isGenericSkip(String label) {
        String value = normalize(label);
        return value.equals("跳过") || value.equals("skip");
    }

    /** @return 是否出现广告相关文字。 */
    public static boolean containsAdContext(String label) {
        String value = normalize(label);
        return value.contains("广告") || value.contains("advertisement")
                || value.equals("adchoices");
    }

    /** @return 是否允许尝试点击。 */
    public static boolean shouldClick(String label, boolean adContext) {
        return isExplicitAdSkip(label) || (adContext && isGenericSkip(label));
    }
}
