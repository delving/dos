#!/bin/bash

mkdir src
mkdir lib
../play/play deps
cd modules
rm -rf scala-0.9.1
ln -s ../../culturehub/module-extra/scala-head scala-head
cd ..
ant -Dplay.path=../play