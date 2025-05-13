package org.oxff.extractor;

import burp.api.montoya.logging.Logging;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则表达式提取器: 使用正则表达式从请求体中提取接口名称
 */
public class RegexExtractor implements InterfaceNameExtractor {
    private final Pattern pattern;
    private final Logging logger;
    
    /**
     * 构造函数: 创建正则表达式提取器
     * @param patternString 正则表达式字符串
     * @param logger 日志记录器
     * @throws PatternSyntaxException 如果正则表达式语法错误
     */
    public RegexExtractor(String patternString, Logging logger) throws PatternSyntaxException {
        this.pattern = Pattern.compile(patternString);
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
            if (content == null || content.isEmpty()) {
                return Optional.empty();
            }
            
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                // 如果正则表达式包含分组，返回第一个分组；否则返回整个匹配
                if (matcher.groupCount() > 0) {
                    return Optional.of(matcher.group(1));
                } else {
                    return Optional.of(matcher.group());
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logToError("正则表达式提取失败: " + e.getMessage());
            }
        }
        
        return Optional.empty();
    }
} 