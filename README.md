# hazelcast-docker-swarm-discovery-spi

[![Build Status](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi.svg?branch=master)](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi)

Provides a Docker Swarm mode based discovery strategy for Hazlecast 3.6+ enabled applications.
This is an easy to configure plug-and-play Hazlecast DiscoveryStrategy that will enable Hazelcast applications to dynamically discover one another when deployed as Docker Swarm services.

* [Status](#status)
* [Releases](#releases)
* [Requirements](#requirements)
* [Maven/Gradle install](#mavengradle)
* [How it works](#howitworks)
* [Features](#features)
* [Usage](#usage)
* [Build from source](#building)
* [Unit tests](#tests)
* [Related Info](#related)
* [Todo](#todo)
* [Troubleshooting](#troubleshooting)

![Diagram of hazelcast docker swarm discovery strategy](/docs/diag1.png "Diagram1")

## <a id="status"></a>Status

This is release candidate code, tested against Hazelcast 3.6-EA+ through 3.9.x Stable releases. See **Releases** below for compatible jars. For use only on Docker 1.12+ "swarm mode" environments.

**IMPORTANT:**: Do not rely on JCenter/Bintray anymore! Update your gradle/maven dependencies to use Maven Central: https://search.maven.org/search?q=g:org.bitsofinfo

## <a id="releases"></a>Releases

### For Hazelcast 3.9+ only (see below for <= 3.8)

* MASTER - in progress, this README refers to what is in the master tag. **Switch to relevant RELEASE tag above to see that version's README**

* **1.0-RC14-20210205**: Same as `1.0-RC14` but made compliant for Maven Central due to JCenter/Bintray closure. 

* [1.0-RC14](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC14) Additional configurable properties (`strict-docker-service-name-comparison / strictDockerServiceNameComparison`) to optionally add a strict "equals" check against names of services returned from docker; docker itself returns services based on a "startsWith" check.

* [1.0-RC13](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC13) Additional configurable properties (`log-all-service-names-on-failed-discovery / logAllServiceNamesOnFailedDiscovery`) to optionally log (FINE) all available docker service names if no containers can be discovered via configured criteria. Better logging to provide the context by which `SwarmDiscoveryUtil` is being utilized.

* [1.0-RC12](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC12) Additional debug logging

* [1.0-RC11](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC11) Added `java.util.Properties` based constructor for `SwarmMemberAddressProvider`

* [1.0-RC10](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC10) Better logging in: `DockerDNSRRMemberAddressProvider` for https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/issues/25

* [1.0-RC9](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC9) Better NPE handling for invalid/null Tasks returned from service spec or no network attachments

* [1.0-RC8](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC8): Incorporated PRs #18 (adjust depedencies declaration), #17

* [1.0-RC7](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC7): Incorporated PRs #14 (initial scan self check), #15 (docker service names optional)

* [1.0-RC6](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC6): Added support for swarm dnsrr based discovery, thanks [Cardds](https://github.com/Cardds), https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/pull/10

* [1.0-RC5](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC5): Added support for SSL swarm manager URIs and skip verify for SSL.

* [1.0-RC4](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC4): Changed gradle dependencies for HZ `3.9.+` & spotify docker-client for `8.+`. Implemented new `MemberAddressProvider` SPI, as alternative option to using `SwarmAddressPicker`

### For Hazelcast 3.8 and below

* [1.0-RC3](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC3): Improved SwarmAddressPicker constructor [PR #6](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/pull/6)

* [1.0-RC2](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC2): Excludes stopped tasks [#2](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/issues/2)

* [1.0-RC1](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC1): Initial release

## <a id="requirements"></a>Requirements

* Java 7+
* [Hazelcast 3.6+](https://hazelcast.org/)
* [Docker 1.12+ Swarm Mode](https://docs.docker.com/engine/swarm/) with one or more swarm manager nodes listening on a `tcp://` socket

## <a id="mavengradle"></a>Maven/Gradle

To use this discovery strategy in your Maven or Gradle project use the dependency samples below.

### Gradle:

```
repositories {
    mavenCentral()
}

dependencies {
  // <!-- Use 1.0.RC3 for Hazelcast < 3.8.x -->
  compile 'org.bitsofinfo:hazelcast-docker-swarm-discovery-spi:1.0-RC14-20210205'
}
```

### Maven:

```
<dependencies>
    <dependency>
        <groupId>org.bitsofinfo</groupId>
        <artifactId>hazelcast-docker-swarm-discovery-spi</artifactId>
        <version>1.0-RC14-20210205</version> 
    </dependency>
</dependencies>
```

## <a id="features"></a>Features

* Will permit Hazelcast instances deployed on a Docker 1.12+ Swarm to automatically discover and connect with one another

## <a id="howitworks"></a>How it works

Hazelcast applications that use this discovery SPI will discover one another when deployed as Docker services in the following way.

* Launch your docker service with its target overlay network name and either its service name or service labels. In addition, specify additional ENVIRONMENT variables via `-e` named `dockerNetworkNames` and optionally `dockerServiceNames` and `dockerServiceLabels`. You should have at least one of `dockerServiceNames` or `dockerServiceLabels` defined. These variables will be consumed by the discovery SPI. The `DOCKER_HOST` environment variable for the container should also be set to a name that resolves to one or more swarm manager nodes, via the format `tcp://`, `http://` or `https://`

* The Docker Swarm Discovery SPI consumes from the `DOCKER_HOST`, `dockerNetworkNames`, optionally `dockerServiceNames`, `dockerServiceLabels` and `hazelcastPeerPort` and begins the following process.

    1. Leverages the custom `MemberAddressProvider` SPI implementation (`SwarmMemberAddressProvider`) or `SwarmAddressPicker` hack (read below!) to talk to the *$DOCKER_HOST* `/networks`, `/services` and `/tasks` APIs to determine the current node's IP address on the docker network, and bind hazelcast on `hazelcastPeerPort` to that address.

    2. Next hazelcast invokes the SPI `discoverMembers()` to determine all peer docker service tasks (containers) ip addresses and attempts to connect to them to form the cluster connecting to the configured `hazelcastPeerPort` (default 5701)

Alternatively, discovery can be limited to the bounds of the docker cluster using [docker endpoint_mode dnsrr](https://docs.docker.com/engine/swarm/networking/#configure-service-discovery).

* Launch your docker service with its service name and target overlay network name with `endpoint_mode` set to `dnsrr`. In addition, specify additional ENVIRONMENT variables via `-e` named `serviceName`, `servicePort`, and `peerServicesCsv`

    1. Leverages the custom `MemberAddressProvider` SPI implementation (`DockerDNSRRMemberAddressProvider`) to resolve the DNS entries for its own service, `serviceName`, and port, `servicePort`, to IP addresses within the docker network.

    2. Next, hazelcast invokes the SPI DiscoveryStrategy to resolve the internal docker DNS entries for all services that the hazelcast instance should consider its peers, and connects to them over the ports specified in `peerServicesCsv` (default 5701)

    *Note: `peerServicesCsv` must contain a reference to the `serviceName` and `servicePort`, if the service is meant to cluster with itself*

## <a id="usage"></a>Usage

Ensure your project has the `hazelcast-docker-swarm-discovery-spi` artifact dependency declared in your maven pom or gradle build file as described above. Or build the jar yourself and ensure the jar is in your project's classpath.

### Option 1: Local network binding via  SwarmMemberAddressProvider

*Note this is only available in RC4+ and only for Hazelcast 3.9+. apps*

Configure your hazelcast.xml configuration file to use the `DockerSwarmDiscoveryStrategy` and `SwarmMemberAddressProvider` (similar to the below): [See hazelcast-docker-swarm-discovery-spi-example-member-address-provider.xml](src/main/resources/hazelcast-docker-swarm-discovery-spi-example-member-address-provider.xml) for an example with documentation of options.


```
Config conf =new ClasspathXmlConfig("yourHzConfig.xml");

HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
        .newHazelcastInstance(conf,"myAppName",new DefaultNodeContext());

```

### Option 2: Swarm DNSRR network binding via DockerDNSRRMemberAddressProvider

*Note this is only available in RC6+ and only for Hazelcast 3.9+. apps*

Configure your hazelcast.xml configuration file to use the `DockerDNSRRDiscoveryStrategy` and `DockerDNSRRMemberAddressProvider` (similar to the below): [See hazelcast-docker-swarm-dnsrr-discovery-spi-example.xml](src/main/resources/hazelcast-docker-swarm-dnsrr-discovery-spi-example.xml) for an example with documentation of options.

```
Config conf =new ClasspathXmlConfig("yourHzConfig.xml");

HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
        .newHazelcastInstance(conf,"myAppName",new DefaultNodeContext());

```

### Option 3: Local network binding via SwarmAddressPicker

*Note this the preferred method for Hazecast <= 3.8.x apps*

Configure your hazelcast.xml configuration file to use the `DockerSwarmDiscoveryStrategy` (similar to the below): [See hazelcast-docker-swarm-discovery-spi-example-address-picker.xml](src/main/resources/hazelcast-docker-swarm-discovery-spi-example-address-picker.xml) for an example with documentation of options.


```
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker;
...

Config conf =new ClasspathXmlConfig("yourHzConfig.xml");

NodeContext nodeContext = new DefaultNodeContext() {
    @Override
    public AddressPicker createAddressPicker(Node node) {
        return new SwarmAddressPicker(new ILogger() {
            // you provide the impl... or use provided "SystemPrintLogger"
        });
    }
};

HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
        .newHazelcastInstance(conf,"myAppName",nodeContext);

```

### If a local network binding option is chosen, proceed with the below:

* Create a Docker image for your application that uses Hazelcast

* Create an overlay network for your service, `docker network create -d overlay [name]`

* Launch your services via `docker service create` against your Docker Swarm cluster:

Note this example command assumes an entrypoint script exists that execs the `java` command. Your *DOCKER_HOST* must be accessible over http (i.e. daemons listening on the *tcp://* socket

NOTE! All `-D` java System properties below can be omitted and alternatively defined within the `<member-address-provider>` Hazelcast XML configuration stanza itself. You can mix/match combination of -D defined properties and those defined in Hazelcast XML. Properties defined in Hazelcast XMl take priority.


**DOCKER_HOST non-tls**
```
docker service create \
    --network [mynet] \
    --name myHzService1 \
    -l myLabel1=value1 \
    -l myLabel2=value2 \
    -e "DOCKER_HOST=http://[swarmmgr]:[port]" \
    [yourappimage] \
    java \
    -DdockerNetworkNames=[mynet] \
    -DdockerServiceNames=myHzService1 \
    -DdockerServiceLabels="myLabel1=value1,myLabel2=value2" \
    -DhazelcastPeerPort=5701 \
    -jar /test.jar
```

**1.0-RC5+ ONLY: DOCKER_HOST SSL w/ optional skip verify**

**1.0-RC13+ ONLY: Optionally, logAllServiceNamesOnFailedDiscovery, FINE logging only**
```
docker service create \
    --network [mynet] \
    --name myHzService1 \
    -l myLabel1=value1 \
    -l myLabel2=value2 \
    [yourappimage] \
    java \
    -DdockerNetworkNames=[mynet] \
    -DdockerServiceNames=myHzService1 \
    -DdockerServiceLabels="myLabel1=value1,myLabel2=value2" \
    -DhazelcastPeerPort=5701 \
    -DswarmMgrUri=http(s)://[swarmmgr]:[port] \
    -DskipVerifySsl=[true|false] \
    -DlogAllServiceNamesOnFailedDiscovery=[true|false] \
    -jar /test.jar
```

**1.0-RC14+ ONLY: Optionally, strictDockerServiceNameComparison
```
docker service create \
    --network [mynet] \
    --name myHzService1 \
    -l myLabel1=value1 \
    -l myLabel2=value2 \
    [yourappimage] \
    java \
    -DdockerNetworkNames=[mynet] \
    -DdockerServiceNames=myHzService1 \
    -DdockerServiceLabels="myLabel1=value1,myLabel2=value2" \
    -DhazelcastPeerPort=5701 \
    -DswarmMgrUri=http(s)://[swarmmgr]:[port] \
    -DskipVerifySsl=[true|false] \
    -DlogAllServiceNamesOnFailedDiscovery=[true|false] \
    -DstrictDockerServiceNameComparison=[true|false] \
    -jar /test.jar
```

NOTE! All `-D` java System properties above can be omitted and alternatively defined within the `<member-address-provider>` Hazelcast XML configuration stanza itself. You can mix/match combination of -D defined properties and those defined in Hazelcast XML. Properties defined in Hazelcast XMl take priority.

NOTE! Use the optional `logAllServiceNamesOnFailedDiscovery` property with caution. If your target swarm cluster contains many services this call may result in logging a considerable amount of un-related docker service names.

Example configuration (using MemberAddressProvider for Hazelcast 3.9+): see the example: (hazelcast-docker-swarm-discovery-spi-example-member-address-provider.xml)[src/main/resources/META-INF/hazelcast-docker-swarm-discovery-spi-example-member-address-provider.xml]

For Hazelcast <= 3.8.x apps: see the example: (hazelcast-docker-swarm-discovery-spi-example-address-picker.xml)[src/main/resources/META-INF/hazelcast-docker-swarm-discovery-spi-example-address-picker.xml]
```
<network>
    <port auto-increment="true">5701</port>

    <interfaces enabled="false">
    </interfaces>

    <join>

        <multicast enabled="false"/>
        <aws enabled="false"/>
        <tcp-ip enabled="false" />

        <!-- for Hazelcast 3.9+ apps only, comment out for <= 3.8.x apps)
        <member-address-provider enabled="true">
            <class-name>org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmMemberAddressProvider</class-name>

            <!--
                OPTIONAL:

                The following will be passed as a java.util.Properties to
                the SwarmMemberAddressProvider(java.util.Properties) constructor.

                If you do not defined these here in XML, by default they will be fetched via
                -D java System.properties by the same names via the default no-arg constructor
                of SwarmMemberAddressProvider

                <properties>
                <property name="dockerNetworkNames">...</property>
                <property name="dockerServiceLabels">...</property>
                <property name="dockerServiceNames">...</property>
                <property name="hazelcastPeerPort">...</property>
                <property name="swarmMgrUri">...</property>
                <property name="skipVerifySsl">true|false</property>
                <property name="logAllServiceNamesOnFailedDiscovery">true|false</property>
                <property name="strictDockerServiceNameComparison">true|false</property>
                </properties>
            -->
        </member-address-provider>

         <!-- Enable a Docker Swarm based discovery strategy -->
         <discovery-strategies>

           <discovery-strategy enabled="true"
               class="org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryStrategy">

             <properties>
                  <!-- Comma delimited list of Docker network names to discover matching services on -->
                  <property name="docker-network-names">${dockerNetworkNames}</property>

                  <!-- Comma delimited list of relevant Docker service names
                       to find tasks/containers on the above networks -->
                  <property name="docker-service-names">${dockerServiceNames}</property>

                  <!-- Comma delimited list of relevant Docker service label=values
                       to find tasks/containers on the above networks -->
                  <property name="docker-service-labels">${dockerServiceLabels}</property>

                  <!-- 1.0-RC5+ ONLY: Swarm Manager URI (overrides DOCKER_HOST) -->
                  <property name="swarm-mgr-uri">${swarmMgrUri}</property>

                  <!-- 1.0-RC5+ ONLY: If Swarm Mgr URI is SSL, to enable skip-verify for it -->
                  <property name="skip-verify-ssl">${skipVerifySsl}</property>
                  
                  <!-- 1.0-RC13+ ONLY! 
                       If enabled logged w/ severity FINE.
                       Use with caution. If your target swarm cluster contains many services this call 
                       may result in logging a considerable amount of un-related docker service names.
                  -->
                  <property name="log-all-service-names-on-failed-discovery">${logAllServiceNamesOnFailedDiscovery}</property>

                  <!-- 1.0-RC14+ ONLY: If enabled, perform strict "equals" name check of services returned from Docker -->
                  <property name="strict-docker-service-name-comparison">${strictDockerServiceNameComparison}</property>

                  <!-- The raw port that hazelcast is listening on

                       IMPORTANT: this is NOT a docker "published" port, nor is it necessarily
                       a EXPOSEd port... it is simply the hazelcast port that the service
                       is configured with, this must be the same for all matched containers
                       in order to work, and just using the default of 5701 is the simplist
                       way to go.
                   -->
                  <property name="hazelcast-peer-port">${hazelcastPeerPort}</property>
             </properties>

           </discovery-strategy>
         </discovery-strategies>

    </join>
</network>
```

### If Swarm DNSRR network binding option is chosen, proceed with the below:

* Create a Docker image for your application that uses Hazelcast

* Create an overlay network for your service, `docker network create -d overlay [mynetname]`

* Launch your services via `docker service create` against your Docker Swarm cluster:

Note this example command assumes an entrypoint script exists that execs the `java` command.

```
docker service create \
    --network [mynetname] \
    --name myHzService1 \
    --endpoint-mode dnsrr
    [yourappimage] \
    java
    -DserviceName=myHzService1
    -DservicePort=5701
    -DpeerServicesCsv=myHzService1:5701
    -jar /test.jar
```

Example configuration, full text at [hazelcast-docker-swarm-dnsrr-discovery-spi-example.xml](src/main/resources/META-INF/hazelcast-docker-swarm-dnsrr-discovery-spi-example.xml)

```
    <properties>
        <!-- Explicitly enable hazelcast discovery join methods -->
        <property name="hazelcast.discovery.enabled">true</property>
    </properties>

    <network>
        <!--
            Auto-increment is turned off for the port; docker containers will
            always be available at the available in-network ports.
        -->
        <port auto-increment="false">${servicePort}</port>
        
        <member-address-provider enabled="true">
            <class-name>org.bitsofinfo.hazelcast.spi.docker.swarm.dnsrr.DockerDNSRRMemberAddressProvider</class-name>
            <properties>
                <!-- Name of the docker service that this instance is running in -->
                <property name="serviceName">${serviceName}</property>

                <!-- Internal port that hazelcast is listening on -->
                <property name="servicePort">${servicePort}</property>
            </properties>
        </member-address-provider>
        
        <join>
            <!-- Explicitly disable other cluster join methods -->
            <multicast enabled="false"/>
            <aws enabled="false"/>
            <tcp-ip enabled="false" />

            <discovery-strategies>
                <discovery-strategy
                    enabled="true"
                    class="org.bitsofinfo.hazelcast.spi.docker.swarm.dnsrr.discovery.DockerDNSRRDiscoveryStrategy"
                >
                    <properties>
                        <!--
                            Comma separated list of docker services and associated ports
                            to be considered peers of this service.

                            Note, this must include itself (the definition of
                            serviceName and servicePort) if the service is to
                            cluster with other instances of this service.
                        -->
                        <property name="peerServicesCsv">${peerServicesCsv}</property>
                    </properties>
                </discovery-strategy>
            </discovery-strategies>
        </join>
    </network>
```

## <a id="building"></a>Building from source

* From the root of this project, build a Jar : `./gradlew jar`

* Include the built jar artifact located at `build/libs/hazelcast-docker-swarm-discovery-spi-[VERSION].jar` in your hazelcast project

* If not already present in your hazelcast application's Maven (pom.xml) or Gradle (build.gradle) dependencies section; ensure that these dependencies are present (versions may vary as appropriate):

```
compile group: 'com.spotify', name: 'docker-client', version: '8.7.3'
```


## <a id="tests"></a>Unit-tests

[![Build Status](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi.svg?branch=master)](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi)

There are really no traditional Java "unit tests" for this SPI due to its reliance on Docker.

There is however a [Travis CI test](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi) that properly
validates the SPI functionality in a real Docker swarm environment that brings up a single instance, scales it to 10 hazelcast
nodes and then back down to 2 nodes. Demonstrating the proper discovery of peer hazelcast members.

See the [.travis.yml](.travis.yml) file for the full details.


## <a id="related"></a>Related info

* https://docs.docker.com/engine/swarm/
* http://docs.hazelcast.org/docs/3.8/manual/html-single/index.html#discovery-spi
* https://github.com/hazelcast/hazelcast/issues/10801
* https://github.com/hazelcast/hazelcast/issues/10802
* https://github.com/hazelcast/hazelcast/issues/11997


## <a id="todo"></a>Todo

* None at this time

## <a id="troubleshooting"></a> Troubleshooting

If you get an exception (e.g. `AbstractMethodError`), this may have been caused by having `jersey-common` library twice.
(One from the plugin itself -as a transitive dependency- and the other from the shaded `docker-client` library. In such
a case you may add an exclusion to your project's build file.

For maven:

```
  <dependency>
    <groupId>org.bitsofinfo</groupId>
    <artifactId>hazelcast-docker-swarm-discovery-spi</artifactId>
    <version>1.0-RC13</version>
    <exclusions>
      <exclusion>
        <groupId>org.glassfish.jersey.core</groupId>
        <artifactId>jersey-common</artifactId>
      </exclusion>
    </exclusions>
  </dependency>
```

For gradle:

```
  compile('org.bitsofinfo:hazelcast-docker-swarm-discovery-spi:1.0-RC13') {
    exclude module: 'jersey-common'
  }
```

For details please see [Pull Request #6](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/pull/6).
