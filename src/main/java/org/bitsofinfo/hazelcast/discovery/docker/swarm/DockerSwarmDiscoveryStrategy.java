package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListNetworksParam;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.EndpointVirtualIp;
import com.spotify.docker.client.messages.swarm.NetworkAttachment;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Service.Criteria;
import com.spotify.docker.client.messages.swarm.Task;

/**
 * DiscoveryStrategy for Docker Swarm
 * 
 * 
 * @author bitsofinfo
 *
 */
public class DockerSwarmDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private SwarmDiscoveryConfig swarmDiscoveryConfig = null;

	/**
	 * Constructor
	 * 
	 * @param localDiscoveryNode
	 * @param logger
	 * @param properties
	 */
	public DockerSwarmDiscoveryStrategy(DiscoveryNode localDiscoveryNode, ILogger logger, Map<String, Comparable> properties ) {

		super( logger, properties );
		
		System.out.println("INIT!");
		
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
	        while (networkInterfaces.hasMoreElements()) {
	            NetworkInterface ni = networkInterfaces.nextElement();
	            Enumeration<InetAddress> e = ni.getInetAddresses();
	            while (e.hasMoreElements()) {
	                InetAddress inetAddress = e.nextElement();
	                System.out.println(inetAddress);
	            }
	        }
		
		} catch(Exception e) {
			e.printStackTrace();
		}

		String rawDockerNetworkNames = getOrDefault("docker-network-names",  DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES, null);
		String rawDockerServiceLabels = getOrDefault("docker-service-labels",  DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS, null);
		String rawDockerServiceNames = getOrDefault("docker-service-names",  DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES, null);
		Integer hazelcastPeerPort = getOrDefault("hazelcast-peer-port",  DockerSwarmDiscoveryConfiguration.HAZELCAST_PEER_PORT, 5701);

		
		this.swarmDiscoveryConfig = new SwarmDiscoveryConfig(rawDockerNetworkNames,
															 rawDockerServiceLabels,
															 rawDockerServiceNames,
															 hazelcastPeerPort);

	}                              

	@Override
	public Iterable<DiscoveryNode> discoverNodes() {
		
		System.out.println("DNODES!");

		List<DiscoveryNode> toReturn = new ArrayList<DiscoveryNode>();

		try {

			Set<DiscoveredContainer> discoveredContainers = this.swarmDiscoveryConfig.discoverContainers();
			
			/**
			 * We have all the containers, convert to DiscoveryNodes and return...
			 */
			System.out.println("discoverNodes() DiscoveredContainers["+discoveredContainers.size()+"]: " + 
					Arrays.toString(discoveredContainers.toArray(new DiscoveredContainer[]{})));
			
			for (DiscoveredContainer container : discoveredContainers) {
				toReturn.add(new SimpleDiscoveryNode(
						new Address(container.getIp(),
									swarmDiscoveryConfig.getHazelcastPeerPort())));
			}

			return toReturn;

		} catch(Exception e) {
			getLogger().severe("discoverNodes() unexpected error: " + e.getMessage(),e);
		}

		return toReturn;
	}



}
