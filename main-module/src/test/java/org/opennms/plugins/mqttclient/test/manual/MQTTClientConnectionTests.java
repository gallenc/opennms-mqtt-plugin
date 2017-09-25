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

import org.junit.Test;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTClientConnectionTests {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTClientConnectionTests.class);


	public static final String SERVER_URL = "tcp://localhost:1883";
	public static final String WRONG_SERVER_URL = "tcp://localhost:1884";
	public static final String MQTT_USERNAME = "mqtt-user";
	public static final String MQTT_PASSWORD = "mqtt-password";
	
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
		String connectionRetryInterval= "10000" ;


		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password,connectionRetryInterval );

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

		LOG.debug("end of test testSimpleConnection() ");
	}

	@Test
	public void testReliableConnection() {
		LOG.debug("start of test testReliableConnection() ");

		String brokerUrl = WRONG_SERVER_URL;
		String clientId = CLIENT_ID;
		String userName =null;
		String password =null;
		String connectionRetryInterval= "1000" ;

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval);

		// wont connect
		LOG.debug("Testing connection retrys with bad url - should not connect ");
		try{
			client.init();
		} catch(Exception e){
			LOG.debug("problem initialising reliable connection", e);
		}

		try {
			Thread.sleep(3000);
			assertFalse(client.isClientConnected());
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		
        client.destroy();
		
        LOG.debug("Testing connection retrys with good url - should connect first time ");
		// will connect
        brokerUrl = SERVER_URL;
		client = new MQTTClientImpl(brokerUrl, clientId, userName, password, connectionRetryInterval);

		try{
			client.init();
		} catch(Exception e){
			LOG.debug("problem initialising reliable connection", e);
		}

		try {
			Thread.sleep(3000);
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

		LOG.debug("end of test testReliableConnection() ");
	}

}
