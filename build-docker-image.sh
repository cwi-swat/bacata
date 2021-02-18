#! /bin/sh

set -e;
mvn clean install

VERSION=` mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout`

docker image build --build-arg rascalNotebookVersion=${VERSION} -f rascal-notebook-docker/Dockerfile -t bacata:${VERSION} .
