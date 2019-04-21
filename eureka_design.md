### 服务的核心操作
对于服务发现来说，围绕服务实例主要有如下几个重要的操作：
- 服务注册 register
- 服务下线 cancel
- 服务租约 renew
- 服务剔除 evict

围绕这几个功能，eureka设计了这几个核心操作类
- com.netflix.eureka.lease.LeaseManager.java
- com.netflix.discovery.shared.LookupService.java
- com.netflix.eureka.registry.InstanceRegistry.java
- com.netflix.eureka.registry.AbstractInstanceRegistry.java
- com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.java

Spring Cloud Eureka 在 Netflix Eureka的基础上，抽象定义了如下几个核心类
- org.springframework.cloud.netflix.eureka.server.InstanceRegistry.java
- org.springframework.cloud.client.serviceregistry.ServiceRegistry.java

- org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry.java
- org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration.java
- org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.java
- org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.java
- org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean.java

其中LeaseManager以及LookupService是Eureka关于服务发现相关操作定义的接口类，前者定义了
服务写操作的相关方法，后者定义了查询操作相关的方法。

#### LeaseManager
LeaseManager接口定义了应用服务实例在服务中心的几个操作方法：register，cancel，renew，evict。
接口源码如下：
```java
public interface LeaseManager<T> {
    // 用于注册服务实例信息
    void register(T var1, int leaseDuration, boolean isReplication);
    // 用于删除服务实例信息
    boolean cancel(String appName, String id, boolean isReplication);
    // 用于与eureka server进行心跳操作，维持租约
    boolean renew(String appName, String id, boolean isReplication);
    // 用于剔除租约过期的服务实例
    void evict();
}
```

#### LookupService
LookupService接口定义了Eureka Client从服务中心获取服务实例的查询方法
```java
public interface LookupService<T> {
    Application getApplication(String var1);

    Applications getApplications();

    List<InstanceInfo> getInstancesById(String var1);

    InstanceInfo getNextServerFromEureka(String virtualHostName, boolean secure);
}
```
这个接口主要是给client端调用的，其定义了获取所有应用信息，根据应用ID获取所有服务实例，以及根据virtualHostName 和 round-robin方式
获取下一个服务实例的方法

### Eureka设计理念
作为一个服务中心，主要解决以下几个问题
- 服务如何注册到服务中心
- 服务实例如何剔除
- 服务信息的一致性问题

#### 服务发现中 AP 优于 CP
在实际生产环境中，服务注册以及发现中心保留可用以及过期的数据总比丢掉可用的数据好。
这样的化，应用实例的注册信息在集群中的所有节点并不是强一致性的。这就需要客户端能够支持负载均衡以及失败重试。
#### Peer to Peer架构
副本之间部分主从，任何副本都可以接受写操作，然后每个副本之间相互进行数据的更新。
由于每个副本都可以接受写请求，不存在写操作压力瓶颈。但是由于每个副本都可以写，各个副本之间的数据同步以及冲突处理是一个比较棘手的问题
#### 客户端
客户端通过如下配置eureka 的 peer节点：
```text
eureka:
    client:
        serviceUrl:
            defaultZone:
                http://host:port/eureka/

```
实际代码里支持preferSameZoneEureka,即有多个分区的化，优先选择与应用实例所在分区一样的其他服务的实例，如果没有找到则默认defaultZone.
客户端使用quarantineSet维护一个不可用的eureka server列表，进行请求的时候，优先从可用列表中进行选择。如果请求失败则切换到下一个server
默认重试次数等于3。
另外为了防止每个client端都按配置文件制定的顺序进行请求造成eureka server节点请求分布不均衡的情况，client端有一个定时任务，每5分钟执行一次，
属性并随机化server 列表。
#### 服务端
eureka server本身以来的eureka client,也就是说每个eureka server 是作为其他server的client。 在单个eureka server启动的时候，会有一个
SyncUp的操作，通过uereka client请求其他eureka server节点中的一个节点获取注册的应用实例信息，然后复制到其他peer节点。
eureka server在 在执行复制操作的时候，使用HEAD_REPLICATION 的http header 来将这个请求与不同应用实例的正常请求操作区分开来，表明这是一个
复制请求，这样其他peer节点收到请求的时候，就不会再对其他peer节点接受到请求的时候，就不会再对他的peer节点进行复制操作，从而避免死循环。
由于采用peer to peer的复制模式，重点要解决的是数据复制的冲突问题，针对这个问题，eureka采用如下两个方式来解决：
- lastDirtyTimeStamp 标识
- heartbeat 心跳
针对数据的不一致，通常通过版本号来解决。最后在不同的副本之间只需要判断请求复制数据的版本号 和 本地数据的版本号高低就可以了。eureka没有直接使用
版本号属性，而是采用一个叫做lastDirtyTimeStamp的字段来对比。
如果开启`SyncWhenTimestampDiffers`配置（默认开启），当lastDirtyTimeStamp部位空的时候，就会进行相应的处理
- 如果请求参数的lastDirtyTimeStamp值大于server本地实例，标示eureka之间的数据出现冲突，返回404，要求应用实例重新注册
- 如果请求参数的lastDirtyTimeStamp值小雨server本地实例，且是peer节点的复制请求，标示数据冲突，返回409给peer节点，要求其同步自己最新的数据信息。
peer节点的相互复制并不能保证所有操作都能成功，如果发现应用实例数据和某个server的数据不一致，应用实例需要重新进行注册
#### Zone以及Region 设计
由于netflix的服务大部分在amazon上，因此eureka的设计有一部分也是基于amazon的zone以及region的基础设施之上。
amazone ec2托管在全球的各个地方，他用region来代表一个独立的地理区域。在每个region下边，设置了多个availabilityZone，一个region对应多个zone.
每个region之间是相互独立以及隔离的，默认情况下资源只在单个region之间的zone进行复制，跨region不会进行资源复制。
zone可以堪称region下边的机房，主要是为了region的高可用设计，当同一个region下边的zone不可用时，还有其他zone可用。由于不同的region不可以进行
资源复制，所以eureka 的高可用是region下边的高可用
eureka client支持preferSameZone，获取eureka server的serviceUrl优先拉取和应用实例处于同一个zone的eureka server地址列表。
一个zone下边可以设置多个eureka server实例，构成peer节点，节点采用peer to peer复制
netflix的ribbon组建针对多个zone提供了zoneAffinity支持，允许客户端路由或者网关路由时，优先园区与自身处于同一个zone的服务实例

