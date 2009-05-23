#!/bin/bash

echo "Generating the content epoch..."
./generate_epoch.sh

echo "Cleaning build..."
rm -rf bin

echo "Installing..."
ant reinstall
