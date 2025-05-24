package org.oxff.extractor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import org.oxff.config.ExtractionRule;
import org.oxff.config.RuleType;

/**
 * 提取器工厂类: 根据规则类型创建适当的提取器
 */
public class ExtractorFactory {
    private MontoyaApi montoyaApi;
    private Logging logger;
    
    /**
     * 默认构造函数: 创建提取器工厂
     */
    public ExtractorFactory() {
    }
    
    /**
     * 带API的构造函数: 创建提取器工厂
     * @param montoyaApi Burp Suite API接口
     */
    public ExtractorFactory(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
    }
    
    /**
     * 根据提取规则创建适当的提取器
     * @param rule 提取规则
     * @return 对应类型的提取器
     */
    public InterfaceNameExtractor createExtractor(ExtractionRule rule) {
        if (rule == null) {
            return null;
        }
        
        switch (rule.getRuleType()) {
            case REGEX:
                return new RegexExtractor(rule.getPattern(), logger);
            case JSON_PATH:
                return new JsonPathExtractor(rule.getPattern(), logger);
            case XPATH:
                return new XPathExtractor(rule.getPattern(), logger);
            case FORM:
                return new FormExtractor(rule.getPattern(), logger);
            default:
                if (logger != null) {
                    logger.logToError("未知的规则类型: " + rule.getRuleType());
                }
                return null;
        }
    }
    
    /**
     * 根据规则类型和模式创建提取器
     * @param ruleType 规则类型
     * @param pattern 提取模式
     * @return 对应类型的提取器
     */
    public InterfaceNameExtractor createExtractor(RuleType ruleType, String pattern) {
        if (ruleType == null || pattern == null) {
            return null;
        }
        
        switch (ruleType) {
            case REGEX:
                return new RegexExtractor(pattern, logger);
            case JSON_PATH:
                return new JsonPathExtractor(pattern, logger);
            case XPATH:
                return new XPathExtractor(pattern, logger);
            case FORM:
                return new FormExtractor(pattern, logger);
            default:
                if (logger != null) {
                    logger.logToError("未知的规则类型: " + ruleType);
                }
                return null;
        }
    }
    
    /**
     * 创建表单数据提取器
     * @param paramName 参数名称
     * @return 表单数据提取器
     */
    public InterfaceNameExtractor createFormExtractor(String paramName) {
        return new FormExtractor(paramName, logger);
    }
} 