package dk.statsbiblioteket.pidregistration.configuration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.statsbiblioteket.pidregistration.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    public static final String PID_PREFIX = "pidregistration.pidPrefix";
    public static final String DOMS_MAX_RESULT_SIZE = "pidregistration.doms.maxResultSize";
    public static final String DOMS_COLLECTIONS = "pidregistration.doms.collections";

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

    public String getPidPrefix() {
        return properties.getProperty(PID_PREFIX);
    }

    public int getDomsMaxResultSize() {
        return Integer.parseInt(properties.getProperty(DOMS_MAX_RESULT_SIZE));
    }

    public List<Collection> getDomsCollections() {
        List<Collection> result = new ArrayList<Collection>();

        Type listType = new TypeToken<List<String>>(){}.getType();
        Gson gson = new Gson();
        String collections = properties.getProperty(DOMS_COLLECTIONS);
        List<String> collectionNames = gson.fromJson(collections, listType);
        for (String collectionName : collectionNames) {
            result.add(new Collection(collectionName));
        }

        return result;
    }
}
