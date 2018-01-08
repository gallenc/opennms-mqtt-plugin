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

package org.opennms.plugins.json.test.manual;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.opennms.plugins.messagenotifier.Controller;
import org.opennms.plugins.messagenotifier.MessageNotificationClientQueueImpl;
import org.opennms.plugins.messagenotifier.NotificationMessageHandler;
import org.opennms.plugins.messagenotifier.rest.MqttRxService;
import org.opennms.plugins.messagenotifier.rest.MqttRxServiceImpl;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestController {
	private static final Logger LOG = LoggerFactory.getLogger(TestController.class);

	public static final String TEST_CONFIG_FILE = "src/test/resources/testConfig.xml";

	@Test
	public void testLoadConfig() {
		LOG.debug("start testLoadConfig()");
		Controller controller = new Controller();
		controller.setConfigFile(TEST_CONFIG_FILE);
		MQTTReceiverConfig mqttReceiverConfig = controller.loadConfigFile();
		assertNotNull(mqttReceiverConfig);
		LOG.debug("end testLoadConfig()");
	}

	@Test
	public void testLoadClients() {
		LOG.debug("start testLoadClients()");
		Controller controller = new Controller();
		controller.setConfigFile(TEST_CONFIG_FILE);

		NodeByForeignSourceCacheImpl mockNodeByForeignSourceCacheImpl= new NodeByForeignSourceCacheImpl();
		controller.setNodeByForeignSourceCacheImpl(mockNodeByForeignSourceCacheImpl);

		MqttRxService mockMqttRxService = new MqttRxServiceImpl();
		mockMqttRxService.setClientType("opennms-rest-client");
		mockMqttRxService.setClientInstanceId("opennms-rest-client");
		List<MqttRxService> messageReceiverServices = Arrays.asList( mockMqttRxService );
		controller.setMessageReceiverServices(messageReceiverServices);
		
		MessageNotificationClientQueueImpl mockMessageNotificationClientQueueImpl=new MessageNotificationClientQueueImpl();
		controller.setMessageNotificationClientQueueImpl(mockMessageNotificationClientQueueImpl);
		
        NotificationMessageHandler mockNotificationMessageHandler = new NotificationMessageHandler();

        controller.setNotificationMessageHandler(mockNotificationMessageHandler);
        
		controller.loadConfig();


		controller.destroy();
		LOG.debug("end testLoadClients()");

	}

}
