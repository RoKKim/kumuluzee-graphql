/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.graphql.mp.config;

import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationDispatcher;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import io.smallrye.graphql.config.ConfigKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps MicroProfile and SmallRye configuration key to KumuluzEE namespace.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class KumuluzConfigMapper implements ConfigurationSource {

    private static final Map<String, String> CONFIG_MAP = new HashMap<>();
    private static final Map<String, String> CONFIG_MAP_LIST = new HashMap<>();
    private static final String SHOW_ERROR_DEFAULTS_CONFIG_KEY =
            "kumuluzee.graphql.exceptions.include-show-error-defaults";
    private static final String[] SHOW_ERROR_DEFAULTS = new String[]{
            "com.kumuluz.ee.rest.exceptions.InvalidEntityFieldException",
            "com.kumuluz.ee.rest.exceptions.InvalidFieldValueException",
            "com.kumuluz.ee.rest.exceptions.NoGenericTypeException",
            "com.kumuluz.ee.rest.exceptions.NoSuchEntityFieldException",
            "com.kumuluz.ee.rest.exceptions.QueryFormatException",
    };

    static {
        CONFIG_MAP.put(ConfigKey.DEFAULT_ERROR_MESSAGE, "kumuluzee.graphql.exceptions.default-error-message");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_SCALARS, "kumuluzee.graphql.schema.include-scalars");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_DEFINITION, "kumuluzee.graphql.schema.include-schema-definition");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_DIRECTIVES, "kumuluzee.graphql.schema.include-directives");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_INTROSPECTION_TYPES, "kumuluzee.graphql.schema.include-introspection-types");
        CONFIG_MAP.put(ConfigKey.ENABLE_METRICS, "kumuluzee.graphql.metrics.enabled");
        CONFIG_MAP.put(ConfigKey.ENABLE_FEDERATION, "kumuluzee.graphql.federation.enabled");
        CONFIG_MAP.put(ConfigKey.ENABLE_FEDERATION_BATCH_RESOLVING, "kumuluzee.graphql.federation.enabled-federation-batch-resolving");

        CONFIG_MAP_LIST.put("mp.graphql.hideErrorMessage", "kumuluzee.graphql.exceptions.hide-error-message");
        CONFIG_MAP_LIST.put("mp.graphql.showErrorMessage", "kumuluzee.graphql.exceptions.show-error-message");
    }

    private ConfigurationUtil configurationUtil;

    /**
     * Low ordinal in order for MP prefix to take precedence.
     */
    @Override
    public Integer getOrdinal() {
        return 10;
    }

    @Override
    public void init(ConfigurationDispatcher configurationDispatcher) {
        configurationUtil = ConfigurationUtil.getInstance();
        // Workaround because of https://github.com/smallrye/smallrye-graphql/issues/1995
        System.setProperty(ConfigKey.ENABLE_FEDERATION, this.get(ConfigKey.ENABLE_FEDERATION).orElse("true"));
    }

    @Override
    public Optional<String> get(String key) {

        String mappedKey = CONFIG_MAP.get(key);

        if (mappedKey != null) {
            return configurationUtil.get(mappedKey);
        }

        mappedKey = CONFIG_MAP_LIST.get(key);

        if (mappedKey != null) {
            Optional<String> returnValue = configurationUtil.getList(mappedKey).map(ls -> String.join(",", ls));

            if ("kumuluzee.graphql.exceptions.show-error-message".equals(mappedKey) &&
                    configurationUtil.getBoolean(SHOW_ERROR_DEFAULTS_CONFIG_KEY).orElse(true)) {

                String defaults = String.join(",", SHOW_ERROR_DEFAULTS);
                returnValue = Optional.of(defaults + returnValue.map(v -> "," + v).orElse(""));
            }

            return returnValue;
        }

        return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getLong(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDouble(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloat(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getListSize(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getMapKeys(String key) {
        return Optional.empty();
    }

    @Override
    public void watch(String key) {

    }

    @Override
    public void set(String key, String value) {

    }

    @Override
    public void set(String key, Boolean value) {

    }

    @Override
    public void set(String key, Integer value) {

    }

    @Override
    public void set(String key, Double value) {

    }

    @Override
    public void set(String key, Float value) {

    }
}
