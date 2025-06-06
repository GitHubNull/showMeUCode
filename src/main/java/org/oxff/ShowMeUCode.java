package org.oxff;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.logging.Logging;
import org.oxff.config.ConfigManager;
import org.oxff.http.RequestProcessor;
import org.oxff.ui.ConfigTab;
import org.oxff.ui.ContextMenuHandler;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * ShowMeUCode 插件主类: 实现BurpExtension接口和HttpHandler接口，拦截HTTP请求并提取接口名称
 */
public class ShowMeUCode implements BurpExtension, HttpHandler {
    // 常量定义
    public static final String EXTENSION_NAME = "showMeUCode";
    public static final String EXTENSION_VERSION = "1.1.0";
    public static final String EXTENSION_DESCRIPTION = "从请求体中提取并显示真实接口名称";
    public static final String EXTENSION_AUTHOR = "GitHubNull";
    public static final String EXTENSION_HOMEPAGE = "https://github.com/GitHubNull/showMeUCode";

    // Burp Suite API
    private MontoyaApi montoyaApi;
    // 日志记录器
    private Logging logger;
    // 请求处理器
    private RequestProcessor requestProcessor;
    // 配置管理器
    private ConfigManager configManager;
    // 右键菜单处理器
    private ContextMenuHandler contextMenuHandler;
    // 需要处理的Burp工具类型
    private final Set<ToolType> toolsToProcess = new HashSet<>();
    // 是否启用插件
    private boolean isEnabled = true;

    /**
     * 插件初始化方法: 由Burp Suite调用
     * @param montoyaApi Burp Suite提供的API接口
     */
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        
        // 设置插件名称
        montoyaApi.extension().setName(EXTENSION_NAME);
        
        // 获取日志记录器
        logger = montoyaApi.logging();
        
        // 初始化日志信息
        logger.logToOutput(EXTENSION_NAME + " v" + EXTENSION_VERSION);
        logger.logToOutput(EXTENSION_DESCRIPTION);
        logger.logToOutput("作者: " + EXTENSION_AUTHOR);
        logger.logToOutput("项目主页: " + EXTENSION_HOMEPAGE);
        logger.logToOutput("插件已加载，正在初始化...");
        
        try {
            // 初始化配置管理器
            configManager = new ConfigManager(montoyaApi);
            
            // 初始化请求处理器
            requestProcessor = new RequestProcessor(montoyaApi, configManager);
            
            // 初始化右键菜单处理器
            contextMenuHandler = new ContextMenuHandler(montoyaApi, configManager);
            
            // 初始化需要处理的工具类型
            initToolTypes();
            
            // 注册HTTP请求处理器
            montoyaApi.http().registerHttpHandler(this);
            
            // 注册右键菜单处理器
            montoyaApi.userInterface().registerContextMenuItemsProvider(contextMenuHandler);
            
            // 注册UI组件
            SwingUtilities.invokeLater(() -> {
                ConfigTab configTab = new ConfigTab(montoyaApi, configManager, this);
                montoyaApi.userInterface().registerSuiteTab("ShowMeUCode", configTab);
            });
            
            logger.logToOutput("插件初始化完成，已准备就绪");
            logger.logToOutput("功能介绍:");
            logger.logToOutput("- 自动提取HTTP请求中的接口名称并添加到备注");
            logger.logToOutput("- 在代理历史记录中右键可批量处理历史请求");
            logger.logToOutput("- 支持Target范围过滤和自定义提取规则");
        } catch (Exception e) {
            logger.logToError("插件初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化需要处理的Burp工具类型: 默认处理代理、入侵者、日志器和扩展的请求
     */
    private void initToolTypes() {
        toolsToProcess.add(ToolType.PROXY);
        toolsToProcess.add(ToolType.INTRUDER);
        toolsToProcess.add(ToolType.LOGGER);
        toolsToProcess.add(ToolType.EXTENSIONS);
    }

    /**
     * 处理即将发送的HTTP请求: 检查请求是否需要处理，然后提取接口名称
     * @param httpRequestToBeSent 即将发送的HTTP请求
     * @return 处理后的HTTP请求操作
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        try {
            // 检查插件是否启用
            if (!isEnabled) {
                return RequestToBeSentAction.continueWith(httpRequestToBeSent);
            }
            
            // 检查当前工具类型是否需要处理
            if (!toolsToProcess.contains(httpRequestToBeSent.toolSource().toolType())) {
                return RequestToBeSentAction.continueWith(httpRequestToBeSent);
            }
            
            // 处理请求，提取接口名称并添加到注释中
            HttpRequestToBeSent processedRequest = requestProcessor.processRequest(httpRequestToBeSent);
            return RequestToBeSentAction.continueWith(processedRequest);
        } catch (Exception e) {
            logger.logToError("处理HTTP请求时发生错误: " + e.getMessage());
            return RequestToBeSentAction.continueWith(httpRequestToBeSent);
        }
    }

    /**
     * 处理收到的HTTP响应: 本插件不处理响应，直接返回原始响应
     * @param httpResponseReceived 收到的HTTP响应
     * @return 处理后的HTTP响应操作
     */
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        // 本插件不处理响应，直接返回原始响应
        return ResponseReceivedAction.continueWith(httpResponseReceived);
    }

    /**
     * 设置插件启用状态
     * @param enabled 是否启用插件
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        logger.logToOutput("插件" + (enabled ? "已启用" : "已禁用"));
    }

    /**
     * 获取插件启用状态
     * @return 插件是否启用
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * 设置需要处理的工具类型
     * @param toolType 工具类型
     * @param selected 是否选中
     */
    public void setToolTypeSelected(ToolType toolType, boolean selected) {
        if (selected) {
            toolsToProcess.add(toolType);
        } else {
            toolsToProcess.remove(toolType);
        }
        logger.logToOutput("更新工具类型设置: " + toolType + " " + (selected ? "已启用" : "已禁用"));
    }

    /**
     * 检查工具类型是否被选中
     * @param toolType 工具类型
     * @return 是否选中
     */
    public boolean isToolTypeSelected(ToolType toolType) {
        return toolsToProcess.contains(toolType);
    }
}
