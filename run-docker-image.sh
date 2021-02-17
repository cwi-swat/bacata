#! /bin/sh

docker run --rm  -p 8888:8888 -p 8000:8000 -p 9050-9100:9050-9100 --name bacata  bacata:unstable
