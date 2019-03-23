package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;

/**
 * Defines constants for our supported Properties
 *
 * @author bitsofinfo
 */
public class DockerSwarmDiscoveryConfiguration {

    private DockerSwarmDiscoveryConfiguration() {
    }

    // comma delimited list of networks to look for services on
    public static final PropertyDefinition DOCKER_NETWORK_NAMES =
            new SimplePropertyDefinition("docker-network-names", PropertyTypeConverter.STRING);

    // comma delimited list of docker service labels to match
    public static final PropertyDefinition DOCKER_SERVICE_LABELS =
            new SimplePropertyDefinition("docker-service-labels", true, PropertyTypeConverter.STRING);

    // comma delimited list of docker service names to match
    public static final PropertyDefinition DOCKER_SERVICE_NAMES =
            new SimplePropertyDefinition("docker-service-names", true, PropertyTypeConverter.STRING);

    // the configured hazelcast port that all instances are listening on
    // this is not the published/exposed docker port, but just the hazelcast listening
    // port that is reachable by any other container on the same overlay network
    public static final PropertyDefinition HAZELCAST_PEER_PORT =
            new SimplePropertyDefinition("hazelcast-peer-port", true, PropertyTypeConverter.INTEGER);

    // Swarm MGR URI, overrides DOCKER_HOST
    public static final PropertyDefinition SWARM_MGR_URI =
            new SimplePropertyDefinition("swarm-mgr-uri", true, PropertyTypeConverter.STRING);

    // Skip Verify SSL
    public static final PropertyDefinition SKIP_VERIFY_SSL =
            new SimplePropertyDefinition("skip-verify-ssl", true, PropertyTypeConverter.BOOLEAN);

    // Log all service names on failed discovery
    public static final PropertyDefinition LOG_ALL_SERVICE_NAMES_ON_FAILED_DISCOVERY =
            new SimplePropertyDefinition("log-all-service-names-on-failed-discovery", true, PropertyTypeConverter.BOOLEAN);

    // Strict service name "equals" check
    public static final PropertyDefinition STRICT_DOCKER_SERVICE_NAME_COMPARISON =
            new SimplePropertyDefinition("strict-docker-service-name-comparison", true, PropertyTypeConverter.BOOLEAN);

}
