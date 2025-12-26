@echo off
REM Docker Hub 镜像推送脚本（Windows）
REM 使用方法: scripts\push-to-dockerhub.bat [your-username] [tag]

setlocal enabledelayedexpansion

REM 检查参数
if "%~1"=="" (
    echo 错误: 请提供 Docker Hub 用户名
    echo 使用方法: %0 ^<your-username^> [tag]
    echo 示例: %0 john
    echo 示例: %0 john v1.0.0
    exit /b 1
)

set DOCKER_USERNAME=%~1
set TAG=%~2
if "%TAG%"=="" set TAG=latest

set IMAGE_NAME=autoprotocoltrans
set FULL_IMAGE_NAME=%DOCKER_USERNAME%/%IMAGE_NAME%:%TAG%

echo 开始推送镜像到 Docker Hub...
echo 用户名: %DOCKER_USERNAME%
echo 镜像: %FULL_IMAGE_NAME%
echo.

REM 检查是否已登录（简单检查）
docker info | findstr /C:"Username" >nul 2>&1
if errorlevel 1 (
    echo 未检测到 Docker 登录，请先登录...
    docker login
)

REM 构建镜像
echo [1/4] 构建镜像...
docker build -t %IMAGE_NAME%:%TAG% .

REM 打标签
echo [2/4] 打标签...
docker tag %IMAGE_NAME%:%TAG% %FULL_IMAGE_NAME%

REM 推送镜像
echo [3/4] 推送镜像到 Docker Hub...
docker push %FULL_IMAGE_NAME%

REM 完成
echo [4/4] 完成！
echo.
echo 镜像已成功推送到 Docker Hub
echo 镜像地址: https://hub.docker.com/r/%DOCKER_USERNAME%/%IMAGE_NAME%
echo.
echo 拉取镜像命令:
echo   docker pull %FULL_IMAGE_NAME%
echo.
echo 在 Kubernetes 中使用:
echo   image: %FULL_IMAGE_NAME%

