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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

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

public class MQTTJsonReceiverTests {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTJsonReceiverTests.class);

	public static final String SERVER_URL = "tcp://localhost:1883";

	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";

	public static final String CLIENT_ID = "receiver1";
	public static final String TOPIC_NAME = "mqtt-events";
	public static final int QOS_LEVEL = 0;

	public static void main(String[] args){
		
		System.out.println("starting mqtt receiver "
				+ " QOS_LEVEL="+QOS_LEVEL
				+ " EVENT_TOPIC_NAME"+TOPIC_NAME
				+ " CLIENT_ID="+CLIENT_ID
				+ " SERVER_URL="+SERVER_URL);
		MQTTJsonReceiverTests receiver = new MQTTJsonReceiverTests();
		receiver.testMqttJsonReceiver();
		System.out.println("mqtt receiver ended");
		
	}
	


	@Test
	public void testMqttJsonReceiver() {
		LOG.debug("start of test testMqttJsonReceiver() ");

		// set up receiver
		Receiver receiver= new Receiver();
		Thread thread = new Thread(receiver);
		thread.start();

		try {
			Thread.sleep(5000);
			assertTrue(receiver.isConnected());
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		LOG.debug("Receiver is connected");


		// wait for received messages
		try {
			Thread.sleep(5*60000); // 5 MINUTES
			assertTrue(receiver.isConnected());
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		//clean up
		receiver.close();

		LOG.debug("end of test testMqttJsonReceiver() ");
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

	public static String jsonTime(Date date){
		SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

		TimeZone tz = TimeZone.getTimeZone( "UTC" );

		df.setTimeZone( tz );

		String output = df.format( date );
		return output;
	}

	private class Receiver implements Runnable {

		MQTTClientImpl client;
		MessageNotificationClientQueueImpl messageNotificationClientQueueImpl;

		public boolean isConnected(){
			if(client!=null) return client.isClientConnected();
			return false;
		}

		public void close(){
			LOG.debug("Receiver closing connection");
			if (client!=null) client.destroy();
			client=null;
			if (messageNotificationClientQueueImpl!=null ) messageNotificationClientQueueImpl.destroy();
		}

		@Override
		public void run() {
			String brokerUrl = SERVER_URL;
			String clientId = CLIENT_ID;
			String userName =MQTT_USERNAME;
			String password =MQTT_PASSWORD;
			String connectionRetryInterval= "1000" ;

			LOG.debug("Receiver initiating connection");

			client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval);

			messageNotificationClientQueueImpl = new MessageNotificationClientQueueImpl();

			messageNotificationClientQueueImpl.setMessageNotifier(client);

			messageNotificationClientQueueImpl.setMaxQueueLength(100);

			Map<String, NotificationClient> topicHandlingClients = new HashMap<String, NotificationClient>();
			NotificationClient notificationClient = new NotificationClient(){

				@Override
				public void sendMessageNotification(MessageNotification messageNotification) {
					String payloadString=new String(messageNotification.getPayload());
					try{
						JSONObject jsonobj = parseJson(payloadString);
						System.out.println(jsonobj.toJSONString());
					} catch (Exception e){
						
					}
				}

				@Override
				public void init() {}

				@Override
				public void destroy() {}
				
			};

			topicHandlingClients.put(TOPIC_NAME, notificationClient);
			messageNotificationClientQueueImpl.setTopicHandlingClients(topicHandlingClients);

			try{
				messageNotificationClientQueueImpl.init();
				client.init();
			} catch(Exception e){
				LOG.debug("Receiver problem initialising reliable connection", e);
			}

			// wait for receiver to connect
			try {
				while (!Thread.currentThread().isInterrupted() && ! client.isClientConnected()){
					Thread.sleep(100);
				}
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			// try receiving messages
			String topic=TOPIC_NAME;
			int qos=QOS_LEVEL;
			try{
				client.subscribe(topic, qos);
			} catch(Exception e){
				LOG.debug("Receiver problem subscribing", e);
			}
			LOG.debug("Receiver connection initialised");

			
		}

	}

}
