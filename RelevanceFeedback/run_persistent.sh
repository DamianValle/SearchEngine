#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d ../../SE/lab1/datasets/davisWiki/ -l ir20.png -p patterns.txt -ni
