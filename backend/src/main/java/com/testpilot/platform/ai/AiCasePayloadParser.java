package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testpilot.platform.common.BusinessException;
import java.util.Locale;
import java.util.Set;

/**
 * AI 用例 JSON 的公共解析工具：生成与对话两条链路此前各自复制了一份解析逻辑，统一收敛到这里。
 */
public final class AiCasePayloadParser {

    private static final Set<String> STANDARD_CASE_FIELDS = Set.of(
            "caseno", "case_no", "用例编号", "title", "用例标题", "modulename", "module_name", "module", "模块", "所属模块",
            "precondition", "pre_condition", "前置条件",
            "steps", "步骤", "测试步骤", "testdata", "test_data", "测试数据",
            "expectedresult", "expected_result", "预期", "预期结果",
            "priority", "优先级", "casetype", "case_type", "type", "类型", "测试类型",
            "suitetype", "suite_type", "套件类型", "tags", "标签"
    );

    private AiCasePayloadParser() {
    }

    /** 从可能带 Markdown 代码块/前后缀的模型输出中截出 JSON 主体。 */
    public static String extractJson(String rawJson) {
        if (rawJson == null) {
            return "";
        }
        String value = rawJson.trim();
        int start = -1;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return value;
        }
        char close = value.charAt(start) == '[' ? ']' : '}';
        int end = value.lastIndexOf(close);
        if (end <= start) {
            return value;
        }
        return value.substring(start, end + 1);
    }

    public static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            if (value.isArray()) {
                StringBuilder joined = new StringBuilder();
                for (JsonNode element : value) {
                    if (element.isNull()) {
                        continue;
                    }
                    String part = element.asText("").trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    if (joined.length() > 0) {
                        joined.append('\n');
                    }
                    joined.append(part);
                }
                return joined.toString();
            }
            if (value.isObject()) {
                return value.toString();
            }
            return value.asText("").trim();
        }
        return "";
    }

    public static String requiredText(JsonNode node, String... names) {
        String value = text(node, names);
        if (value.isBlank()) {
            throw new BusinessException(50205, "AI 返回缺少必填字段: " + names[0]);
        }
        return value;
    }

    public static String normalizePriority(String priority) {
        String value = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "P0", "HIGH", "CRITICAL", "BLOCKER", "URGENT", "高", "紧急", "严重" -> "P0";
            case "P1", "MEDIUM", "MAJOR", "中", "重要" -> "P1";
            case "P2", "NORMAL", "MINOR", "低", "一般" -> "P2";
            case "P3", "LOW", "TRIVIAL", "较低", "轻微" -> "P3";
            default -> "P2";
        };
    }

    public static String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FUNCTIONAL", "POSITIVE", "NORMAL", "SUCCESS", "正向" -> "FUNCTIONAL";
            case "EXCEPTION", "NEGATIVE", "FAILURE", "ERROR", "异常", "反向" -> "EXCEPTION";
            case "BOUNDARY", "边界" -> "BOUNDARY";
            default -> "FUNCTIONAL";
        };
    }

    public static String validateCaseNo(String value) {
        String caseNo = value.trim().toUpperCase(Locale.ROOT);
        if (!caseNo.matches("[A-Z][A-Z0-9-]*_[0-9]{3}")) {
            throw new BusinessException(50205,
                    "AI 返回的用例编号不符合规范，应为模块大写拼音缩写_三位序号，例如 DENGLU_001");
        }
        return caseNo;
    }

    public static String casePrefix(String caseNo) {
        if (caseNo != null) {
            int separator = caseNo.lastIndexOf('_');
            if (separator > 0) {
                String prefix = caseNo.substring(0, separator).trim().toUpperCase(Locale.ROOT);
                if (!prefix.isBlank()) {
                    return prefix;
                }
            }
        }
        return "MODULE";
    }

    public static long sequenceOf(String caseNo) {
        if (caseNo == null) {
            return 0;
        }
        int separator = caseNo.lastIndexOf('_');
        if (separator < 0) {
            return 0;
        }
        try {
            return Long.parseLong(caseNo.substring(separator + 1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static String extractExtraFieldsJson(JsonNode node, ObjectMapper objectMapper) {
        if (!node.isObject()) {
            return "{}";
        }
        ObjectNode extras = objectMapper.createObjectNode();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!STANDARD_CASE_FIELDS.contains(key.trim().toLowerCase(Locale.ROOT))) {
                extras.set(key, entry.getValue());
            }
        });
        if (extras.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(extras);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
