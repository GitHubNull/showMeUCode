package org.oxff.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.logging.Logging;
import org.oxff.ShowMeUCode;
import org.oxff.config.ConfigManager;
import org.oxff.config.ExtractionRule;
import org.oxff.config.RuleType;
import org.oxff.config.UrlPattern;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 配置面板类: 提供用户界面，用于配置插件参数
 */
public class ConfigTab extends JPanel {
    // Burp Suite API
    private final MontoyaApi montoyaApi;
    // 日志记录器
    private final Logging logger;
    // 配置管理器
    private final ConfigManager configManager;
    // 主插件类
    private final ShowMeUCode showMeUCode;
    
    // UI组件
    private JCheckBox enabledCheckBox;
    private JTable urlPatternsTable;
    private DefaultTableModel urlPatternsModel;
    private JTable extractionRulesTable;
    private DefaultTableModel extractionRulesModel;
    private JComboBox<RuleType> ruleTypeComboBox;
    private JTextField patternTextField;
    private JComboBox<ToolType> toolTypeComboBox;
    private JCheckBox toolTypeCheckBox;
    
    /**
     * 构造函数: 创建配置面板
     * @param montoyaApi Burp Suite API接口
     * @param configManager 配置管理器
     * @param showMeUCode 主插件类
     */
    public ConfigTab(MontoyaApi montoyaApi, ConfigManager configManager, ShowMeUCode showMeUCode) {
        this.montoyaApi = montoyaApi;
        this.logger = montoyaApi.logging();
        this.configManager = configManager;
        this.showMeUCode = showMeUCode;
        
        // 初始化UI
        initUI();
        
        // 加载配置
        loadConfig();
    }
    
    /**
     * 初始化UI组件
     */
    private void initUI() {
        setLayout(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建顶部面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enabledCheckBox = new JCheckBox("启用插件", configManager.isEnabled());
        enabledCheckBox.addActionListener(e -> {
            boolean enabled = enabledCheckBox.isSelected();
            configManager.setEnabled(enabled);
            showMeUCode.setEnabled(enabled);
            updateComponentsState(enabled);
        });
        topPanel.add(enabledCheckBox);
        
        // 添加导入/导出按钮
        topPanel.add(Box.createHorizontalStrut(20));
        JButton exportButton = new JButton("导出配置");
        JButton importButton = new JButton("导入配置");
        
        exportButton.addActionListener(e -> exportConfig());
        importButton.addActionListener(e -> importConfig());
        
        topPanel.add(exportButton);
        topPanel.add(importButton);
        
        // 创建工具类型面板
        JPanel toolTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolTypePanel.setBorder(BorderFactory.createTitledBorder("处理工具类型"));
        
        // 只支持指定的工具类型：Proxy, Intruder, Logger, Extensions
        ToolType[] supportedToolTypes = {ToolType.PROXY, ToolType.INTRUDER, ToolType.LOGGER, ToolType.EXTENSIONS};
        toolTypeComboBox = new JComboBox<>(supportedToolTypes);
        toolTypeCheckBox = new JCheckBox("启用");
        
        toolTypeComboBox.addActionListener(e -> {
            ToolType selectedTool = (ToolType) toolTypeComboBox.getSelectedItem();
            if (selectedTool != null) {
                toolTypeCheckBox.setSelected(showMeUCode.isToolTypeSelected(selectedTool));
            }
        });
        
        toolTypeCheckBox.addActionListener(e -> {
            ToolType selectedTool = (ToolType) toolTypeComboBox.getSelectedItem();
            if (selectedTool != null) {
                showMeUCode.setToolTypeSelected(selectedTool, toolTypeCheckBox.isSelected());
            }
        });
        
        toolTypePanel.add(new JLabel("工具类型:"));
        toolTypePanel.add(toolTypeComboBox);
        toolTypePanel.add(toolTypeCheckBox);
        
        // 初始化工具类型选择状态
        toolTypeComboBox.setSelectedItem(ToolType.PROXY);
        toolTypeCheckBox.setSelected(showMeUCode.isToolTypeSelected(ToolType.PROXY));
        
        // 创建主内容面板
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // URL匹配规则面板
        JPanel urlPatternsPanel = createUrlPatternsPanel();
        tabbedPane.addTab("URL匹配规则", urlPatternsPanel);
        
        // 提取规则面板
        JPanel extractionRulesPanel = createExtractionRulesPanel();
        tabbedPane.addTab("提取规则", extractionRulesPanel);
        
        // 添加组件到主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(toolTypePanel, BorderLayout.CENTER);
        mainPanel.add(tabbedPane, BorderLayout.SOUTH);
        
        // 添加主面板到配置面板
        add(new JScrollPane(mainPanel));
    }
    
    /**
     * 创建URL匹配规则面板
     * @return URL匹配规则面板
     */
    private JPanel createUrlPatternsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("URL匹配规则"));
        
