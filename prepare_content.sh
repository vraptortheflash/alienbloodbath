#!/bin/bash

./create_demo.sh

TARGET="res/raw/content_package.zip"
EPOCH="content_package/epoch.txt"

ALL_FILES=`find -L content_package | grep -v \~ | grep -v \.svn | grep -v \.drop`
DEMO_FILES=`find -L content_package | grep -v \~ | grep -v \.svn | grep -v \.drop | grep -v Classic | grep -v The_Second_Wave`

CONTENT_FILES=$DEMO_FILES

rm $TARGET
rm $EPOCH
cat $CONTENT_FILES | sha1sum > $EPOCH
zip $TARGET $CONTENT_FILES $EPOCH
