FROM java:8u111-jre-alpine

ADD build/libs/hazelcast-docker-swarm-discovery-spi-1.0-RC2-all.jar /test.jar


# Create our entrypoint
RUN echo "set -e; command=\"\$1\"; if [ \"\$command\" != \"java\" ]; then echo \"ERROR: command must start with: java\"; exit 1; fi; exec \"\$@\"" > /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["sh","/entrypoint.sh"]
CMD ["java"]