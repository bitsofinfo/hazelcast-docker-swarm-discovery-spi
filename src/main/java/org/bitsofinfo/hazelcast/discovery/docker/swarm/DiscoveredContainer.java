package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.NetworkAttachment;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;

/**
 * Contains information about a discovered container
 * within a docker swarm service that is on a particular network
 * <p>
 * The Container's VIP on the related network is it's unique id
 *
 * @author bitsofinfo
 */
public class DiscoveredContainer {

    private Network network;

    private Service service;

    private Task task;

    private NetworkAttachment relevantNetworkAttachment;


    public DiscoveredContainer(Network network, Service service, Task task, NetworkAttachment relevantNetworkAttachment) {
        this.network = network;
        this.service = service;
        this.task = task;
        this.relevantNetworkAttachment = relevantNetworkAttachment;
    }


    public String getIp() {
        // return the first address, should only be one"x.x.x.x/mask"
        return relevantNetworkAttachment.addresses().iterator().next().split("/")[0];
    }


    public String getNetworkName() {
        return network.name();
    }


    public String getNetworkId() {
        return network.id();
    }


    public String getServiceName() {
        return service.spec().name();
    }


    public String getServiceId() {
        return service.id();
    }


    public String getContainerId() {
        return task.status().containerStatus().containerId();
    }


    public String getContainerImage() {
        return task.spec().containerSpec().image();
    }


    @Override
    public int hashCode() {
        return this.getIp().hashCode();
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof DiscoveredContainer) {
            return o.hashCode() == this.hashCode();
        }

        return false;
    }


    @Override
    public String toString() {
        return this.getIp() + " : " + this.getContainerId();
    }


}
