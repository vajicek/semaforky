#!/bin/bash

set -e

# checkout and update
[[ -d "www" ]] || svn co svn://cgg.mff.cuni.cz/MedV3D/trunk/reg/doc/www www
svn update www

# copy and commit
cp server/app/build/outputs/apk/release/app-release.apk www/semaforky/semaforky-1.0.0.apk
svn commit -m "Publish new version of semaforky" www/semaforky/semaforky-1.0.0.apk
