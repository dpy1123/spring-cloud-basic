#!/usr/bin/env bash

if [ ! -d /tmp/consul-config ]; then
    mkdir /tmp/consul-config
fi

cp acl_config.json /tmp/consul-config/

nohup ./consul agent -ui -node=client-dd -data-dir /tmp/consul -config-dir=/tmp/consul-config > /tmp/consul.log 2>&1 &

./consul join 172.16.6.218

tail -f /tmp/consul.log