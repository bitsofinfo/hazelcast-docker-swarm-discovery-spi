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

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;

import java.util.Arrays;
import java.util.Collection;

/**
 * Configuration required for <code>DockerDNSRRDiscoveryStrategy</code>
 *
 * @author Cardds
 */
public class DockerDNSRRDiscoveryConfiguration {

    /**
     * Property definition to load CSV of services
     */
    public static final PropertyDefinition SERVICESCSV =
            new SimplePropertyDefinition(
                    "peerServicesCsv",
                    PropertyTypeConverter.STRING
            );

    /**
     * Full list of all properties referenced by this configuration
     */
    public static final Collection<PropertyDefinition> PROPERTIES = Arrays.asList(SERVICESCSV);


    private DockerDNSRRDiscoveryConfiguration() {
    }
}
