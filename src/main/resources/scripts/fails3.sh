#!/bin/bash
# Script for FailS3 Chaos Monkey

# See http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region

echo "127.0.0.1 s3.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-external-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-us-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-us-west-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-eu-west-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-ap-southeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-ap-southeast-2.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-ap-northeast-1.amazonaws.com" >> /etc/hosts
echo "127.0.0.1 s3-sa-east-1.amazonaws.com" >> /etc/hosts

