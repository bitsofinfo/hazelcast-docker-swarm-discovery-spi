package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.nio.channels.ServerSocketChannel;

import com.hazelcast.instance.AddressPicker;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;

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
    private static final String PROP_DOCKER_NETWORK_NAMES = "dockerNetworkNames";
    private static final String PROP_DOCKER_SERVICE_LABELS = "dockerServiceLabels";
    private static final String PROP_DOCKER_SERVICE_NAMES = "dockerServiceNames";
    private static final String PROP_HAZELCAST_PEER_PORT = "hazelcastPeerPort";

    private SwarmDiscoveryUtil swarmDiscoveryUtil = null;

    /**
     * Constructor
     */
    public SwarmAddressPicker(final ILogger iLogger) {
        final String dockerNetworkNames = System.getProperty(PROP_DOCKER_NETWORK_NAMES);
        final String dockerServiceLabels = System.getProperty(PROP_DOCKER_SERVICE_LABELS);
        final String dockerServiceNames = System.getProperty(PROP_DOCKER_SERVICE_NAMES);
        final Integer hazelcastPeerPort = Integer.valueOf(System.getProperty(PROP_HAZELCAST_PEER_PORT));

        initialize(iLogger, dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort);
    }

    public SwarmAddressPicker(final ILogger iLogger, final String dockerNetworkNames, final String dockerServiceLabels,
        final String dockerServiceNames, final Integer hazelcastPeerPort) {

        initialize(iLogger, dockerNetworkNames, dockerServiceLabels, dockerServiceNames, hazelcastPeerPort);
    }

    private void initialize(final ILogger iLogger, final String dockerNetworkNames, final String dockerServiceLabels,
        final String dockerServiceNames, final Integer hazelcastPeerPort) {

        final int port;

        if (hazelcastPeerPort != null) {
            port = hazelcastPeerPort;
        } else {
            port = 5701;
        }

        try {
            this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(
                iLogger,
                dockerNetworkNames,
                dockerServiceLabels,
                dockerServiceNames,
                port,
                true
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
