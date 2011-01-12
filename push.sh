#!/bin/bash

set -x

version=$(egrep -o "[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?" project.clj  | head -1)
jar=fs-${version}.jar

if [ -f $jar ]; then
    rm $jar
fi
lein jar
lein pom
scp pom.xml $jar clojars@clojars.org:
