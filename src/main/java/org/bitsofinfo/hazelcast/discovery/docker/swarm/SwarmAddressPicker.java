package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.instance.AddressPicker;
import com.hazelcast.nio.Address;

public class SwarmAddressPicker implements AddressPicker {
	
	// from: https://github.com/hazelcast/hazelcast/blob/210475c806328c6655ea551f6fc59ef8220b601d/hazelcast/src/main/java/com/hazelcast/instance/DefaultAddressPicker.java
	private static final int SOCKET_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(1);
	private static final int SOCKET_BACKLOG_LENGTH = 100;
	
	private SwarmDiscoveryConfig swarmDiscoveryConfig = null;
	
	private DiscoveredContainer myContainer = null;
	private Address myAddress = null;
	private ServerSocketChannel serverSocketChannel = null;
	
	public SwarmAddressPicker() {
		
		String rawDockerNetworkNames = System.getProperty("dockerNetworkNames");
		String rawDockerServiceLabels = System.getProperty("dockerServiceLabels");
		String rawDockerServiceNames = System.getProperty("dockerServiceNames");
		Integer hazelcastPeerPort = Integer.valueOf(System.getProperty("hazelcastPeerPort"));
		
		this.swarmDiscoveryConfig = new SwarmDiscoveryConfig(rawDockerNetworkNames,
															 rawDockerServiceLabels,
															 rawDockerServiceNames,
															 hazelcastPeerPort);
		
	}

	@Override
	public void pickAddress() throws Exception {
		Set<DiscoveredContainer> discoveredContainers = this.swarmDiscoveryConfig.discoverContainers();
		
		Map<String,DiscoveredContainer> ip2ContainerMap = new HashMap<String,DiscoveredContainer>();
		for (DiscoveredContainer dc : discoveredContainers) {
			ip2ContainerMap.put(dc.getIp(), dc);
		}
		
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> e = ni.getInetAddresses();
            while (e.hasMoreElements()) {
                InetAddress inetAddress = e.nextElement();
                
                DiscoveredContainer dc = ip2ContainerMap.get(inetAddress.getHostAddress());
                
                if (dc != null) {
                	this.myContainer = dc;
                	this.myAddress = new Address(myContainer.getIp(), this.swarmDiscoveryConfig.getHazelcastPeerPort());
                	
                	
                	this.serverSocketChannel = ServerSocketChannel.open();
                    ServerSocket serverSocket = serverSocketChannel.socket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
                    
                    
                    try {
                    	InetSocketAddress inetSocketAddress = new InetSocketAddress(this.myAddress.getHost(), this.swarmDiscoveryConfig.getHazelcastPeerPort());
                        System.out.println("Trying to bind inet socket address: " + inetSocketAddress);
                        serverSocket.bind(inetSocketAddress, SOCKET_BACKLOG_LENGTH);
                        System.out.println("Bind successful to inet socket address: " + serverSocket.getLocalSocketAddress());
                        this.serverSocketChannel.configureBlocking(false);
                        
                    } catch (Exception e2) {
                        serverSocket.close();
                        serverSocketChannel.close();
                        throw new HazelcastException(e2.getMessage(), e2);
                    }

                	break;
                }
            }
        }
		
	}

	@Override
	public Address getBindAddress() {
		return this.myAddress;	
	}

	@Override
	public Address getPublicAddress() {
		return this.myAddress;
	}

	@Override
	public ServerSocketChannel getServerSocketChannel() {
		return this.serverSocketChannel;
	}

}
