@echo off
:: ShowMeUCode 版本发布脚本 (Windows版本)
:: 用法: scripts\release.bat [版本号]
:: 示例: scripts\release.bat 1.0.1

setlocal enabledelayedexpansion

:: 设置编码为UTF-8
chcp 65001 > nul

:: 检查参数
if "%1"=="" (
    echo ShowMeUCode 版本发布脚本
    echo.
    echo 用法:
    echo   %0 ^<版本号^>
    echo.
    echo 示例:
    echo   %0 1.0.1     # 发布版本 1.0.1
    echo   %0 1.1.0     # 发布版本 1.1.0
    echo   %0 2.0.0     # 发布版本 2.0.0
    echo.
    echo 版本号格式: x.y.z (语义化版本)
    echo.
    echo 发布流程:
    echo 1. 验证版本号格式
    echo 2. 检查Git状态
    echo 3. 更新项目版本号
    echo 4. 构建和测试
    echo 5. 创建版本tag
    echo 6. 推送到GitHub（触发自动发布）
    exit /b 1
)

if "%1"=="-h" (
    goto :show_usage
)

if "%1"=="--help" (
    goto :show_usage
)

set VERSION=%1
set TAG=v%VERSION%

echo 🚀 ShowMeUCode 版本发布脚本
echo ================================
echo.

:: 验证版本号格式
echo [INFO] 验证版本号格式...
echo %VERSION% | findstr /r "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" > nul
if errorlevel 1 (
    echo [ERROR] 版本号格式无效: %VERSION%
    echo [ERROR] 版本号必须是 x.y.z 格式，例如: 1.0.0, 1.2.3, 2.0.0
    exit /b 1
)
echo [SUCCESS] 版本号格式验证通过: %VERSION%

:: 检查Git状态
echo [INFO] 检查Git状态...

:: 检查是否在git仓库中
git rev-parse --git-dir > nul 2>&1
if errorlevel 1 (
    echo [ERROR] 当前目录不是Git仓库
    exit /b 1
)

:: 检查是否有未提交的更改
git diff-index --quiet HEAD --
if errorlevel 1 (
    echo [ERROR] 存在未提交的更改
    echo [INFO] 请先提交所有更改后再发布版本
    git status --short
    exit /b 1
)

:: 检查tag是否已存在
git tag -l | findstr /x "%TAG%" > nul
if not errorlevel 1 (
    echo [ERROR] Tag %TAG% 已存在
    echo [INFO] 现有tags:
    git tag -l
    exit /b 1
)

echo [SUCCESS] Git状态检查通过

:: 更新版本号
echo [INFO] 更新项目版本号到 %VERSION%...
if exist "pom.xml" (
    mvn versions:set -DnewVersion=%VERSION% -DgenerateBackupPoms=false
    if errorlevel 1 (
        echo [ERROR] 更新pom.xml版本号失败
        exit /b 1
    )
    echo [SUCCESS] 已更新 pom.xml 版本号
)

:: 更新Java常量
if exist "src\main\java\org\oxff\ShowMeUCode.java" (
    powershell -Command "(Get-Content 'src\main\java\org\oxff\ShowMeUCode.java') -replace 'EXTENSION_VERSION = \"[^\"]*\"', 'EXTENSION_VERSION = \"%VERSION%\"' | Set-Content 'src\main\java\org\oxff\ShowMeUCode.java'"
    echo [SUCCESS] 已更新 ShowMeUCode.java 版本常量
)

echo [SUCCESS] 版本号更新完成

:: 构建项目
echo [INFO] 构建项目...
mvn clean compile
if errorlevel 1 (
    echo [ERROR] 编译失败
    exit /b 1
)
echo [SUCCESS] 编译完成

mvn package -DskipTests
if errorlevel 1 (
    echo [ERROR] 打包失败
    exit /b 1
)
echo [SUCCESS] 打包完成

:: 验证JAR文件
set JAR_FILE=target\showMeUCode-%VERSION%.jar
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR文件不存在: %JAR_FILE%
    exit /b 1
)

for %%A in ("%JAR_FILE%") do set FILE_SIZE=%%~zA
set /a FILE_SIZE_KB=%FILE_SIZE%/1024
echo [SUCCESS] JAR文件构建成功: %JAR_FILE% (%FILE_SIZE_KB% KB)

:: 运行测试
echo [INFO] 运行测试...
mvn test > nul 2>&1
if errorlevel 1 (
    echo [WARNING] 没有测试或测试跳过
) else (
    echo [SUCCESS] 所有测试通过
)

:: 创建发布提交
echo [INFO] 创建发布提交...
git add pom.xml
if exist "src\main\java\org\oxff\ShowMeUCode.java" (
    git add src\main\java\org\oxff\ShowMeUCode.java
)

git diff --cached --quiet
if errorlevel 1 (
    git commit -m "chore: 发布版本 %VERSION%"
    echo [SUCCESS] 已创建发布提交
) else (
    echo [INFO] 没有版本文件需要提交
)

:: 创建tag
echo [INFO] 创建版本tag: %TAG%
git tag -a "%TAG%" -m "ShowMeUCode %TAG%

🚀 版本 %VERSION% 发布

📦 主要变更:
- 请查看 GitHub Release 页面获取详细变更日志

🔗 相关链接:
- 下载地址: https://github.com/GitHubNull/showMeUCode/releases/tag/%TAG%
- 用户指南: https://github.com/GitHubNull/showMeUCode/blob/%TAG%/docs/user-guide.md
- 项目主页: https://github.com/GitHubNull/showMeUCode

发布时间: %date% %time%"

if errorlevel 1 (
    echo [ERROR] 创建tag失败
    exit /b 1
)
echo [SUCCESS] 已创建 tag: %TAG%

:: 推送tag
echo [INFO] 推送到GitHub...
git push origin "%TAG%"
if errorlevel 1 (
    echo [ERROR] 推送tag失败
    exit /b 1
)
echo [SUCCESS] 已推送 tag: %TAG%

git push origin HEAD
if errorlevel 1 (
    echo [WARNING] 推送提交时出现问题
) else (
    echo [SUCCESS] 已推送最新提交
)

:: 显示发布信息
echo.
echo [SUCCESS] 🎉 版本 %VERSION% 发布流程完成！
echo.
echo [INFO] 📋 发布信息:
echo   版本号: %VERSION%
echo   Tag: %TAG%
echo   GitHub Actions: https://github.com/GitHubNull/showMeUCode/actions
echo   Release页面: https://github.com/GitHubNull/showMeUCode/releases/tag/%TAG%
echo.
echo [INFO] ⏳ 接下来:
echo   1. GitHub Actions 将自动构建和发布
echo   2. 大约需要 5-10 分钟完成自动发布
echo   3. 完成后可在 Release 页面下载 JAR 文件
echo.
echo [WARNING] 📝 注意:
echo   - 请在GitHub上检查Actions执行状态
echo   - 如果发布失败，请检查GitHub Actions日志
echo   - 发布成功后记得测试下载的JAR文件

goto :eof

:show_usage
echo ShowMeUCode 版本发布脚本
echo.
echo 用法:
echo   %0 ^<版本号^>
echo.
echo 示例:
echo   %0 1.0.1     # 发布版本 1.0.1
echo   %0 1.1.0     # 发布版本 1.1.0
echo   %0 2.0.0     # 发布版本 2.0.0
echo.
echo 版本号格式: x.y.z (语义化版本)
exit /b 0 