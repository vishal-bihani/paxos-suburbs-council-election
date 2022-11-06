package com.suburbs.council.election.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ConfigurationUtil {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationUtil.class);

    public static InputStream loadConfigurationFromFile(String filePath) throws IOException {
        try {
            URL fileUrl = new URL(filePath);
            return getInputStream(fileUrl);

        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", filePath);
            throw e;
        }
    }

    private static InputStream getInputStream(URL url) throws IOException {
        return new BufferedInputStream(url.openStream());
    }

}
