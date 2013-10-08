#!/bin/bash
# Script for FailDynamoDb Chaos Monkey

# Block well-known Amazon DynamoDB API endpoints
echo "127.0.0.1 dynamodb.us-east-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.us-northeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.us-gov-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.us-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.us-west-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.sa-east-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.ap-southeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.ap-southeast-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 dynamodb.eu-west-1.amazonaws.com" >> /etc/hosts


