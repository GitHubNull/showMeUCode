package org.oxff.extractor;

import burp.api.montoya.logging.Logging;

import java.util.Optional;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
            
            // 使用JavaScript引擎来执行JSON路径表达式
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            
            // 将内容转换为JavaScript对象
            engine.eval("var jsonObj = " + content + ";");
            
            // 构建路径表达式
            String pathExpression = buildPathExpression(jsonPath);
            
            // 执行表达式并获取结果
            Object result = engine.eval(pathExpression);
            
            // 如果结果不为空，返回结果
            if (result != null) {
                return Optional.of(result.toString());
            }
        } catch (ScriptException e) {
            if (logger != null) {
                logger.logToError("JSON路径提取失败: " + e.getMessage());
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
    
    /**
     * 构建路径表达式
     * @param path JSON路径
     * @return JavaScript路径表达式
     */
    private String buildPathExpression(String path) {
        // 如果路径以$开头，表示根节点
        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.startsWith("$")) {
            path = path.substring(1);
        }
        
        // 替换[]表示法为JavaScript访问方式
        path = path.replaceAll("\\['([^']+)'\\]", ".$1");
        path = path.replaceAll("\\[\"([^\"]+)\"\\]", ".$1");
        path = path.replaceAll("\\[(\\d+)\\]", "[$1]");
        
        // 构建最终表达式
        return "jsonObj" + (path.startsWith(".") ? "" : ".") + path;
    }
} 