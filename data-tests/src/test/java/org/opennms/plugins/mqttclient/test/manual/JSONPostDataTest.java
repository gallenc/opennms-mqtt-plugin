package org.opennms.plugins.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.opennms.plugin.jsontest.test.manual.JSONPosTestDataTransmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONPostDataTest {
	private static final Logger LOG = LoggerFactory.getLogger(JSONPostDataTest.class);
	
	public static final String PROPERTIES_FILE = "./src/test/resources/test.properties";
	
	public static final String jsonTestMessage="{"
			+ " \"time\": \"2017-10-19 10:15:02.854888\","
			+ " \"id\": \"monitorID\","
			+ " \"cityName\": \"Southampton\","
			+ " \"stationName\": \"Common#1\","
			+ " \"latitude\": 0,"
			+ " \"longitude\": 0,"
			+ " \"averaging\": 0,"
			+ " \"PM1\": 10,"
			+ " \"PM25\": 100,"
			+ " \"PM10\": 1000"
			+ "}";

	//@Test  // disabled for now
	public void test1message() {
		 LOG.debug("JSON tester starting up");

		 JSONPosTestDataTransmitter messageTransmitter = new JSONPosTestDataTransmitter();
		 
		 String filename=null;
		 try {
			 filename = PROPERTIES_FILE ;
			 File f = new File(filename);
			 LOG.debug("loading properties file: "+f.getAbsolutePath());
			 messageTransmitter.readProperties(filename);
			 
		 } catch (Exception ex){
			 LOG.error("cannot load properties file: "+filename, ex);
		 }
		 LOG.debug("loaded the following properties from supplied filename: "+filename+"\n"+messageTransmitter.toString());
		 LOG.debug("starting transmitter use ctrl-c to exit");
		 
		 String message=jsonTestMessage;
		 messageTransmitter.transmitString(message);
		 //mqttTest.testJsonTransmitter();
		 LOG.debug("JSON tester shutting down");
	}
	
	@Test
	public void testAllMessage() {
		 LOG.debug("JSON tester starting up");

		 JSONPosTestDataTransmitter messageTransmitter = new JSONPosTestDataTransmitter();
		 
		 String filename=null;
		 try {
			 filename = PROPERTIES_FILE ;
			 File f = new File(filename);
			 LOG.debug("loading properties file: "+f.getAbsolutePath());
			 messageTransmitter.readProperties(filename);
			 
		 } catch (Exception ex){
			 LOG.error("cannot load properties file: "+filename, ex);
		 }
		 LOG.debug("loaded the following properties from supplied filename: "+filename+"\n"+messageTransmitter.toString());
		 LOG.debug("starting transmitter use ctrl-c to exit");
		 
		 messageTransmitter.testJsonTransmitter();
		 //mqttTest.testJsonTransmitter();
		 LOG.debug("JSON tester shutting down");
	}

}
