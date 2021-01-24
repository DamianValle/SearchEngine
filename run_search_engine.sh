#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d datasets/davisWiki -l ir20.png -p patterns.txt
