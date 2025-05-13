package org.oxff.extractor;

import burp.api.montoya.logging.Logging;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 表单数据提取器: 从表单数据中提取接口名称
 */
public class FormExtractor implements InterfaceNameExtractor {
    private final String paramName;
    private final Logging logger;
    private final Pattern formPattern = Pattern.compile("([^&=]+)=([^&]*)", Pattern.CASE_INSENSITIVE);
    
    /**
     * 构造函数: 创建表单数据提取器
     * @param paramName 参数名称
     * @param logger 日志记录器
     */
    public FormExtractor(String paramName, Logging logger) {
        this.paramName = paramName;
        this.logger = logger;
    }
    
    /**
     * 从内容中提取接口名称
     * @param content 需要提取的内容
     * @return 提取的接口名称，如果无法提取则返回空
     */
    @Override
    public Optional<String> extract(String content) {
        try {
            // 检查内容是否为空
            if (content == null || content.isEmpty()) {
                return Optional.empty();
            }
            
            // 检查内容是否是表单格式
            if (!isFormContent(content)) {
                return Optional.empty();
            }
            
            // 解析表单数据
            String[] pairs = content.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                    if (key.equals(paramName)) {
                        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                        return Optional.of(value);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            if (logger != null) {
                logger.logToError("表单数据提取失败: " + e.getMessage());
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logToError("表单数据提取异常: " + e.getMessage());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 检查内容是否是表单格式
     * @param content 需要检查的内容
     * @return 如果内容是表单格式，则返回true，否则返回false
     */
    private boolean isFormContent(String content) {
        // 简单检查内容是否是表单格式（包含键值对）
        return formPattern.matcher(content).find();
    }
} 