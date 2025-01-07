#!/bin/bash

docker run \
    -it \
    --rm \
    -d \
    -v $(pwd):/app \
    -w /app \
    --name cst-package \
    openjdk:17-slim

docker exec cst-package apt update
docker exec cst-package apt install rpm fakeroot binutils -y

docker exec cst-package ./gradlew jpackage

docker exec -w /app cst-package chown -R $(id -u):$(id -g) build/

docker stop cst-package
