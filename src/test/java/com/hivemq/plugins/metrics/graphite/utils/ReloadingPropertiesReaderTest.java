package com.hivemq.plugins.metrics.graphite.utils;

import com.google.common.base.Optional;
import com.hivemq.spi.config.SystemInformation;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class ReloadingPropertiesReaderTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    public PluginExecutorService pluginExecutorService;

    @Mock
    public SystemInformation systemInformation;

    @Mock
    public EnvironmentReader environmentReader;

    private ReloadingPropertiesReader reader;

    private File tempFile;

    @Before
    public void before() throws Exception {

        MockitoAnnotations.initMocks(this);


        tempFile = tmpFolder.newFile(RandomStringUtils.randomAlphabetic(10) + ".properties");

        final Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        properties.setProperty("key3", "value3");
        properties.store(new FileOutputStream(tempFile), "");

        when(systemInformation.getConfigFolder()).thenReturn(new File(tempFile.getAbsolutePath()));
        when(environmentReader.getEnvironmentVariable(anyString())).thenReturn(Optional.<String>absent());
        reader = new TestReloadingPropertiesReader(pluginExecutorService, systemInformation, environmentReader, "");

    }

    @Test
    public void test_no_properties_file() throws Exception {

        reader = new TestReloadingPropertiesReader(pluginExecutorService, systemInformation, environmentReader, "notexisting");

        reader.postConstruct();

        assertNotNull(reader.getProperties());
    }

    @Test
    public void test_file_removed() throws Exception {

        reader = new TestReloadingPropertiesReader(pluginExecutorService, systemInformation, environmentReader,"");

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        assertTrue(tempFile.delete());

        reader.reload();

        assertEquals("value1", reader.getProperties().get("key1"));

    }

    @Test
    public void test_post_construct() throws Exception {

        reader.postConstruct();

        verify(pluginExecutorService, times(1)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        assertNotNull(reader.getProperties());
    }

    @Test
    public void test_reload() throws Exception {

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        final Properties properties = new Properties();
        properties.setProperty("batchMode", "false");
        properties.setProperty("port", "1234");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();

        assertEquals("1234", reader.getProperties().get("port"));
    }

    @Test
    public void test_reload_with_overriden_properties() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("batchMode", "false");
        properties.setProperty("port", "1234");
        properties.store(new FileOutputStream(tempFile), "");


        reader.postConstruct();

        assertEquals("false", reader.getProperties().get("batchMode"));
        assertEquals("1234", reader.getProperties().get("port"));

        when(environmentReader.getEnvironmentVariable("HIVEMQ_GRAPHITE_PORT")).thenReturn(Optional.of("78787"));
        when(environmentReader.getEnvironmentVariable("HIVEMQ_GRAPHITE_BATCH_MODE")).thenReturn(Optional.of("true"));

        reader.reload();

        assertEquals("78787", reader.getProperties().get("port"));
        assertEquals("true", reader.getProperties().get("batchMode"));
    }

    @Test
    public void test_addCallback() throws Exception {

        reader.postConstruct();

        assertEquals("value2", reader.getProperties().get("key2"));

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] callbackValue = new String[1];

        reader.addCallback("key2", new ValueChangedCallback<String>() {
            @Override
            public void valueChanged(final String newValue) {
                callbackValue[0] = newValue;
                latch.countDown();
            }
        });

        final Properties properties = new Properties();
        properties.setProperty("key2", "otherValue2");
        properties.setProperty("port", "1234");
        properties.setProperty("host", "localhost");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();

        assertEquals(true, latch.await(4, TimeUnit.SECONDS));
        assertEquals("otherValue2", callbackValue[0]);
        assertEquals("otherValue2", reader.getProperties().get("key2"));
    }

    @Test
    public void test_getProperties() throws Exception {

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));
        assertEquals("value2", reader.getProperties().get("key2"));
        assertEquals("value3", reader.getProperties().get("key3"));
    }

    @Test
    public void test_overriden_getProperties_with_environment_variables() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("batchMode", "false");
        properties.setProperty("port", "1234");
        properties.store(new FileOutputStream(tempFile), "");


        when(environmentReader.getEnvironmentVariable("HIVEMQ_GRAPHITE_PORT")).thenReturn(Optional.of("8787"));
        when(environmentReader.getEnvironmentVariable("HIVEMQ_GRAPHITE_BATCH_MODE")).thenReturn(Optional.of("true"));

        reader.postConstruct();

        assertEquals("8787", reader.getProperties().get("port"));
        assertEquals("true", reader.getProperties().get("batchMode"));
    }


    @Test
    public void test_fail_reload_port_wrong_negative() throws  Exception{

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        final Properties properties = new Properties();
        properties.setProperty("key1", "newValue");
        properties.setProperty("port", "-1");
        properties.setProperty("host", "localhost");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();
        assertEquals("value1", reader.getProperties().get("key1"));
        assertFalse(reader.getProperties().containsKey("port"));
    }

    @Test
    public void test_fail_reload_port_wrong_no_number() throws  Exception{

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        final Properties properties = new Properties();
        properties.setProperty("key1", "newValue");
        properties.setProperty("port", "noNumber");
        properties.setProperty("host", "localhost");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();
        assertEquals("value1", reader.getProperties().get("key1"));
        assertFalse(reader.getProperties().containsKey("port"));
    }

    @Test
    public void test_fail_reload_reportingInterval_wrong() throws  Exception{

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        final Properties properties = new Properties();
        properties.setProperty("key1", "newValue");
        properties.setProperty("port", "1");
        properties.setProperty("reportingInterval", "noNumber");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();

        assertEquals("value1", reader.getProperties().get("key1"));
        assertFalse(reader.getProperties().containsKey("reportingInterval"));
    }

    @Test
    public void test_fail_reload_batchMode_wrong() throws  Exception{

        reader.postConstruct();

        assertEquals("value1", reader.getProperties().get("key1"));

        final Properties properties = new Properties();
        properties.setProperty("key1", "newValue");
        properties.setProperty("batchMode", "nowtfBoolean");
        properties.setProperty("port", "1");
        properties.store(new FileOutputStream(tempFile), "");

        reader.reload();
        System.out.println(reader.properties);
        assertEquals("value1", reader.getProperties().get("key1"));
        assertFalse(reader.getProperties().containsKey("batchMode"));
    }






    private static class TestReloadingPropertiesReader extends ReloadingPropertiesReader {

        private final String filename;

        public TestReloadingPropertiesReader(final PluginExecutorService pluginExecutorService,
                                             final SystemInformation systemInformation,
                                             final EnvironmentReader environmentReader,
                                             final String filename) {
            super(pluginExecutorService, systemInformation, environmentReader);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}