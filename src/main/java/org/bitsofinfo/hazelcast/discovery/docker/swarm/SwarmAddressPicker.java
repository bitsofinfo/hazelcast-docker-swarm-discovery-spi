package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.nio.channels.ServerSocketChannel;

import com.hazelcast.instance.AddressPicker;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;

/**
 * Custom AddressPicker that works for hazelcast
 * instances running in swarm service instances
 * 
 * There are four JVM System properties to be defined:
 * 
 * - dockerNetworkNames = required, min one network: comma delimited list of relevant docker network names
 *                        that matching services must have a VIP on
 *                        
 * - hazelcastPeerPort = optional, default 5701, the hazelcast port all service members are listening on
 *  
 * ONE or BOTH of the following can be defined:
 * 
 * - dockerServiceLabels = zero or more comma delimited service 'label=value' pairs to match. 
 *       If ANY match, that services' containers will be included in list of discovered containers
 * 
 * - dockerServiceNames = zero or more comma delimited service "names" to match. 
 *        If ANY match, that services' containers will be included in list of discovered containers
 * 
 * @see https://github.com/hazelcast/hazelcast/issues/10801
 * @author bitsofinfo
 *
 */
public class SwarmAddressPicker implements AddressPicker {
	
	public static final String PROP_DOCKER_NETWORK_NAMES = "dockerNetworkNames";
	public static final String PROP_DOCKER_SERVICE_LABELS = "dockerServiceLabels";
	public static final String PROP_DOCKER_SERVICE_NAMES = "dockerServiceNames";
	public static final String PROP_HAZELCAST_PEER_PORT = "hazelcastPeerPort";

	private SwarmDiscoveryUtil swarmDiscoveryUtil = null;
	private ILogger logger = null;

	/**
	 * Constructor
	 */
	public SwarmAddressPicker(ILogger iLogger) {
		
		this.logger = iLogger;
		
		String rawDockerNetworkNames = System.getProperty(PROP_DOCKER_NETWORK_NAMES);
		String rawDockerServiceLabels = System.getProperty(PROP_DOCKER_SERVICE_LABELS);
		String rawDockerServiceNames = System.getProperty(PROP_DOCKER_SERVICE_NAMES);
		
		Integer hazelcastPeerPort = 5701;
		if (System.getProperty("hazelcastPeerPort") != null) {
			hazelcastPeerPort = Integer.valueOf(System.getProperty(PROP_HAZELCAST_PEER_PORT));
		}
		
		try {
			this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(iLogger,
															 rawDockerNetworkNames,
															 rawDockerServiceLabels,
															 rawDockerServiceNames,
															 hazelcastPeerPort,
															 true);
		} catch(Exception e) {
			throw new RuntimeException("SwarmAddressPicker: Error constructing SwarmDiscoveryUtil: " + e.getMessage(),e);
		}
		
	}

	@Override
	public void pickAddress() throws Exception {
		// nothing to do, done in SwarmDiscoveryUtil above
	}

	@Override
	public Address getBindAddress() {
		return this.swarmDiscoveryUtil.getMyAddress();
	}

	@Override
	public Address getPublicAddress() {
		return this.swarmDiscoveryUtil.getMyAddress();
	}

	@Override
	public ServerSocketChannel getServerSocketChannel() {
		return this.swarmDiscoveryUtil.getServerSocketChannel();
	}

}
