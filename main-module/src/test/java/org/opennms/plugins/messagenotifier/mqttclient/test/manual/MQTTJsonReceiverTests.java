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

package org.opennms.plugins.messagenotifier.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.MessageNotificationClientQueueImpl;
import org.opennms.plugins.messagenotifier.MessageNotifier;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.opennms.plugins.messagenotifier.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTJsonReceiverTests {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTJsonReceiverTests.class);

	// works with 2017-10-19 10:15:02.854888
	private static final String DEFAULT_DATE_TIME_FORMAT_PATTERN="yyyy-MM-dd HH:mm:ss.SSSSSS";

	public static final String SERVER_URL = "tcp://0.0.0.0:1883";
//	public static final String SERVER_URL = "tcp://192.168.0.2:1883";

	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";

	public static final String CLIENT_ID = "receiver1";
	public static final String TOPIC_NAME = "mqtt-data";
	public static final int QOS_LEVEL = 0;

	public static final String CONNECTION_RETRY_INTERVAL = "60000"; 
	public static final String CLIENT_CONNECTION_MAX_WAIT = "40000";

	public static void main(String[] args){

		System.out.println("starting mqtt receiver "
				+ " QOS_LEVEL="+QOS_LEVEL
				+ " EVENT_TOPIC_NAME="+TOPIC_NAME
				+ " CLIENT_ID="+CLIENT_ID
				+ " SERVER_URL="+SERVER_URL
				+ " CONNECTION_RETRY_INTERVAL="+CONNECTION_RETRY_INTERVAL
				+ " CLIENT_CONNECTION_MAX_WAIT="+CLIENT_CONNECTION_MAX_WAIT);
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

		try{ 
			thread.start();

			try {
				Thread.sleep(Long.parseLong(CONNECTION_RETRY_INTERVAL));
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			assertTrue(receiver.isConnected());
			LOG.debug("Receiver is connected");


			// wait for received messages
			try {
				Thread.sleep(5*60000); // 5 MINUTES
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

		} finally {
			//clean up
			LOG.debug("shutting down receiver thread ");
			receiver.close();
		}


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
		SimpleDateFormat df = new SimpleDateFormat( DEFAULT_DATE_TIME_FORMAT_PATTERN );

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
			String clientId =  CLIENT_ID;
			String userName =  MQTT_USERNAME;
			String password =  MQTT_PASSWORD;
			String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
			String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;

			LOG.debug("Receiver initiating connection");


			client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval, clientConnectionMaxWait);

			messageNotificationClientQueueImpl = new MessageNotificationClientQueueImpl();

			messageNotificationClientQueueImpl.setMaxMessageQueueThreads(1);
			messageNotificationClientQueueImpl.setMaxMessageQueueLength(100);

			List<MessageNotifier> mqttClientList = new ArrayList<MessageNotifier>();
			mqttClientList.add(client);
			messageNotificationClientQueueImpl.setIncommingMessageNotifiers(mqttClientList);

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

			List<NotificationClient> notificationHandlingClients = Arrays.asList(notificationClient);
			messageNotificationClientQueueImpl.setOutgoingNotificationHandlingClients(notificationHandlingClients);

			LOG.debug("initialising notification queue");
			try{
				messageNotificationClientQueueImpl.init();
				LOG.debug("notification queue initialised");
			} catch(Exception e){
				LOG.debug("Receiver problem initialising reliable notification queue", e);
			}

			LOG.debug("initialising client");
			try{
				client.init();
				LOG.debug("client initialised");
			} catch(Exception e){
				LOG.debug("Receiver problem initialising client reliable connection", e);
			}

			// wait for receiver to connect
			try {
				while (!Thread.currentThread().isInterrupted() && ! client.isClientConnected()){
					LOG.debug("Waiting for receiver to connect");
					Thread.sleep(10000); // 10 secs
				}
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			// try receiving messages
			String topic=TOPIC_NAME;
			int qos=QOS_LEVEL;
			LOG.debug("Receiver trying to subscribe to topic:"+topic+" qos:"+qos);
			try{
				client.subscribe(topic, qos);
			} catch(Exception e){
				LOG.debug("Receiver problem subscribing", e);
			}
			LOG.debug("Receiver connection initialised");


		}

	}

}
