#!/bin/bash

TARGET="res/raw/content_package.zip"
FILES=`find -L content_package | grep -v \~ | grep -v \.svn | grep -v \.drop`
rm $TARGET
zip $TARGET $FILES
