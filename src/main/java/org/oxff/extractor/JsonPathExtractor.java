package org.oxff.extractor;

import burp.api.montoya.logging.Logging;

import java.util.Optional;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * JSON路径提取器: 使用JSON路径从JSON格式的请求体中提取接口名称
 */
public class JsonPathExtractor implements InterfaceNameExtractor {
    private final String jsonPath;
    private final Logging logger;
    private final Pattern jsonPattern = Pattern.compile("^\\s*\\{.*}\\s*$", Pattern.DOTALL);
    
    /**
     * 构造函数: 创建JSON路径提取器
     * @param jsonPath JSON路径表达式
     * @param logger 日志记录器
     */
    public JsonPathExtractor(String jsonPath, Logging logger) {
        this.jsonPath = jsonPath;
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
            
            // 检查内容是否是JSON格式
            if (!isJsonContent(content)) {
                return Optional.empty();
            }

            // 使用 Jackson 解析 JSON 并配置 JsonPath
            ObjectMapper objectMapper = new ObjectMapper();
            Configuration configuration = Configuration.builder()
                    .jsonProvider(new JacksonJsonProvider(objectMapper))
                    .mappingProvider(new JacksonMappingProvider(objectMapper))
                    .build();

            // 执行 JsonPath 表达式并获取结果
            Object result = JsonPath.using(configuration).parse(content).read(jsonPath);

            // 如果结果不为空，返回结果
            if (result != null) {
                return Optional.of(result.toString());
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logToError("JSON路径提取异常: " + e.getMessage());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 检查内容是否是JSON格式
     * @param content 需要检查的内容
     * @return 如果内容是JSON格式，则返回true，否则返回false
     */
    private boolean isJsonContent(String content) {
        // 简单检查内容是否是JSON格式（以{开始，以}结束）
        return jsonPattern.matcher(content.trim()).matches();
    }
    
} 