#!/bin/bash
# Script for NullRoute Chaos Monkey

ip route add blackhole 10.0.0.0/8
