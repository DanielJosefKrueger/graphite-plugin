
The HiveMQ Graphite plugin allows to publish all Metrics of HiveMQ to your Graphite installation. This plugin collects
internal HiveMQ statistics and also uses custom metrics hooked into HiveMQ with the MetricService of the plugin system.

This Graphite plugin also supports batching, so if you want to have metrics in second resolution in Graphite but don't want
to have the huge network overhead by sending it actually every few seconds, you can batch the send action of the metrics.

Installation
=============

1. Copy the jar file to your [HIVEMQ_HOME]/plugins folder
2. Copy the graphite-plugin.properties file to your [HIVEMQ_HOME]/conf folder
4. Modify the graphite-plugin.properties file for your Graphite installation
3. Done




Usage
======

1. Run HiveMQ
2. HiveMQ will report automatically publish all Metrics (including custom metrics registered by other plugins) to Graphite in the configured time interval





Configuration
=============

The configuration file graphite-plugin.properties can be changed at runtime. It supports the following configuration options:


-------------------------------------------------------------------------
Property            | Description                                       |
-------------------------------------------------------------------------
|host               | The hostname or IP address                        |
|port               | The Graphite Port                                 |
|batchMode          | If metrics should be batched                      |
|batchSize          | The number of batches before sending the data     |
|reportingInterval  | The interval to send metrics                      |
|prefix             | The prefix of all metrics                         |
-------------------------------------------------------------------------

Furthermore there is the option to use environmental variables to configure the plugin.
They correspond to the options set in the graphite-plugin.properties file, but will overwrite them.

-----------------------------------------------------------------------------------------
Environmental Variable              | Description                                       |
-----------------------------------------------------------------------------------------
|HIVEMQ_GRAPHITE_HOST               | The hostname or IP address                        |
|HIVEMQ_GRAPHITE_PORT               | The Graphite Port                                 |
|HIVEMQ_GRAPHITE_BATCH_MODE         | If metrics should be batched                      |
|HIVEMQ_GRAPHITE_BATCH_SIZE         | The number of batches before sending the data     |
|HIVEMQ_GRAPHITE_REPORTING_INTERVAL | The interval to send metrics                      |
|HIVEMQ_GRAPHITE_PREFIX             | The prefix of all metrics                         |
-----------------------------------------------------------------------------------------


Example configuration file:


# Hostname/IP of your graphite server
host = localhost

# Port of your graphite server
port = 2003

# If metrics should be written in batches
batchMode = false
batchSize = 3

# interval in seconds in which metrics get pushed
reportingInterval = 60

# prefix which is shown in graphite
prefix =