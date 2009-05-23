#!/bin/bash

./create_demo.sh

EPOCH="assets/epoch.txt"

ALL_FILES=`find -L assets | grep -v \~ | grep -v \.svn | grep -v \.drop`

rm $EPOCH
cat $ALL_FILES | sha1sum > $EPOCH
