package org.oxff.config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * URL匹配规则类: 用于匹配需要处理的URL
 */
public class UrlPattern {
    private String pattern;
    private Pattern compiledPattern;
    private boolean enabled;
    
    /**
     * 构造函数: 创建URL匹配规则，默认启用
     * @param pattern 正则表达式模式
     * @throws PatternSyntaxException 如果正则表达式语法错误
     */
    public UrlPattern(String pattern) throws PatternSyntaxException {
        this(pattern, true);
    }
    
    /**
     * 构造函数: 创建URL匹配规则
     * @param pattern 正则表达式模式
     * @param enabled 是否启用规则
     * @throws PatternSyntaxException 如果正则表达式语法错误
     */
    public UrlPattern(String pattern, boolean enabled) throws PatternSyntaxException {
        this.pattern = pattern;
        this.enabled = enabled;
        this.compiledPattern = Pattern.compile(pattern);
    }
    
    /**
     * 检查URL是否匹配此规则
     * @param url 要检查的URL
     * @return 如果URL匹配规则且规则已启用，则返回true，否则返回false
     */
    public boolean matches(String url) {
        if (!enabled) {
            return false;
        }
        return compiledPattern.matcher(url).find();
    }
    
    /**
     * 获取正则表达式模式
     * @return 正则表达式模式
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * 设置正则表达式模式
     * @param pattern 正则表达式模式
     * @throws PatternSyntaxException 如果正则表达式语法错误
     */
    public void setPattern(String pattern) throws PatternSyntaxException {
        this.pattern = pattern;
        this.compiledPattern = Pattern.compile(pattern);
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
        return pattern + (enabled ? " [已启用]" : " [已禁用]");
    }
} 