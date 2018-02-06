/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTJsonTransmitterTest {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTJsonTransmitterTest.class);
	
	public static AtomicInteger messagecount = new AtomicInteger(0);

	// works with 2017-10-19 10:15:02.854888
	public static final String DEFAULT_DATE_TIME_FORMAT_PATTERN="yyyy-MM-dd HH:mm:ss.SSSSSS";

	//public static final String jsonTestFile="./src/test/resources/testData.json";
	public static final String TEST_JSON_FILE="./src/test/resources/TestData/testData2.json";

	public static final String MAIN_PROPERTIES_FILE="./src/test/resources/TestData/mqttclienttest.properties";

	public static final String SERVER_URL = "tcp://localhost:1883"; //172.18.0.4
	//public static final String SERVER_URL = "tcp://192.168.202.1:1883";
	//public static final String SERVER_URL = "tcp://172.18.0.4:1883"; // karaf1
	//public static final String SERVER_URL = "tcp://139.162.227.142:1883"; // linode
	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";
	public static final String CONNECTION_RETRY_INTERVAL = "60000"; 
	public static final String CLIENT_CONNECTION_MAX_WAIT = "40000";

	public static final String CLIENT_ID = "transmitter1";
	public static final String TOPIC_NAME = "mqtt-data"; // mqtt-data
	//public static final String TOPIC_NAME = "mqtt-events"; // mqtt-events
	public static final int QOS_LEVEL = 0;

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

	// set up transmitter
	private String brokerUrl = SERVER_URL;
	private String clientId = CLIENT_ID;
	private String userName =MQTT_USERNAME;
	private String password =MQTT_PASSWORD;
	private String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
	private String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;
	private String topic=TOPIC_NAME;
	private int qos=QOS_LEVEL;

	private long persistInterval = 1000 * 60; // 1 minute // 5 * 1000 * 60; // 5 MINUTES
	private boolean useRepeatTimer=true;
	private boolean useJsonFile = true;
	private String jsonTestFile=TEST_JSON_FILE;
	private String dateTimeFormatPattern = DEFAULT_DATE_TIME_FORMAT_PATTERN;
	// default to local time offset
	private ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();


	@Test
	public void testJsonMessage(){
		LOG.debug("start testJsonMessage()");
		JSONObject jsonobj;
		try {
			jsonobj = parseJson(jsonTestMessage);
			LOG.debug("testJsonMessage() JSON object:"+jsonobj.toJSONString() );
		} catch (ParseException e) {
			LOG.debug("failed to parse message:"+jsonTestMessage,e);		
			fail("failed to parse message:"+jsonTestMessage);
		}
		LOG.debug("end testJsonMessage()");
	}
	
	@Test
	public void testReadProperties(){
		LOG.debug("start testReadProperties()");
		MQTTJsonTransmitterTest mqttTest = new MQTTJsonTransmitterTest();
		mqttTest.readProperties(MAIN_PROPERTIES_FILE);
		LOG.debug("testReadProperties = properties read from file"+mqttTest.toString());
		LOG.debug("end testReadProperties()");
	}


	@Test
	public void testMqttJsonTransmitter() {
		LOG.debug("start of test testMqttJsonTransmitter() ");
		LOG.debug("   usejsonFile="+useJsonFile
				+ "   useRepeatTimer="+useRepeatTimer
				+ "   persistInterval="+persistInterval);


		// will connect

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval,clientConnectionMaxWait);

		try{
			client.init();
		} catch(Exception e){
			LOG.debug("problem initialising reliable transmitter connection", e);
		}

		// wait for connection
		try {
			Thread.sleep(3000);
			assertTrue(client.isClientConnected());
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		// try sending messages

		boolean repeat= true;

		if (!useJsonFile){
			//send json message
			while(repeat){
				repeat=false;
				try{
					JSONObject jsonobj = parseJson(jsonTestMessage);

					String message=jsonobj.toJSONString();
					// change time to now
					// round to minutes
					long actualTime = new Date().getTime();
					actualTime = 60 * 1000 * Math.round(actualTime / (60.0 * 1000.0));
					String timeStr = jsonTime(new Date(actualTime));
					jsonobj.put("time", timeStr);

					LOG.debug("sending single json message:"+message);
					byte[] payload = message.getBytes();
					client.publishSynchronous(topic, qos, payload);
				} catch(Exception e){
					LOG.debug("problem publishing json message", e);
				}

				if(useRepeatTimer){
					LOG.debug("waiting "+persistInterval + " ms before next message");
					repeat= true;
					try {
						Thread.sleep(persistInterval);
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			}
		} else { // use json file
			LOG.debug("sending json messages from file "+jsonTestFile);
			// send json messages from file
			JSONArray jsonArray= this.readJsonFile();

			while(repeat){ 
				for (Object obj : jsonArray){
					try{
						JSONObject jsonobj = (JSONObject) obj;

						if(useRepeatTimer){
							// set absolute times
							String timeStr = jsonTime(new Date());
							jsonobj.put("time", timeStr);
						}

						String message=jsonobj.toJSONString();
						LOG.debug("sending json message"+message);
						byte[] payload = message.getBytes();
						client.publishSynchronous(topic, qos, payload);
						LOG.debug("message sent to topic "+topic+" qos "+qos
								+ " count:"+messagecount.addAndGet(1));
					} catch(Exception e){
						LOG.debug("problem publishing json message", e);
					}
					if(useRepeatTimer){
						// wait between sending
						LOG.debug("waiting "+persistInterval + " ms before next message");
						try {
							Thread.sleep(persistInterval);
						} catch(InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				}
				if(useRepeatTimer){
					LOG.debug("useRepeatTimer=true. Starting again at beginning of file");
					repeat = useRepeatTimer;
				}

			}
		}

		//clean up
		client.destroy();

		LOG.debug("end of test testMqttJsonTransmitter() ");
	}
	


	public String jsonTime(Date date){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormatPattern);
		Instant instantFromDate = date.toInstant();
		LocalDateTime endLocalDateTime = LocalDateTime.ofInstant(instantFromDate,zoneOffset);
		String outputStr2 = endLocalDateTime.format(formatter);

		return outputStr2;
	}


	private JSONObject parseJson(String payloadString) throws ParseException{
		JSONObject jsonObject=null;

		LOG.debug("parseJson payloadString:" + payloadString);
		JSONParser parser = new JSONParser();
		Object obj;
		obj = parser.parse(payloadString);
		jsonObject = (JSONObject) obj;
		LOG.debug("parseJson JsonObject.toString():" + jsonObject.toString());

		return jsonObject;
	}

	private JSONArray readJsonFile(){
		JSONParser parser = new JSONParser();
		try {
			File f = new File(jsonTestFile);
			LOG.debug("reading test data from file:"+f.getAbsolutePath());
			JSONArray array = (JSONArray) parser.parse(new FileReader(f));
			return array;
		} catch (IOException | ParseException e) {
			throw new RuntimeException("could not parse json file ",e);
		}
	}
	

	public void readProperties(String propFileName){
		File propFile = new File(propFileName);
		LOG.debug("reading properties from file:"+propFile.getAbsolutePath());
		if (!propFile.canRead()) throw new RuntimeException("cannot read file"+propFileName);
		readProperties(propFile);
	}

	public void readProperties(File propFile){
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(propFile);
			prop.load(input);

			//Configuration for the mqtt test client client
			//mqttclienttest.properties
			//
			//useRepeatTimer If true the test will use a timer to send data. 
			//The timer is repeats at the number of seconds set by "org.opennms.plugin.mqttclient.message.persist.interval
			useRepeatTimer = Boolean.valueOf(prop.getProperty("org.opennms.plugin.mqttclient.test.useRepeatTimer", "false").trim());
			//
			//useJsonFile if true the data is taken from a supplied json file
			useJsonFile = Boolean.valueOf(prop.getProperty("org.opennms.plugin.mqttclient.test.useJsonFile", "false").trim());

			jsonTestFile=prop.getProperty("org.opennms.plugin.mqttclient.test.jsonFileName",TEST_JSON_FILE);

			//These interval and RRA definitions are always needed but primarily apply if data is stored in rrd files
			//As with rrd definitions the interval sets the time interval in seconds between collections (300 = 5 mins) 
			persistInterval = 1000 * Long.parseLong(prop.getProperty("org.opennms.plugin.mqttclient.message.persist.interval","300"));

			// brokerUrl. url of broker to connect to. the Paho client supports two types of connection 
			// tcp:// for a TCP connection and ssl:// for a TCP connection secured by SSL/TLS. 
			// For example: tcp://localhost:1883 or ssl://localhost:8883

			brokerUrl= prop.getProperty("org.opennms.plugin.mqttclient.brokerUrl",SERVER_URL);

			// clientId. Note that this must be a unique id from the point of view of the broker
			//"org.opennms.plugin.mqttclient.clientId=testclient
			clientId = prop.getProperty("org.opennms.plugin.mqttclient.clientId",CLIENT_ID);
			//userName to connect to the broker. If left empty anonymous connection will be attempted
			//			"org.opennms.plugin.mqttclient.userName=mqtt-user
			userName = prop.getProperty("org.opennms.plugin.mqttclient.userName",MQTT_USERNAME);
			//password to connect to the broker. If left empty a password will not be sent
			//			"org.opennms.plugin.mqttclient.password=mqtt-password
			password = prop.getProperty("org.opennms.plugin.mqttclient.password",MQTT_PASSWORD);

			//  topic on which to send data or events
			// org.opennms.plugin.mqttclient.test.topic=mqtt-data
			topic= prop.getProperty("org.opennms.plugin.mqttclient.test.topic",TOPIC_NAME);

			//Qos of connection to event and data topics
			//"org.opennms.plugin.mqttclient.qos=0

			qos= Integer.parseInt(prop.getProperty("org.opennms.plugin.mqttclient.qos",Integer.toString(QOS_LEVEL)));

			//If client fails to connect to the broker, interval (ms) before re attempting connection
			connectionRetryInterval= prop.getProperty("org.opennms.plugin.mqttclient.connectionRetryInterval","1000");

			//time format pattern. Determines how json date time is interpreted  
			//yyyy-MM-dd HH:mm:ss.SSSSSS works with 2017-10-19 10:15:02.854888
			//see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns for other examples
			//"org.opennms.plugin.mqttclient.message.time-format-pattern=yyyy-MM-dd HH:mm:ss.SSSSSS
			dateTimeFormatPattern = prop.getProperty("org.opennms.plugin.mqttclient.message.time-format-pattern",DEFAULT_DATE_TIME_FORMAT_PATTERN);

			//time zone offset used by time received in message
			//see https://docs.oracle.com/javase/8/docs/api/java/time/ZoneOffset.html#of-java.lang.String-  for other examples
			//set to empty property = use local time zone

			String offsetId = prop.getProperty("org.opennms.plugin.mqttclient.message.time-zone-offset","");
			if(! "".equals(offsetId.trim())) zoneOffset=ZoneOffset.of(offsetId);
		} catch (IOException ex) {
			throw new RuntimeException("problem loading properties file:"+propFile.getName(),ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) { }
			}
		}
	}

	@Override
	public String toString() {
		return "MQTTJsonTransmitterTest [brokerUrl=" + brokerUrl
				+ ", clientId=" + clientId + ", userName=" + userName
				+ ", password=" + password + ", connectionRetryInterval="
				+ connectionRetryInterval + ", topic=" + topic + ", qos=" + qos
				+ ", persistInterval=" + persistInterval + ", useRepeatTimer="
				+ useRepeatTimer + ", useJsonFile=" + useJsonFile
				+ ", jsonTestFile=" + jsonTestFile + ", dateTimeFormatPattern="
				+ dateTimeFormatPattern + ", zoneOffset=" + zoneOffset + "]";
	}




}
