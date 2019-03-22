package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class DockerSwarmDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

    private static final Collection<PropertyDefinition> PROPERTIES =
            Arrays.asList(DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS,
                    DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES,
                    DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES,
                    DockerSwarmDiscoveryConfiguration.HAZELCAST_PEER_PORT,
                    DockerSwarmDiscoveryConfiguration.SWARM_MGR_URI,
                    DockerSwarmDiscoveryConfiguration.SKIP_VERIFY_SSL,
                    DockerSwarmDiscoveryConfiguration.LOG_ALL_SERVICE_NAMES_ON_FAILED_DISCOVERY,
                    DockerSwarmDiscoveryConfiguration.STRICT_DOCKER_SERVICE_NAME_COMPARISON);


    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        // Returns the actual class type of the DiscoveryStrategy
        // implementation, to match it against the configuration
        return DockerSwarmDiscoveryStrategy.class;
    }


    public Collection<PropertyDefinition> getConfigurationProperties() {
        return PROPERTIES;
    }


    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode,
                                                  ILogger logger,
                                                  Map<String, Comparable> properties) {

        return new DockerSwarmDiscoveryStrategy(logger, properties);
    }

}
