package org.oxff.config;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.logging.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 配置管理类: 负责插件配置的存储和加载
 */
public class ConfigManager {
    // 配置常量定义
    private static final String CONFIG_ENABLED = "config.enabled";
    private static final String CONFIG_URL_PATTERNS = "config.url_patterns";
    private static final String CONFIG_EXTRACTION_RULES = "config.extraction_rules";
    private static final String CONFIG_DEFAULT_RULE_TYPE = "config.default_rule_type";
    private static final String CONFIG_COUNT_URL_PATTERNS = "config.count.url_patterns";
    private static final String CONFIG_COUNT_EXTRACTION_RULES = "config.count.extraction_rules";
    
    // Burp Suite API
    private final MontoyaApi montoyaApi;
    // 日志记录器
    private final Logging logger;
    // 持久化对象，用于保存配置
    private final PersistedObject persistedObject;
    
    // 配置项
    private boolean enabled = true;
    private List<UrlPattern> urlPatterns = new ArrayList<>();
    private List<ExtractionRule> extractionRules = new ArrayList<>();
    private RuleType defaultRuleType = RuleType.REGEX;
    
    /**
     * 构造函数: 初始化配置管理器并加载保存的配置
     * @param montoyaApi Burp Suite API接口
     */
    public ConfigManager(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
        this.persistedObject = montoyaApi.persistence().extensionData();
        
        // 加载保存的配置
        loadConfig();
        
        // 如果没有配置规则，添加默认规则
        if (urlPatterns.isEmpty()) {
            addDefaultUrlPatterns();
        }
        if (extractionRules.isEmpty()) {
            addDefaultExtractionRules();
        }
    }
    
    /**
     * 加载保存的配置: 从Burp Suite的持久化存储中加载配置
     */
    private void loadConfig() {
        try {
            // 加载插件启用状态
            if (persistedObject.getBoolean(CONFIG_ENABLED) != null) {
                enabled = persistedObject.getBoolean(CONFIG_ENABLED);
            }
            
            // 加载URL匹配规则
            Integer count = persistedObject.getInteger(CONFIG_COUNT_URL_PATTERNS);
            if (count != null && count > 0) {
                urlPatterns = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String pattern = persistedObject.getString(CONFIG_URL_PATTERNS + "." + i);
                    if (pattern != null) {
                        String[] parts = pattern.split("\\|", 2);
                        if (parts.length == 2) {
                            try {
                                Pattern.compile(parts[0]);  // 检查正则表达式是否有效
                                urlPatterns.add(new UrlPattern(parts[0], Boolean.parseBoolean(parts[1])));
                            } catch (PatternSyntaxException e) {
                                logger.logToError("无效的URL匹配规则: " + parts[0]);
                            }
                        }
                    }
                }
            }
            
            // 加载提取规则
            count = persistedObject.getInteger(CONFIG_COUNT_EXTRACTION_RULES);
            if (count != null && count > 0) {
                extractionRules = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String rule = persistedObject.getString(CONFIG_EXTRACTION_RULES + "." + i);
                    if (rule != null) {
                        String[] parts = rule.split("\\|", 3);
                        if (parts.length == 3) {
                            try {
                                RuleType ruleType = RuleType.valueOf(parts[0]);
                                extractionRules.add(new ExtractionRule(ruleType, parts[1], Boolean.parseBoolean(parts[2])));
                            } catch (IllegalArgumentException e) {
                                logger.logToError("无效的规则类型: " + parts[0]);
                            }
                        }
                    }
                }
            }
            
            // 加载默认规则类型
            if (persistedObject.getString(CONFIG_DEFAULT_RULE_TYPE) != null) {
                try {
                    defaultRuleType = RuleType.valueOf(persistedObject.getString(CONFIG_DEFAULT_RULE_TYPE));
                } catch (IllegalArgumentException e) {
                    logger.logToError("无效的默认规则类型: " + persistedObject.getString(CONFIG_DEFAULT_RULE_TYPE));
                }
            }
            
