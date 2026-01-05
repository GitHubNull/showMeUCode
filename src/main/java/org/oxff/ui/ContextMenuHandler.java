package org.oxff.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import org.oxff.config.ConfigManager;
import org.oxff.config.ExtractionRule;
import org.oxff.config.UrlPattern;
import org.oxff.extractor.ExtractorFactory;
import org.oxff.extractor.InterfaceNameExtractor;
import org.oxff.http.HistoryProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 右键菜单处理器: 为历史记录页面提供批量处理接口名称的功能
 */
public class ContextMenuHandler implements ContextMenuItemsProvider {
    // Burp Suite API
    private final MontoyaApi montoyaApi;
    // 日志记录器
    private final Logging logger;
    // 配置管理器
    private final ConfigManager configManager;
    // 历史记录处理器
    private final HistoryProcessor historyProcessor;
    // 提取器工厂
    private final ExtractorFactory extractorFactory;

    /**
     * 构造函数: 初始化右键菜单处理器
     * 
     * @param montoyaApi    Burp Suite API接口
     * @param configManager 配置管理器
     */
    public ContextMenuHandler(MontoyaApi montoyaApi, ConfigManager configManager) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
        this.configManager = configManager;
        this.historyProcessor = new HistoryProcessor(montoyaApi, configManager);
        this.extractorFactory = new ExtractorFactory();
    }

    /**
     * 提供上下文菜单项: 根据当前上下文决定显示哪些菜单项
     * 
     * @param event 上下文菜单事件
     * @return 菜单项列表
     */
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        
        // 支持 Proxy 和 Target 工具类型
        if (!event.isFromTool(ToolType.PROXY) && !event.isFromTool(ToolType.TARGET)) {
            return menuItems;
        }

        // 添加批量处理菜单项
        JMenuItem batchProcessItem = new JMenuItem("批量提取接口名称(所有请求)");
        batchProcessItem.setToolTipText("为历史记录中的所有请求批量提取并添加接口名称备注");
        batchProcessItem.addActionListener(new BatchProcessActionListener());
        menuItems.add(batchProcessItem);

        // 获取选中的请求列表
        List<HttpRequestResponse> selectedRequests = event.selectedRequestResponses();
        
        // 如果选中了多个请求，添加处理选中请求的菜单项
        if (selectedRequests != null && !selectedRequests.isEmpty()) {
            // 添加分隔符
            menuItems.add(new JSeparator());
            
            JMenuItem selectedProcessItem = new JMenuItem("提取选中请求接口名称 (" + selectedRequests.size() + "个)");
            selectedProcessItem.setToolTipText("为选中的 " + selectedRequests.size() + " 个请求提取并添加接口名称备注");
            selectedProcessItem.addActionListener(new SelectedRequestsProcessActionListener(selectedRequests));
            menuItems.add(selectedProcessItem);
        }

        // 添加单个请求处理菜单项（如果在消息编辑器中选中了单个请求）
        if (event.messageEditorRequestResponse().isPresent()) {
            menuItems.add(new JSeparator());
            
            JMenuItem singleProcessItem = new JMenuItem("提取当前请求接口名称");
            singleProcessItem.setToolTipText("为当前选中的请求提取并添加接口名称备注");
            singleProcessItem.addActionListener(new SingleProcessActionListener(event));
            menuItems.add(singleProcessItem);
        }

        // 添加分隔符
        menuItems.add(new JSeparator());
        
        // 添加复制接口名称到剪贴板菜单项（所有请求）
        JMenuItem copyAllToClipboardItem = new JMenuItem("复制所有接口名称到剪贴板(去重)");
        copyAllToClipboardItem.setToolTipText("从所有历史记录中提取接口名称，去重后复制到剪贴板");
        copyAllToClipboardItem.addActionListener(new CopyAllToClipboardActionListener());
        menuItems.add(copyAllToClipboardItem);
        
        // 如果选中了请求，添加复制选中请求接口名称到剪贴板菜单项
        if (selectedRequests != null && !selectedRequests.isEmpty()) {
            JMenuItem copySelectedToClipboardItem = new JMenuItem("复制选中请求接口名称到剪贴板(去重) (" + selectedRequests.size() + "个)");
            copySelectedToClipboardItem.setToolTipText("从选中的请求中提取接口名称，去重后复制到剪贴板");
            copySelectedToClipboardItem.addActionListener(new CopySelectedToClipboardActionListener(selectedRequests));
            menuItems.add(copySelectedToClipboardItem);
        }

        return menuItems;
    }

    /**
     * 复制所有接口名称到剪贴板动作监听器
     */
    private class CopyAllToClipboardActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread(() -> {
                try {
                    Set<String> interfaceNames = historyProcessor.extractAllInterfaceNames();
                    
                    if (interfaceNames.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "未找到任何接口名称。\n请检查历史记录和提取规则配置。",
                                    "提示",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                        return;
                    }
                    
                    // 将接口名称转换为字符串，每行一个
                    String result = String.join("\n", interfaceNames);
                    
                    // 复制到剪贴板
                    copyToClipboard(result);
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "已复制 " + interfaceNames.size() + " 个不重复的接口名称到剪贴板！\n可用于检查接口测试覆盖情况。",
                                "复制成功",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                    logger.logToOutput("已复制 " + interfaceNames.size() + " 个接口名称到剪贴板");
                } catch (Exception ex) {
                    logger.logToError("复制接口名称到剪贴板时发生错误: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "复制过程中发生错误：" + ex.getMessage(),
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }
    
    /**
     * 复制选中请求接口名称到剪贴板动作监听器
     */
    private class CopySelectedToClipboardActionListener implements ActionListener {
        private final List<HttpRequestResponse> selectedRequests;
        
        public CopySelectedToClipboardActionListener(List<HttpRequestResponse> selectedRequests) {
            this.selectedRequests = selectedRequests;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread(() -> {
                try {
                    Set<String> interfaceNames = historyProcessor.extractInterfaceNamesFromSelected(selectedRequests);
                    
                    if (interfaceNames.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "未从选中的请求中找到任何接口名称。\n请检查请求内容和提取规则配置。",
                                    "提示",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                        return;
                    }
                    
                    // 将接口名称转换为字符串，每行一个
                    String result = String.join("\n", interfaceNames);
                    
                    // 复制到剪贴板
                    copyToClipboard(result);
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "已复制 " + interfaceNames.size() + " 个不重复的接口名称到剪贴板！\n可用于检查接口测试覆盖情况。",
                                "复制成功",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                    logger.logToOutput("已复制 " + interfaceNames.size() + " 个接口名称到剪贴板");
                } catch (Exception ex) {
                    logger.logToError("复制选中请求接口名称到剪贴板时发生错误: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "复制过程中发生错误：" + ex.getMessage(),
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }
    
    /**
     * 复制文本到系统剪贴板
     * @param text 要复制的文本
     */
    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * 批量处理动作监听器: 处理批量提取接口名称的操作
     */
    private class BatchProcessActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 在后台线程中执行批量处理，避免阻塞UI
            SwingUtilities.invokeLater(() -> {
                // 显示确认对话框
                int option = JOptionPane.showConfirmDialog(
                        null,
                        "即将批量处理历史记录中的所有请求，提取接口名称并添加到备注中。\n" +
                                "处理范围：根据Target模块Scope设置（如果未定义则处理所有请求）\n" +
                                "是否继续？",
                        "批量处理确认",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    // 显示进度对话框
                    // JDialog progressDialog = createProgressDialog();
                    // progressDialog.setVisible(true);

                    // 在后台线程中执行处理
                    new Thread(() -> {
                        try {
                            int processedCount = historyProcessor.processHistory();

                            // 关闭进度对话框
                            SwingUtilities.invokeLater(() -> {
                                // progressDialog.dispose();

                                // 显示处理结果
                                JOptionPane.showMessageDialog(
                                        null,
                                        "批量处理完成！\n成功为 " + processedCount + " 个请求添加了接口名称备注。",
                                        "处理完成",
                                        JOptionPane.INFORMATION_MESSAGE);
                            });
                        } catch (Exception ex) {
                            logger.logToError("批量处理时发生错误: " + ex.getMessage());
                            SwingUtilities.invokeLater(() -> {
                                // progressDialog.dispose();
                                JOptionPane.showMessageDialog(
                                        null,
                                        "处理过程中发生错误：" + ex.getMessage(),
                                        "错误",
                                        JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                }
            });
        }
    }

    /**
     * 选中请求处理动作监听器: 处理选中的多个请求的接口名称提取
     */
    private class SelectedRequestsProcessActionListener implements ActionListener {
        private final List<HttpRequestResponse> selectedRequests;

        public SelectedRequestsProcessActionListener(List<HttpRequestResponse> selectedRequests) {
            this.selectedRequests = selectedRequests;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(() -> {
                // 显示确认对话框
                int option = JOptionPane.showConfirmDialog(
                        null,
                        "即将处理选中的 " + selectedRequests.size() + " 个请求，提取接口名称并添加到备注中。\n" +
                                "标记规则：使用配置中已启用的提取规则\n" +
                                "是否继续？",
                        "处理选中请求确认",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    // 在后台线程中执行处理
                    new Thread(() -> {
                        try {
                            int processedCount = historyProcessor.processSelectedRequests(selectedRequests);

                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "处理完成！\n成功为 " + processedCount + " 个请求添加了接口名称备注。",
                                        "处理完成",
                                        JOptionPane.INFORMATION_MESSAGE);
                            });
                        } catch (Exception ex) {
                            logger.logToError("处理选中请求时发生错误: " + ex.getMessage());
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "处理过程中发生错误：" + ex.getMessage(),
                                        "错误",
                                        JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                }
            });
        }
    }

    /**
     * 单个请求处理动作监听器: 处理单个请求的接口名称提取
     */
    private class SingleProcessActionListener implements ActionListener {
        private final ContextMenuEvent event;

        public SingleProcessActionListener(ContextMenuEvent event) {
            this.event = event;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                MessageEditorHttpRequestResponse messageEditor = event.messageEditorRequestResponse().orElse(null);
                if (messageEditor == null) {
                    return;
                }

                // 获取当前请求
                String url = messageEditor.requestResponse().request().url();
                String body = messageEditor.requestResponse().request().bodyToString().trim();

                if (body.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            null,
                            "当前请求没有请求体，无法提取接口名称。",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // 检查URL是否匹配配置的模式
                if (!isUrlMatchPattern(url)) {
                    JOptionPane.showMessageDialog(
                            null,
                            "当前请求URL不匹配配置的处理规则。\n请检查插件配置中的URL匹配模式。",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // 尝试提取接口名称
                Optional<String> interfaceNameOpt = extractInterfaceName(body);
                if (interfaceNameOpt.isPresent()) {
                    String interfaceName = interfaceNameOpt.get();

                    // 设置备注
                    messageEditor.requestResponse().annotations().setNotes(interfaceName);

                    logger.logToOutput("为请求 [" + url + "] 添加接口备注: " + interfaceName);
                    JOptionPane.showMessageDialog(
                            null,
                            "成功提取接口名称！\n接口名称: " + interfaceName + "\n已添加到请求备注中。",
                            "提取成功",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "未能从当前请求中提取到接口名称。\n请检查请求体格式和插件配置中的提取规则。",
                            "提取失败",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                logger.logToError("处理单个请求时发生错误: " + ex.getMessage());
                JOptionPane.showMessageDialog(
                        null,
                        "处理过程中发生错误：" + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 检查URL是否匹配配置的模式: 复用HistoryProcessor中的逻辑
     * 
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
     * 提取接口名称: 复用HistoryProcessor中的逻辑
     * 
     * @param body 请求体
     * @return 提取到的接口名称，如果没有匹配的规则或提取失败，则返回空
     */
    private Optional<String> extractInterfaceName(String body) {
        for (ExtractionRule rule : configManager.getExtractionRules()) {
            if (rule.isEnabled()) {
                InterfaceNameExtractor extractor = extractorFactory.createExtractor(rule.getRuleType(),
                        rule.getPattern());
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

    /**
     * 创建进度对话框: 显示批量处理进度
     * 
     * @return 进度对话框
     */
    private JDialog createProgressDialog() {
        JDialog dialog = new JDialog((Frame) null, "批量处理中", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("正在批量处理历史记录，请稍候...");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);

        dialog.add(panel);

        return dialog;
    }
}