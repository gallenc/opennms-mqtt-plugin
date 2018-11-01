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

import java.net.URI;
import java.util.Scanner;

/**
 * Simple client which listens for messages on a given input topic
 * Note the interrupt method may not work for stopping the class.
 * @author admin
 *
 */

public class MQTTClientConnectionManualTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(MQTTClientConnectionManualTest.class);
	
	//public static final String SERVER_URL = "tcp://localhost:1883";
	//public static final String MQTT_USERNAME = "mqtt-username";
	//public static final String MQTT_PASSWORD = "mqtt-password";
	//public static final String TOPIC_NAME = "seawatch";
	//public static final String CLIENT_ID = "seawatch";

	public static final String SERVER_URL = "tcp://eu.thethings.network:1883";
	public static final String MQTT_USERNAME = "schools-aqn";
	public static final String MQTT_PASSWORD = "mqtt-password";
	public static final String CONNECTION_RETRY_INTERVAL = "60000";
	public static final String CLIENT_CONNECTION_MAX_WAIT = "40000";

	public static final String CLIENT_ID = "opennms";
	public static final String TOPIC_NAME = "schools-aqn/devices/+/up";
	public static final int QOS_LEVEL = 0;
	
	boolean shutdown = false;
	
	
	public static void main(String[] args) {
		MQTTClientConnectionManualTest  connectiontest = new MQTTClientConnectionManualTest();
		connectiontest.getInputAndRun();
	}
	
	@Test
	public void test(){
		getInputAndRun();
	}
	
	
	public void getInputAndRun(){

		String brokerUrl = null;
		String clientId = null;
		String userName = null;
		String password = null;
		String topicFilter = null;

		System.out.println("Simple program to recieve mqtt messages");

		Scanner scan = new Scanner(System.in);

		// url
		boolean correctInput = false;
		String lineStr = "";
		while (!correctInput) {
			try {
				System.out.println("enter broker url [" + SERVER_URL + "]\n");
				lineStr = scan.nextLine().trim();
				if (lineStr.isEmpty()) {
					lineStr = SERVER_URL;
				}
				URI uri = new URI(lineStr);
				brokerUrl = lineStr;
				correctInput = true;
			} catch (Exception ex) {
				correctInput = false;
				System.out.println("cannot read url " + lineStr);
			}
		}

		// client id
		correctInput = false;
		lineStr = "";
		while (!correctInput) {
			try {
				System.out.println("client id [" + CLIENT_ID + "]\n");
				lineStr = scan.nextLine().trim();
				if (lineStr.isEmpty()) {
					lineStr = CLIENT_ID;
				}
				clientId = lineStr;
				correctInput = true;
			} catch (Exception ex) {
				correctInput = false;
				System.out.println("cannot read clientid " + lineStr);
			}
		}

		// username
		correctInput = false;
		lineStr = "";
		while (!correctInput) {
			try {
				System.out.println("username: [" + MQTT_USERNAME + "]\n");
				lineStr = scan.nextLine().trim();
				if (lineStr.isEmpty()) {
					lineStr = MQTT_USERNAME;
				}
				userName = lineStr;
				correctInput = true;
			} catch (Exception ex) {
				correctInput = false;
				System.out.println("cannot read username " + lineStr);
			}
		}

		// password
		correctInput = false;
		lineStr = "";
		while (!correctInput) {
			try {
				System.out.println("password [" + MQTT_PASSWORD + "]\n");
				lineStr = scan.nextLine().trim();
				if (lineStr.isEmpty()) {
					lineStr = MQTT_PASSWORD;
				}
				password = lineStr;
				correctInput = true;
			} catch (Exception ex) {
				correctInput = false;
				System.out.println("cannot read password " + lineStr);
			}
		}

		// topic filter
		correctInput = false;
		lineStr = "";
		while (!correctInput) {
			try {
				System.out.println("topicFilter [" + TOPIC_NAME + "]\n");
				lineStr = scan.nextLine().trim();
				if (lineStr.isEmpty()) {
					lineStr = TOPIC_NAME;
				}
				topicFilter = lineStr;
				correctInput = true;
			} catch (Exception ex) {
				correctInput = false;
				System.out.println("cannot read TOPIC" + lineStr);
			}
		}
		
		makeSimpleConnection(brokerUrl, clientId, userName, password, topicFilter) ;

	}

	public void makeSimpleConnection(String brokerUrl, String clientId,
			String userName, String password, String topicFilter) {

		String connectionRetryInterval = CONNECTION_RETRY_INTERVAL;
		String clientConnectionMaxWait = CLIENT_CONNECTION_MAX_WAIT;

		MQTTClientImpl client = new MQTTClientImpl(brokerUrl, clientId,
				userName, password, connectionRetryInterval,
				clientConnectionMaxWait, null, null);

		try {
			try {
				boolean connected = client.connect();
				LOG.debug("client connected=" + connected);
			} catch (Exception e) {
				LOG.debug("problem connecting", e);
			}

			int qos = QOS_LEVEL;
			try {
				client.subscribe(topicFilter, qos);
			} catch (Exception e) {
				LOG.debug("problem subscribing", e);
			}
			
			// this just waits until ctrl c
			Runtime.getRuntime().addShutdownHook(new Thread() {
		        public void run() {
		            try {
		                Thread.sleep(200);
		                System.out.println("Shutting down ...");
		                //some cleaning up code...
		                
		                shutdown=true;

		            } catch (InterruptedException e) {
		                // TODO Auto-generated catch block
		                e.printStackTrace();
		            }
		        }
		    });
			
			while (!shutdown){
				try{
				Thread.sleep(200);
				} catch (InterruptedException e) {
					
				}
			}

		
		} finally {
			// clean up
			if (client != null)
				client.destroy();
		}

		LOG.debug("end of test testSimpleConnection() ");
	}

}
