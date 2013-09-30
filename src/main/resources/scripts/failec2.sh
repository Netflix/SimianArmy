#!/bin/bash
# Script for FailEc2 Chaos Monkey

# Block well-known Amazon EC2 API endpoints
echo "127.0.0.1 ec2.us-east-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.us-northeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.us-gov-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.us-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.us-west-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.sa-east-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.ap-southeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.ap-southeast-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 ec2.eu-west-1.amazonaws.com" >> /etc/hosts


