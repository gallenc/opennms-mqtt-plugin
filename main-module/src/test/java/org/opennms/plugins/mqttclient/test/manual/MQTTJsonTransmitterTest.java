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
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.MessageNotificationClientQueueImpl;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.opennms.plugins.messagenotifier.VerySimpleMessageNotificationClient;
import org.opennms.plugins.messagenotifier.eventnotifier.MqttEventNotificationClient;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTJsonTransmitterTest {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTJsonTransmitterTest.class);

	// works with 2017-10-19 10:15:02.854888
	private static final String DEFAULT_DATE_TIME_FORMAT_PATTERN="yyyy-MM-dd HH:mm:ss.SSSSSS";
	
	//public static final String TEST_JSON_FILE="./src/test/resources/testData.json";
	public static final String TEST_JSON_FILE="./src/test/resources/testData2.json";
	
	//public static final String SERVER_URL = "tcp://localhost:1883"; 172.18.0.4
	//public static final String SERVER_URL = "tcp://192.168.202.1:1883";
	//public static final String SERVER_URL = "tcp://172.18.0.4:1883"; // karaf1
	public static final String SERVER_URL = "tcp://139.162.227.142:1883"; // linode
	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";
	
	public static final String CLIENT_ID = "transmitter1";
	public static final String TOPIC_NAME = "mqtt-data"; // mqtt-data
	//public static final String TOPIC_NAME = "mqtt-events"; // mqtt-events
	public static final int QOS_LEVEL = 0;

	public static final String jsonTestMessage="{"
			+ " \"time\": \""+ jsonTime(new Date())+ "\","
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

	public static String jsonTime(Date date){
		SimpleDateFormat df = new SimpleDateFormat( DEFAULT_DATE_TIME_FORMAT_PATTERN );

		TimeZone tz = TimeZone.getTimeZone( "UTC" );

		df.setTimeZone( tz );

		String output = df.format( date );
		return output;
	}

	@Test
	public void testMqttJsonTransmitter() {
		LOG.debug("start of test testMqttJsonTransmitter() ");

		// set up transmitter
		String brokerUrl = SERVER_URL;
		String clientId = CLIENT_ID;
		String userName =MQTT_USERNAME;
		String password =MQTT_PASSWORD;
		String connectionRetryInterval= "1000" ;

		// will connect

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval);

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
		String topic=TOPIC_NAME;
		int qos=QOS_LEVEL;

		// send json message
		//		try{
		//			JSONObject jsonobj = parseJson(jsonTestMessage);
		//			String message=jsonobj.toJSONString();
		//			byte[] payload = message.getBytes();
		//			client.publishSynchronous(topic, qos, payload);
		//		} catch(Exception e){
		//			LOG.debug("problem publishing json message", e);
		//		}

		// send json messages from file
		JSONArray jsonArray= this.readJsonFile();
		for (Object obj : jsonArray){
			try{
				JSONObject jsonobj = (JSONObject) obj;
				
				LOG.debug("sending json message"+jsonobj.toJSONString());
				
				String message=jsonobj.toJSONString();
				byte[] payload = message.getBytes();
				client.publishSynchronous(topic, qos, payload);
				LOG.debug("message sent");
			} catch(Exception e){
				LOG.debug("problem publishing json message", e);
			}
		}

		//clean up
		client.destroy();

		LOG.debug("end of test testMqttJsonTransmitter() ");
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
			File f = new File(TEST_JSON_FILE);
			LOG.debug("reading test data from file:"+f.getAbsolutePath());
			JSONArray array = (JSONArray) parser.parse(new FileReader(f));
			return array;
		} catch (IOException | ParseException e) {
			throw new RuntimeException("could not parse json file ",e);
		}
	}


}
