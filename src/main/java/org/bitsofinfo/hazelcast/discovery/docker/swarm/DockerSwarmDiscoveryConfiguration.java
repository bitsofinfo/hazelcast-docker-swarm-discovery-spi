package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;

/**
 * Defines constants for our supported Properties
 * 
 * @author bitsofinfo
 *
 */
public class DockerSwarmDiscoveryConfiguration {
	
	public static final PropertyDefinition DOCKER_NETWORK_NAMES = 
			new SimplePropertyDefinition("docker-network-names", PropertyTypeConverter.STRING);
	
	public static final PropertyDefinition DOCKER_SERVICE_LABELS = 
			new SimplePropertyDefinition("docker-service-labels", PropertyTypeConverter.STRING);
	
	public static final PropertyDefinition DOCKER_SERVICE_NAMES = 
			new SimplePropertyDefinition("docker-service-names", PropertyTypeConverter.STRING);
	
	public static final PropertyDefinition HAZELCAST_PEER_PORT = 
			new SimplePropertyDefinition("hazelcast-peer-port", PropertyTypeConverter.INTEGER);
	
}
