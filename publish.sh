#!/bin/bash

cp server/app/build/outputs/apk/app-release.apk ../medv3d/trunk/reg/doc/www/semaforky/semaforky-1.0.0.apk
svn commit -m "Publish new version of semaforky" ../medv3d/trunk/reg/doc/www/semaforky/semaforky-1.0.0.apk
