package org.bitsofinfo.hazelcast.spi.docker.swarm.dnsrr;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

import com.spotify.docker.client.shaded.org.apache.http.util.Asserts;
import org.apache.commons.lang.StringUtils;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import com.hazelcast.spi.MemberAddressProvider;

/**
 * {@link MemberAddressProvider} that configures the bind address for Hazelcast
 * clustering by using the network interface name and service port.<br />
 * <p>
 * <b>Configuration properties</b>
 * </p>
 * <i>{@link #NETWORK_INTERFACE_NAME}</i> - The network interface to use to
 * determine bind address. Defaults to <i>eth0</i>. <br />
 * <i>{@link DockerDNSRRMemberAddressProviderConfig#SERVICEPORT}</i> - The
 * service port to use bind to. Defaults to <i>5701</i>.
 */
public class DockerNetworkInterfaceBasedMemberAddressProvider implements MemberAddressProvider {

    ILogger logger = Logger.getLogger(DockerNetworkInterfaceBasedMemberAddressProvider.class);

    protected static final String DEFAULT_NETWORK_INTERFACE = "eth0";

    protected static final int DEFAULT_SERVICE_PORT = 5701;

    private InetSocketAddress bindAddress = null;

    public DockerNetworkInterfaceBasedMemberAddressProvider(Properties properties)
            throws NumberFormatException, SocketException, UnknownHostException {
        Asserts.notNull(properties, "Configuration properties are null");
        Integer servicePort = determineServicePortToUse(properties);
        String networkInterfaceToUse = determineNetworkInterfaceToUse(properties);
        this.bindAddress = determineBindAddressToUse(servicePort, networkInterfaceToUse);
    }

    private InetSocketAddress determineBindAddressToUse(Integer servicePort,
                                                        String networkInterfaceToUse) throws SocketException {
        InetSocketAddress bind = null;
        NetworkInterface netInterface = NetworkInterface.getByName(networkInterfaceToUse);
        if (netInterface != null) {
            Enumeration<InetAddress> netInterfaceAddresses = netInterface.getInetAddresses();
            while (netInterfaceAddresses != null && netInterfaceAddresses.hasMoreElements()) {
                InetAddress address = netInterfaceAddresses.nextElement();
                bind = new InetSocketAddress(address, servicePort);
                logger.info("Binding to address {}" + bind);
                break;
            }
        }
        return bind;
    }

    protected Integer determineServicePortToUse(Properties properties) {
        String servicePortFromProperties = properties
                .getProperty(DockerDNSRRMemberAddressProviderConfig.SERVICEPORT);
        Integer servicePort = DEFAULT_SERVICE_PORT;
        if (!StringUtils.isEmpty(servicePortFromProperties)) {
            try {
                servicePort = Integer.valueOf(servicePortFromProperties);
            } catch (NumberFormatException nfe) {
                logger.severe("Configured service port {} is not a valid number"
                        + servicePortFromProperties);
                throw nfe;
            }
        }
        logger.info("Using service port {}" + String.valueOf(servicePort));
        return servicePort;
    }

    private String determineNetworkInterfaceToUse(Properties properties) {
        String networkInterfaceNameFromProperties = properties
                .getProperty(DockerDNSRRMemberAddressProviderConfig.NETWORK_INTERFACE_NAME);
        String networkInterfaceToUse = DEFAULT_NETWORK_INTERFACE;
        if (!StringUtils.isEmpty(networkInterfaceNameFromProperties)) {
            networkInterfaceToUse = networkInterfaceNameFromProperties;
        }
        logger.info("Using network interface {}" + networkInterfaceToUse);
        return networkInterfaceToUse;
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    @Override
    public InetSocketAddress getPublicAddress() {
        return bindAddress;
    }
}