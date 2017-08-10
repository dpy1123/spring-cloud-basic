# consul-client

### consul-server
1.搭建服务端集群环境  
搭建一个3节点集群, 172.16.6.112, 172.16.6.113, 172.16.6.218, 其中在218上enable web-ui  
* setup 218 
```bash
nohup ./consul agent -server -ui -bootstrap-expect=3 -data-dir=/tmp/consul \
-node=agent-218 -bind=172.16.6.218 -enable-script-checks=true \
-config-dir=./consul_conf.d -client=0.0.0.0 > /tmp/consul.log 2>&1 &
```
* setup 112&113 
```bash
nohup ./consul agent -server -data-dir=/tmp/consul -node=agent-112 \
-bind=172.16.6.112 -enable-script-checks=true -config-dir=./consul_conf.d \
-client=0.0.0.0 > /tmp/consul.log 2>&1 &
```
* 在112&113上执行join命令，加入到218所在集群
```bash
./consul join 172.16.6.218
```

2.check集群环境  
```bash
 ./consul members
```
```bash
Node       Address            Status  Type    Build  Protocol  DC
agent-112  172.16.6.112:8301  alive   server  0.9.0  2         dc1
agent-113  172.16.6.113:8301  alive   server  0.9.0  2         dc1
agent-218  172.16.6.218:8301  alive   server  0.9.0  2         dc1
```

### consul-client
![arc](http://images2015.cnblogs.com/blog/510/201701/510-20170114220134916-652497084.png)
* Client——一个Client是一个转发所有RPC到server的代理。这个client是相对无状态的。client唯一执行的后台活动是加入LAN gossip池。这有一个最低的资源开销并且仅消耗少量的网络带宽。  
* Server——一个server是一个有一组扩展功能的代理，这些功能包括参与Raft选举，维护集群状态，响应RPC查询，与其他数据中心交互WAN gossip和转发查询给leader或者远程数据中心。  

因此每个应用server上需要起一个以client方式运行的consul-agent，在本例中通过/src/main/bash/run_xx.sh可以启动client并加入服务端的集群。  

### 启用acl
1.首先在server端启动的时候，conf目录下放入acl_config.json，其中只有218配置acl_token，即只能通过218来管理，其他server节点的members和info命令将看不到集群信息。  
2.客户端的conf目录下，放置客户端的acl配置文。客户端的acl_token以及该token对应的权限需要先在server上配置。  

