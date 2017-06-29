package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

/**
 * DiscoveryStrategy for Docker Swarm
 * 
 * You must have a system ENVIRONMENT variable defined
 * for DOCKER_HOST for this to work
 * 
 * @author bitsofinfo
 *
 */
public class DockerSwarmDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private SwarmDiscoveryUtil swarmDiscoveryUtil = null;

	/**
	 * Constructor
	 * 
	 * @param localDiscoveryNode
	 * @param logger
	 * @param properties
	 */
	public DockerSwarmDiscoveryStrategy(DiscoveryNode localDiscoveryNode, ILogger logger, Map<String, Comparable> properties ) {

		super( logger, properties );

		String rawDockerNetworkNames = getOrDefault("docker-network-names",  DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES, null);
		String rawDockerServiceLabels = getOrDefault("docker-service-labels",  DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS, null);
		String rawDockerServiceNames = getOrDefault("docker-service-names",  DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES, null);
		Integer hazelcastPeerPort = getOrDefault("hazelcast-peer-port",  DockerSwarmDiscoveryConfiguration.HAZELCAST_PEER_PORT, 5701);

		try {
			this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(logger,
															 rawDockerNetworkNames,
															 rawDockerServiceLabels,
															 rawDockerServiceNames,
															 hazelcastPeerPort,
															 false); // dont bind channel, the AddressPicker does this
		} catch(Exception e) {
			String msg = "Unexpected error configuring SwarmDiscoveryUtil: " + e.getMessage();
			logger.severe(msg,e);
			throw new RuntimeException(msg,e);
		}

	}                              

	@Override
	public Iterable<DiscoveryNode> discoverNodes() {

		List<DiscoveryNode> toReturn = new ArrayList<DiscoveryNode>();

		try {

			Set<DiscoveredContainer> discoveredContainers = this.swarmDiscoveryUtil.discoverContainers();
			
			/**
			 * We have all the containers, convert to DiscoveryNodes and return...
			 */
			getLogger().info("discoverNodes() DiscoveredContainers["+discoveredContainers.size()+"]: " + 
					Arrays.toString(discoveredContainers.toArray(new DiscoveredContainer[]{})));
			
			for (DiscoveredContainer container : discoveredContainers) {
				toReturn.add(new SimpleDiscoveryNode(
						new Address(container.getIp(),
									swarmDiscoveryUtil.getHazelcastPeerPort())));
			}

			return toReturn;

		} catch(Exception e) {
			getLogger().severe("discoverNodes() unexpected error: " + e.getMessage(),e);
		}

		return toReturn;
	}



}
