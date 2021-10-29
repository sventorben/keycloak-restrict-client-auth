package de.sventorben.keycloak.authorization.client;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KeycloakXContainer extends KeycloakContainer {

    public KeycloakXContainer(String image) {
        super(image);
    }

    @Override
    protected void createKeycloakExtensionDeployment(String deploymentLocation, String extensionName, String extensionClassFolder) {
        String classesLocation = this.resolveExtensionClassLocation(extensionClassFolder);
        if ((new File(classesLocation)).exists()) {
            final File file;
            try {
                file = Files.createTempFile(Path.of("target"), "restrict-client-auth", ".jar").toFile();
                file.setReadable(true, false);
                file.deleteOnExit();
                ShrinkWrap.create(JavaArchive.class, extensionName)
                    .as(ExplodedImporter.class)
                    .importDirectory(classesLocation)
                    .as(ZipExporter.class)
                    .exportTo(file, true);
                this.addFileSystemBind(file.getAbsolutePath(), deploymentLocation + "/" + extensionName, BindMode.READ_ONLY, SelinuxContext.SINGLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getAuthServerUrl() {
        return String.format("http://%s:%s%s", this.getContainerIpAddress(), this.getMappedPort(8080), "/");
    }

    @Override
    protected void configure() {
        super.configure();
        this.setWaitStrategy(Wait.forHttp("/").forPort(8080).withStartupTimeout(this.getStartupTimeout()));
        this.withEnv("KEYCLOAK_ADMIN", this.getAdminUsername());
        this.withEnv("KEYCLOAK_ADMIN_PASSWORD", this.getAdminPassword());
        withCommand(
            "--auto-config",
            "--db=h2",
            "--http-enabled=true"
        );
    }
}