            logger.logToOutput("配置加载完成");
        } catch (Exception e) {
            logger.logToError("加载配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存配置: 将当前配置保存到Burp Suite的持久化存储中
     */
    public void saveConfig() {
        try {
            // 保存插件启用状态
            persistedObject.setBoolean(CONFIG_ENABLED, enabled);
            
            // 保存URL匹配规则
            persistedObject.setInteger(CONFIG_COUNT_URL_PATTERNS, urlPatterns.size());
            for (int i = 0; i < urlPatterns.size(); i++) {
                UrlPattern pattern = urlPatterns.get(i);
                persistedObject.setString(CONFIG_URL_PATTERNS + "." + i, 
                        pattern.getPattern() + "|" + pattern.isEnabled());
            }
            
            // 保存提取规则
            persistedObject.setInteger(CONFIG_COUNT_EXTRACTION_RULES, extractionRules.size());
            for (int i = 0; i < extractionRules.size(); i++) {
                ExtractionRule rule = extractionRules.get(i);
                persistedObject.setString(CONFIG_EXTRACTION_RULES + "." + i, 
                        rule.getRuleType().name() + "|" + rule.getPattern() + "|" + rule.isEnabled());
            }
            
            // 保存默认规则类型
            persistedObject.setString(CONFIG_DEFAULT_RULE_TYPE, defaultRuleType.name());
            
            logger.logToOutput("配置已保存");
        } catch (Exception e) {
            logger.logToError("保存配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加默认URL匹配规则: 在没有配置规则的情况下添加默认规则
     */
    private void addDefaultUrlPatterns() {
        urlPatterns.add(new UrlPattern(".*api.*", true));
        urlPatterns.add(new UrlPattern(".*gateway.*", true));
        urlPatterns.add(new UrlPattern(".*service.*", true));
        logger.logToOutput("已添加默认URL匹配规则");
    }
    
    /**
     * 添加默认提取规则: 在没有配置规则的情况下添加默认规则
     */
    private void addDefaultExtractionRules() {
        extractionRules.add(new ExtractionRule(RuleType.REGEX, "\"method\"\\s*:\\s*\"([^\"]+)\"", true));
        extractionRules.add(new ExtractionRule(RuleType.REGEX, "\"action\"\\s*:\\s*\"([^\"]+)\"", true));
        extractionRules.add(new ExtractionRule(RuleType.JSON_PATH, "$.method", true));
        extractionRules.add(new ExtractionRule(RuleType.JSON_PATH, "$.action", true));
        logger.logToOutput("已添加默认提取规则");
    }
    
    // Getter和Setter方法
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }
    
    public List<UrlPattern> getUrlPatterns() {
        return urlPatterns;
    }
    
    public void setUrlPatterns(List<UrlPattern> urlPatterns) {
        this.urlPatterns = urlPatterns;
        saveConfig();
    }
    
    public void addUrlPattern(UrlPattern urlPattern) {
        urlPatterns.add(urlPattern);
        saveConfig();
    }
    
    public void removeUrlPattern(UrlPattern urlPattern) {
        urlPatterns.remove(urlPattern);
        saveConfig();
    }
    
    public List<ExtractionRule> getExtractionRules() {
        return extractionRules;
    }
    
    public void setExtractionRules(List<ExtractionRule> extractionRules) {
        this.extractionRules = extractionRules;
        saveConfig();
    }
    
    public void addExtractionRule(ExtractionRule extractionRule) {
        extractionRules.add(extractionRule);
        saveConfig();
    }
    
    public void removeExtractionRule(ExtractionRule extractionRule) {
        extractionRules.remove(extractionRule);
        saveConfig();
    }
    
    public RuleType getDefaultRuleType() {
        return defaultRuleType;
    }
    
    public void setDefaultRuleType(RuleType defaultRuleType) {
        this.defaultRuleType = defaultRuleType;
        saveConfig();
    }
} 