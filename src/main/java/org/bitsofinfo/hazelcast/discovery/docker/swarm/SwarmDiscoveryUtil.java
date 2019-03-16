package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.Address;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListNetworksParam;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.EndpointVirtualIp;
import com.spotify.docker.client.messages.swarm.NetworkAttachment;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Service.Criteria;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;
import com.spotify.docker.client.shaded.com.google.common.collect.ImmutableList;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.filter.NameBasedServiceFilter;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.filter.NullServiceFilter;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.filter.ServiceFilter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * SwarmDiscoveryUtil is the workhorse of this discovery SPI implementation
 *
 * @author bitsofinfo
 */
public class SwarmDiscoveryUtil {

    // from: https://github.com/hazelcast/hazelcast/blob/210475c806328c6655ea551f6fc59ef8220b601d/hazelcast/src/main/java/com/hazelcast/instance/DefaultAddressPicker.java
    private static final int SOCKET_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(1);
    private static final int SOCKET_BACKLOG_LENGTH = 100;

    private Set<String> dockerNetworkNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, String> dockerServiceLabels = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    private Set<String> dockerServiceNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private String rawDockerNetworkNames = null;
    private String rawDockerServiceLabels = null;
    private String rawDockerServiceNames = null;

    private Integer hazelcastPeerPort = 5701;

    private DiscoveredContainer myContainer = null;
    private Address myAddress = null;

    private boolean bindSocketChannel = true;
    private ServerSocketChannel serverSocketChannel = null;

    private URI swarmMgrUri = null;
    private boolean skipVerifySsl = false;

    private boolean logAllServiceNamesOnFailedDiscovery = false;
    private boolean strictDockerServiceNameComparison = false;

    // Since SwarmDiscoveryUtil is used by several components
    // the context lets us distinguish instances in logs
    private String context = null;

    private ILogger logger = Logger.getLogger(SwarmDiscoveryUtil.class);

    public SwarmDiscoveryUtil(String context,
                              String rawDockerNetworkNames,
                              String rawDockerServiceLabels,
                              String rawDockerServiceNames,
                              Integer hazelcastPeerPort,
                              boolean bindSocketChannel,
                              boolean logAllServiceNamesOnFailedDiscovery,
                              boolean strictDockerServiceNameComparison) throws Exception {

        this(context,
                rawDockerNetworkNames,
                rawDockerServiceLabels,
                rawDockerServiceNames,
                hazelcastPeerPort,
                bindSocketChannel,
                new URI(System.getenv("DOCKER_HOST")),
                false,
                logAllServiceNamesOnFailedDiscovery,
                strictDockerServiceNameComparison);

    }

