#!/bin/bash

(jarsigner -verbose -keystore release-key.keystore -certs bin/.LevelSelectActivity-unsigned.apk release-key &&
 mv bin/.LevelSelectActivity-unsigned.apk bin/alienbloodbath-release.apk)
