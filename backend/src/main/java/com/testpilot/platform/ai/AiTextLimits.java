package com.testpilot.platform.ai;

/**
 * AI 上下文/落库字段的长度保护。
 *
 * <p>AI 返回内容与用户上传内容都不可信：prompt 过大既费 token 也可能超时；
 * AI 生成的超长字段写入 VARCHAR/TEXT 列会触发整批回滚。统一在这里裁剪。
 */
public final class AiTextLimits {

    public static final int DEFAULT_MAX_CONTEXT_CHARS = 20000;
    public static final int MAX_TITLE_CHARS = 255;
    public static final int MAX_CASE_NO_CHARS = 64;
    public static final int MAX_MODULE_CHARS = 128;
    public static final int MAX_TAGS_CHARS = 500;
    public static final int MAX_TEXT_FIELD_CHARS = 20000;
    public static final int MAX_EXTRA_FIELDS_JSON_CHARS = 64000;

    private AiTextLimits() {
    }

    /**
     * 截断到指定字符数；null / 非正上限原样返回。
     */
    public static String truncate(String value, int maxChars) {
        if (value == null || maxChars <= 0 || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    public static String truncateOrEmpty(String value, int maxChars) {
        String truncated = truncate(value, maxChars);
        return truncated == null ? "" : truncated;
    }
}
