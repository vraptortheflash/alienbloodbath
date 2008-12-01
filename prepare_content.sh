#!/bin/bash

TARGET="res/raw/content_package.zip"
FILES=`find content_package | grep -v \~ | grep -v \.svn`
rm $TARGET
zip $TARGET $FILES
