package com.suburbs.council.election.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for configuration parsing.
 */
public class ConfigurationUtil {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationUtil.class);

    private ConfigurationUtil() {
    }

    /**
     * Returns input stream to given file path.
     *
     * @param filePath Configuration file path
     * @return Input stream
     * @throws IOException if exception occurs
     */
    public static InputStream loadConfigurationFromFile(String filePath) throws IOException {
        try {
            URL fileUrl = new URL(filePath);
            return getInputStream(fileUrl);

        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", filePath);
            throw e;
        }
    }

    /**
     * Returns input stream for the provided URL.
     *
     * @param url URL
     * @return InputStream
     * @throws IOException If exception occurs
     */
    private static InputStream getInputStream(URL url) throws IOException {
        return new BufferedInputStream(url.openStream());
    }

}
