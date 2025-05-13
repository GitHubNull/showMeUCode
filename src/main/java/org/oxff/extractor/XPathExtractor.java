package org.oxff.extractor;

import burp.api.montoya.logging.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * XPath提取器: 使用XPath从XML格式的请求体中提取接口名称
 */
public class XPathExtractor implements InterfaceNameExtractor {
    private final String xpathExpression;
    private final Logging logger;
    private final Pattern xmlPattern = Pattern.compile("^\\s*<[?\\w].*>\\s*$", Pattern.DOTALL);
    
    /**
     * 构造函数: 创建XPath提取器
     * @param xpathExpression XPath表达式
     * @param logger 日志记录器
     */
    public XPathExtractor(String xpathExpression, Logging logger) {
        this.xpathExpression = xpathExpression;
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
            // 检查内容是否为空
            if (content == null || content.isEmpty()) {
                return Optional.empty();
            }
            
            // 检查内容是否是XML格式
            if (!isXmlContent(content)) {
                return Optional.empty();
            }
            
            // 创建XPath对象
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression expr = xpath.compile(xpathExpression);
            
            // 解析XML内容
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用外部实体处理，防止XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            
            // 执行XPath表达式
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            
            // 处理结果
            if (result instanceof NodeList) {
                NodeList nodes = (NodeList) result;
                if (nodes.getLength() > 0) {
                    Node node = nodes.item(0);
                    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                        return Optional.of(node.getNodeValue());
                    } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                        return Optional.of(node.getTextContent());
                    } else {
                        return Optional.of(node.getNodeValue());
                    }
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logToError("XPath提取失败: " + e.getMessage());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 检查内容是否是XML格式
     * @param content 需要检查的内容
     * @return 如果内容是XML格式，则返回true，否则返回false
     */
    private boolean isXmlContent(String content) {
        // 简单检查内容是否是XML格式（以<开始，以>结束）
        return xmlPattern.matcher(content.trim()).matches();
    }
} 