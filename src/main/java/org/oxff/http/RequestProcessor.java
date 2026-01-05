package org.oxff.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.logging.Logging;
import org.oxff.config.ConfigManager;
import org.oxff.config.ExtractionRule;
import org.oxff.config.UrlPattern;
import org.oxff.extractor.ExtractorFactory;
import org.oxff.extractor.InterfaceNameExtractor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求处理器: 负责处理HTTP请求，提取接口名称并返回带有备注的Annotations
 */
public class RequestProcessor {
    // Burp Suite API
    private final MontoyaApi montoyaApi;
    // 日志记录器
    private final Logging logger;
    // 配置管理器
    private final ConfigManager configManager;
    // 提取器工厂
    private final ExtractorFactory extractorFactory;
    
    /**
     * 处理结果类: 包含请求和Annotations
     */
    public static class ProcessResult {
        private final HttpRequestToBeSent request;
        private final Annotations annotations;
        private final boolean modified;
        
        public ProcessResult(HttpRequestToBeSent request, Annotations annotations, boolean modified) {
            this.request = request;
            this.annotations = annotations;
            this.modified = modified;
        }
        
        public HttpRequestToBeSent getRequest() {
            return request;
        }
        
        public Annotations getAnnotations() {
            return annotations;
        }
        
        public boolean isModified() {
            return modified;
        }
    }
    
    /**
     * 构造函数: 初始化请求处理器
     * @param montoyaApi Burp Suite API接口
     * @param configManager 配置管理器
     */
    public RequestProcessor(MontoyaApi montoyaApi, ConfigManager configManager) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
        this.configManager = configManager;
        this.extractorFactory = new ExtractorFactory();
    }
    
    /**
     * 处理HTTP请求: 先尝试从URL提取接口名称，失败则从请求体提取
     * @param request 需要处理的HTTP请求
     * @return 处理结果，包含Annotations
     */
    public ProcessResult processRequest(HttpRequestToBeSent request) {
        try {
            // 获取请求URL
            String url = request.url();
            logger.logToOutput("[DEBUG] 处理请求: " + url);
            
            // 第一步：尝试从URL提取接口名称
            Optional<String> interfaceNameOpt = extractFromUrl(url);
            if (interfaceNameOpt.isPresent()) {
                String interfaceName = interfaceNameOpt.get();
                logger.logToOutput("[DEBUG] 从URL提取到接口: " + interfaceName);
                Annotations annotations = request.annotations().withNotes(interfaceName);
                return new ProcessResult(request, annotations, true);
            }
            logger.logToOutput("[DEBUG] URL未匹配到接口名称，继续尝试body匹配");
            
            // 第二步：从URL提取失败，尝试从请求体提取
            String body = request.bodyToString().trim();
            if (body.isEmpty()) {
                logger.logToOutput("[DEBUG] 请求体为空，跳过处理");
                return new ProcessResult(request, request.annotations(), false);
            }
            logger.logToOutput("[DEBUG] 请求体长度: " + body.length());
            
            interfaceNameOpt = extractInterfaceName(body);
            if (interfaceNameOpt.isPresent()) {
                String interfaceName = interfaceNameOpt.get();
                logger.logToOutput("[DEBUG] 从body提取到接口: " + interfaceName);
                Annotations annotations = request.annotations().withNotes(interfaceName);
                return new ProcessResult(request, annotations, true);
            }
            
            logger.logToOutput("[DEBUG] URL和body均未提取到接口名称");
            return new ProcessResult(request, request.annotations(), false);
        } catch (Exception e) {
            logger.logToError("处理请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ProcessResult(request, request.annotations(), false);
        }
    }
    
    /**
     * 从URL提取接口名称: 使用URL规则的正则捕获组提取
     * @param url 请求URL
     * @return 提取到的接口名称，如果没有匹配则返回空
     */
    private Optional<String> extractFromUrl(String url) {
        for (UrlPattern urlPattern : configManager.getUrlPatterns()) {
            if (urlPattern.isEnabled()) {
                try {
                    Pattern pattern = Pattern.compile(urlPattern.getPattern());
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        // 如果有捕获组，返回第一个捕获组的内容
                        if (matcher.groupCount() > 0) {
                            String extracted = matcher.group(1);
                            if (extracted != null && !extracted.isEmpty()) {
                                return Optional.of(extracted);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.logToError("URL规则匹配出错: " + urlPattern.getPattern() + " - " + e.getMessage());
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * 提取接口名称: 遍历所有启用的提取规则，尝试使用每个规则提取接口名称
     * @param body 请求体
     * @return 提取到的接口名称，如果没有匹配的规则或提取失败，则返回空
     */
    private Optional<String> extractInterfaceName(String body) {
        for (ExtractionRule rule : configManager.getExtractionRules()) {
            if (rule.isEnabled()) {
                InterfaceNameExtractor extractor = extractorFactory.createExtractor(rule.getRuleType(), rule.getPattern());
                if (extractor != null) {
                    Optional<String> interfaceName = extractor.extract(body);
                    if (interfaceName.isPresent() && !interfaceName.get().isEmpty()) {
                        return interfaceName;
                    }
                }
            }
        }
        return Optional.empty();
    }
} 