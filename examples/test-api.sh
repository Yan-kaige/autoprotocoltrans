#!/bin/bash

# API 转换测试脚本
# 使用方法: ./test-api.sh

API_URL="http://localhost:8080/api/v2/transform"

echo "=== 测试 JSON 到 JSON 转换 ==="
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "{\"user\":{\"name\":\"张三\",\"age\":30}}",
    "mappingConfig": {
      "sourceProtocol": "JSON",
      "targetProtocol": "JSON",
      "prettyPrint": true,
      "rules": [
        {
          "sourcePath": "$.user.name",
          "targetPath": "customer.name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        },
        {
          "sourcePath": "$.user.age",
          "targetPath": "customer.age",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }' | jq '.'

echo -e "\n=== 测试 JSON 到 XML 转换 ==="
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "{\"name\":\"张三\",\"age\":30}",
    "mappingConfig": {
      "sourceProtocol": "JSON",
      "targetProtocol": "XML",
      "prettyPrint": true,
      "xmlRootElementName": "user",
      "includeXmlDeclaration": true,
      "rules": [
        {
          "sourcePath": "$.name",
          "targetPath": "name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        },
        {
          "sourcePath": "$.age",
          "targetPath": "age",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }' | jq -r '.transformedData'

echo -e "\n=== 测试 XML 到 JSON 转换 ==="
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "<user><name>张三</name><age>30</age></user>",
    "mappingConfig": {
      "sourceProtocol": "XML",
      "targetProtocol": "JSON",
      "prettyPrint": true,
      "rules": [
        {
          "sourcePath": "$.name",
          "targetPath": "customer.name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        },
        {
          "sourcePath": "$.age",
          "targetPath": "customer.age",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }' | jq '.'












