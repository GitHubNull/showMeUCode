package org.oxff.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.logging.Logging;
import org.oxff.config.ConfigManager;
import org.oxff.config.ExtractionRule;
import org.oxff.config.UrlPattern;
import org.oxff.extractor.ExtractorFactory;
import org.oxff.extractor.InterfaceNameExtractor;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 请求处理器: 负责处理HTTP请求，提取接口名称并添加到注释中
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
     * 处理HTTP请求: 检查请求是否匹配配置的URL模式，然后提取接口名称并添加到注释中
     * @param request 需要处理的HTTP请求
     * @return 处理后的HTTP请求
     */
    public HttpRequestToBeSent processRequest(HttpRequestToBeSent request) {
        try {
            // 获取请求URL
            String url = request.url();
            
            // 检查URL是否匹配配置的模式
            if (!isUrlMatchPattern(url)) {
                return request;
            }
            
            // 获取请求体
            String body = request.bodyToString().trim();
            if (body.isEmpty()) {
                return request;
            }
            
            // 获取Content-Type头
            Optional<HttpHeader> contentTypeHeader = request.headers().stream()
                    .filter(header -> header.name().equalsIgnoreCase("Content-Type"))
                    .findFirst();
            
            String contentType = contentTypeHeader.isPresent() ? contentTypeHeader.get().value() : "";
            
            // 尝试提取接口名称
            Optional<String> interfaceNameOpt = extractInterfaceName(body);
            if (!interfaceNameOpt.isPresent()) {
                return request;
            }
            
            String interfaceName = interfaceNameOpt.get();
            
            // 将接口名称添加到请求备注中
            logger.logToOutput("从请求 [" + url + "] 中提取到接口: " + interfaceName);
            
            // 获取请求的注释对象并修改
            Annotations annotations = request.annotations();
            annotations.setNotes(interfaceName);
            annotations.setHighlightColor(HighlightColor.BLUE);
            
            // 直接返回原始请求，注释已经设置
            return request;
        } catch (Exception e) {
            logger.logToError("处理请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return request;
        }
    }
    
    /**
     * 检查URL是否匹配配置的模式: 遍历所有启用的URL模式，检查URL是否匹配其中一个
     * @param url 需要检查的URL
     * @return 如果URL匹配配置的模式，则返回true，否则返回false
     */
    private boolean isUrlMatchPattern(String url) {
        for (UrlPattern urlPattern : configManager.getUrlPatterns()) {
            if (urlPattern.isEnabled() && Pattern.matches(urlPattern.getPattern(), url)) {
                return true;
            }
        }
        return false;
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