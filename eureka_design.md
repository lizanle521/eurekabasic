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

#### SelfPreServation设计


