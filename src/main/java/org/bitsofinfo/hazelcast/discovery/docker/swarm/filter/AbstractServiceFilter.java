package org.bitsofinfo.hazelcast.discovery.docker.swarm.filter;

import com.spotify.docker.client.messages.swarm.Service;

/**
 * Abstract filter, providing a concrete implementation of reject() base on negation of accept()
 */
public abstract class AbstractServiceFilter implements ServiceFilter {

    /**
     * @param service Service returned from criteria-based /services request
     * @return
     * @see ServiceFilter#reject(Service)
     */
    @Override
    public boolean reject(Service service) {
        return !accept(service);
    }
}
