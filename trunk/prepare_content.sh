#!/bin/bash

TARGET="res/raw/content_package.zip"
FILES=`find -L content_package | grep -v \~ | grep -v \.svn | grep -v \.drop`
DEMO_FILES=`find -L content_package | grep -v \~ | grep -v \.svn | grep -v \.drop | grep -v Classic | grep -v The_Second_Wave`
rm $TARGET
zip $TARGET $DEMO_FILES
