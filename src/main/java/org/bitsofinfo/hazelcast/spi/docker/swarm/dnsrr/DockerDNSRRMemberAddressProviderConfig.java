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

/**
 * Configuration for <code>DockerDNSRRMemberAddressProvider</code>
 *
 * @author Cardds
 *
 */
public class DockerDNSRRMemberAddressProviderConfig {
    /**
     * Property definition to load name of this docker service
     */
    public static final String SERVICENAME = "serviceName";

    /**
     * Property definition to load exposed port for hazelcast
     * 
     * Note: This exists because MemberAddressProvider has no way to access
     *       the network configuration as of the time of writing this.
     */
    public static final String SERVICEPORT = "servicePort";
}
