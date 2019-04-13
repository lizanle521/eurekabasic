### eureka实例信息描述
```text
<instance>
//实例ID
<instanceId>192.168.1.7:client:8081</instanceId>
//主机名
<hostName>192.168.1.7</hostName>
//应用名
<app>CLIENT</app>
//ip地址
<ipAddr>192.168.1.7</ipAddr>
//实例状态，例如UP,DOWN,STARTING,OUT_OF_SERVICE,UNKNOW
<status>UP</status>
//外界需要强制覆盖的值
<overriddenstatus>UNKNOWN</overriddenstatus>
//端口号
<port enabled="true">8081</port>
//https端口号
<securePort enabled="false">443</securePort>
//被废弃属性
<countryId>1</countryId>
//数据中心信息，Netflix 或者 Amazon 或者 MyOwn
<dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
<name>MyOwn</name>
</dataCenterInfo>
<leaseInfo>
// client端续约的间隔周期
<renewalIntervalInSecs>30</renewalIntervalInSecs>
// client端需要设定的租约的有效时长
<durationInSecs>90</durationInSecs>
//server端设置的该租约的第一次注册时间
<registrationTimestamp>1554907353407</registrationTimestamp>
//server端设置的该租约的最后一次注册时间
<lastRenewalTimestamp>1554907459035</lastRenewalTimestamp>
//server端设置的该租约被剔除的时间
<evictionTimestamp>0</evictionTimestamp>
//server端设置的该实例标记为UP的时间
<serviceUpTimestamp>1554904067802</serviceUpTimestamp>
</leaseInfo>
<metadata>
<management.port>8081</management.port>
<profile>canary</profile>
</metadata>
//应用实例的首页
<homePageUrl>http://192.168.1.7:8081/</homePageUrl>
//应用实例的状态页url
<statusPageUrl>http://192.168.1.7:8081/actuator/info</statusPageUrl>
//应用实例健康检查的url
<healthCheckUrl>http://192.168.1.7:8081/actuator/health</healthCheckUrl>
//虚拟IP地址
<vipAddress>client</vipAddress>
//https的虚拟IP地址
<secureVipAddress>client</secureVipAddress>
//首先标示是否是discoveryServer，其次标示该discoveryServer是否是响应你请求的实例
<isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
//状态最后更新时间
<lastUpdatedTimestamp>1554907353407</lastUpdatedTimestamp>
//实例信息最新的过期时间，在client端用于标示该实例信息是否与EurekaServer一致
//在server端用于多个Eureka Server之间的信息同步处理
<lastDirtyTimestamp>1554904097825</lastDirtyTimestamp>
//标示server对该实例执行的操作，包括added，modified，deleted三类
<actionType>ADDED</actionType>
</instance>
```