#### SelfPreServation设计
在分布式系统设计里边，通常需要对应用的存活进行健康检查，这里比较关键的问题就是处理好网络偶尔抖动 或者 短暂不可用造成的误判。
另外eureka server端与client端 之间如果出现网络分区，极端情况下可能使得eureka server清空部分服务的实例列表，严重影响到uereka server的可用性。
因此，eureka server 引入了 selfpreservation机制
client 和 server端 之间有一个租约，client要定时发送心跳来维持这个租约，标示自己还活着，uereka 通过当前注册的实例数，去计算每分钟应该从应用实例
接受到的心跳数。如果最近一分钟收到的续约次数小于制定的阈值的化，关闭租约失效剔除，禁止定时任务删除失效的实例，保护注册信息

### eureka 参数调优以及监控
#### 核心参数
主要分为client端 和 server两大类来建树以下eureka的几个核心参数
##### client端
client端的参数分为基本参数  ，定时任务参数，http参数 三大类来梳理
1）基本参数如下表

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.client.availability-zones||告知client有些region以及availability-zones,支持配置修改运行时生效|
|eureka.client.filter-only-instances| true| 是否过滤出InstanceStatus 为 up的实例|
|eureka.client.region|us-east-1|指定该应用实例所在的region，asw datacenter适用|
|eureka.client.register-with-eureka|true|是否将应用实例注册到eureka server|
|eureka.client.prefer-same-zone-eureka|true|是否优先使用与该实例处于同zone的eureka server|
|eureka.client.on-demand-upate-status-change|true|是否将本地实例状态的更新通过ApplicationInfoManager实时触发同步到eureka server|
|eureka.instance.metadata-map||指定应用实例的元数据信息|
|eureka.instance.prefer-ip-address|false|是否优先使用ip地址来代替host name作为实例的hostName字段值|
|eureka.instance.lease-expiration-duration-in-seconds|90|指定eureka client间隔多久需要向 server发送心跳告知server实例还存活|

2）定时任务参数如下表

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.client.cache-refresh-executor-thread-pool-size|2|刷新缓存的CacheRefreshThread的线程池大小|
|eureka.client.cache-refresh-executor-exponential-back-off-bound|10|调度任务执行超时时下次的调度延时时间|
|eureka.client.heartbeat-executor-thread-pool-size|2|心跳线程HeartbeatThread的线程池大小|
|eureka.client.heartbeat-executor-exponential-back-off-bound|10|调度任务执行超时时下次的调度延时时间|
|eureka.client.registry-fetch-interval-seconds|30|CacheRefreshThread线程的调度频率|
|eureka.client.eureka-service-url-poll-interval-seconds|5*60|AsyncResolver.updateTask刷新Eureka server地址的时间间隔|
|eureka.client.initial-instance-info-replication-interval-seconds|40|InstanceInfoReplicator将实例信息变更同步到eureka serve的初始延长时间|
|eureka.client.initial-info-replication-interval-seconds|30|InstanceInfoReplicator将实例信息变更同步到eureka serve的时间间隔|
|eureka.instance.lease-renewal-interval-in-seconds|30|eureka client向 eureka server发送心跳的时间间隔|

