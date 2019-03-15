package org.bitsofinfo.hazelcast.discovery.docker.swarm.test;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.AddressPicker;
import com.hazelcast.instance.DefaultNodeContext;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.NodeContext;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker;
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SystemPrintLogger;

/**
 * Simple class for manually spawning hz instances and watching what happens
 * as they discover one another
 *
 * @author bitsofinfo
 */
public class DockerTestRunner {

    public static void main(String[] args) throws Exception {


        if (System.getProperty("swarm-bind-method").equalsIgnoreCase("address-picker")) {

            Config conf = new ClasspathXmlConfig("hazelcast-docker-swarm-discovery-spi-example-address-picker.xml");

            NodeContext nodeContext = new DefaultNodeContext() {
                @Override
                public AddressPicker createAddressPicker(Node node) {
                    return new SwarmAddressPicker(new SystemPrintLogger());
                }
            };

            HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
                    .newHazelcastInstance(conf, "hazelcast-docker-swarm-discovery-spi-example", nodeContext);


        } else if (System.getProperty("swarm-bind-method").equalsIgnoreCase("member-address-provider")) {

            Config conf = new ClasspathXmlConfig("hazelcast-docker-swarm-discovery-spi-example-member-address-provider.xml");


            HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
                    .newHazelcastInstance(conf, "hazelcast-docker-swarm-discovery-spi-example", new DefaultNodeContext());
        } else if (System.getProperty("swarm-bind-method").equalsIgnoreCase("dockerDNSRR")) {
            Config conf =
                    new ClasspathXmlConfig(
                            "hazelcast-docker-swarm-dnsrr-discovery-spi-example.xml"
                    );

            HazelcastInstance hazelcastInstance =
                    HazelcastInstanceFactory.newHazelcastInstance(conf);
        }


        Thread.currentThread().sleep(400000);

        System.exit(0);
    }
}
