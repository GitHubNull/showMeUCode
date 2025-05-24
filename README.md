# ShowMeUCode

ShowMeUCode是一个Burp Suite插件，用于在HTTP历史记录中显示隐藏在请求体中的真实接口名称。

## 项目背景

在Web安全测试过程中，许多现代Web应用采用统一的API网关架构，所有API请求都指向同一个URL（如`/api/v1/gateway`或`/api/service`），而真正的接口名称或方法名则被包含在请求体中。这使得安全测试人员在使用Burp Suite查看历史记录时，难以区分不同的API请求，从而影响测试效率和测试覆盖度的把控。

ShowMeUCode插件通过自动从HTTP请求体中提取真实接口名称，并将其显示在Burp Suite的请求备注中，帮助测试人员更直观地管理测试进度。

## 主要功能

- 自动从HTTP请求体中提取真实接口名称
- 支持多种常见数据格式（JSON、XML、表单数据）
- 提供灵活的提取规则配置（正则表达式、JSON路径、XPath）
- 提供简洁直观的用户界面
- 可配置的URL匹配规则
- 配置保存与加载功能

## 安装方法

1. 下载最新版本的`showMeUCode.jar`文件
2. 打开Burp Suite
3. 进入"Extensions"标签页
4. 点击"Add"按钮
5. 在"Extension Type"下选择"Java"
6. 在"Extension File"中选择下载的JAR文件
7. 点击"Next"完成安装

## 使用方法

1. 安装完成后，在Burp Suite的Extensions标签页中找到ShowMeUCode插件
2. 进入插件配置面板，设置需要监听的URL模式和提取规则
3. 开启插件功能
4. 使用Burp Suite拦截或发送请求
5. 在HTTP历史记录中查看带有提取出的接口名称的请求备注

## 配置说明

### URL匹配规则

指定哪些URL的请求需要进行处理。支持正则表达式，例如：
- `.*api/gateway.*` - 匹配所有包含api/gateway的URL
- `^https://example.com/api/.*` - 匹配特定域名下的API请求

### 提取规则

定义如何从请求体中提取接口名称。支持多种格式：

1. **正则表达式**：适用于各种文本格式
   例如：`"method"\s*:\s*"([^"]+)"`

2. **JSON路径**：适用于JSON格式的请求
   例如：`$.method` 或 `$.data.action`

3. **XPath**：适用于XML格式的请求
   例如：`//methodName` 或 `/root/action/@name`

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

#### 快速发布新版本
```bash
# 使用发布脚本（推荐）
./scripts/release.sh 1.0.1

# 或手动创建tag
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

#### 自动发布功能
- ✅ 自动编译构建JAR文件
- ✅ 自动创建GitHub Release
- ✅ 自动生成发布说明
- ✅ 自动上传构建产物

详见：[GitHub Actions自动发布指南](docs/github-actions-guide.md)

## 系统要求

- Java 17或更高版本
- Burp Suite Professional/Community 2020.12或更高版本

## 开源许可

本项目采用MIT许可证。

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