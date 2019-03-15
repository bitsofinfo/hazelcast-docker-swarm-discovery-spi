package org.bitsofinfo.hazelcast.discovery.docker.swarm.filter;

import com.spotify.docker.client.messages.swarm.Service;

/**
 * A filter which determines whether a Service returned from a criteria-based /services request should actually be considered as a potential location of cluster peers
 */

public interface ServiceFilter {
    /**
     * Apply criteria and return true if this service meets the criteria.
     * @param service Service returned from criteria-based /services request
     * @return true if this Service meets additional criteria necessary for consideration
     */
    boolean accept(Service service);

    /**
     * Apply criteria and return false if this service meets the criteria.
     * This method exists so that tests for rejection can be expressed without inline negation.
     * @param service Service returned from criteria-based /services request
     * @return true if this Service does not meet additional criteria necessary for consideration
     */
    boolean reject(Service service);
}
