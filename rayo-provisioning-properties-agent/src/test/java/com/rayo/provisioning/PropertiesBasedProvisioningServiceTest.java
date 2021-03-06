package com.rayo.provisioning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import com.rayo.provisioning.storage.PropertiesBasedStorageServiceClient;

/**
 * Set of provisioning tests using a datastore backed by a properties file
 * 
 * @author martin
 *
 */
public class PropertiesBasedProvisioningServiceTest extends DefaultProvisioningAgentTest {
	
	private File propsFile;
	private File tempProvisioningFile;
	
	@Before
	public void setup() throws Exception {
		
		propsFile = File.createTempFile("temp", ".properties");
		propsFile.deleteOnExit();
		tempProvisioningFile = File.createTempFile("temp", ".properties");
		tempProvisioningFile.deleteOnExit();
		
		InputStream sourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-provisioning.properties");
		Properties sourceProperties = new Properties();
		sourceProperties.load(sourceStream);
		sourceProperties.put(PropertiesBasedStorageServiceClient.ROUTING_PROPERTIES_FILE, propsFile.getAbsolutePath());
		sourceProperties.put(PropertiesBasedStorageServiceClient.PROPERTIES_RELOAD_INTERVAL, "500");
		sourceStream.close();
		
		FileOutputStream fos = new FileOutputStream(tempProvisioningFile);
		sourceProperties.store(fos, null);
		fos.close();
		
		propertiesFile = tempProvisioningFile.getAbsolutePath();
		
		provisioningService = new PropertiesBasedProvisioningAgent();
				
		super.setup();
	}
	
	@After
	public void shutdown()  {
		
		super.shutdown();
		propsFile.delete();
		tempProvisioningFile.delete();
	}
}
