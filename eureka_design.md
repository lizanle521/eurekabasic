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
#### Peer to Peer架构
#### Zone以及Region 设计
#### SelfPreServation设计


