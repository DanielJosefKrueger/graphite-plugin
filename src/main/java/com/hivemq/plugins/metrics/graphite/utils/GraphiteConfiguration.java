/*
 * Copyright 2015 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.plugins.metrics.graphite.utils;

import com.hivemq.spi.config.SystemInformation;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Properties;

/**
 * This reads a property file and provides some utility methods for working with {@link Properties}
 *
 * @author Christoph Schäbel
 */
@Singleton
public class GraphiteConfiguration extends ReloadingPropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(ReloadingPropertiesReader.class);

    private RestartListener listener;

    @Inject
    public GraphiteConfiguration(final PluginExecutorService pluginExecutorService,
                                 final SystemInformation systemInformation,
                                 final EnvironmentReader environmentReader) {
        super(pluginExecutorService, systemInformation, environmentReader);

        final ValueChangedCallback callback = new ValueChangedCallback() {
            @Override
            public void valueChanged(final Object newValue) {
                if (listener != null) {
                    listener.restart();
                }
            }
        };

        addCallback("host", callback);
        addCallback("port", callback);
        addCallback("batchSize", callback);
        addCallback("batchMode", callback);
        addCallback("reportingInterval", callback);
        addCallback("prefix", callback);
    }

    public boolean isBatchMode() {
        return Boolean.parseBoolean(properties.getProperty("batchMode", "false"));
    }

    public String getHost() {
        String strHost = properties.getProperty("host");
        if(strHost == null){
            log.error("Host configuration is missing. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }
        return strHost;
    }

    public int getPort() {
        String strPort = properties.getProperty("port");
        if(strPort == null){
            log.error("Port configuration is missing. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }
        try{
            return Integer.parseInt(strPort);
        }catch(Exception e){
            log.error("Port configuration could not be parsed", e);
            throw new UnrecoverableException();
        }
    }

    public int getBatchSize() {
        try{
            return Integer.parseInt(properties.getProperty("batchSize", "3"));
        }catch(Exception e){
            log.error("Error while parsing configuration of batchSize. Shutting down HiveMQ", e);
            throw new UnrecoverableException(false);
        }
    }

    public int getReportingInterval() {
        try{
            return Integer.parseInt(properties.getProperty("reportingInterval", "60"));
        }catch(Exception e){
            log.error("Error while parsing configuration of reportingInterval. Shutting down HiveMQ", e);
            throw new UnrecoverableException(false);
        }
    }

    public String getPrefix() {
        return properties.getProperty("prefix", "");
    }

    @Override
    public String getFilename() {
        return "graphite-plugin.properties";
    }

    public void setRestartListener(final RestartListener listener) {
        this.listener = listener;
    }

    public static interface RestartListener {

        public void restart();

    }
}
