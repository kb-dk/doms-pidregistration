package dk.statsbiblioteket.pidregistration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class PropertyBasedRegistrarConfiguration
        implements RegistrarConfiguration {
    private Properties properties;
    private final Log log = LogFactory.getLog(getClass());
    public static final String FEDORA_LOCATION_KEY = "dk.statsbiblioteket.doms.tools.handleregistrar.fedoraLocation";
    public static final String USER_NAME_KEY = "dk.statsbiblioteket.doms.tools.handleregistrar.userName";
    public static final String PASSWORD_KEY = "dk.statsbiblioteket.doms.tools.handleregistrar.password";
    public static final String DOMS_WS_API_ENDPOINT_KEY = "dk.statsbiblioteket.doms.tools.handleregistrar.domsWSAPIEndpoint";
    public static final String HANDLE_PREFIX_KEY = "dk.statsbiblioteket.doms.tools.handleregistrar.handlePrefix";
    public static final String PRIVATE_KEY_PATH = "dk.statsbiblioteket.doms.tools.handleregistrar.privateKeyPath";
    public static final String PRIVATE_KEY_PASSWORD = "dk.statsbiblioteket.doms.tools.handleregistrar.privateKeyPassword";

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

    @Override
    public String getFedoraLocation() {
        return properties.getProperty(FEDORA_LOCATION_KEY);
    }

    @Override
    public String getUsername() {
        return properties.getProperty(USER_NAME_KEY);
    }

    @Override
    public String getPassword() {
        return properties.getProperty(PASSWORD_KEY);
    }

    @Override
    public URL getDomsWSAPIEndpoint() {
        try {
            return new URL(properties.getProperty(DOMS_WS_API_ENDPOINT_KEY));
        } catch (MalformedURLException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_KEY + "'", e);
        }
    }

    @Override
    public String getHandlePrefix() {
        return properties.getProperty(HANDLE_PREFIX_KEY);
    }

    @Override
    public String getPrivateKeyPath() {
        return properties.getProperty(PRIVATE_KEY_PATH);
    }

    @Override
    public String getPrivateKeyPassword() {
        return properties.getProperty(PRIVATE_KEY_PASSWORD);
    }
}
