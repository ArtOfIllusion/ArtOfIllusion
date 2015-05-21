#!/bin/sh

memory=$(cat /proc/meminfo | head -n 1 | awk '{print $2}')
mem=$(($memory/1100))
echo $mem
