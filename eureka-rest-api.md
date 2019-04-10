| 方法 | 路径 | 解释 |
| ----- | -------| ------| 
|GET| http://localhost:8761/eureka/apps | 返回所有的注册的实例信息:curl -i http://localhost:8761/eureka/apps |
|GET| http://localhost:8761/eureka/apps/{appName} | 返回对应名字的注册的应用:curl -i http://localhost:8761/eureka/apps/CLIENT |
|POST| http://localhost:8761/eureka/apps/{appName} | 注册新的应用实例 |
|GET| http://localhost:8761/eureka/instances/{instanceId} | 返回对应名字对应实例ID的注册的应用 |
|DELETE| http://localhost:8761/eureka/apps/{appName}/{instanceId} | 删除对应名字的注册的应用实例 |
|PUT| http://localhost:8761/eureka/apps/{appName}/{instanceId} | 给对应名字的注册的应用实例发送心跳 |
|PUT| http://localhost:8761/eureka/apps/{appName}/{instanceId}/status?value=OUT_OF_SERVICE | 暂停实例 |
|DELETE| http://localhost:8761/eureka/apps/{appName}/{instanceId}/status?value=UP | 恢复实例 |
|PUT| http://localhost:8761/eureka/apps/{appName}/{instanceId}/metadata?key=value | 更新元数据 |
|GET| http://localhost:8761/eureka/vips/{vipAddress} | 根据vip地址查询 |
|GET| http://localhost:8761/eureka/svips/{svipAddress} | 根据svip地址查询 |


### rest-api实例
- 查询所有实例
```text
curl -i http://localhost:8761/eureka/apps
```
- 根据appId查询
```text
curl -i http://localhost:8761/eureka/apps/CLIENT
```
- 根据appId以及instanceId查询
```text
curl -i http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081
```
- 根据instanceId查询
```text
curl -i http://localhost:8761/eureka/instances/192.168.1.7:client:8081
```
- 注册新的应用实例
```text
curl -i -H "Content-Type:appliation/xml" -H "Content-Length:773" -H "Accept-Encoding:gzip"
 -X POST -d '<instance>
               <instanceId>192.168.1.7:client:8082</instanceId>
               <hostName>192.168.1.7</hostName>
               <app>CLIENT</app>
               <ipAddr>192.168.1.7</ipAddr>
               <status>UP</status>
               <overriddenstatus>UNKNOWN</overriddenstatus>
               <port enabled="true">8082</port>
               <securePort enabled="false">443</securePort>
               <countryId>1</countryId>
               <dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
                 <name>MyOwn</name>
               </dataCenterInfo>
               <leaseInfo>
                 <renewalIntervalInSecs>30</renewalIntervalInSecs>
                 <durationInSecs>90</durationInSecs>
                 <registrationTimestamp>1554904097829</registrationTimestamp>
                 <lastRenewalTimestamp>1554906318637</lastRenewalTimestamp>
                 <evictionTimestamp>0</evictionTimestamp>
                 <serviceUpTimestamp>1554904067802</serviceUpTimestamp>
               </leaseInfo>
               <metadata>
                 <management.port>8081</management.port>
               </metadata>
               <homePageUrl>http://192.168.1.7:8082/</homePageUrl>
               <statusPageUrl>http://192.168.1.7:8082/actuator/info</statusPageUrl>
               <healthCheckUrl>http://192.168.1.7:8082/actuator/health</healthCheckUrl>
               <vipAddress>client</vipAddress>
               <secureVipAddress>client</secureVipAddress>
               <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
               <lastUpdatedTimestamp>1554904097829</lastUpdatedTimestamp>
               <lastDirtyTimestamp>1554904097825</lastDirtyTimestamp>
               <actionType>ADDED</actionType>
             </instance>' http://localhost:8761/eureka/apps/client2

```
- 注销应用实例
```text
curl -i -X DELETE http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081
```

- 暂停实例
```text
curl -i -X PUT http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081/status?status=OUT_OF_SERVICE
```

- 恢复实例
```text
curl -i -X DELETE http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081/status
```

- 应用实例发送心跳
```text
curl -i -X PUT http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081
```

- 修改应用实例元数据
```text
curl -i -X PUT http://localhost:8761/eureka/apps/CLIENT/192.168.1.7:client:8081/metadata?profile=canary
```


