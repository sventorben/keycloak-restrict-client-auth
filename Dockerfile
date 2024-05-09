FROM registry.access.redhat.com/ubi9 AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN mkdir /otel
RUN dnf install --installroot /mnt/rootfs curl --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup
RUN curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar --output /otel/opentelemetry-javaagent.jar

FROM quay.io/keycloak/keycloak:23.0.7

USER root
RUN mkdir /otel
COPY --from=ubi-micro-build --chown=keycloak:keycloak /otel /otel
ENV JAVA_OPTS_APPEND="-javaagent:/otel/opentelemetry-javaagent.jar"
USER keycloak
ENV KC_METRICS_ENABLED=true
