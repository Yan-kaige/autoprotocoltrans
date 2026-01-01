#!/bin/bash

# Docker Hub 镜像推送脚本
# 使用方法: ./scripts/push-to-dockerhub.sh [your-username] [tag]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查参数
if [ -z "$1" ]; then
    echo -e "${RED}错误: 请提供 Docker Hub 用户名${NC}"
    echo "使用方法: $0 <your-username> [tag]"
    echo "示例: $0 john"
    echo "示例: $0 john v1.0.0"
    exit 1
fi

DOCKER_USERNAME=$1
TAG=${2:-latest}
IMAGE_NAME="autoprotocoltrans"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${TAG}"

echo -e "${GREEN}开始推送镜像到 Docker Hub...${NC}"
echo "用户名: ${DOCKER_USERNAME}"
echo "镜像: ${FULL_IMAGE_NAME}"
echo ""

# 检查是否已登录
if ! docker info | grep -q "Username"; then
    echo -e "${YELLOW}未检测到 Docker 登录，请先登录...${NC}"
    docker login
fi

# 构建镜像
echo -e "${GREEN}[1/4] 构建镜像...${NC}"
docker build -t ${IMAGE_NAME}:${TAG} .

# 打标签
echo -e "${GREEN}[2/4] 打标签...${NC}"
docker tag ${IMAGE_NAME}:${TAG} ${FULL_IMAGE_NAME}

# 推送镜像
echo -e "${GREEN}[3/4] 推送镜像到 Docker Hub...${NC}"
docker push ${FULL_IMAGE_NAME}

# 完成
echo -e "${GREEN}[4/4] 完成！${NC}"
echo ""
echo -e "${GREEN}镜像已成功推送到 Docker Hub${NC}"
echo "镜像地址: https://hub.docker.com/r/${DOCKER_USERNAME}/${IMAGE_NAME}"
echo ""
echo "拉取镜像命令:"
echo "  docker pull ${FULL_IMAGE_NAME}"
echo ""
echo "在 Kubernetes 中使用:"
echo "  image: ${FULL_IMAGE_NAME}"