    public SwarmDiscoveryUtil(String context,
                              String rawDockerNetworkNames,
                              String rawDockerServiceLabels,
                              String rawDockerServiceNames,
                              Integer hazelcastPeerPort,
                              boolean bindSocketChannel,
                              URI swarmMgrUri,
                              boolean skipVerifySsl,
                              boolean logAllServiceNamesOnFailedDiscovery,
                              boolean strictDockerServiceNameComparison) throws Exception {

        this.context = context;
        this.swarmMgrUri = swarmMgrUri;
        this.skipVerifySsl = skipVerifySsl;
        this.logAllServiceNamesOnFailedDiscovery = logAllServiceNamesOnFailedDiscovery;
        this.strictDockerServiceNameComparison = strictDockerServiceNameComparison;


        if (this.swarmMgrUri == null) {
            if (System.getenv("DOCKER_HOST") != null && System.getenv("DOCKER_HOST").trim().isEmpty()) {
                this.swarmMgrUri = new URI(System.getenv("DOCKER_HOST"));
            }
        }

        this.bindSocketChannel = bindSocketChannel;
        this.rawDockerNetworkNames = rawDockerNetworkNames;
        this.rawDockerServiceLabels = rawDockerServiceLabels;
        this.rawDockerServiceNames = rawDockerServiceNames;
        this.hazelcastPeerPort = hazelcastPeerPort;

        if (rawDockerNetworkNames != null && !rawDockerNetworkNames.trim().isEmpty()) {
            for (String rawElement : rawDockerNetworkNames.split(",")) {
                if (!rawElement.trim().isEmpty()) {
                    dockerNetworkNames.add(rawElement.trim());
                }
            }
        } else {
            String msg = "SwarmDiscoveryUtil[" + this.context + "]() You must specify at least one value for 'docker-network-names'";
            throw new Exception(msg);
        }

        if (rawDockerServiceLabels != null && !rawDockerServiceLabels.trim().isEmpty()) {
            for (String rawElement : rawDockerServiceLabels.split(",")) {
                if (!rawElement.trim().isEmpty() && rawElement.indexOf('=') != -1) {
                    String[] labelVal = rawElement.split("=");
                    dockerServiceLabels.put(labelVal[0].trim(), labelVal[1].trim());
                }
            }
        }

        if (rawDockerServiceNames != null && !rawDockerServiceNames.trim().isEmpty()) {
            for (String rawElement : rawDockerServiceNames.split(",")) {
                if (!rawElement.trim().isEmpty()) {
                    dockerServiceNames.add(rawElement.trim());
                }
            }
        }

        // invalid setup
        if (dockerServiceLabels.size() == 0 && dockerServiceNames.size() == 0) {
            String msg = "SwarmDiscoveryUtil[" + this.context + "]() You must specify at least one value for "
                    + "either 'docker-service-names' or 'docker-service-labels'";
            throw new Exception(msg);
        }

        // discover self
        discoverSelf();
    }


