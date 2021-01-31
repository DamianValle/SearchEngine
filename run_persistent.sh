#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d dummy_dataset/ -l ir20.png -p patterns.txt -ni
