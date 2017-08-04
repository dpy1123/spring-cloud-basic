#!/usr/bin/env bash

mkdir /tmp/consul-config

nohup ./consul agent -node=client-dd -data-dir /tmp/consul -config-dir=/tmp/consul-config > /tmp/consul.log 2>&1 &

./consul join 172.16.6.218

tail -f /tmp/consul.log