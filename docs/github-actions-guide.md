# GitHub Actions 自动发布指南

## 📖 概述

本项目配置了完整的GitHub Actions自动发布流程，当您推送新的版本tag时，会自动执行以下操作：

1. **编译构建**：自动编译Java代码并打包JAR文件
2. **版本验证**：验证tag格式和版本号
3. **生成Release**：创建GitHub Release并上传构建产物
4. **发布说明**：自动生成包含变更日志的发布说明

## 🚀 快速发布

### 方式一：使用发布脚本（推荐）

```bash
# 给脚本执行权限（首次使用）
chmod +x scripts/release.sh

# 发布新版本
./scripts/release.sh 1.0.1
```

### 方式二：手动创建tag

```bash
# 更新版本号（可选）
mvn versions:set -DnewVersion=1.0.1 -DgenerateBackupPoms=false

# 提交版本更改
git add pom.xml
git commit -m "chore: 发布版本 1.0.1"

# 创建并推送tag
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

## 📋 发布流程详解

### 1. 触发条件

自动发布会在以下情况触发：
- 推送格式为 `v*` 的tag（如：v1.0.0、v2.1.3）
- tag必须符合语义化版本规范（x.y.z）

### 2. 构建环境

- **操作系统**：Ubuntu Latest
- **Java版本**：17 (Temurin)
- **构建工具**：Maven
- **权限**：contents: write（用于创建release）

### 3. 执行步骤

#### 步骤1：检出代码
```yaml
- name: 检出代码
  uses: actions/checkout@v4
  with:
    fetch-depth: 0  # 获取完整历史记录
```

#### 步骤2：设置Java环境
```yaml
- name: 设置Java环境
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
```

#### 步骤3：缓存Maven依赖
```yaml
- name: 缓存Maven依赖
  uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
```

#### 步骤4：版本处理
- 从tag中提取版本号
- 验证版本号格式
- 更新pom.xml中的版本号

#### 步骤5：编译构建
```bash
mvn clean compile
mvn package -DskipTests
```

#### 步骤6：生成变更日志
- 获取上一个tag到当前tag的提交记录
- 生成结构化的发布说明
- 包含下载链接和使用说明

#### 步骤7：创建GitHub Release
- 创建Release页面
- 上传JAR文件作为Asset
- 上传项目文档

## 🎯 版本号规范

### 语义化版本

遵循 [Semantic Versioning](https://semver.org/) 规范：

```
v主版本号.次版本号.修订号
```

**示例**：
- `v1.0.0` - 首个正式版本
- `v1.0.1` - 修复bug的版本
- `v1.1.0` - 新增功能的版本
- `v2.0.0` - 重大更新的版本

### 版本号含义

| 版本类型 | 说明 | 示例 |
|----------|------|------|
| **主版本号** | 不兼容的API修改 | 1.0.0 → 2.0.0 |
| **次版本号** | 向下兼容的功能性新增 | 1.0.0 → 1.1.0 |
| **修订号** | 向下兼容的问题修正 | 1.0.0 → 1.0.1 |

## 📦 构建产物

每次发布会生成以下文件：

### JAR文件
- **文件名**：`showMeUCode-{版本号}.jar`
- **位置**：Release页面的Assets部分
- **用途**：Burp Suite插件文件，可直接安装使用

### 文档文件
- **README.md**：项目说明文档
- **链接到详细文档**：用户指南、API文档等

## 🔍 发布状态监控

### 查看构建状态

1. **GitHub Actions页面**
   ```
   https://github.com/GitHubNull/showMeUCode/actions
   ```

2. **具体工作流程**
   - 点击对应的工作流程运行实例
   - 查看各个步骤的执行状态和日志

### 状态标识

| 状态 | 图标 | 说明 |
|------|------|------|
| 运行中 | 🟡 | 正在执行构建流程 |
| 成功 | ✅ | 发布成功完成 |
| 失败 | ❌ | 发布过程中出现错误 |

## 🐛 常见问题

### Q1: 推送tag后没有触发自动发布？

**可能原因**：
1. tag格式不正确（必须以v开头）
2. 版本号不符合x.y.z格式
3. GitHub Actions被禁用

**解决方案**：
```bash
# 检查tag格式
git tag -l

# 重新创建正确格式的tag
git tag -d v1.0.0  # 删除错误的tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### Q2: 构建失败怎么办？

**排查步骤**：
1. 查看GitHub Actions日志
2. 检查编译错误
3. 验证pom.xml配置
4. 本地测试构建流程

**常见错误**：
```bash
# 本地测试构建
mvn clean compile
mvn package -DskipTests

# 检查Java版本
java -version
mvn -version
```

### Q3: JAR文件构建成功但无法使用？

**检查项目**：
1. JAR文件完整性
2. Manifest文件配置
3. 依赖包是否正确包含

### Q4: Release页面没有出现？

**可能原因**：
1. GitHub Token权限不足
2. 仓库权限设置问题
3. Actions执行失败

**解决方案**：
```bash
# 检查权限设置
# 仓库设置 → Actions → General → Workflow permissions
# 确保选择了 "Read and write permissions"
```

## 🔧 自定义配置

### 修改触发条件

编辑 `.github/workflows/release.yml`：

```yaml
on:
  push:
    tags:
      - 'v*'        # 所有v开头的tag
      - 'release/*' # release/开头的tag
```

### 添加测试步骤

在构建步骤中添加：

```yaml
- name: 运行测试
  run: mvn test
```

### 自定义Release说明

修改变更日志生成部分：

```bash
# 使用自定义模板
cat custom-release-template.md > changelog_temp.md
```

## 📚 相关文档

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [Semantic Versioning 规范](https://semver.org/)
- [Maven 版本管理](https://maven.apache.org/maven-release/maven-release-plugin/)
- [Burp Suite 扩展开发](https://portswigger.net/burp/documentation/desktop/extensions)

## 💡 最佳实践

### 发布前检查清单

- [ ] 代码已提交并推送
- [ ] 本地构建测试通过
- [ ] 版本号符合语义化规范
- [ ] 更新相关文档
- [ ] 测试JAR文件功能

### 发布后验证

- [ ] 检查GitHub Release页面
- [ ] 下载并测试JAR文件
- [ ] 验证发布说明内容
- [ ] 更新项目文档中的版本信息

---

🎉 **自动发布让版本管理变得简单高效！** 