# hazelcast-docker-swarm-discovery-spi

This is working however this is **ALPHA** and still under development. 

[![Build Status](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi.svg?branch=master)](https://travis-ci.org/bitsofinfo/hazelcast-docker-swarm-discovery-spi)

Provides a Docker Swarm mode based discovery strategy for Hazlecast 3.6+ enabled applications.
This is an easy to configure plug-and-play Hazlecast DiscoveryStrategy that will enable Hazelcast applications to dynamically discover one another when deployed as Docker Swarm services.

* [Status](#status)
* [Releases](#releases)
* [Requirements](#requirements)
* [Maven/Gradle install](#mavengradle)
* [Features](#features)
* [Usage](#usage)
* [Build from source](#building)
* [Unit tests](#tests)
* [Related Info](#related)
* [Todo](#todo)
* [Notes](#notes)
* [Docker info](#docker)


## <a id="status"></a>Status

Working however this is **ALPHA** and still under development. 

## <a id="releases"></a>Releases

* MASTER - in progress, this README refers to what is in the master tag. Switch to relevant RELEASE tag above to see that versions README

## <a id="requirements"></a>Requirements

* Java 7+
* [Hazelcast 3.6+](https://hazelcast.org/)
* [Docker Swarm Mode](https://docs.docker.com/engine/swarm/)

## <a id="mavengradle"></a>Maven/Gradle

To use this discovery strategy in your Maven or Gradle project use the dependency samples below.

### Gradle:

```
repositories {
    jcenter()
}

dependencies {
	compile 'org.bitsofinfo:hazelcast-docker-swarm-discovery-spi:NOT-RELEASED-YET'
}
```

### Maven:

```
<dependencies>
    <dependency>
        <groupId>org.bitsofinfo</groupId>
        <artifactId>hazelcast-docker-swarm-discovery-spi</artifactId>
        <version>NOT-RELEASED-YET</version>
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

* Will permit Hazelcast instances deployed on a Docker Swarm to automatically discover and connect with one another

* Provides a custom `AddressPicker` to workaround Hazelcast interface/binding issues that are present when deploying in a Docker Swarm environment.
[hazelcast/issues/10801](https://github.com/hazelcast/hazelcast/issues/10801)

## <a id="usage"></a>Usage

* Ensure your project has the `hazelcast-docker-swarm-discovery-spi` artifact dependency declared in your maven pom or gradle build file as described above. Or build the jar yourself and ensure the jar is in your project's classpath.

* Configure your hazelcast.xml configuration file to use the `DockerSwarmDiscoveryStrategy` (similar to the below): [See hazelcast-docker-swarm-discovery-spi-example.xml](src/main/resources/hazelcast-docker-swarm-discovery-spi-example.xml) for an example with documentation of options.

**CAVEAT** Due to [hazelcast/issues/10801](https://github.com/hazelcast/hazelcast/issues/10801) you MUST start your Hazelcast instance in the following way in order to use this SPI:

```
Config conf =new ClasspathXmlConfig("yourHzConfig.xml");

NodeContext nodeContext = new DefaultNodeContext() {
    @Override
    public AddressPicker createAddressPicker(Node node) {
        return new SwarmAddressPicker();
    }
};

HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
        .newHazelcastInstance(conf,"myAppName",nodeContext);
        
```

* Create a Docker image for your application that uses Hazelcast

* Create an overlay network for your service, `docker network create -d overlay [name]`

* Launch your services via `docker service create ` against a Docker Swarm cluster:

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
    -DhazelcastInterface=10.0.5.* \
    -jar /test.jar
```

```
<network>
    <port auto-increment="true">5701</port>
    
    <interfaces enabled="true">
        <interface>${hazelcastInterface}</interface>        
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

IN-PROCESS

* From the root of this project, build a Jar : `./gradlew build`

* Include the built jar artifact located at `build/libs/hazelcast-docker-swarm-discovery-spi-[VERSION].jar` in your hazelcast project

* If not already present in your hazelcast application's Maven (pom.xml) or Gradle (build.gradle) dependencies section; ensure that these dependencies are present (versions may vary as appropriate):

```
compile group: 'com.spotify', name: 'docker-client', version: '8.7.3'
```


## <a id="tests"></a>Unit-tests

TBD


## <a id="related"></a>Related info

* https://docs.docker.com/engine/swarm/
* http://docs.hazelcast.org/docs/3.8/manual/html-single/index.html#discovery-spi
* https://github.com/hazelcast/hazelcast/issues/10801

## <a id="todo"></a>Todo

TBD

## <a id="notes"></a> Notes

### <a id="docker"></a>Containerization (Docker) notes

TBD
