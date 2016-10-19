package dk.statsbiblioteket.pidregistration.configuration;

import dk.statsbiblioteket.pidregistration.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration read by property file.
 */
public class PropertyBasedRegistrarConfiguration {
    private static final Logger log = LoggerFactory.getLogger(PropertyBasedRegistrarConfiguration.class);

    private Properties properties;
    public static final String FEDORA_LOCATION_KEY = "doms-pidregistration.fedoraLocation";
    public static final String USER_NAME_KEY = "doms-pidregistration.userName";
    public static final String PASSWORD_KEY = "doms-pidregistration.password";
    public static final String DOMS_WS_API_ENDPOINT_KEY = "doms-pidregistration.domsWSAPIEndpoint";
    public static final String DOMS_WS_API_ENDPOINT_TIMEOUT_KEY = "doms-pidregistration.domsWSAPIEndpointTimeoutInMillis";
    public static final String HANDLE_PREFIX_KEY = "doms-pidregistration.handlePrefix";
    public static final String PRIVATE_KEY_PATH = "doms-pidregistration.privateKeyPath";
    public static final String PRIVATE_KEY_PASSWORD = "doms-pidregistration.privateKeyPassword";
    public static final String PID_PREFIX = "doms-pidregistration.pidPrefix";
    public static final String DOMS_MAX_RESULT_SIZE = "doms-pidregistration.doms.maxResultSize";
    public static final String DOMS_COLLECTIONS_PREFIX = "doms-pidregistration.doms.collections.";
    public static final String DATABASE_URL_KEY = "doms-pidregistration.database.url";
    public static final String DATABASE_USERNAME_KEY = "doms-pidregistration.database.username";
    public static final String DATABASE_PASSWORD_KEY = "doms-pidregistration.database.password";
    public static final String NUMBER_OF_THREADS = "doms-pidregistration.numOfThreads";

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

    public int getDomsWSAPIEndpointTimeout() {
        try {
            return Integer.parseInt(properties.getProperty(DOMS_WS_API_ENDPOINT_TIMEOUT_KEY));
        } catch (NumberFormatException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_TIMEOUT_KEY + "'", e);
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

    public String getPidPrefix() {
        return properties.getProperty(PID_PREFIX);
    }

    public int getDomsMaxResultSize() {
        return Integer.parseInt(properties.getProperty(DOMS_MAX_RESULT_SIZE));
    }

    public Set<Collection> getDomsCollections() {
        Set<Collection> result = new HashSet<Collection>();

        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(DOMS_COLLECTIONS_PREFIX)) {
                String id = propertyName.substring(DOMS_COLLECTIONS_PREFIX.length(), propertyName.length());
                String domsCollection = properties.getProperty(propertyName);
                result.add(new Collection(id, domsCollection));
            }
        }
        return result;
    }

    public String getDatabaseUrl() {
        return properties.getProperty(DATABASE_URL_KEY);
    }

    public String getDatabaseUsername() {
        return properties.getProperty(DATABASE_USERNAME_KEY);
    }

    public String getDatabasePassword() {
        return properties.getProperty(DATABASE_PASSWORD_KEY);
    }

    public int getNumberOfThreads() {
        String result = properties.getProperty(NUMBER_OF_THREADS);
        return result == null ? 1 : Integer.parseInt(result);
    }
}
