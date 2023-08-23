FROM maven:3.9.3-eclipse-temurin-17-focal
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml -B clean package
RUN mkdir -p /opt/keycloak/providers
RUN mv /home/app/target/keycloak-restrict-client-auth.jar /opt/keycloak/providers
