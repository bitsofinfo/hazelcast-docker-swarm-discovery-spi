package org.bitsofinfo.hazelcast.discovery.docker.swarm.filter;

import com.spotify.docker.client.messages.swarm.Service;

/**
 * A concrete implementation of ServiceFilter which accepts all given Service instances, rejecting none.
 */
public class NullServiceFilter extends AbstractServiceFilter {

    /**
     * static instance for use, to avid instantiation overheads
     */
    private static final ServiceFilter instance = new NullServiceFilter();

    /**
     * private no-arg constructor to preclude unwarranted instantiation
     */
    private NullServiceFilter() {
    }

    /**
     * @return static singleton instance
     */
    public static ServiceFilter getInstance() {
        return instance;
    }

    /**
     * @see ServiceFilter#accept(Service)
     */
    @Override
    public boolean accept(Service service) {
        return true;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "NullServiceFilter";
    }
}
