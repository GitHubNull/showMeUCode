package org.oxff.config;

/**
 * 规则类型枚举: 定义支持的提取规则类型
 */
public enum RuleType {
    /**
     * 正则表达式规则
     */
    REGEX("正则表达式"),
    
    /**
     * JSON路径规则
     */
    JSON_PATH("JSON路径"),
    
    /**
     * XPath规则
     */
    XPATH("XPath");
    
    private final String displayName;
    
    RuleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 