package dk.statsbiblioteket.pidregistration.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Configuration read by property file.
 */
public class PropertyBasedRegistrarConfiguration {
    private static final Logger log = LoggerFactory.getLogger(PropertyBasedRegistrarConfiguration.class);

    private Properties properties;
    public static final String FEDORA_LOCATION_KEY = "pidregistration.fedoraLocation";
    public static final String USER_NAME_KEY = "pidregistration.userName";
    public static final String PASSWORD_KEY = "pidregistration.password";
    public static final String DOMS_WS_API_ENDPOINT_KEY = "pidregistration.domsWSAPIEndpoint";
    public static final String HANDLE_PREFIX_KEY = "pidregistration.handlePrefix";
    public static final String PRIVATE_KEY_PATH = "pidregistration.privateKeyPath";
    public static final String PRIVATE_KEY_PASSWORD = "pidregistration.privateKeyPassword";
    public static final String PID_RESOLVER_PREFIX = "pidregistration.pidResolverPrefix";

    public PropertyBasedRegistrarConfiguration(File propertiesFile) {
        try {
            loadProperties(new FileInputStream(propertiesFile));
        } catch (FileNotFoundException e) {
            throw new InitializationFailedException("Properties file not found (" + propertiesFile.getAbsolutePath() + ")", e);
        }
        log.debug("Read properties for '" + propertiesFile.getAbsolutePath() + "'");
    }

    public PropertyBasedRegistrarConfiguration(InputStream properties) {
        loadProperties(properties);
    }

    private void loadProperties(InputStream inputStream) {
        this.properties = new Properties();
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {
            throw new InitializationFailedException("Unable to load properties from", e);
        }
        log.debug("Read configuration properties");
    }

    public String getFedoraLocation() {
        return properties.getProperty(FEDORA_LOCATION_KEY);
    }

    public String getUsername() {
        return properties.getProperty(USER_NAME_KEY);
    }

    public String getPassword() {
        return properties.getProperty(PASSWORD_KEY);
    }

    public URL getDomsWSAPIEndpoint() {
        try {
            return new URL(properties.getProperty(DOMS_WS_API_ENDPOINT_KEY));
        } catch (MalformedURLException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_KEY + "'", e);
        }
    }

    public String getHandlePrefix() {
        return properties.getProperty(HANDLE_PREFIX_KEY);
    }

    public String getPrivateKeyPath() {
        return properties.getProperty(PRIVATE_KEY_PATH);
    }

    public String getPrivateKeyPassword() {
        return properties.getProperty(PRIVATE_KEY_PASSWORD);
    }

    public String getPidResolverPrefix() {
        return properties.getProperty(PID_RESOLVER_PREFIX);
    }
}
