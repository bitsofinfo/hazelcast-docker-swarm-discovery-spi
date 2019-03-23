package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.instance.AddressPicker;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;

import java.net.URI;
import java.nio.channels.ServerSocketChannel;

/**
 * Custom AddressPicker that works for hazelcast instances running in swarm service instances
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
 * Another way to initiate this class is to pass above properties when creating a new {@link SwarmAddressPicker}
 * instance. This eliminates the need to pass properties in both hazelcast.xml (for setting up discovery) and with
 * JVM properties.
 *
 * @author bitsofinfo
 * @see <a href="https://github.com/hazelcast/hazelcast/issues/10801">Hazelcast GitHub Issue #10801</a>
 */
public class SwarmAddressPicker implements AddressPicker {

    public static final String PROP_DOCKER_NETWORK_NAMES = "dockerNetworkNames";
    public static final String PROP_DOCKER_SERVICE_LABELS = "dockerServiceLabels";
    public static final String PROP_DOCKER_SERVICE_NAMES = "dockerServiceNames";
    public static final String PROP_HAZELCAST_PEER_PORT = "hazelcastPeerPort";
    public static final String PROP_SWARM_MGR_URI = "swarmMgrUri";
    public static final String PROP_SKIP_VERIFY_SSL = "skipVerifySsl";

    private SwarmDiscoveryUtil swarmDiscoveryUtil = null;

    /**
     * Constructor
     */


    public SwarmAddressPicker() {
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

    public SwarmAddressPicker(final String dockerNetworkNames, final String dockerServiceLabels,
                              final String dockerServiceNames, final Integer hazelcastPeerPort) {

        String swarmMgrUri = System.getenv("DOCKER_HOST");
        Boolean skipVerifySsl = false;

        initialize(dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort, swarmMgrUri, skipVerifySsl);
    }

    private void initialize(final String dockerNetworkNames, final String dockerServiceLabels,
                            final String dockerServiceNames, final Integer hazelcastPeerPort, final String swarmMgrUri, final Boolean skipVerifySsl) {

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
            }

            this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(
                    this.getClass().getSimpleName(),
                    dockerNetworkNames,
                    dockerServiceLabels,
                    dockerServiceNames,
                    port,
                    true,
                    swarmMgr,
                    skipVerifySsl,
                    false,
                    false
            );
        } catch (final Exception e) {
            throw new RuntimeException(
                    "SwarmAddressPicker: Error constructing SwarmDiscoveryUtil: " + e.getMessage(),
                    e
            );
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