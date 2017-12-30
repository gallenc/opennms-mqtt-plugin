/** This file is part of OpenNMS(R).
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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.opennms.plugins.mqtt.config.JsonParserConfig;
import org.opennms.plugins.mqtt.config.MQTTClientConfig;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqtt.config.MQTTTopicSubscription;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMqttConfigMarshalling {
	private static final Logger LOG = LoggerFactory.getLogger(TestMqttConfigMarshalling.class);
	
	private String brokerUrl="tcp://localhost:1883";
	private String clientId="opennms";
	private String clientInstanceId="opennms";
	private String connectionRetryInterval="10";
	private String password="xxx";
	private String userName="yyy";



	@Test
	public void testMQTTClientConfig() {
		LOG.debug("start testMQTTClientConfig()");
		
		// create clients and topics
		Set<MQTTTopicSubscription> topicList =  new LinkedHashSet<MQTTTopicSubscription>();
		
		MQTTTopicSubscription msub1= new MQTTTopicSubscription();
		msub1.setQos("0");
		msub1.setTopic("topic1");
		topicList.add(msub1);
		
		MQTTTopicSubscription msub2= new MQTTTopicSubscription();
		msub2.setQos("1");
		msub2.setTopic("topic2");
		topicList.add(msub2);
		
		MQTTClientConfig mConfig = new MQTTClientConfig();
		
		mConfig.setTopicList(topicList);
		
		mConfig.setBrokerUrl(brokerUrl);
		mConfig.setClientId(clientId);
		mConfig.setClientInstanceId(clientInstanceId);
		mConfig.setConnectionRetryInterval(connectionRetryInterval);
		mConfig.setPassword(password);
		mConfig.setUserName(userName);
		
		MQTTReceiverConfig rxconfig =new MQTTReceiverConfig();
		Set<MQTTClientConfig> mqttClients = new LinkedHashSet<MQTTClientConfig>();
		mqttClients.add(mConfig);
		rxconfig.setMqttClients(mqttClients);
		
		// create processors
		JsonParserConfig jsonparser= new JsonParserConfig();

//		XmlRrd rra= new XmlRrd();
//		rra.addRra("xxx");
//		jsonparser.setRra(rra);
		rxconfig.setJsonparser(jsonparser);

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MQTTClientConfig.class,MQTTReceiverConfig.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter stringWriter = new StringWriter();
			jaxbMarshaller.marshal(rxconfig,stringWriter);
			
			String xmlString = stringWriter.toString();
			LOG.debug("output string mConfig: \n"+xmlString);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			
			StringReader reader = new StringReader(xmlString);
			MQTTReceiverConfig unmarshalledmConfig = (MQTTReceiverConfig) jaxbUnmarshaller.unmarshal(reader);
			

		} catch (JAXBException e) {
			throw new RuntimeException("Problem testing mqttclient",e);
		}
		LOG.debug("end testMQTTClientConfig()");
	}

}
