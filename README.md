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
* [Notes](#notes)

![Diagram of hazelcast docker swarm discovery strategy](/docs/diag1.png "Diagram1")

## <a id="status"></a>Status

This is release candidate code, tested against Hazelcast 3.6-EA+ through 3.8.x Stable releases.

## <a id="releases"></a>Releases

* MASTER - in progress, this README refers to what is in the master tag. Switch to relevant RELEASE tag above to see that versions README

* [1.0-RC1](https://github.com/bitsofinfo/hazelcast-docker-swarm-discovery-spi/releases/tag/1.0-RC1): Initial release

## <a id="requirements"></a>Requirements

* Java 7+
* [Hazelcast 3.6+](https://hazelcast.org/)
* [Docker 1.12+ Swarm Mode](https://docs.docker.com/engine/swarm/)

## <a id="mavengradle"></a>Maven/Gradle

To use this discovery strategy in your Maven or Gradle project use the dependency samples below.

### Gradle:

```
repositories {
    jcenter()
}

dependencies {
	compile 'org.bitsofinfo:hazelcast-docker-swarm-discovery-spi:1.0-RC1'
}
```

### Maven:

```
<dependencies>
    <dependency>
        <groupId>org.bitsofinfo</groupId>
        <artifactId>hazelcast-docker-swarm-discovery-spi</artifactId>
        <version>1.0-RC1</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray</name>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>
```

## <a id="features"></a>Features

* Will permit Hazelcast instances deployed on a Docker 1.12+ Swarm to automatically discover and connect with one another

* Provides a custom `AddressPicker` to workaround Hazelcast interface/binding issues that are present when deploying in a Docker Swarm environment.
[hazelcast/issues/10801](https://github.com/hazelcast/hazelcast/issues/10801)

## <a id="howitworks"></a>How it works

Hazelcast applications that use this discovery SPI will discover one another when deployed as Docker services in the following way.

* Launch your docker service with its service name and target overlay network name. In addition specify additional ENVIRONMENT variables via `-e` named `dockerNetworkNames`, `dockerServiceNames` and optionally `dockerServiceLabels`. These variables will be consumed by the discovery SPI. The `DOCKER_HOST` environment variable for the container should also be set to a name that resolves to one or more swarm manager nodes.

* The Docker Swarm Discovery SPI consumes from the `DOCKER_HOST`, `dockerNetworkNames`, `dockerServiceNames`, optionally `dockerServiceLabels` and `hazelcastPeerPort` and begins the following process.

    1. Leverages the custom `SwarmAddressPicker` to talk to the *$DOCKER_HOST* `/networks`, `/services` and `/tasks` APIs to determine the current node's IP address on the docker network, and bind hazelcast on `hazelcastPeerPort` to that address.
    
    2. Next hazelcast invokes the SPI `discoverMembers()` to determine all peer docker service tasks (containers) ip addresses and attempts to connect to them to form the cluster connecting to the configured `hazelcastPeerPort` (default 5701)

## <a id="usage"></a>Usage

* Ensure your project has the `hazelcast-docker-swarm-discovery-spi` artifact dependency declared in your maven pom or gradle build file as described above. Or build the jar yourself and ensure the jar is in your project's classpath.

* Configure your hazelcast.xml configuration file to use the `DockerSwarmDiscoveryStrategy` (similar to the below): [See hazelcast-docker-swarm-discovery-spi-example.xml](src/main/resources/hazelcast-docker-swarm-discovery-spi-example.xml) for an example with documentation of options.

**CAVEAT** Due to [hazelcast/issues/10801](https://github.com/hazelcast/hazelcast/issues/10801) you MUST start your Hazelcast instance by first customizing the `AddressPicker` that Hazelcast will use to be the `SwarmAddressPicker`; you can do this in the following way:

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

* Create a Docker image for your application that uses Hazelcast

* Create an overlay network for your service, `docker network create -d overlay [name]`

* Launch your services via `docker service create` against your Docker Swarm cluster:

Note this example command assumes an entrypoint script exists that execs the `java` command

```
docker service create \
    --network [mynet] \
    --name myHzService1 \
    -e "DOCKER_HOST=http://swarmmgrnode:2376" \
    [yourappimage] \
    java \
    -DdockerNetworkNames=[mynet] \
    -DdockerServiceNames=myHzService1 \
    -DhazelcastPeerPort=5701 \
    -jar /test.jar
```

Example configuration: see the example: (hazelcast-docker-swarm-discovery-spi-example.xml)[src/main/resources/META-INF/hazelcast-docker-swarm-discovery-spi-example.xml]
```
<network>
    <port auto-increment="true">5701</port>
    
    <interfaces enabled="false">       
    </interfaces> 
    
    <join> 
    
        <multicast enabled="false"/>
        <aws enabled="false"/>
        <tcp-ip enabled="false" />
          
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


## <a id="todo"></a>Todo

None at this time

## <a id="notes"></a> Notes


