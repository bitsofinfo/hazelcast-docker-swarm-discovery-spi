package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.util.Properties;

import com.hazelcast.instance.AddressPicker;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.MemberAddressProvider;

/**
 * Custom MemberAddressProvider that works for hazelcast instances running in swarm service instances
 * <p>
 * There are four JVM System properties to be defined:
 * <p>
 * - dockerNetworkNames = required, min one network: comma delimited list of relevant docker network names
 * that matching services must have a VIP on
 * <p>
 * - hazelcastPeerPort = optional, default 5701, the hazelcast port all service members are listening on
 * <p>
 * ONE or BOTH of the following can be defined:
 * <p>
 * - dockerServiceLabels = zero or more comma delimited service 'label=value' pairs to match.
 * If ANY match, that services' containers will be included in list of discovered containers
 * <p>
 * - dockerServiceNames = zero or more comma delimited service "names" to match.
 * If ANY match, that services' containers will be included in list of discovered containers
 * <p>
 * Another way to initiate this class is to pass above properties when creating a new {@link SwarmMemberAddressProvider}
 * instance. This eliminates the need to pass properties in both hazelcast.xml (for setting up discovery) and with
 * JVM properties.
 *
 * @author bitsofinfo
 * @see https://github.com/hazelcast/hazelcast/issues/10801
 * @see https://github.com/hazelcast/hazelcast/blob/44045949b683b958e4e245040b65f947f143a9ef/hazelcast/src/main/resources/hazelcast-full-example.xml#L408
 * @see https://github.com/hazelcast/hazelcast/pull/11548
 */
public class SwarmMemberAddressProvider implements MemberAddressProvider {
	
    private static final String PROP_DOCKER_NETWORK_NAMES = "dockerNetworkNames";
    private static final String PROP_DOCKER_SERVICE_LABELS = "dockerServiceLabels";
    private static final String PROP_DOCKER_SERVICE_NAMES = "dockerServiceNames";
    private static final String PROP_HAZELCAST_PEER_PORT = "hazelcastPeerPort";

    public static final String PROP_SWARM_MGR_URI = "swarmMgrUri";
    public static final String PROP_SKIP_VERIFY_SSL = "skipVerifySsl";
    
    private SwarmDiscoveryUtil swarmDiscoveryUtil = null;
    
    private ILogger logger = Logger.getLogger(SwarmMemberAddressProvider.class);

    /**
     * Constructor
     */
    public SwarmMemberAddressProvider() {
    	
    		final String dockerNetworkNames = System.getProperty(PROP_DOCKER_NETWORK_NAMES);
        final String dockerServiceLabels = System.getProperty(PROP_DOCKER_SERVICE_LABELS);
        final String dockerServiceNames = System.getProperty(PROP_DOCKER_SERVICE_NAMES);
        final Integer hazelcastPeerPort = Integer.valueOf(System.getProperty(PROP_HAZELCAST_PEER_PORT));
       
       String swarmMgrUri = System.getProperty(PROP_SWARM_MGR_URI);
        if (swarmMgrUri == null || swarmMgrUri.trim().isEmpty()) {
        		swarmMgrUri = System.getenv("DOCKER_HOST");
        }
        
        Boolean skipVerifySsl = false;
        if (System.getProperty(PROP_SKIP_VERIFY_SSL) != null) {
        		skipVerifySsl = Boolean.valueOf(System.getProperty(PROP_SKIP_VERIFY_SSL));
        }

        initialize(dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort, swarmMgrUri, skipVerifySsl);
    }
    

    public SwarmMemberAddressProvider(final String dockerNetworkNames, final String dockerServiceLabels,
        final String dockerServiceNames, final Integer hazelcastPeerPort) {

    		String swarmMgrUri = System.getenv("DOCKER_HOST"); 
		Boolean skipVerifySsl = false;
		
		initialize(dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort, swarmMgrUri, skipVerifySsl);
    }
    
