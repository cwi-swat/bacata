#! /bin/sh

VERSION=` mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout`

docker run --rm  -p 8888:8888 -p 8000:8000 -p 9050-9100:9050-9100 --name bacata-${VERSION} bacata:${VERSION}
