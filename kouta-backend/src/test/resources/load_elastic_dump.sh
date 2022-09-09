#!/bin/sh

docker run --add-host=host.docker.internal:host-gateway --rm -v $(pwd)/src/test/resources/elastic_dump:/tmp elasticdump/elasticsearch-dump \
multielasticdump --direction=load --input=/tmp --output=$1 --includeType=$2
