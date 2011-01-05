#!/bin/bash

lein jar
lein pom
scp pom.xml fs*.jar clojars@clojars.org:
