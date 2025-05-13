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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
        
        // 创建工具类型面板
        JPanel toolTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolTypePanel.setBorder(BorderFactory.createTitledBorder("处理工具类型"));
        
        toolTypeComboBox = new JComboBox<>(ToolType.values());
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
                return column == 1; // 只允许编辑状态列
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class; // 状态列使用复选框
                }
                return String.class;
            }
        };
        
        // 创建表格
        urlPatternsTable = new JTable(urlPatternsModel);
        urlPatternsTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        urlPatternsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        urlPatternsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        // 设置操作列渲染器和编辑器
        urlPatternsTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer("删除"));
        urlPatternsTable.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(new JCheckBox(), "删除") {
            @Override
            protected void buttonClicked() {
                int row = urlPatternsTable.getSelectedRow();
                if (row >= 0) {
                    // 删除规则
                    UrlPattern pattern = configManager.getUrlPatterns().get(row);
                    configManager.removeUrlPattern(pattern);
                    urlPatternsModel.removeRow(row);
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
                return column == 2; // 只允许编辑状态列
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Boolean.class; // 状态列使用复选框
                }
                return String.class;
            }
        };
        
        // 创建表格
        extractionRulesTable = new JTable(extractionRulesModel);
        extractionRulesTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        extractionRulesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        extractionRulesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        extractionRulesTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        // 设置操作列渲染器和编辑器
        extractionRulesTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer("删除"));
        extractionRulesTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox(), "删除") {
            @Override
            protected void buttonClicked() {
                int row = extractionRulesTable.getSelectedRow();
                if (row >= 0) {
                    // 删除规则
                    ExtractionRule rule = configManager.getExtractionRules().get(row);
                    configManager.removeExtractionRule(rule);
                    extractionRulesModel.removeRow(row);
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
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
        
        protected abstract void buttonClicked();
    }
} 