package org.oxff.extractor;

import java.util.Optional;

/**
 * 接口名称提取器接口: 定义从请求体中提取接口名称的方法
 */
public interface InterfaceNameExtractor {
    /**
     * 从内容中提取接口名称
     * @param content 需要提取的内容
     * @return 提取的接口名称，如果无法提取则返回空
     */
    Optional<String> extract(String content);
} 