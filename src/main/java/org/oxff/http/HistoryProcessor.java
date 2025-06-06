package org.oxff.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.scope.Scope;
import org.oxff.config.ConfigManager;
import org.oxff.config.ExtractionRule;
import org.oxff.config.UrlPattern;
import org.oxff.extractor.ExtractorFactory;
import org.oxff.extractor.InterfaceNameExtractor;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 历史记录处理器: 批量处理历史记录中的请求，提取接口名称并添加到备注中
 */
public class HistoryProcessor {
    // Burp Suite API
    private final MontoyaApi montoyaApi;
    // 日志记录器
    private final Logging logger;
    // 配置管理器
    private final ConfigManager configManager;
    // 提取器工厂
    private final ExtractorFactory extractorFactory;
    
    /**
     * 构造函数: 初始化历史记录处理器
     * @param montoyaApi Burp Suite API接口
     * @param configManager 配置管理器
     */
    public HistoryProcessor(MontoyaApi montoyaApi, ConfigManager configManager) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
        this.configManager = configManager;
        this.extractorFactory = new ExtractorFactory();
    }
    
    /**
     * 批量处理历史记录: 遍历代理历史记录，对满足条件的请求提取接口名称并添加到备注中
     * @return 处理的请求数量
     */
    public int processHistory() {
        int processedCount = 0;
        int foundInterfaceCount = 0;
        
        try {
            logger.logToOutput("开始批量处理历史记录...");
            
            // 获取代理历史记录
            List<ProxyHttpRequestResponse> proxyHistory = montoyaApi.proxy().history();
            logger.logToOutput("历史记录总数: " + proxyHistory.size());
            
            // 获取目标范围
            Scope scope = montoyaApi.scope();
            
            for (ProxyHttpRequestResponse historyItem : proxyHistory) {
                try {
                    // 检查是否在目标范围内（如果有定义scope的话）
                    if (!isInScope(historyItem, scope)) {
                        continue;
                    }
                    
                    processedCount++;
                    
                    // 检查URL是否匹配配置的模式
                    String url = historyItem.finalRequest().url();
//                    if (!isUrlMatchPattern(url)) {
//                        continue;
//                    }
                    
                    // 获取请求体
                    String body = historyItem.finalRequest().bodyToString().trim();
                    if (body.isEmpty()) {
                        continue;
                    }
                    
                    // 检查是否已经有备注了，避免重复处理
                    // String existingNotes = historyItem.annotations().notes();
                    // if (existingNotes != null && !existingNotes.trim().isEmpty()) {
                    //     continue;
                    // }
                    
                    // 尝试提取接口名称
                    Optional<String> interfaceNameOpt = extractInterfaceName(body);
                    if (interfaceNameOpt.isPresent()) {
                        String interfaceName = interfaceNameOpt.get();
                        
                        // 设置备注
                        Annotations annotations = historyItem.annotations();
                        annotations.setNotes(interfaceName);
                        
                        foundInterfaceCount++;
                        logger.logToOutput("为历史记录 [" + url + "] 添加接口备注: " + interfaceName);
                    }
                } catch (Exception e) {
                    logger.logToError("处理历史记录项时发生错误: " + e.getMessage());
                }
            }
            
            logger.logToOutput("批量处理完成！处理的请求数: " + processedCount + 
                              ", 找到接口名称的请求数: " + foundInterfaceCount);
        } catch (Exception e) {
            logger.logToError("批量处理历史记录时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return foundInterfaceCount;
    }
    
    /**
     * 检查请求是否在目标范围内: 如果没有定义scope则返回true，否则检查是否在scope中
     * @param historyItem 历史记录项
     * @param scope 目标范围
     * @return 是否在范围内
     */
    private boolean isInScope(ProxyHttpRequestResponse historyItem, Scope scope) {
        try {
            // 如果scope为空或者没有包含任何URL，则处理所有请求
            if (scope == null) {
                return true;
            }
            
            // 检查是否在scope范围内，使用URL字符串而不是HttpRequest对象
            return scope.isInScope(historyItem.request().url());
        } catch (Exception e) {
            // 如果出现异常，默认不在范围内
            logger.logToError("检查scope时发生错误: " + e.getMessage());
            return false;
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