        // 创建表格模型
        String[] columnNames = {"规则", "状态", "操作"};
        urlPatternsModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 1 || column == 2; // 允许编辑规则内容、状态列和操作列
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class; // 状态列使用复选框
                }
                return String.class;
            }
            
            @Override
            public void setValueAt(Object value, int row, int column) {
                super.setValueAt(value, row, column);
                
                // 当规则内容或状态发生变化时，更新配置管理器
                if (row >= 0 && row < configManager.getUrlPatterns().size()) {
                    UrlPattern pattern = configManager.getUrlPatterns().get(row);
                    
                    if (column == 0) { // 规则内容变化
                        String newPattern = (String) value;
                        if (newPattern != null && !newPattern.trim().isEmpty()) {
                            try {
                                // 验证正则表达式
                                Pattern.compile(newPattern);
                                // 更新规则
                                pattern.setPattern(newPattern);
                                logger.logToOutput("已更新URL匹配规则: " + newPattern);
                            } catch (PatternSyntaxException ex) {
                                // 恢复原值
                                super.setValueAt(pattern.getPattern(), row, column);
                                JOptionPane.showMessageDialog(ConfigTab.this,
                                        "无效的正则表达式: " + ex.getMessage(),
                                        "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            // 恢复原值
                            super.setValueAt(pattern.getPattern(), row, column);
                        }
                    } else if (column == 1) { // 状态变化
                        boolean enabled = (Boolean) value;
                        pattern.setEnabled(enabled);
                        logger.logToOutput("已" + (enabled ? "启用" : "禁用") + "URL匹配规则: " + pattern.getPattern());
                    }
                }
            }
        };
        
        // 创建表格
        urlPatternsTable = new JTable(urlPatternsModel);
        urlPatternsTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        urlPatternsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        urlPatternsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        // 设置表格选择模式
        urlPatternsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urlPatternsTable.setRowSelectionAllowed(true);
        urlPatternsTable.setColumnSelectionAllowed(false);
        
        // 设置操作列渲染器和编辑器
        urlPatternsTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer("删除"));
        urlPatternsTable.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(new JCheckBox(), "删除") {
            @Override
            protected void buttonClicked() {
                if (row >= 0 && row < configManager.getUrlPatterns().size()) {
                    // 确认删除
                    int confirm = JOptionPane.showConfirmDialog(
                        ConfigTab.this,
                        "确定要删除这个URL匹配规则吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        // 删除规则
                        UrlPattern pattern = configManager.getUrlPatterns().get(row);
                        configManager.removeUrlPattern(pattern);
                        urlPatternsModel.removeRow(row);
                        logger.logToOutput("已删除URL匹配规则: " + pattern.getPattern());
                    }
                }
            }
        });
        
        // 创建添加规则面板
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField patternField = new JTextField(30);
        JButton addButton = new JButton("添加");
        
        addButton.addActionListener(e -> {
            String pattern = patternField.getText().trim();
            if (!pattern.isEmpty()) {
                try {
                    // 检查正则表达式语法
                    Pattern.compile(pattern);
                    
                    // 创建新规则
                    UrlPattern urlPattern = new UrlPattern(pattern, true);
                    configManager.addUrlPattern(urlPattern);
                    
                    // 更新表格
                    Object[] row = {pattern, true, "删除"};
                    urlPatternsModel.addRow(row);
                    
                    // 清空输入框
                    patternField.setText("");
                } catch (PatternSyntaxException ex) {
                    JOptionPane.showMessageDialog(this,
                            "无效的正则表达式: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        addPanel.add(new JLabel("URL正则表达式:"));
        addPanel.add(patternField);
        addPanel.add(addButton);
        
        // 添加组件到面板
        panel.add(new JScrollPane(urlPatternsTable), BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建提取规则面板
     * @return 提取规则面板
     */
    private JPanel createExtractionRulesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("提取规则"));
        
        // 创建表格模型
        String[] columnNames = {"类型", "规则", "状态", "操作"};
        extractionRulesModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 1 || column == 2 || column == 3; // 允许编辑所有列
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Boolean.class; // 状态列使用复选框
                }
                return String.class;
            }
            
            @Override
            public void setValueAt(Object value, int row, int column) {
                super.setValueAt(value, row, column);
                
                // 当规则内容或状态发生变化时，更新配置管理器
                if (row >= 0 && row < configManager.getExtractionRules().size()) {
                    ExtractionRule rule = configManager.getExtractionRules().get(row);
                    
                    if (column == 0) { // 规则类型变化
                        String typeDisplayName = (String) value;
                        for (RuleType ruleType : RuleType.values()) {
                            if (ruleType.getDisplayName().equals(typeDisplayName)) {
                                rule.setRuleType(ruleType);
                                logger.logToOutput("已更新提取规则类型: " + typeDisplayName);
                                break;
                            }
                        }
                    } else if (column == 1) { // 规则内容变化
                        String newPattern = (String) value;
                        if (newPattern != null && !newPattern.trim().isEmpty()) {
                            rule.setPattern(newPattern);
                            logger.logToOutput("已更新提取规则: " + newPattern);
                        } else {
                            // 恢复原值
                            super.setValueAt(rule.getPattern(), row, column);
                        }
                    } else if (column == 2) { // 状态变化
                        boolean enabled = (Boolean) value;
                        rule.setEnabled(enabled);
                        logger.logToOutput("已" + (enabled ? "启用" : "禁用") + "提取规则: " + rule.getPattern());
                    }
                }
            }
        };
        
        // 创建表格
        extractionRulesTable = new JTable(extractionRulesModel);
        extractionRulesTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        extractionRulesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        extractionRulesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        extractionRulesTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        // 设置表格选择模式
        extractionRulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        extractionRulesTable.setRowSelectionAllowed(true);
        extractionRulesTable.setColumnSelectionAllowed(false);
        
        // 为规则类型列设置下拉框编辑器
        JComboBox<String> typeComboBox = new JComboBox<>();
        for (RuleType ruleType : RuleType.values()) {
            typeComboBox.addItem(ruleType.getDisplayName());
        }
        extractionRulesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(typeComboBox));
        
        // 设置操作列渲染器和编辑器
        extractionRulesTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer("删除"));
        extractionRulesTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox(), "删除") {
            @Override
            protected void buttonClicked() {
                if (row >= 0 && row < configManager.getExtractionRules().size()) {
                    // 确认删除
                    int confirm = JOptionPane.showConfirmDialog(
                        ConfigTab.this,
                        "确定要删除这个提取规则吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        // 删除规则
                        ExtractionRule rule = configManager.getExtractionRules().get(row);
                        configManager.removeExtractionRule(rule);
                        extractionRulesModel.removeRow(row);
                        logger.logToOutput("已删除提取规则: " + rule.getRuleType().getDisplayName() + " - " + rule.getPattern());
                    }
                }
            }
        });
        
        // 创建添加规则面板
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ruleTypeComboBox = new JComboBox<>(RuleType.values());
        patternTextField = new JTextField(30);
        JButton addButton = new JButton("添加");
        
        addButton.addActionListener(e -> {
            RuleType ruleType = (RuleType) ruleTypeComboBox.getSelectedItem();
            String pattern = patternTextField.getText().trim();
            
            if (ruleType != null && !pattern.isEmpty()) {
                // 创建新规则
                ExtractionRule rule = new ExtractionRule(ruleType, pattern, true);
                configManager.addExtractionRule(rule);
                
                // 更新表格
                Object[] row = {ruleType.getDisplayName(), pattern, true, "删除"};
                extractionRulesModel.addRow(row);
                
                // 清空输入框
                patternTextField.setText("");
            }
        });
        
        addPanel.add(new JLabel("规则类型:"));
        addPanel.add(ruleTypeComboBox);
        addPanel.add(new JLabel("规则:"));
        addPanel.add(patternTextField);
        addPanel.add(addButton);
        
        // 添加组件到面板
        panel.add(new JScrollPane(extractionRulesTable), BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 加载配置到UI组件
     */
    private void loadConfig() {
        // 设置启用状态
        enabledCheckBox.setSelected(configManager.isEnabled());
        
        // 加载URL匹配规则
        for (UrlPattern pattern : configManager.getUrlPatterns()) {
            Object[] row = {pattern.getPattern(), pattern.isEnabled(), "删除"};
            urlPatternsModel.addRow(row);
        }
        
        // 加载提取规则
        for (ExtractionRule rule : configManager.getExtractionRules()) {
            Object[] row = {rule.getRuleType().getDisplayName(), rule.getPattern(), rule.isEnabled(), "删除"};
            extractionRulesModel.addRow(row);
        }
        
        // 设置默认规则类型
        ruleTypeComboBox.setSelectedItem(configManager.getDefaultRuleType());
        
        // 更新组件状态
        updateComponentsState(configManager.isEnabled());
    }
    
    /**
     * 更新组件状态: 根据插件启用状态启用或禁用组件
     * @param enabled 是否启用
     */
    private void updateComponentsState(boolean enabled) {
        Arrays.stream(getComponents()).forEach(component -> {
            if (component != enabledCheckBox) {
                component.setEnabled(enabled);
            }
        });
    }
    
    /**
     * 按钮渲染器: 在表格中显示按钮
     */
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer(String text) {
            setText(text);
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            return this;
        }
    }
    
    /**
     * 按钮编辑器: 处理表格中按钮的点击事件
     */
    private abstract static class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        protected JTable table;
        protected int row;
        protected int column;
        
        public ButtonEditor(JCheckBox checkBox, String text) {
            super(checkBox);
            button = new JButton(text);
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                buttonClicked();
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.table = table;
            this.row = row;
            this.column = column;
            
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(UIManager.getColor("Button.background"));
            }
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
        
        protected abstract void buttonClicked();
    }
    
    /**
     * 导出配置到JSON文件
     */
    private void exportConfig() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导出配置文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件 (*.json)", "json"));
            fileChooser.setSelectedFile(new File("showMeUCode-config.json"));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                // 确保文件扩展名为.json
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                
                // 生成配置JSON字符串
                String configJson = generateConfigJson();
                
                // 写入文件
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    writer.write(configJson);
                }
                
                JOptionPane.showMessageDialog(this, "配置已成功导出到: " + file.getAbsolutePath(), 
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
                logger.logToOutput("配置已导出到: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.logToError("导出配置失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "导出配置失败: " + e.getMessage(), 
                "导出失败", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 从JSON文件导入配置
     */
    private void importConfig() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导入配置文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件 (*.json)", "json"));
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                // 确认是否覆盖当前配置
                int confirm = JOptionPane.showConfirmDialog(this,
                    "导入配置将覆盖当前所有设置，是否继续？",
                    "确认导入", JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    // 读取文件内容
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    }
                    
                    // 解析并应用配置
                    parseAndApplyConfig(content.toString());
                    
                    // 重新加载UI
                    refreshUI();
                    
                    JOptionPane.showMessageDialog(this, "配置已成功导入", 
                        "导入成功", JOptionPane.INFORMATION_MESSAGE);
                    logger.logToOutput("配置已从文件导入: " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.logToError("导入配置失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "导入配置失败: " + e.getMessage(), 
                "导入失败", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 生成配置JSON字符串
     * @return JSON格式的配置字符串
     */
    private String generateConfigJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"enabled\": ").append(configManager.isEnabled()).append(",\n");
        json.append("  \"urlPatterns\": [\n");
        
        // 导出URL匹配规则
        boolean first = true;
        for (UrlPattern pattern : configManager.getUrlPatterns()) {
            if (!first) json.append(",\n");
            json.append("    {\n");
            json.append("      \"pattern\": \"").append(escapeJson(pattern.getPattern())).append("\",\n");
            json.append("      \"enabled\": ").append(pattern.isEnabled()).append("\n");
            json.append("    }");
            first = false;
        }
        
        json.append("\n  ],\n");
        json.append("  \"extractionRules\": [\n");
        
        // 导出提取规则
        first = true;
        for (ExtractionRule rule : configManager.getExtractionRules()) {
            if (!first) json.append(",\n");
            json.append("    {\n");
            json.append("      \"ruleType\": \"").append(rule.getRuleType().name()).append("\",\n");
            json.append("      \"pattern\": \"").append(escapeJson(rule.getPattern())).append("\",\n");
            json.append("      \"enabled\": ").append(rule.isEnabled()).append("\n");
            json.append("    }");
            first = false;
        }
        
        json.append("\n  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 解析并应用配置
     * @param configJson JSON格式的配置字符串
     */
    private void parseAndApplyConfig(String configJson) {
        // 简单的JSON解析（基于字符串处理）
        // 在实际项目中，建议使用专业的JSON库如Jackson或Gson
        
        // 清空现有配置
        configManager.clearAllRules();
        
        // 解析启用状态
        if (configJson.contains("\"enabled\": true")) {
            configManager.setEnabled(true);
        } else {
            configManager.setEnabled(false);
        }
        
        // 解析URL模式（简化版本）
        String[] lines = configJson.split("\n");
        boolean inUrlPatterns = false;
        boolean inExtractionRules = false;
        String currentPattern = null;
        Boolean currentEnabled = null;
        String currentRuleType = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.contains("\"urlPatterns\"")) {
                inUrlPatterns = true;
                inExtractionRules = false;
                continue;
            } else if (line.contains("\"extractionRules\"")) {
                inUrlPatterns = false;
                inExtractionRules = true;
                continue;
            }
            
            if (line.startsWith("\"pattern\":")) {
                currentPattern = extractJsonStringValue(line);
            } else if (line.startsWith("\"enabled\":")) {
                currentEnabled = extractJsonBooleanValue(line);
            } else if (line.startsWith("\"ruleType\":")) {
                currentRuleType = extractJsonStringValue(line);
            }
            
            // 当读完一个对象时
            if (line.equals("}") && currentPattern != null && currentEnabled != null) {
                if (inUrlPatterns) {
                    UrlPattern urlPattern = new UrlPattern(currentPattern, currentEnabled);
                    configManager.addUrlPattern(urlPattern);
                } else if (inExtractionRules && currentRuleType != null) {
                    try {
                        RuleType ruleType = RuleType.valueOf(currentRuleType);
                        ExtractionRule rule = new ExtractionRule(ruleType, currentPattern, currentEnabled);
                        configManager.addExtractionRule(rule);
                    } catch (IllegalArgumentException e) {
                        logger.logToError("无效的规则类型: " + currentRuleType);
                    }
                }
                
                // 重置变量
                currentPattern = null;
                currentEnabled = null;
                currentRuleType = null;
            }
        }
    }
    
    /**
     * 从JSON行中提取字符串值
     * @param line JSON行
     * @return 提取的字符串值
     */
    private String extractJsonStringValue(String line) {
        int start = line.indexOf("\"", line.indexOf(":") + 1) + 1;
        int end = line.lastIndexOf("\"");
        if (start > 0 && end > start) {
            return line.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return null;
    }
    
    /**
     * 从JSON行中提取布尔值
     * @param line JSON行
     * @return 提取的布尔值
     */
    private Boolean extractJsonBooleanValue(String line) {
        return line.contains("true");
    }
    
    /**
     * 转义JSON字符串
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    /**
     * 刷新UI显示
     */
    private void refreshUI() {
        // 清空表格
        urlPatternsModel.setRowCount(0);
        extractionRulesModel.setRowCount(0);
        
        // 重新加载配置
        loadConfig();
    }
} 