package com.compsource.app.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to load the properties from the properties file
 */
public class ConfigUtil {
    private static Properties property = null;

    /**
     * Returns the property object from properties file
     *
     * @return - Properties object
     */
    public static Properties loadProperty() {
        if (property == null) {
            property = new Properties();
            try (InputStream input = ConfigUtil.class.getClassLoader()
                    .getResourceAsStream("config/config.properties")) {
                if (input != null)
                    property.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return property;
    }

}
