package org.bitsofinfo.hazelcast.discovery.docker.swarm;

import javax.net.ssl.HostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import javax.net.ssl.SSLContext;

import com.spotify.docker.client.DockerCertificatesStore;

public class SkipVerifyDockerCertificatesStore implements DockerCertificatesStore {

	@Override
	public SSLContext sslContext() {
		return SSLContexts.createDefault();
	}

	@Override
	public HostnameVerifier hostnameVerifier() {
		return NoopHostnameVerifier.INSTANCE;
	}

}
