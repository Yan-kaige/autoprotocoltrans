#!/bin/bash

# Docker Hub 镜像推送修复脚本
# 用于解决推送权限问题

set -e

DOCKER_USERNAME="kaige"
IMAGE_NAME="autoprotocoltrans"
TAG="latest"
LOCAL_IMAGE="${IMAGE_NAME}:${TAG}"
REMOTE_IMAGE="${DOCKER_USERNAME}/${IMAGE_NAME}:${TAG}"

echo "=== Docker Hub 镜像推送修复脚本 ==="
echo ""

# 1. 检查是否已构建镜像
echo "[步骤 1] 检查本地镜像..."
if ! docker images | grep -q "^${IMAGE_NAME}"; then
    echo "本地镜像不存在，开始构建..."
    docker build -t ${LOCAL_IMAGE} .
else
    echo "✓ 本地镜像已存在"
    docker images | grep ${IMAGE_NAME}
fi
echo ""

# 2. 打标签
echo "[步骤 2] 为镜像打标签..."
echo "  本地镜像: ${LOCAL_IMAGE}"
echo "  远程镜像: ${REMOTE_IMAGE}"
docker tag ${LOCAL_IMAGE} ${REMOTE_IMAGE}
echo "✓ 标签已创建"
echo ""

# 3. 验证标签
echo "[步骤 3] 验证镜像标签..."
docker images | grep ${DOCKER_USERNAME}/${IMAGE_NAME}
echo ""

# 4. 推送镜像
echo "[步骤 4] 推送镜像到 Docker Hub..."
echo "  推送: ${REMOTE_IMAGE}"
docker push ${REMOTE_IMAGE}
echo ""

echo "=== 完成！ ==="
echo "镜像地址: https://hub.docker.com/r/${DOCKER_USERNAME}/${IMAGE_NAME}"
echo ""
echo "拉取镜像: docker pull ${REMOTE_IMAGE}"





