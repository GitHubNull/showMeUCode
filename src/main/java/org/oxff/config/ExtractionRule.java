package org.oxff.config;

/**
 * 提取规则类: 用于从HTTP请求体中提取接口名称
 */
public class ExtractionRule {
    private RuleType ruleType;
    private String pattern;
    private boolean enabled;
    
    /**
     * 构造函数: 创建提取规则，默认启用
     * @param ruleType 规则类型
     * @param pattern 提取模式
     */
    public ExtractionRule(RuleType ruleType, String pattern) {
        this(ruleType, pattern, true);
    }
    
    /**
     * 构造函数: 创建提取规则
     * @param ruleType 规则类型
     * @param pattern 提取模式
     * @param enabled 是否启用规则
     */
    public ExtractionRule(RuleType ruleType, String pattern, boolean enabled) {
        this.ruleType = ruleType;
        this.pattern = pattern;
        this.enabled = enabled;
    }
    
    /**
     * 获取规则类型
     * @return 规则类型
     */
    public RuleType getRuleType() {
        return ruleType;
    }
    
    /**
     * 设置规则类型
     * @param ruleType 规则类型
     */
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    
    /**
     * 获取提取模式
     * @return 提取模式
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * 设置提取模式
     * @param pattern 提取模式
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    /**
     * 检查规则是否启用
     * @return 如果规则已启用，则返回true，否则返回false
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置规则启用状态
     * @param enabled 是否启用规则
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return ruleType.getDisplayName() + ": " + pattern + (enabled ? " [已启用]" : " [已禁用]");
    }
} 