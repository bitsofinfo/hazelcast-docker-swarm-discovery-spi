package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListNetworksParam;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.EndpointVirtualIp;
import com.spotify.docker.client.messages.swarm.NetworkAttachment;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.Service.Criteria;

public class SwarmDiscoveryConfig {
	
	private Set<String> dockerNetworkNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	private Map<String,String> dockerServiceLabels = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
	private Set<String> dockerServiceNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	
	private String rawDockerNetworkNames = null;
	private String rawDockerServiceLabels = null;
	private String rawDockerServiceNames = null;
	
	private Integer hazelcastPeerPort = 5701;


	public SwarmDiscoveryConfig(String rawDockerNetworkNames, 
								String rawDockerServiceLabels,
								String rawDockerServiceNames, 
								Integer hazelcastPeerPort) {
		
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
			String msg = "You must specify at least one value for 'docker-network-names' in the swarm discovery SPI config";
			System.out.println(msg);
			throw new RuntimeException(msg);
		}

		if (rawDockerServiceLabels != null && !rawDockerServiceLabels.trim().isEmpty()) {
			for (String rawElement : rawDockerServiceLabels.split(",")) {
				if (!rawElement.trim().isEmpty() && rawElement.indexOf('=') != -1) {
					String[] labelVal = rawElement.split("=");
					dockerServiceLabels.put(labelVal[0].trim(),labelVal[1].trim());
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
			String msg = "You must specify at least one value for "
					+ "either 'docker-service-names' or 'docker-service-labels' in the swarm discovery SPI config";
			System.out.println(msg);
			throw new RuntimeException(msg);
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
		
		System.out.println("DNODES!");

		List<DiscoveryNode> toReturn = new ArrayList<DiscoveryNode>();

		try {

			// our client
			final DockerClient docker = DefaultDockerClient.fromEnv().build();
			
			StringBuffer sb = new StringBuffer("discoverNodes(): via DOCKER_HOST: " + docker.getHost() + "\n");
			sb.append("docker-network-names = " + this.getRawDockerNetworkNames() + "\n");
			sb.append("docker-service-names = " + this.getRawDockerServiceNames() + "\n");
			sb.append("docker-service-labels = " + this.getRawDockerServiceLabels() + "\n");
			System.out.println(sb.toString());

			// our discovered containers
			Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

			// the relevant networks we are looking for
			Map<String,Network> relevantNetIds2Networks = new TreeMap<String,Network>(String.CASE_INSENSITIVE_ORDER);

			// build list of relevant networkIds -> Network objects we care about
			for (String dockerNetworkName : this.getDockerNetworkNames()) {
				List<Network> networks = docker.listNetworks(ListNetworksParam.byNetworkName(dockerNetworkName));
				for (Network network : networks) {
					relevantNetIds2Networks.put(network.id(),network);
					System.out.println("Found relevant network: " + network.name() +"["+ network.id() + "]");
				}
			}

			// Collect all relevant containers for services with services-names on the relevant networks
			for (String dockerServiceName : this.getDockerServiceNames()) {
				discoveredContainers.addAll(
						discoverContainersViaCriteria(docker,
										   relevantNetIds2Networks,
										   Criteria.builder().serviceName(dockerServiceName).build()));
			}
			
			
			// Collect all relevant containers for services matching the labels on the relevant networks
			for (String dockerServiceLabel : this.getDockerServiceLabels().keySet()) {
				String labelValue = this.getDockerServiceLabels().get(dockerServiceLabel);
				discoveredContainers.addAll(
						discoverContainersViaCriteria(docker,
										   relevantNetIds2Networks,
										   Criteria.builder().addLabel(dockerServiceLabel, labelValue).build()));
			}


			return discoveredContainers;

		} catch(Exception e) {
			throw new Exception("discoverContainers() unexpected error: " + e.getMessage(),e);
		}
	}
	
	/**
	 * Discover containers on the relevant networks that match the given
	 * service critiera. 
	 * 
	 * @param docker
	 * @param relevantNetIds2Networks
	 * @param criteria
	 * @return set of DiscoveredContainer instances
	 * @throws Exception
	 */
	private Set<DiscoveredContainer> discoverContainersViaCriteria(DockerClient docker, 
			Map<String,Network> relevantNetIds2Networks, 
			Service.Criteria criteria) throws Exception {

		Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

		// find all relevant services given the criteria....
		List<Service> services = docker.listServices(criteria);
		
		for (Service service : services) {

			// crawl through all VIPs the service is on
			for (EndpointVirtualIp vip : service.endpoint().virtualIps()) {

				// does the service have a VIP on one of the networks we care about?
				if (relevantNetIds2Networks.containsKey(vip.networkId())) {

					// get the network object that the vip is on
					Network network = relevantNetIds2Networks.get(vip.networkId());

					// if so, then lets find all its tasks (actual container instances of the service)
					List<Task> tasks = docker.listTasks(Task.Criteria.builder().serviceName(service.spec().name()).build());

					// for every task, lets get its network attachments
					for (Task task : tasks) {
						for (NetworkAttachment networkAttachment : task.networkAttachments()) {

							// if the network ID the task is = the current network we care about for 
							// the service.. then lets treat it as a "dicovered container"
							// that we actually care aboute
							if (networkAttachment.network().id().equals(vip.networkId())) {
							
								// add it!
								discoveredContainers.add(
										new DiscoveredContainer(network, service, task, networkAttachment));
							}

						}
					}

				}
			}
		}

		return discoveredContainers;
	}

}



