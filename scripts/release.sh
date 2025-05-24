#!/bin/bash

# ShowMeUCode 版本发布脚本
# 用法: ./scripts/release.sh [版本号]
# 示例: ./scripts/release.sh 1.0.1

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 显示使用说明
show_usage() {
    echo "ShowMeUCode 版本发布脚本"
    echo ""
    echo "用法:"
    echo "  $0 <版本号>"
    echo ""
    echo "示例:"
    echo "  $0 1.0.1     # 发布版本 1.0.1"
    echo "  $0 1.1.0     # 发布版本 1.1.0"
    echo "  $0 2.0.0     # 发布版本 2.0.0"
    echo ""
    echo "版本号格式: x.y.z (语义化版本)"
    echo ""
    echo "发布流程:"
    echo "1. 验证版本号格式"
    echo "2. 检查Git状态"
    echo "3. 更新项目版本号"
    echo "4. 构建和测试"
    echo "5. 创建版本tag"
    echo "6. 推送到GitHub（触发自动发布）"
}

# 验证版本号格式
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "版本号格式无效: $version"
        print_error "版本号必须是 x.y.z 格式，例如: 1.0.0, 1.2.3, 2.0.0"
        exit 1
    fi
    print_success "版本号格式验证通过: $version"
}

# 检查Git状态
check_git_status() {
    print_info "检查Git状态..."
    
    # 检查是否在git仓库中
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "当前目录不是Git仓库"
        exit 1
    fi
    
    # 检查是否在main或master分支
    current_branch=$(git branch --show-current)
    if [[ "$current_branch" != "main" && "$current_branch" != "master" ]]; then
        print_warning "当前不在main/master分支: $current_branch"
        read -p "是否继续？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "发布已取消"
            exit 0
        fi
    fi
    
    # 检查是否有未提交的更改
    if ! git diff-index --quiet HEAD --; then
        print_error "存在未提交的更改"
        print_info "请先提交所有更改后再发布版本"
        git status --short
        exit 1
    fi
    
    # 检查是否有未推送的提交
    if [ $(git rev-list HEAD --not --remotes | wc -l) -gt 0 ]; then
        print_warning "存在未推送的提交"
        read -p "是否先推送这些提交？(Y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            git push
            print_success "已推送最新提交"
        fi
    fi
    
    print_success "Git状态检查通过"
}

# 检查tag是否已存在
check_tag_exists() {
    local version=$1
    local tag="v$version"
    
    if git tag -l | grep -q "^$tag$"; then
        print_error "Tag $tag 已存在"
        print_info "现有tags:"
        git tag -l | sort -V
        exit 1
    fi
    
    # 检查远程是否存在此tag
    if git ls-remote --tags origin | grep -q "refs/tags/$tag$"; then
        print_error "远程已存在 tag $tag"
        exit 1
    fi
    
    print_success "Tag $tag 不存在，可以创建"
}

# 更新版本号
update_version() {
    local version=$1
    
    print_info "更新项目版本号到 $version..."
    
    # 更新pom.xml
    if [ -f "pom.xml" ]; then
        mvn versions:set -DnewVersion=$version -DgenerateBackupPoms=false
        print_success "已更新 pom.xml 版本号"
    fi
    
    # 更新Java常量（如果存在）
    if [ -f "src/main/java/org/oxff/ShowMeUCode.java" ]; then
        sed -i.bak "s/EXTENSION_VERSION = \"[^\"]*\"/EXTENSION_VERSION = \"$version\"/" src/main/java/org/oxff/ShowMeUCode.java
        rm -f src/main/java/org/oxff/ShowMeUCode.java.bak
        print_success "已更新 ShowMeUCode.java 版本常量"
    fi
    
    print_success "版本号更新完成"
}

# 构建项目
build_project() {
    print_info "构建项目..."
    
    # 清理并编译
    mvn clean compile
    print_success "编译完成"
    
    # 打包
    mvn package -DskipTests
    print_success "打包完成"
    
    # 验证JAR文件
    local version=$1
    local jar_file="target/showMeUCode-$version.jar"
    if [ ! -f "$jar_file" ]; then
        print_error "JAR文件不存在: $jar_file"
        exit 1
    fi
    
    local file_size=$(du -h "$jar_file" | cut -f1)
    print_success "JAR文件构建成功: $jar_file ($file_size)"
}

# 运行测试（如果有）
run_tests() {
    print_info "运行测试..."
    
    if mvn test 2>/dev/null; then
        print_success "所有测试通过"
    else
        print_warning "没有测试或测试跳过"
    fi
}

# 创建发布提交
create_release_commit() {
    local version=$1
    
    print_info "创建发布提交..."
    
    # 添加修改的文件
    git add pom.xml
    if [ -f "src/main/java/org/oxff/ShowMeUCode.java" ]; then
        git add src/main/java/org/oxff/ShowMeUCode.java
    fi
    
    # 检查是否有要提交的更改
    if git diff --cached --quiet; then
        print_info "没有版本文件需要提交"
    else
        git commit -m "chore: 发布版本 $version"
        print_success "已创建发布提交"
    fi
}

# 创建和推送tag
create_and_push_tag() {
    local version=$1
    local tag="v$version"
    
    print_info "创建版本tag: $tag"
    
    # 生成tag消息
    local tag_message="ShowMeUCode v$version

🚀 版本 $version 发布

📦 主要变更:
- 请查看 GitHub Release 页面获取详细变更日志

🔗 相关链接:
- 下载地址: https://github.com/GitHubNull/showMeUCode/releases/tag/$tag
- 用户指南: https://github.com/GitHubNull/showMeUCode/blob/$tag/docs/user-guide.md
- 项目主页: https://github.com/GitHubNull/showMeUCode

发布时间: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    
    # 创建tag
    git tag -a "$tag" -m "$tag_message"
    print_success "已创建 tag: $tag"
    
    # 推送tag
    print_info "推送到GitHub..."
    git push origin "$tag"
    print_success "已推送 tag: $tag"
    
    # 推送可能的提交
    git push origin HEAD
    print_success "已推送最新提交"
}

# 显示发布信息
show_release_info() {
    local version=$1
    local tag="v$version"
    
    echo ""
    print_success "🎉 版本 $version 发布流程完成！"
    echo ""
    print_info "📋 发布信息:"
    echo "  版本号: $version"
    echo "  Tag: $tag"
    echo "  GitHub Actions: https://github.com/GitHubNull/showMeUCode/actions"
    echo "  Release页面: https://github.com/GitHubNull/showMeUCode/releases/tag/$tag"
    echo ""
    print_info "⏳ 接下来:"
    echo "  1. GitHub Actions 将自动构建和发布"
    echo "  2. 大约需要 5-10 分钟完成自动发布"
    echo "  3. 完成后可在 Release 页面下载 JAR 文件"
    echo ""
    print_warning "📝 注意:"
    echo "  - 请在GitHub上检查Actions执行状态"
    echo "  - 如果发布失败，请检查GitHub Actions日志"
    echo "  - 发布成功后记得测试下载的JAR文件"
}

# 主函数
main() {
    echo "🚀 ShowMeUCode 版本发布脚本"
    echo "================================"
    echo ""
    
    # 检查参数
    if [ $# -eq 0 ]; then
        show_usage
        exit 1
    fi
    
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    local version=$1
    
    # 执行发布流程
    validate_version "$version"
    check_git_status
    check_tag_exists "$version"
    update_version "$version"
    build_project "$version"
    run_tests
    create_release_commit "$version"
    create_and_push_tag "$version"
    show_release_info "$version"
}

# 运行主函数
main "$@" 