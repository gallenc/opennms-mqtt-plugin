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

package org.opennms.plugins.json.test;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;
import org.opennms.netmgt.events.api.EventIpcManager;
import org.opennms.netmgt.events.api.EventListener;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.messagenotifier.Controller;
import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.MessageNotificationClientQueueImpl;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.opennms.plugins.messagenotifier.NotificationMessageHandler;
import org.opennms.plugins.messagenotifier.datanotifier.DataPersistor;
import org.opennms.plugins.messagenotifier.eventnotifier.EventPersistor;
import org.opennms.plugins.messagenotifier.rest.MqttRxService;
import org.opennms.plugins.messagenotifier.rest.MqttRxServiceImpl;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqtt.config.MessageDataParserConfig;
import org.opennms.plugins.mqtt.config.MessageEventParserConfig;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestController {
	private static final Logger LOG = LoggerFactory.getLogger(TestController.class);

	public static final String TEST_CONFIG_FILE = "src/test/resources/ControllerTests/testConfig.xml";

	private MqttRxService mockMqttRxService=null;

	private AtomicInteger dataMessagesReceived= new AtomicInteger(0);

	private AtomicInteger eventMessagesReceived= new AtomicInteger(0);

	@Test
	public void testLoadConfig() {
		LOG.debug("start testLoadConfig()");
		Controller controller = new Controller();
		controller.setConfigFile(TEST_CONFIG_FILE);
		MQTTReceiverConfig mqttReceiverConfig = controller.loadConfigFile();
		assertNotNull(mqttReceiverConfig);

		// print out unmarshalled jaxbconfig
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MQTTReceiverConfig.class);

			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter writer= new StringWriter();
			jaxbMarshaller.marshal(mqttReceiverConfig, writer);

			LOG.debug("marshaled configuration:\n"+writer.toString());

		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		Set<MessageDataParserConfig> messageDataParsers = mqttReceiverConfig.getMessageDataParsers();
		for(MessageDataParserConfig messageDataParserConfig:messageDataParsers){
			assertNotNull(messageDataParserConfig.getPayloadType());
			assertNotNull(messageDataParserConfig.getSubscriptionTopics());
			assertNotNull(messageDataParserConfig.getXmlGroups());
		}

		Set<MessageEventParserConfig> messageEventParsers = mqttReceiverConfig.getMessageEventParsers();
		for(MessageEventParserConfig messageEventParserConfig:messageEventParsers){
			assertNotNull(messageEventParserConfig.getPayloadType());
			assertNotNull(messageEventParserConfig.getSubscriptionTopics());
			assertNotNull(messageEventParserConfig.getXmlGroups());
		}

		LOG.debug("end testLoadConfig()");
	}

	@Test
	public void testLoadClients() {
		LOG.debug("start testLoadClients()");

		Controller controller = loadClients(TEST_CONFIG_FILE);

		controller.destroy();
	}

	@Test
	public void testSendMessages() {
		LOG.debug("start testSendMessages()");

		LOG.debug("testSendMessages() loading controller configuration:");
		Controller controller = loadClients(TEST_CONFIG_FILE);

		LOG.debug("testSendMessages() starting controller:");
		controller.start();

		// reset counters
		dataMessagesReceived.set(0);
		eventMessagesReceived.set(0);

		// send 1 event message
		String topic = "mqtt-events1";
		int qos = 0 ;
		byte[] payload = TestingSniffyKuraMessages.SNIFFY_TEST_MESSAGE.getBytes(StandardCharsets.UTF_8);

		MessageNotification messageNotification = new MessageNotification(topic, qos, payload);

		try {
			mockMqttRxService.messageArrived(messageNotification);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// send 10 data messages
		for(int i=0;i<10 ; i++){
			String topic2 = "mqtt-data2";
			int qos2 = 0 ;
			byte[] payload2 = TestingSniffyKuraMessages.SNIFFY_TEST_MESSAGE.getBytes(StandardCharsets.UTF_8);

			MessageNotification messageNotification2 = new MessageNotification(topic2, qos2, payload2);

			try {
				mockMqttRxService.messageArrived(messageNotification2);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		// wait for all messages to arrive before testing
		try {
			Thread.sleep(5000); // 5 seconds
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		assertEquals(10,dataMessagesReceived.get());
		assertEquals(1, eventMessagesReceived.get());

		// clean up
		controller.destroy();

		// reset counters
		dataMessagesReceived.set(0);
		eventMessagesReceived.set(0);

		LOG.debug("end testSendMessages()");
	}

	private Controller loadClients(String configFile){

		Controller controller = new Controller();
		controller.setConfigFile(configFile);

		NodeByForeignSourceCacheImpl mockNodeByForeignSourceCacheImpl= new NodeByForeignSourceCacheImpl();
		mockNodeByForeignSourceCacheImpl.setEventIpcManager(mockEventIpcManager);

		controller.setNodeByForeignSourceCacheImpl(mockNodeByForeignSourceCacheImpl);

		mockMqttRxService = new MqttRxServiceImpl();
		mockMqttRxService.setClientType("opennms-rest-client");
		mockMqttRxService.setClientInstanceId("opennms-rest-client");
		List<MqttRxService> messageReceiverServices = Arrays.asList( mockMqttRxService );
		controller.setMessageReceiverServices(messageReceiverServices);

		MessageNotificationClientQueueImpl mockMessageNotificationClientQueueImpl=new MessageNotificationClientQueueImpl();
		controller.setMessageNotificationClientQueueImpl(mockMessageNotificationClientQueueImpl);

		// this is done in the blueprint
		NotificationMessageHandler mockNotificationMessageHandler = new NotificationMessageHandler();
		List<NotificationClient> notificationHandlingClients = Arrays.asList(mockNotificationMessageHandler);
		mockMessageNotificationClientQueueImpl.setOutgoingNotificationHandlingClients(notificationHandlingClients);

		mockNotificationMessageHandler.setDataPersistor(mockDataPersistor);
		mockNotificationMessageHandler.setEventPersistor(mockEventPersistor);

		controller.setNotificationMessageHandler(mockNotificationMessageHandler);

		controller.loadConfig();

		return controller;

	}

	private EventPersistor mockEventPersistor = new EventPersistor(){

		@Override
		public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap) {
			int eventMessagesRx = eventMessagesReceived.addAndGet(1);
			LOG.debug("eventPersistor (eventMessagesRx="+eventMessagesRx+") persisting attributeMap: "+attributeMap.toString());

		}

	};

	private DataPersistor mockDataPersistor = new DataPersistor(){

		@Override
		public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap) {
			int dataMessagesRx = dataMessagesReceived.addAndGet(1);
			LOG.debug("dataPersistor (dataMessagesRx="+dataMessagesRx+") persisting attributeMap: "+attributeMap.toString());

		}

	};

	private EventIpcManager mockEventIpcManager = new EventIpcManager(){

		@Override
		public void addEventListener(EventListener listener) {
			// Auto-generated method stub

		}

		@Override
		public void addEventListener(EventListener listener,
				Collection<String> ueis) {
			// Auto-generated method stub

		}

		@Override
		public void addEventListener(EventListener listener, String uei) {
			// Auto-generated method stub

		}

		@Override
		public void removeEventListener(EventListener listener) {
			// Auto-generated method stub

		}

		@Override
		public void removeEventListener(EventListener listener,
				Collection<String> ueis) {
			// Auto-generated method stub

		}

		@Override
		public void removeEventListener(EventListener listener, String uei) {
			// Auto-generated method stub

		}

		@Override
		public boolean hasEventListener(String uei) {
			// Auto-generated method stub
			return false;
		}

		@Override
		public void send(Event event) throws EventProxyException {
			// Auto-generated method stub

		}

		@Override
		public void send(Log eventLog) throws EventProxyException {
			// Auto-generated method stub

		}

		@Override
		public void sendNow(Event event) {
			// Auto-generated method stub

		}

		@Override
		public void sendNow(Log eventLog) {
			// Auto-generated method stub

		}

		@Override
		public void sendNowSync(Event event) {
			// Auto-generated method stub

		}

		@Override
		public void sendNowSync(Log eventLog) {
			// Auto-generated method stub

		}

	};

}
