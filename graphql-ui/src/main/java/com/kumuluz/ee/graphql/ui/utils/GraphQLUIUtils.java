package com.kumuluz.ee.graphql.ui.utils;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

/**
 * Utility class for GraphQL UI related configurations
 *
 * @author Rok Miklavčič
 * @since 1.2.0
 */
public class GraphQLUIUtils {
    public static String getPath(String configKey, String defaultValue) {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        String path = configurationUtil.get(configKey).orElse(defaultValue);

        // strip "/"
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path = "/" + path;

        return path;
    }
}
