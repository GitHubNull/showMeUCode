# ShowMeUCode

[![GitHub release](https://img.shields.io/github/v/release/GitHubNull/showMeUCode)](https://github.com/GitHubNull/showMeUCode/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)

ShowMeUCode是一个Burp Suite插件，用于在HTTP历史记录中自动提取并显示隐藏在请求中的真实接口名称。

## 项目背景

在Web安全测试过程中，许多现代Web应用采用统一的API网关架构，所有API请求都指向同一个URL（如`/api/v1/gateway`或`/api/service`），而真正的接口名称或方法名则被包含在请求体或URL参数中。这使得安全测试人员在使用Burp Suite查看历史记录时，难以区分不同的API请求，从而影响测试效率和测试覆盖度的把控。

ShowMeUCode插件通过自动从HTTP请求的URL参数或请求体中提取真实接口名称，并将其显示在Burp Suite的请求备注中，帮助测试人员更直观地管理测试进度。

## 主要功能

### 核心功能

- **自动实时提取**：HTTP请求经过时自动提取接口名称并标记到备注列
- **URL优先提取**：先从URL参数中提取接口名称，失败后再从请求体提取
- **多种数据格式支持**：支持JSON、XML、表单数据等常见格式
- **灵活提取规则**：支持正则表达式、JSON路径、XPath等多种提取方式

### 批量处理功能

- **批量提取（所有请求）**：一键为所有历史请求提取并标记接口名称
- **选中请求提取**：仅对选中的请求进行接口名称提取
- **单个请求提取**：对当前查看的请求进行接口名称提取

### 接口名称导出

- **复制所有接口名称到剪贴板**：提取所有历史记录中的接口名称，去重后复制到剪贴板
- **复制选中请求接口名称到剪贴板**：提取选中请求的接口名称，去重后复制到剪贴板
- 方便用户检查接口测试覆盖情况

## 安装方法

1. 从 [Releases](https://github.com/GitHubNull/showMeUCode/releases) 下载最新版本的`showMeUCode-x.x.x.jar`文件
2. 打开Burp Suite
3. 进入"Extensions"标签页
4. 点击"Add"按钮
5. 在"Extension Type"下选择"Java"
6. 在"Extension File"中选择下载的JAR文件
7. 点击"Next"完成安装

## 使用方法

### 自动提取

安装完成后，插件会自动对经过Burp的HTTP请求进行接口名称提取，并标记到请求的备注列中。

### 右键菜单操作

在HTTP History或Target中右键点击，可以看到以下菜单选项：

| 菜单项 | 功能说明 |
|--------|----------|
| 批量提取接口名称(所有请求) | 为所有历史请求批量提取并标记接口名称 |
| 提取选中请求接口名称 (N个) | 仅对选中的N个请求进行提取 |
| 提取当前请求接口名称 | 对当前查看的单个请求进行提取 |
| 复制所有接口名称到剪贴板(去重) | 提取并复制所有不重复的接口名称 |
| 复制选中请求接口名称到剪贴板(去重) | 提取并复制选中请求的接口名称 |

## 配置说明

### URL提取规则

用于从URL参数中提取接口名称，使用正则表达式的**捕获组**提取。

**示例场景**：接口名称在URL参数中
- URL: `http://test.com/api.do?method=getUserById&id=1`
- 规则: `method=([^&]+)`
- 提取结果: `getUserById`

### 请求体提取规则

定义如何从请求体中提取接口名称，支持多种格式：

1. **正则表达式**：适用于各种文本格式
   ```
   "method"\s*:\s*"([^"]+)"
   ```

2. **JSON路径**：适用于JSON格式的请求
   ```
   $.method
   $.data.action
   ```

3. **XPath**：适用于XML格式的请求
   ```
   //methodName
   /root/action/@name
   ```

4. **表单参数**：适用于表单提交
   ```
   method
   action
   ```

### 提取逻辑

1. 首先使用URL规则尝试从URL中提取接口名称
2. 如果URL提取成功，直接使用提取结果
3. 如果URL提取失败，继续使用请求体规则从body中提取
4. 如果都失败，则不进行标记

## 构建说明

### 从源码构建

```bash
git clone https://github.com/GitHubNull/showMeUCode.git
cd showMeUCode
mvn clean package
```

构建完成后，JAR文件将位于`target`目录中。

### 自动发布

本项目配置了GitHub Actions自动发布流程：

```bash
# 使用发布脚本（推荐）
./scripts/release.sh 1.1.0

# 或手动创建tag
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0
```

## 系统要求

- Java 17或更高版本
- Burp Suite Professional/Community 2020.12或更高版本

## 开源许可

本项目采用 [MIT许可证](LICENSE)。

## 免责声明

本工具仅供合法的安全研究、渗透测试和教育学习目的使用。使用者必须确保在获得适当授权的情况下使用本工具。详见 [免责声明](DISCLAIMER.md)。

## 贡献指南

欢迎提交问题报告、功能请求或代码贡献。请遵循以下步骤：

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启一个Pull Request

## 作者

GitHubNull - [https://github.com/GitHubNull](https://github.com/GitHubNull)

## 致谢

- 感谢Burp Suite提供的优秀安全测试平台
- 感谢所有为本项目提供反馈和建议的用户
