package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 宽容的 LocalDateTime 反序列化器：同时接受下面几种写法，避免填错格式直接报 500。
 *
 * <pre>
 *   2026-01-01 12:00:00      ← 空格分隔，推荐写法
 *   2026-01-01T12:00:00      ← 带 T（ISO-8601）
 *   2026-01-01T12:00:00.000  ← 带毫秒
 *   2026-01-01 12:00         ← 不带秒
 *   2026-01-01               ← 只填日期，按当天 00:00:00 处理
 * </pre>
 *
 * <p>背景：Jackson 默认只认 ISO 格式（带 T）。如果只加
 * {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")}，又会让带 T 的写法报错 ——
 * 单一格式总有另一种写法会挂。这里按顺序尝试多种格式，全部失败才抛异常。</p>
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = text.trim();

        // 只填了日期（yyyy-MM-dd），按当天 00:00:00 处理
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
                // 换下一种格式继续试
            }
        }

        throw new IOException("时间格式无法识别：" + value
                + "，请使用 2026-01-01 12:00:00 或 2026-01-01T12:00:00");
    }
}
