#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
API 转换测试脚本
使用方法: python test-api.py
"""

import requests
import json

API_URL = "http://localhost:8080/api/v2/transform"


def print_section(title):
    """打印章节标题"""
    print(f"\n{'=' * 50}")
    print(f"  {title}")
    print('=' * 50)


def transform(source_data, mapping_config):
    """调用转换接口"""
    request_body = {
        "sourceData": source_data,
        "mappingConfig": mapping_config
    }
    
    try:
        response = requests.post(API_URL, json=request_body, timeout=10)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"请求失败: {e}")
        return None


def test_json_to_json():
    """测试 JSON 到 JSON 转换"""
    print_section("测试 JSON 到 JSON 转换")
    
    source_data = json.dumps({
        "user": {
            "name": "张三",
            "age": 30
        }
    }, ensure_ascii=False)
    
    mapping_config = {
        "sourceProtocol": "JSON",
        "targetProtocol": "JSON",
        "prettyPrint": True,
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
    
    result = transform(source_data, mapping_config)
    if result and result.get("success"):
        print("转换成功:")
        print(result.get("transformedData"))
    else:
        print(f"转换失败: {result.get('errorMessage') if result else '未知错误'}")


def test_json_to_xml():
    """测试 JSON 到 XML 转换"""
    print_section("测试 JSON 到 XML 转换")
    
    source_data = json.dumps({
        "name": "张三",
        "age": 30
    }, ensure_ascii=False)
    
    mapping_config = {
        "sourceProtocol": "JSON",
        "targetProtocol": "XML",
        "prettyPrint": True,
        "xmlRootElementName": "user",
        "includeXmlDeclaration": True,
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
    
    result = transform(source_data, mapping_config)
    if result and result.get("success"):
        print("转换成功:")
        print(result.get("transformedData"))
    else:
        print(f"转换失败: {result.get('errorMessage') if result else '未知错误'}")


def test_xml_to_json():
    """测试 XML 到 JSON 转换"""
    print_section("测试 XML 到 JSON 转换")
    
    source_data = "<user><name>张三</name><age>30</age></user>"
    
    mapping_config = {
        "sourceProtocol": "XML",
        "targetProtocol": "JSON",
        "prettyPrint": True,
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
    
    result = transform(source_data, mapping_config)
    if result and result.get("success"):
        print("转换成功:")
        print(result.get("transformedData"))
    else:
        print(f"转换失败: {result.get('errorMessage') if result else '未知错误'}")


def test_many_to_one():
    """测试多对一映射"""
    print_section("测试多对一映射")
    
    source_data = json.dumps({
        "firstName": "张",
        "lastName": "三"
    }, ensure_ascii=False)
    
    mapping_config = {
        "sourceProtocol": "JSON",
        "targetProtocol": "JSON",
        "prettyPrint": True,
        "rules": [
            {
                "sourcePath": "$.firstName",
                "additionalSources": ["$.lastName"],
                "targetPath": "fullName",
                "mappingType": "MANY_TO_ONE",
                "transformType": "GROOVY",
                "transformConfig": {
                    "groovyScript": "def parts = input as List; return (parts[0] ?: '') + ' ' + (parts[1] ?: '')"
                }
            }
        ]
    }
    
    result = transform(source_data, mapping_config)
    if result and result.get("success"):
        print("转换成功:")
        print(result.get("transformedData"))
    else:
        print(f"转换失败: {result.get('errorMessage') if result else '未知错误'}")


if __name__ == "__main__":
    print("API 转换测试开始...")
    
    try:
        test_json_to_json()
        test_json_to_xml()
        test_xml_to_json()
        test_many_to_one()
        
        print_section("所有测试完成")
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
    except Exception as e:
        print(f"\n\n测试过程中发生错误: {e}")