3）http参数，eureka client底层 http client 与 eureka server通信，提供的参数如下表

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.client.eureka-server-connect-timeout-seconds|5|连接超时时间|
|eureka.client.eureka-server-read-timeout-seconds|8|读超时时间|
|eureka.client.eureka-server-total-connections|200|连接池最大活动连接数|
|eureka.client.eureka-server-total-connections-per-host|50|每个host能使用的最大连接数|
|eureka.client.eureka-connection-idle-timeout-seconds|30|连接池中连接的空闲时间|

##### server端
server端 的参数分为如下几类，基本参数，response cache参数，peer相关参数，http参数

1）基本参数

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.server.enable-self-preservation|true|是否开启自我保护模式|
|eureka.server.renewal-percent-threshold|0.85|每分钟需要收到的续约次数的阈值|
|eureka.instance.registry.expected-number-of-renews-per-min|1|指定的每分钟需要收到的续约次数值，实际该值被写死为count*2，另外也会被更新|
|eureka.server.renewal-threshold-update-interval-ms|15min|指定updateRenewalThreshold定时任务的调度频率，来动态更新expectedNumberOfRenewsPerMin 及 numberOfRenewsPerMinThreshold值|
|eureka.server.eviction-interval-timer-in-ms|60*1000|指定EvictionTask定时任务的调度频率，用于剔除过期的实例|

2）response cache
eureka server为了提升自身的REST API接口的性能，提供了两个缓存，一个是基于ConcurrentMap的readOnlyCacheMap，一个是基于Guava Cache的 
readWriteCacheMap。参数表如下

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.server.use-read-only-response-cache|true|是否使用制度的response-cache|
|eureka.server.response-cache-update-interval-ms|30*1000|设置CacheUpdateTask的调度时间间隔，用于readWriteCacheMap更新数据到readOnlyCacheMap|
|eureka.server.reponse-cache-auto-expiration-in-seconds|180|设置readWriteCacheMap的expireAfterWrite参数，指定写入多久后过期|


3）peer参数


| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.server.peer-eureka-nodes-update-interval-ms|10min|指定peersUpdateTask调度的时间间隔，用于配置文件刷新peerEurekaNodes节点的配置信息|
|eureka.server.peer-eureka-status-refresh-time-interval-ms|30*1000|指定更新peer nodes状态信息的时间间隔（目前没有看到代码中有使用）|

4）http参数

| 参数 | 默认值 | 说明 |
| ----- | ----- | -------- |
|eureka.server.peer-node-connect-timeout-ms|200|连接超时时间|
|eureka.server.peer-node-read-timeout-ms|200|读超时时间|
|eureka.server.peer-node-total-connections|1000|连接池最大活动连接数|
|eureka.server.peer-node-total-connections-per-host|5000|每个host能使用的最大连接数|
|eureka.server.peer-node-connection-idle-timeout-seconds|30|连接池中连接的空闲时间|

#### 参数调优
1. 常见问题
对于新接触eureka的开发人员来说，一般会有几个困惑
- 为什么服务下线了，eureka server接口返回的信息还会存在
- 为什么服务上线了，eureka client不能及时获取到
- 为什么有时候会出现 EMEGNENCY 的提示

2. 解决方案
对于第一个问题，eureka server并不是强一致性的，因此registy 会保留过期的实例信息，这里又分为几个原因：
- 应用实例异常挂掉，没能在挂掉之前告诉eureka server要下线掉该服务实例信息。就需要以来Eureka server的Eviction Task去剔除
- 应用实例下线时有告诉Eureka Server下线，但是由于Eureka server的REST API有response cache，因此需要等待缓存过期才能更新。
- eureka server由于开启并引入了SELF PRESERVATION 模式，导致registry的信息不会因为过期而被剔除，直到退出该模式

针对client下线没有通知eureka server的问题，可以调整 Eviction Task的调度频率，比如将下边配置将调度间隔从默认的60秒，调整为5秒：
```text
eureka.server.eviction-interval-timer-in-ms=5000
```
针对第二个reponse cache，可以根据情况考虑关闭readonlyCacheMap:
```text
eureka.server.use-read-only-response-cache=false
```
活着调整readWriteCacheMap过期时间：
```text
eureka.server.reponse-cache-auto-exporation-in-seconds=60
```
针对SELF PRESERVATION的问题，在测试环境中可以将enable-self-preservation设置为false:
eureka.server.enable-self-preservation=false

针对新服务上线，eureka client获取信息不及时的问题，在测试环境中，可以适当提高client端 拉取server注册信息的频率，例如下面的默认30秒改5秒：
```text
eureka.client.registry-fetch-interval-seconds=5
```

在生产环境中，可以把renewalPercentThreshold 以及 leaseRenewalIntervalInSeconds参数调小一点，进而提高触发 SELF PRESERVATION机制的
门槛。

