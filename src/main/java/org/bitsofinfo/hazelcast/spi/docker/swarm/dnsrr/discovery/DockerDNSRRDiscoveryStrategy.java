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

package org.bitsofinfo.hazelcast.spi.docker.swarm.dnsrr.discovery;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Hazelcast discovery strategy to be used with docker endpoint_mode: dnsrr
 * This expects a CSV of peer services and resolves each one to component IP
 * addresses within the docker network to connect to each individually.
 *
 * @author Cardds
 *
 */
public class DockerDNSRRDiscoveryStrategy
        extends AbstractDiscoveryStrategy
{
    private ILogger logger;

    public DockerDNSRRDiscoveryStrategy(
            ILogger logger,
            //The Comparable raw type is defined by AbstractDiscoveryStrategy as
            //the value for the properties element; passing through here
            @SuppressWarnings("rawtypes") Map<String, Comparable> properties
    ) {
        super(logger, properties);
        this.logger = logger;
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        LinkedList<DiscoveryNode> discoveryNodes =
                new LinkedList<DiscoveryNode>();

        //Pull properties
        String servicesCsv = getOrDefault(
                DockerDNSRRDiscoveryConfiguration.SERVICESCSV,
                ""
        );

        //If there are no services configured, no point in doing anything.
        if(
                servicesCsv == null ||
                        servicesCsv.trim().isEmpty()
        ) {
            return discoveryNodes;
        }

        Set<InetAddress> serviceNameResolutions =
                new HashSet<InetAddress>();
        String[] serviceHostnameAndPort;
        Integer port = 5701;

        //Loop for every service defined in the CSV
        for(String service: servicesCsv.split(",")) {
            if(!service.trim().isEmpty()) {
                //CSV should be composed of hostname:port
                serviceHostnameAndPort = service.split(":");

                //Validate hostname exists
                if (
                        serviceHostnameAndPort[0] == null ||
                                serviceHostnameAndPort[0].trim().isEmpty()
                ) {
                    logger.info(
                            "Unable to resolve service hostname " +
                                    serviceHostnameAndPort[0] +
                                    " Skipping service entry."
                    );
                    continue;
                }
                //Validate port exists; assume default port if it doesn't
                if (
                        serviceHostnameAndPort.length <= 1 ||
                                serviceHostnameAndPort[1] == null ||
                                serviceHostnameAndPort[1].trim().isEmpty()
                ) {
                    port = 5701;
                } else {
                    try {
                        port = Integer.valueOf(
                                serviceHostnameAndPort[1]
                        );
                    } catch(NumberFormatException nfe) {
                        logger.info(
                                "Unable to parse port " +
                                        serviceHostnameAndPort[1] +
                                        " Skipping service entry."
                        );
                        continue;
                    }
                }

                //Resolve service hostname to a set of IP addresses, if any
                serviceNameResolutions =
                        resolveDomainNames(
                                serviceHostnameAndPort[0]
                        );

                //Add all IP addresses for service hostname with the given port.
                for(InetAddress resolution: serviceNameResolutions) {
                    discoveryNodes.add(
                            new SimpleDiscoveryNode(
                                    new Address(
                                            resolution,
                                            port
                                    )
                            )
                    );
                }
            }
        }

        return discoveryNodes;
    }

    private Set<InetAddress> resolveDomainNames(String domainName) {
        Set<InetAddress> addresses = new HashSet<InetAddress>();

        try {
            InetAddress[] inetAddresses;
            inetAddresses = InetAddress.getAllByName(domainName);

            addresses.addAll(
                    Arrays.asList(inetAddresses)
            );

            logger.info(
                    "Resolved domain name '" + domainName + "' to address(es): " + addresses
            );
        } catch(UnknownHostException e) {
            logger.severe(
                    "Unable to resolve domain name " + domainName
            );
        }

        return addresses;
    }
}
