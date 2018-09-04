/* ***************************************************************************
 * Copyright 2018 OpenNMS Group Inc, Entimoss Ltd. Or their affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ****************************************************************************/

package org.opennms.plugins.messagenotifier.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.opennms.plugins.messagenotifier.MessageNotificationClientQueueImpl;
import org.opennms.plugins.messagenotifier.MessageNotifier;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.opennms.plugins.messagenotifier.VerySimpleMessageNotificationClient;
import org.opennms.plugins.messagenotifier.mqttclient.MQTTClientImpl;
import org.opennms.plugins.messagenotifier.mqttclient.MQTTTopicSubscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTClientTopicListTests {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTClientTopicListTests.class);

	public static final String SERVER_URL = "tcp://localhost:1883";
	//public static final String SERVER_URL = "tcp://192.168.202.1:1883";
	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";
	public static final String CONNECTION_RETRY_INTERVAL = "60000"; 
	public static final String CLIENT_CONNECTION_MAX_WAIT = "40000";

	public static final String CLIENT_ID = "receiver1";
	public static final String CLIENT_ID2 = "transmitter1";
	public static final String EVENT_TOPIC_NAME = "mqtt-events";
	public static final String DATA_TOPIC_NAME = "mqtt-data";
	public static final String QOS_LEVEL = "0";


	// MQTTTopicSubscription(String topic, String qos)
	Set<MQTTTopicSubscription> topicList = new HashSet<MQTTTopicSubscription>(Arrays.asList(
			new MQTTTopicSubscription(EVENT_TOPIC_NAME, QOS_LEVEL),
			new MQTTTopicSubscription(DATA_TOPIC_NAME, QOS_LEVEL)));


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
			String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
			String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;

			LOG.debug("Receiver initiating connection");
			
			LOG.debug("Receiver TOPIC LIST");
			for(MQTTTopicSubscription sub:topicList){
				LOG.debug("   qos:"+sub.getQos()+"   topic:"+sub.getTopic());
			}

			client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval,clientConnectionMaxWait, null, null);
			client.setTopicList(topicList);
			
			List<MessageNotifier> mqttClientList = new ArrayList<MessageNotifier>();
			mqttClientList.add(client);
			
			messageNotificationClientQueueImpl = new MessageNotificationClientQueueImpl();
			
			messageNotificationClientQueueImpl.setIncommingMessageNotifiers(mqttClientList);

			messageNotificationClientQueueImpl.setMaxMessageQueueThreads(1);
			messageNotificationClientQueueImpl.setMaxMessageQueueLength(100);

			NotificationClient notificationClient = new VerySimpleMessageNotificationClient();

			List<NotificationClient> notificationHandlingClients = Arrays.asList(notificationClient);
			messageNotificationClientQueueImpl.setOutgoingNotificationHandlingClients(notificationHandlingClients);

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

			// subscription already done
			//			String topic=EVENT_TOPIC_NAME;
			//			int qos=Integer.parseInt(QOS_LEVEL);
			//			try{
			//				client.subscribe(topic, qos);
			//			} catch(Exception e){
			//				LOG.debug("Receiver problem subscribing", e);
			//			}
			LOG.debug("Receiver connection initialised");
		}

	}

	@Test
	public void testTopicConnection() {
		LOG.debug("start of test testTopicConnection() ");

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

		// set up transmitter
		String brokerUrl = SERVER_URL;
		String clientId = CLIENT_ID2;
		String userName =MQTT_USERNAME;
		String password =MQTT_PASSWORD;
		String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
		String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;

		// will connect
		brokerUrl = SERVER_URL;
		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval,clientConnectionMaxWait, null, null);

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
		String topic=EVENT_TOPIC_NAME;
		int qos=Integer.parseInt(QOS_LEVEL);

		for (int count=0; count<20; count++){
			try{
				String message="message sent count="+count;
				byte[] payload = message.getBytes();
				client.publishSynchronous(topic, qos, payload);
			} catch(Exception e){
				LOG.debug("problem publishing message", e);
			}
		}

		// wait for received messages
		try {
			Thread.sleep(30000);

		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		//clean up
		client.destroy();
		receiver.close();


		LOG.debug("end of test testTopicConnection() ");
	}

}
