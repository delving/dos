#!/bin/bash

mkdir src
mkdir lib
../play/play deps
cd modules
ln -s ../../culturehub/module-extra/scala-head scala-head
cd ..
ant -Dplay.path=../play