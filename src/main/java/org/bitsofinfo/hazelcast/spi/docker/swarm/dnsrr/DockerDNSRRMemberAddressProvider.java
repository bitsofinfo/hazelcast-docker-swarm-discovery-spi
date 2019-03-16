/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitsofinfo.hazelcast.spi.docker.swarm.dnsrr;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.spi.MemberAddressProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Member Address Provider for hazelcast that cross-references the service
 * name resolution against the loaded networks in-container to determine the
 * appropriate bind address for the hazelcast instance
 *
 * @author Cardds
 */
public class DockerDNSRRMemberAddressProvider
        implements MemberAddressProvider {
    public static Properties properties;
    public static InetSocketAddress bindAddress = null;
    ILogger logger = Logger.getLogger(DockerDNSRRMemberAddressProvider.class);

    public DockerDNSRRMemberAddressProvider(Properties properties)
            throws
            NumberFormatException,
            SocketException,
            UnknownHostException {
        DockerDNSRRMemberAddressProvider.properties = properties;

        if (properties != null) {
            String serviceName = properties.getProperty(
                    DockerDNSRRMemberAddressProviderConfig.SERVICENAME
            );
            String portString = properties.getProperty(
                    DockerDNSRRMemberAddressProviderConfig.SERVICEPORT
            );
            Integer port = 5701;

            if (
                    portString != null &&
                            !"".equals(
                                    portString.trim()
                            )
            ) {
                try {
                    port = Integer.valueOf(portString);
                } catch (NumberFormatException nfe) {
                    logger.severe(
                            "Unable to parse " +
                                    DockerDNSRRMemberAddressProviderConfig.SERVICEPORT +
                                    " with value " +
                                    portString
                    );
                    throw nfe;
                }
            }

            Set<InetAddress> potentialInetAddresses =
                    resolveServiceName(serviceName);

            Enumeration<NetworkInterface> networkInterfaces;
            Enumeration<InetAddress> networkInterfaceAddresses;
            InetAddress address;

            try {
                networkInterfaces = NetworkInterface.getNetworkInterfaces();

                while (
                        networkInterfaces.hasMoreElements() &&
                                bindAddress == null
                ) {
                    networkInterfaceAddresses =
                            networkInterfaces.nextElement().getInetAddresses();

                    while (networkInterfaceAddresses.hasMoreElements()) {
                        address = networkInterfaceAddresses.nextElement();
                        if (address != null) {
                            logger.info("Checking address " + address.toString());
                        }

                        if (
                                potentialInetAddresses.contains(address)
                        ) {
                            bindAddress = new InetSocketAddress(
                                    address,
                                    port
                            );
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                logger.severe(
                        "Unable to bind socket: " + e.toString()
                );
                throw e;
            }

        }
    }

    private Set<InetAddress> resolveServiceName(String serviceName)
            throws UnknownHostException {
        Set<InetAddress> addresses = new HashSet<>();

        try {
            InetAddress[] inetAddresses;
            inetAddresses = InetAddress.getAllByName(serviceName);

            addresses.addAll(
                    Arrays.asList(inetAddresses)
            );

            logger.info(
                    "Resolved domain name '" + serviceName +
                            "' to address(es): " + addresses
            );
        } catch (UnknownHostException e) {
            logger.severe(
                    "Unable to resolve service name " + serviceName
            );
            throw e;
        }

        return addresses;
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