    private void discoverSelf() throws Exception {

        Set<DiscoveredContainer> discoveredContainers = this.discoverContainers();

        Map<String, DiscoveredContainer> ip2ContainerMap = new HashMap<String, DiscoveredContainer>();
        for (DiscoveredContainer dc : discoveredContainers) {
            ip2ContainerMap.put(dc.getIp(), dc);
        }

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> e = ni.getInetAddresses();
            while (e.hasMoreElements()) {
                InetAddress inetAddress = e.nextElement();

                DiscoveredContainer dc = ip2ContainerMap.get(inetAddress.getHostAddress());

                // found myself..
                if (dc != null) {

                    // set local references
                    this.myContainer = dc;
                    this.myAddress = new Address(myContainer.getIp(), this.getHazelcastPeerPort());

                    if (bindSocketChannel) {

                        // configure ServerSocketChannel
                        this.serverSocketChannel = ServerSocketChannel.open();
                        ServerSocket serverSocket = serverSocketChannel.socket();
                        serverSocket.setReuseAddress(true);
                        serverSocket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);


                        try {
                            InetSocketAddress inetSocketAddress = new InetSocketAddress(this.myAddress.getHost(), this.getHazelcastPeerPort());
                            logger.info("SwarmDiscoveryUtil[" + this.context + "] Trying to bind inet socket address: " + inetSocketAddress);
                            serverSocket.bind(inetSocketAddress, SOCKET_BACKLOG_LENGTH);
                            logger.info("SwarmDiscoveryUtil[" + this.context + "] Bind successful to inet socket address: " + serverSocket.getLocalSocketAddress());
                            this.serverSocketChannel.configureBlocking(false);

                        } catch (Exception e2) {
                            serverSocket.close();
                            serverSocketChannel.close();
                            throw new HazelcastException(e2.getMessage(), e2);
                        }
                    }

                    break;
                }
            }
        }

    }


    public Set<String> getDockerNetworkNames() {
        return dockerNetworkNames;
    }


    public void setDockerNetworkNames(Set<String> dockerNetworkNames) {
        this.dockerNetworkNames = dockerNetworkNames;
    }


    public Map<String, String> getDockerServiceLabels() {
        return dockerServiceLabels;
    }


    public void setDockerServiceLabels(Map<String, String> dockerServiceLabels) {
        this.dockerServiceLabels = dockerServiceLabels;
    }


    public Set<String> getDockerServiceNames() {
        return dockerServiceNames;
    }


    public void setDockerServiceNames(Set<String> dockerServiceNames) {
        this.dockerServiceNames = dockerServiceNames;
    }


    public String getRawDockerNetworkNames() {
        return rawDockerNetworkNames;
    }


    public void setRawDockerNetworkNames(String rawDockerNetworkNames) {
        this.rawDockerNetworkNames = rawDockerNetworkNames;
    }


    public String getRawDockerServiceLabels() {
        return rawDockerServiceLabels;
    }


    public void setRawDockerServiceLabels(String rawDockerServiceLabels) {
        this.rawDockerServiceLabels = rawDockerServiceLabels;
    }


    public String getRawDockerServiceNames() {
        return rawDockerServiceNames;
    }


    public void setRawDockerServiceNames(String rawDockerServiceNames) {
        this.rawDockerServiceNames = rawDockerServiceNames;
    }


    public Integer getHazelcastPeerPort() {
        return hazelcastPeerPort;
    }


    public void setHazelcastPeerPort(Integer hazelcastPeerPort) {
        this.hazelcastPeerPort = hazelcastPeerPort;
    }

    public Set<DiscoveredContainer> discoverContainers() throws Exception {

        try {

            // our client
            DockerClient docker = null;

            Builder dockerBuilder = DefaultDockerClient.fromEnv();

            if (skipVerifySsl) {
                dockerBuilder.dockerCertificates(new SkipVerifyDockerCertificatesStore());
            }

            if (this.swarmMgrUri != null) {
                dockerBuilder.uri(this.swarmMgrUri);
            }

            docker = dockerBuilder.build();

            StringBuffer sb = new StringBuffer("SwarmDiscoveryUtil[" + this.context + "].discoverNodes(): via DOCKER_HOST: " + docker.getHost() + "\n");
            sb.append("docker-network-names = " + this.getRawDockerNetworkNames() + "\n");
            sb.append("docker-service-names = " + this.getRawDockerServiceNames() + "\n");
            sb.append("docker-service-labels = " + this.getRawDockerServiceLabels() + "\n");
            sb.append("swarmMgrUri = " + this.swarmMgrUri + "\n");
            sb.append("skipVerifySsl = " + this.skipVerifySsl + "\n");
            sb.append("logAllServiceNamesOnFailedDiscovery = " + this.logAllServiceNamesOnFailedDiscovery + "\n");
            sb.append("strictDockerServiceNameComparison = " + this.strictDockerServiceNameComparison + "\n");
            logger.info(sb.toString());

            // our discovered containers
            Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

            // the relevant networks we are looking for
            Map<String, Network> relevantNetIds2Networks = new TreeMap<String, Network>(String.CASE_INSENSITIVE_ORDER);

            // build list of relevant networkIds -> Network objects we care about
            for (String dockerNetworkName : this.getDockerNetworkNames()) {
                List<Network> networks = docker.listNetworks(ListNetworksParam.byNetworkName(dockerNetworkName));
                for (Network network : networks) {
                    relevantNetIds2Networks.put(network.id(), network);
                    logger.info("SwarmDiscoveryUtil[" + this.context + "] Found relevant docker network: " + network.name() + "[" + network.id() + "]");
                }
            }

            if (relevantNetIds2Networks.size() == 0) {
                logger.warning("SwarmDiscoveryUtil[" + this.context + "] Did not find relevant docker network for: " + this.getDockerNetworkNames());
            }

            // Collect all relevant containers for services with services-names on the relevant networks
            for (String dockerServiceName : this.getDockerServiceNames()) {
                logger.info("SwarmDiscoveryUtil[" + this.context + "] Invoking criteria-based container discovery for dockerServiceName=" + dockerServiceName);
                ServiceFilter serviceFilter = strictDockerServiceNameComparison ? new NameBasedServiceFilter(dockerServiceName) : NullServiceFilter.getInstance();
                discoveredContainers.addAll(
                        discoverContainersViaCriteria(docker,
                                relevantNetIds2Networks,
                                Criteria.builder().serviceName(dockerServiceName).build(),
                                serviceFilter));
            }


            // Collect all relevant containers for services matching the labels on the relevant networks
            for (String dockerServiceLabel : this.getDockerServiceLabels().keySet()) {
                String labelValue = this.getDockerServiceLabels().get(dockerServiceLabel);
                logger.info("SwarmDiscoveryUtil[" + this.context + "] Invoking criteria-based container discovery for service label: " + dockerServiceLabel + "=" + labelValue);
                discoveredContainers.addAll(
                        discoverContainersViaCriteria(docker,
                                relevantNetIds2Networks,
                                Criteria.builder().addLabel(dockerServiceLabel, labelValue).build()));
            }

            // Optionally dump all available services names when configured criteria
            // yields zero containers
            if (discoveredContainers.size() == 0 && logAllServiceNamesOnFailedDiscovery) {
                try {
                    List<Service> allServices = docker.listServices();

                    StringBuilder sb2 = new StringBuilder();
                    if (allServices != null) {
                        String delim = "";
                        for (Service s : allServices) {
                            sb2.append(delim).append(s.spec().name());
                            delim = ",";
                        }
                    }

                    logger.fine("SwarmDiscoveryUtil[" + this.context + "] discoveredContainers.size()=0; logAllServiceNamesOnFailedDiscovery=true, "
                            + "ALL available docker service names=[" + sb2.toString() + "]");

                } catch (Throwable e) {
                    logger.warning("SwarmDiscoveryUtil[" + this.context + "] Unexpected error in logAllServiceNamesOnFailedDiscovery=true handling:" + e.getMessage(), e);
                }
            }

            return discoveredContainers;

        } catch (Exception e) {
            throw new Exception("SwarmDiscoveryUtil[" + this.context + "].discoverContainers() unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Test the {@link NetworkAttachment} against the local {@link NetworkInterface}s.
     * This returns true if the {@link NetworkAttachment} contains an address that is
     * in {@link NetworkInterface#getNetworkInterfaces()}
     *
     * @param networkAttachment
     * @return true if the given {@link NetworkAttachment} is this local node
     * @throws SocketException
     */
    private boolean isSelf(NetworkAttachment networkAttachment) throws SocketException {
        for (String addrWithSubnet : networkAttachment.addresses()) {
            String addr = addrWithSubnet.split("/")[0];
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> e = ni.getInetAddresses();
                while (e.hasMoreElements()) {
                    InetAddress inetAddress = e.nextElement();
                    //Split the networkAttachment address by the "/" since it contains the subnet
                    if (inetAddress.getHostAddress().equals(addr)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Discover containers on the relevant networks that match the given
     * service criteria, using additionally a NullServiceFilter
     *
     * @param docker
     * @param relevantNetIds2Networks
     * @param criteria
     * @return set of DiscoveredContainer instances
     * @throws Exception
     */
    private Set<DiscoveredContainer> discoverContainersViaCriteria(DockerClient docker,
                                                                   Map<String, Network> relevantNetIds2Networks,
                                                                   Service.Criteria criteria) throws Exception {
        // no ServiceFilter provided, so use one with no constraints
        return discoverContainersViaCriteria(docker, relevantNetIds2Networks, criteria, NullServiceFilter.getInstance());
    }

    /**
     * Discover containers on the relevant networks that match the given
     * service criteria and service filter
     *
     * @param docker
     * @param relevantNetIds2Networks
     * @param criteria                to be passed as a parameter to the /services request
     * @param serviceFilter           additional criteria to be applied to Services returned from the /services request
     * @return set of DiscoveredContainer instances
     * @throws Exception
     */
    private Set<DiscoveredContainer> discoverContainersViaCriteria(DockerClient docker,
                                                                   Map<String, Network> relevantNetIds2Networks,
                                                                   Service.Criteria criteria,
                                                                   ServiceFilter serviceFilter) throws Exception {

        Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

        // find all relevant services given the criteria....
        List<Service> services = docker.listServices(criteria);

        if (services == null) {
            logger.fine("SwarmDiscoveryUtil[" + this.context + "] No service match for given criteria, docker.listServices(criteria) returned null.");
            return discoveredContainers;
        }

        if (services.size() == 0) {
            logger.fine("SwarmDiscoveryUtil[" + this.context + "] No service match for given criteria, docker.listServices(criteria) returned 0");
            return discoveredContainers;
        }

        logger.fine("SwarmDiscoveryUtil[" + this.context + "] Number of services matching given criteria = " + services.size());

        for (Service service : services) {

            if (serviceFilter.reject(service)) {
                logger.fine("Service with name " + service.spec().name() + " rejected by filter " + serviceFilter);
                continue;
            }

            logger.fine("SwarmDiscoveryUtil[" + this.context + "] Processing service with name=" + service.spec().name());

            // crawl through all VIPs the service is on
            for (EndpointVirtualIp vip : service.endpoint().virtualIps()) {

                logger.fine("SwarmDiscoveryUtil[" + this.context + "] Processing service endpoint with networkId=" + vip.networkId() + ", addr=" + vip.addr() + " for service with name=" + service.spec().name());

                // does the service have a VIP on one of the networks we care about?
                if (relevantNetIds2Networks.containsKey(vip.networkId())) {

                    // get the network object that the vip is on
                    Network network = relevantNetIds2Networks.get(vip.networkId());

                    logger.info("SwarmDiscoveryUtil[" + this.context + "] Found qualifying docker service[" + service.spec().name() + "] "
                            + "on network: " + network.name() + "[" + network.id() + ":" + vip.addr() + "]");

                    // if so, then lets find all its tasks (actual container instances of the service)
                    List<Task> tasks = docker.listTasks(Task.Criteria.builder().serviceName(service.spec().name()).build());

                    if (tasks == null) {
                        logger.warning("SwarmDiscoveryUtil[" + this.context + "] docker.listTasks() returned NULL for service:" + service.spec().name() + ", skipping this service");
                        continue;
                    }

                    // for every task, lets get its network attachments
                    for (Task task : tasks) {

                        ImmutableList<NetworkAttachment> networkAttachments = task.networkAttachments();
                        if (networkAttachments == null) {
                            logger.warning("SwarmDiscoveryUtil[" + this.context + "] task.networkAttachments() returned NULL for task "
                                    + "id:" + task.id() +
                                    " name:" + task.name() +
                                    " nodeid:" + task.nodeId() +
                                    " I am skipping this task for service: " + service.spec().name());
                            continue;
                        }

                        for (NetworkAttachment networkAttachment : networkAttachments) {

                            // if the network ID the task is = the current network we care about for
                            // the service.. then lets treat it as a "discovered container"
                            // that we actually care about
                            if (networkAttachment.network().id().equals(vip.networkId())) {
                                boolean foundSelfService = isSelf(networkAttachment);
                                if (foundSelfService) {
                                    logger.info("SwarmDiscoveryUtil[" + this.context + "] Found own task, adding regardless of state.");
                                }
                                // if container is in status 'running', then add it!
                                if (TaskStatus.TASK_STATE_RUNNING.equals(task.status().state()) || foundSelfService) {

                                    logger.info("SwarmDiscoveryUtil[" + this.context + "] Found qualifying docker service task[taskId: " + task.id() + ", container: " + task.status().containerStatus().containerId() + ", state: " + task.status().state() + "] "
                                            + "on network: " + network.name() + "[" + network.id() + ":" + networkAttachment.addresses().iterator().next() + "]");

                                    discoveredContainers.add(new DiscoveredContainer(network, service, task, networkAttachment));
                                }
                            }
                        }
                    }
                }
            }
        }
        logger.info("SwarmDiscoveryUtil[" + this.context + "] Returning set of discovered containers with size=" + discoveredContainers.size());
        return discoveredContainers;
    }


    public DiscoveredContainer getMyContainer() {
        return myContainer;
    }


    public Address getMyAddress() {
        return myAddress;
    }


    public ServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

}



