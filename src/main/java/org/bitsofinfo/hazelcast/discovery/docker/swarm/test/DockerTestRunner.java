package org.bitsofinfo.hazelcast.discovery.docker.swarm.test;

import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmMemberAddressProvider;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SystemPrintLogger;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.AddressPicker;
import com.hazelcast.instance.DefaultNodeContext;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.NodeContext;

/**
 * Simple class for manually spawning hz instances and watching what happens
 * as they discover one another
 * 
 * @author bitsofinfo
 *
 */
public class DockerTestRunner {

	public static void main(String[] args) throws Exception {
		
		Config conf =new ClasspathXmlConfig("hazelcast-docker-swarm-discovery-spi-example.xml");

		HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
				.newHazelcastInstance(conf,"hazelcast-docker-swarm-discovery-spi-example",new DefaultNodeContext());
		
		Thread.currentThread().sleep(300000);
		
		System.exit(0);
	}
}
