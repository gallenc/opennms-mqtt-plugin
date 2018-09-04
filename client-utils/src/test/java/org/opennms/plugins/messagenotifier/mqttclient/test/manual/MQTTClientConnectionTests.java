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

import org.junit.Test;
import org.opennms.plugins.messagenotifier.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTClientConnectionTests {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTClientConnectionTests.class);


	public static final String SERVER_URL = "tcp://localhost:1883";
	public static final String WRONG_SERVER_URL = "tcp://localhost:1884";
	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";
	public static final String CONNECTION_RETRY_INTERVAL = "60000"; 
	public static final String CLIENT_CONNECTION_MAX_WAIT = "40000";

	public static final String CLIENT_ID = "sewatech";
	public static final String TOPIC_NAME = "sewatech";
	public static final int QOS_LEVEL = 0;


	@Test
	public void testSimpleConnection() {
		LOG.debug("start of test testSimpleConnection() ");

		String brokerUrl = SERVER_URL;
		String clientId = CLIENT_ID;
		String userName =MQTT_USERNAME;
		String password =MQTT_PASSWORD;
		String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
		String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password,connectionRetryInterval,clientConnectionMaxWait );

		try{
			try{
				boolean connected = client.connect();
				LOG.debug("client connected="+connected);
			} catch(Exception e){
				LOG.debug("problem connecting", e);
			}

			String topic=TOPIC_NAME;
			int qos=QOS_LEVEL;
			try{
				client.subscribe(topic, qos);
			} catch(Exception e){
				LOG.debug("problem subscribing", e);
			}

			byte[] payload = "Hello from testSimpleConnection()".getBytes();

			try{
				client.publishSynchronous(topic, qos, payload);
			} catch(Exception e){
				LOG.debug("problem publishing message", e);
			}

		} finally{
			// clean up
			if(client!=null) client.destroy();
		}

		LOG.debug("end of test testSimpleConnection() ");
	}

	@Test
	public void testReliableConnection() {
		LOG.debug("start of test testReliableConnection() ");

		String brokerUrl = WRONG_SERVER_URL;
		String clientId = CLIENT_ID;
		String userName =null;
		String password =null;
		String connectionRetryInterval= CONNECTION_RETRY_INTERVAL;
		String clientConnectionMaxWait= CLIENT_CONNECTION_MAX_WAIT;

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval, clientConnectionMaxWait);

		try{
			// wont connect
			LOG.debug("Testing connection retrys with bad url - should not connect ");
			try{
				client.init();
			} catch(Exception e){
				LOG.debug("problem initialising reliable connection", e);
			}

			try {
				Thread.sleep(Long.parseLong(clientConnectionMaxWait));
				assertFalse(client.isClientConnected());
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			client.destroy();

			LOG.debug("Testing connection retrys with good url - should connect first time ");
			// will connect
			brokerUrl = SERVER_URL;
			client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval,clientConnectionMaxWait);

			try{
				client.init();
			} catch(Exception e){
				LOG.debug("problem initialising reliable connection", e);
			}

			try {
				Thread.sleep(Long.parseLong(clientConnectionMaxWait));
				assertTrue(client.isClientConnected());
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			// try sending message
			String topic=TOPIC_NAME;
			int qos=QOS_LEVEL;
			try{
				client.subscribe(topic, qos);
			} catch(Exception e){
				LOG.debug("problem subscribing", e);
			}

			byte[] payload = "Hello from testReliableConnection()".getBytes();

			try{
				client.publishSynchronous(topic, qos, payload);
			} catch(Exception e){
				LOG.debug("problem publishing message", e);
			}

		} finally{
			if(client!=null) client.destroy();
		}

		LOG.debug("end of test testReliableConnection() ");
	}

}
