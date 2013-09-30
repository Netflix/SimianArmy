#!/bin/bash
# Script for FillDisk Chaos Monkey

# 65 GB should be enough to fill up all EC2 root disks!
nohup dd if=/dev/urandom of=/burn bs=1M count=65536 iflag=fullblock &
