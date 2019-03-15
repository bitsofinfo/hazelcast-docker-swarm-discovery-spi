package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DiscoveryStrategy for Docker Swarm
 * <p>
 * You must have a system ENVIRONMENT variable defined
 * for DOCKER_HOST for this to work
 *
 * @author bitsofinfo
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
    public DockerSwarmDiscoveryStrategy(DiscoveryNode localDiscoveryNode, ILogger logger, Map<String, Comparable> properties) {

        super(logger, properties);

        String rawDockerNetworkNames = getOrDefault("docker-network-names", DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES, null);
        String rawDockerServiceLabels = getOrDefault("docker-service-labels", DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS, null);
        String rawDockerServiceNames = getOrDefault("docker-service-names", DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES, null);
        Integer hazelcastPeerPort = getOrDefault("hazelcast-peer-port", DockerSwarmDiscoveryConfiguration.HAZELCAST_PEER_PORT, 5701);
        String swarmMgrUri = getOrDefault("swarm-mgr-uri", DockerSwarmDiscoveryConfiguration.SWARM_MGR_URI, null);
        Boolean skipVerifySsl = getOrDefault("skip-verify-ssl", DockerSwarmDiscoveryConfiguration.SKIP_VERIFY_SSL, false);
        Boolean logAllServiceNamesOnFailedDiscovery = getOrDefault("log-all-service-names-on-failed-discovery", DockerSwarmDiscoveryConfiguration.LOG_ALL_SERVICE_NAMES_ON_FAILED_DISCOVERY, false);
        Boolean strictDockerServiceNameComparison = getOrDefault("strict-docker-service-name-comparison", DockerSwarmDiscoveryConfiguration.STRICT_DOCKER_SERVICE_NAME_COMPARISON, false);

        try {

            URI swarmMgr = null;
            if (swarmMgrUri == null && System.getenv("DOCKER_HOST") != null) {
                swarmMgr = new URI(System.getenv("DOCKER_HOST"));
            } else {
                swarmMgr = new URI(swarmMgrUri);
            }

            this.swarmDiscoveryUtil = new SwarmDiscoveryUtil(this.getClass().getSimpleName(),
                    rawDockerNetworkNames,
                    rawDockerServiceLabels,
                    rawDockerServiceNames,
                    hazelcastPeerPort,
                    false, // dont bind channel, the AddressPicker does this
                    swarmMgr,
                    skipVerifySsl,
                    logAllServiceNamesOnFailedDiscovery,
                    strictDockerServiceNameComparison);
        } catch (Exception e) {
            String msg = "Unexpected error configuring SwarmDiscoveryUtil: " + e.getMessage();
            logger.severe(msg, e);
            throw new RuntimeException(msg, e);
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
            getLogger().info("discoverNodes() DiscoveredContainers[" + discoveredContainers.size() + "]: " +
                    Arrays.toString(discoveredContainers.toArray(new DiscoveredContainer[]{})));

            for (DiscoveredContainer container : discoveredContainers) {
                toReturn.add(new SimpleDiscoveryNode(
                        new Address(container.getIp(),
                                swarmDiscoveryUtil.getHazelcastPeerPort())));
            }

            return toReturn;

        } catch (Exception e) {
            getLogger().severe("discoverNodes() unexpected error: " + e.getMessage(), e);
        }

        return toReturn;
    }


}
