#! /bin/sh

mvn clean install
docker image build -f rascal-notebook-docker/Dockerfile -t bacata:unstable .
