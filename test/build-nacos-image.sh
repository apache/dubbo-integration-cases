#!/bin/bash

mkdir nacos-mysql
curl -o nacos-mysql/Dockerfile https://raw.githubusercontent.com/nacos-group/nacos-docker/master/example/image/mysql/8/Dockerfile
docker build -t nacos-mysql:8.0.31 nacos-mysql/