    /**
     * If you do not provide any properties, the class may have either a 
     * no-arg constructor or a constructor accepting a single java.util.Properties instance. 
     * On the other hand, if you do provide properties in the configuration, 
     * the class must have a constructor accepting a single java.util.Properties instance.
     * 
     * See: https://docs.hazelcast.org/docs/3.9.4/manual/html-single/index.html#member-address-provider-spi
     * @param properties
     */
    public SwarmMemberAddressProvider(Properties properties) {
    	
    		String dockerNetworkNames = (String)properties.get(PROP_DOCKER_NETWORK_NAMES);
    		if (dockerNetworkNames == null || dockerNetworkNames.trim().isEmpty()) {
    			dockerNetworkNames = System.getProperty(PROP_DOCKER_NETWORK_NAMES);
    		}
    		
    		String dockerServiceLabels = (String)properties.get(PROP_DOCKER_SERVICE_LABELS);
    		if (dockerServiceLabels == null || dockerServiceLabels.trim().isEmpty()) {
    			dockerServiceLabels = System.getProperty(PROP_DOCKER_SERVICE_LABELS);
    		}
    		
    		String dockerServiceNames = (String)properties.get(PROP_DOCKER_SERVICE_NAMES);
    		if (dockerServiceNames == null || dockerServiceNames.trim().isEmpty()) {
    			dockerServiceNames = System.getProperty(PROP_DOCKER_SERVICE_NAMES);
    		}
    		
    		
    		Object rawHazelcastPeerPort = properties.get(PROP_HAZELCAST_PEER_PORT);
    		if (rawHazelcastPeerPort == null || rawHazelcastPeerPort.toString().trim().isEmpty()) {
    			rawHazelcastPeerPort = System.getProperty(PROP_HAZELCAST_PEER_PORT);
    		}
    		Integer hazelcastPeerPort = 5701;
    		if (rawHazelcastPeerPort instanceof String) {
    			try {
    				hazelcastPeerPort = Integer.valueOf(rawHazelcastPeerPort.toString());
    			} catch(Throwable ignore) {}
    		} else if (rawHazelcastPeerPort instanceof Integer) {
    			hazelcastPeerPort = (Integer)rawHazelcastPeerPort;
    		}
    		
    		
    		String swarmMgrUri = (String)properties.get(PROP_SWARM_MGR_URI);
    		if (swarmMgrUri == null || swarmMgrUri.trim().isEmpty()) {
    			swarmMgrUri = System.getProperty(PROP_SWARM_MGR_URI);
    		}
    		if (swarmMgrUri == null || swarmMgrUri.trim().isEmpty()) {
        		swarmMgrUri = System.getenv("DOCKER_HOST");
        }
    		
    		
    		Object rawSkipVerifySsl = properties.get(PROP_SKIP_VERIFY_SSL);
    		if (rawSkipVerifySsl == null || rawSkipVerifySsl.toString().trim().isEmpty()) {
    			rawSkipVerifySsl = System.getProperty(PROP_SKIP_VERIFY_SSL);
    		}
    		Boolean skipVerifySsl = false;
	    if (rawSkipVerifySsl != null) {
	    		skipVerifySsl = Boolean.valueOf(rawSkipVerifySsl.toString());
	    }
    		
    	
    		initialize(dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort, swarmMgrUri, skipVerifySsl);
    }

    public SwarmMemberAddressProvider(final String dockerNetworkNames, 
    									 final String dockerServiceLabels,
    									 final String dockerServiceNames, 
    									 final Object hazelcastPeerPort) {

    		String swarmMgrUri = System.getenv("DOCKER_HOST"); 
		Boolean skipVerifySsl = false;
		
		initialize(dockerNetworkNames, dockerServiceLabels, dockerServiceNames, swarmMgrUri, skipVerifySsl, hazelcastPeerPort);
    }
    
    private void initialize(final String dockerNetworkNames, final String dockerServiceLabels,
            			final String dockerServiceNames, final Integer hazelcastPeerPort, final String swarmMgrUri, final Boolean skipVerifySsl) {
    		initialize(dockerNetworkNames,dockerServiceLabels,dockerServiceNames,swarmMgrUri,skipVerifySsl,hazelcastPeerPort);
    }


    private void initialize(final String dockerNetworkNames, 
    						   final String dockerServiceLabels,
    						   final String dockerServiceNames, 
    						   final String swarmMgrUri, 
    						   final Boolean skipVerifySsl, 
    						   final Object rawHazelcastPeerPort) {
    	
    		logger.info("SwarmMemberAddressProvider.initialize() passed: " + 
    						dockerNetworkNames + ":"+dockerNetworkNames + " " +
    						dockerServiceLabels + ":"+dockerServiceLabels + " " +
    						dockerServiceNames + ":"+dockerServiceNames + " " +
    						swarmMgrUri + ":"+swarmMgrUri + " " +
    						skipVerifySsl + ":"+skipVerifySsl + " " +
    						rawHazelcastPeerPort + ":"+rawHazelcastPeerPort
    						);

    	
    		Integer hazelcastPeerPort = null;
		if (rawHazelcastPeerPort != null) {
			if (rawHazelcastPeerPort instanceof String) {
				try {
					hazelcastPeerPort = Integer.valueOf(rawHazelcastPeerPort.toString());
				} catch(Throwable ignore) {}
			} else if (rawHazelcastPeerPort instanceof Integer) {
				hazelcastPeerPort = (Integer)rawHazelcastPeerPort;
			}
		}
    	
        final int port;

        if (hazelcastPeerPort != null) {
            port = hazelcastPeerPort;
        } else {
            port = 5701;
        }

        try {
        	 	 URI swarmMgr = null;
             if (swarmMgrUri == null || swarmMgrUri.trim().isEmpty()) {
             	swarmMgr = new URI(System.getenv("DOCKER_HOST")); 
             } else {
            	 	swarmMgr = new URI(swarmMgrUri);
             }
             
            this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(
                dockerNetworkNames,
                dockerServiceLabels,
                dockerServiceNames,
                port,
                
                // do NOT bindSocketChannel
                // this flag was originally here for SwarmAddressPicker, 
                // see: https://github.com/hazelcast/hazelcast/issues/11997#issuecomment-354107373
                false ,
                
                swarmMgr,
                skipVerifySsl
            );
        } catch (final Exception e) {
            throw new RuntimeException(
                "SwarmAddressPicker: Error constructing SwarmDiscoveryUtil: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public InetSocketAddress getBindAddress() {
        Address addr = this.swarmDiscoveryUtil.getMyAddress();
        return new InetSocketAddress(addr.getHost(), addr.getPort());
    }

    @Override
    public InetSocketAddress getPublicAddress() {
        Address addr = this.swarmDiscoveryUtil.getMyAddress();
        return new InetSocketAddress(addr.getHost(), addr.getPort());
    }

}
