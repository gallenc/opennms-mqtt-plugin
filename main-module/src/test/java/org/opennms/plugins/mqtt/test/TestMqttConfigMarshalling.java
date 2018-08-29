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

package org.opennms.plugins.mqtt.test;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.opennms.netmgt.collection.api.AttributeType;
import org.opennms.plugins.messagehandler.CompressionMethods;
import org.opennms.plugins.messagehandler.MessagePayloadTypeHandler;
import org.opennms.plugins.mqtt.config.ConfigProperty;
import org.opennms.plugins.mqtt.config.MessageEventParserConfig;
import org.opennms.plugins.mqtt.config.MessageDataParserConfig;
import org.opennms.plugins.mqtt.config.MQTTClientConfigXml;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqtt.config.MQTTTopicSubscriptionXml;
import org.opennms.plugins.mqtt.config.MessageClientConfig;
import org.opennms.protocols.xml.config.XmlGroup;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlObject;
import org.opennms.protocols.xml.config.XmlResourceKey;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMqttConfigMarshalling {
	private static final Logger LOG = LoggerFactory.getLogger(TestMqttConfigMarshalling.class);
	
	private String brokerUrl="tcp://localhost:1883";
	private String clientId="opennms";
	private String clientInstanceId="client1";
	private String connectionRetryInterval="30000";
	private String clientConnectionMaxWait="20000";
	private String password="xxx";
	private String userName="yyy";

	
	private List<String> eventSubscriptionTopics= Arrays.asList("mqtt-events","mqtt-events2");
	
	private List<String> pmSubscriptionTopics= Arrays.asList("mqtt-data","mqtt-data2");
	// set RRAS values
	// default 5 minutes
	private Integer step=300;
	
	private List<String> m_rras = Arrays.asList("RRA:AVERAGE:0.5:1:2016",
			"RRA:AVERAGE:0.5:12:1488",
			"RRA:AVERAGE:0.5:288:366",
			"RRA:MAX:0.5:288:366",
			"RRA:MIN:0.5:288:366");

	private Integer maxMessageQueueLength=1001;
	
	private Integer maxMessageQueueThreads=2;

	private String timestampFormat="yyyy-MM-dd HH:mm:ss.SSSSSS";

	private List<String> keyXpathList= Arrays.asList("@name","@id");
	
    private Boolean createMissingNodes=true;
    
    private Boolean createDummyInterfaces=true;
    
    private Boolean createNodeAssetData=true;
    
    private String foreignSource="mqtt";

	private Integer nodeCacheMaxSize=1000;

	private Integer nodeCacheMaxTtl=0;
	
	private String compression = CompressionMethods.UNCOMPRESSED;

	private String ueiRoot = "uei.opennms.org/plugin/MqttReceiver/TestEvent";

	@Test
	public void testMQTTClientConfig() {
		LOG.debug("start testMQTTClientConfig()");
		
		//define xmlgroup
		XmlGroup xmlGroup=new XmlGroup();
		xmlGroup.setName("name");
		xmlGroup.setResourceType("resourceType");
		xmlGroup.setResourceXpath("resourceXpath");
		xmlGroup.setTimestampXpath("timestampXpath");
		xmlGroup.setTimestampFormat(timestampFormat);
		xmlGroup.setKeyXpath("keyXpath");
		
		XmlResourceKey xmlResourceKey = new XmlResourceKey();
		xmlResourceKey.setKeyXpathList(keyXpathList);
		xmlGroup.setXmlResourceKey(xmlResourceKey );
		
		XmlObject xmlObject=new XmlObject();
		AttributeType dataType=AttributeType.GAUGE;
		xmlObject.setDataType(dataType);
		xmlObject.setName("name");
		xmlObject.setXpath("xpath");
		xmlGroup.addXmlObject(xmlObject);
		
		XmlGroups xmlGroups = new XmlGroups();
		xmlGroups.addXmlGroup(xmlGroup);
		
		// create clients and topics
		Set<MQTTTopicSubscriptionXml> topicList =  new LinkedHashSet<MQTTTopicSubscriptionXml>();
		
		MQTTTopicSubscriptionXml msub1= new MQTTTopicSubscriptionXml();
		msub1.setQos("0");
		msub1.setTopic("mqtt-events");
		topicList.add(msub1);
		
		MQTTTopicSubscriptionXml msub2= new MQTTTopicSubscriptionXml();
		msub2.setQos("1");
		msub2.setTopic("mqtt-data");
		topicList.add(msub2);
		
		MQTTClientConfigXml mConfig = new MQTTClientConfigXml();
		
		mConfig.setTopicList(topicList);
		
		mConfig.setBrokerUrl(brokerUrl);
		mConfig.setClientId(clientId);
		mConfig.setClientInstanceId(clientInstanceId);
		mConfig.setConnectionRetryInterval(connectionRetryInterval);
		mConfig.setClientConnectionMaxWait(clientConnectionMaxWait);
		mConfig.setPassword(password);
		mConfig.setUserName(userName);
		
		MQTTReceiverConfig rxconfig =new MQTTReceiverConfig();
		rxconfig.setMaxMessageQueueLength(maxMessageQueueLength);
		rxconfig.setMaxMessageQueueThreads(maxMessageQueueThreads);
		rxconfig.setCreateDummyInterfaces(createDummyInterfaces);
		rxconfig.setCreateMissingNodes(createMissingNodes);
		rxconfig.setCreateNodeAssetData(createNodeAssetData);
		rxconfig.setNodeCacheMaxSize(nodeCacheMaxSize);
		rxconfig.setNodeCacheMaxTtl(nodeCacheMaxTtl);
		
		MessageClientConfig messageClientConfig = new MessageClientConfig();
		messageClientConfig.setClientType("opennms-rest-client");
		messageClientConfig.setClientInstanceId("rest-client");
		messageClientConfig.setTopicList(topicList);

		Set<ConfigProperty> configuration= new LinkedHashSet<ConfigProperty>();
		ConfigProperty property = new ConfigProperty();
		property.setName("TBD");
		property.setValue("TBD");
		configuration.add(property);
		messageClientConfig.setConfiguration(configuration);

		Set<MessageClientConfig> messageClients = new LinkedHashSet<MessageClientConfig>();
		messageClients.add(messageClientConfig);
		rxconfig.setMessageClients(messageClients);

		Set<MQTTClientConfigXml> mqttClients = new LinkedHashSet<MQTTClientConfigXml>();
		mqttClients.add(mConfig);
		rxconfig.setMqttClients(mqttClients);
		
		// create event processors
		Set<MessageEventParserConfig> messageEventParsers= new LinkedHashSet<MessageEventParserConfig>();
		rxconfig.setMessageEventParsers(messageEventParsers);
		
		MessageEventParserConfig eventconfig = new MessageEventParserConfig();
		eventconfig.setPayloadType(MessagePayloadTypeHandler.JSON);
		eventconfig.setForeignSource(foreignSource);
		eventconfig.setCompression(compression);
		eventconfig.setUeiRoot(ueiRoot);
		messageEventParsers.add(eventconfig);
		
		eventconfig.setXmlGroups(xmlGroups);
		
		eventconfig.setSubscriptionTopics(eventSubscriptionTopics);
		
		// create pm processors
        Set<MessageDataParserConfig> messageDataParsers = new LinkedHashSet<MessageDataParserConfig>();
		rxconfig.setMessageDataParsers(messageDataParsers);
		
		MessageDataParserConfig dataconfig= new MessageDataParserConfig();
		dataconfig.setPayloadType(MessagePayloadTypeHandler.JSON);
		dataconfig.setForeignSource(foreignSource);
		dataconfig.setCompression(compression);
		
		messageDataParsers.add(dataconfig);
		
		dataconfig.setXmlGroups(xmlGroups);
		
		dataconfig.setSubscriptionTopics(pmSubscriptionTopics);
		
		XmlRrd xmlRrd= new XmlRrd();
		xmlRrd.setStep(step);
		xmlRrd.setXmlRras(m_rras);
		dataconfig.setXmlRrd(xmlRrd);

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MQTTClientConfigXml.class,MQTTReceiverConfig.class);
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
			
			// test convert back
			Marshaller jaxbMarshaller2 = jaxbContext.createMarshaller();
			jaxbMarshaller2.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			jaxbMarshaller2.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter stringWriter2 = new StringWriter();
			jaxbMarshaller.marshal(unmarshalledmConfig,stringWriter2);
			
			String xmlString2 = stringWriter.toString();
			LOG.debug("re unmarshalled string string mConfig: \n"+xmlString2);
			
			assertEquals(xmlString,xmlString2);

		} catch (JAXBException e) {
			throw new RuntimeException("Problem testing mqttclient",e);
		}
		LOG.debug("end testMQTTClientConfig()");
	}

}
