#!/bin/bash
# Script for NetworkLatency Chaos Monkey

# Adds 1000ms +- 500ms of latency to each packet
tc qdisc add dev eth0 root latency delay 1000ms 500ms

