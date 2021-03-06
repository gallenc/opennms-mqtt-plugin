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

package org.opennms.plugins.messagehandler;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.opennms.plugins.mqtt.config.MessageDataParserConfig;
import org.opennms.plugins.mqtt.config.MessageEventParserConfig;
import org.opennms.plugins.persistor.datanotifier.DataPersistor;
import org.opennms.plugins.persistor.eventnotifier.EventPersistor;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationMessageHandler implements NotificationClient {
	private static final Logger LOG = LoggerFactory.getLogger(NotificationMessageHandler.class);

	// map of topic / data parser config
	private Map<String,MessageDataParserConfig> m_topicDataParserMap = new ConcurrentHashMap<String,MessageDataParserConfig>(); 

	// map of topic / event parser config
	private Map<String,MessageEventParserConfig> m_topicEventParserMap = new ConcurrentHashMap<String,MessageEventParserConfig>(); 

	private DataPersistor dataPersistor=null;

	private EventPersistor eventPersistor=null;

	public void setTopicDataParserMap(Map<String,MessageDataParserConfig> topicDataParserMap){
		m_topicDataParserMap.clear();
		m_topicDataParserMap.putAll(topicDataParserMap);
	}

	public void setTopicEventParserMap(Map<String,MessageEventParserConfig> topicEventParserMap){
		m_topicEventParserMap.clear();
		m_topicEventParserMap.putAll(topicEventParserMap);
	}

	public DataPersistor getDataPersistor() {
		return dataPersistor;
	}

	public void setDataPersistor(DataPersistor dataPersistor) {
		this.dataPersistor = dataPersistor;
	}

	public EventPersistor getEventPersistor() {
		return eventPersistor;
	}

	public void setEventPersistor(EventPersistor eventPersistor) {
		this.eventPersistor = eventPersistor;
	}

	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		if(messageNotification==null) throw new RuntimeException("messageNotification cannot be null");

		String topic = messageNotification.getTopic();
		Integer qos = messageNotification.getQos();
		byte[] payload = messageNotification.getPayload();
		
		// print out payload in hex
		if(LOG.isDebugEnabled()){
			StringBuffer sb = new StringBuffer("parsing messageNotification received from topic:").append(topic);
			sb.append(" qos:").append(qos).append(" payload(HEX):");
			if(payload!=null) for (byte b : payload) {
				sb.append(Integer.toHexString((int) (b & 0xff)));
			}
			LOG.debug(sb.toString());
		}

		// see if this topic should be processed as a data source
		// looks for direct match of incoming topic against topicFilter map
		// e.g. subscribed topic filter /a/b == received topic name /a/b
		// if no direct match, iterate and try and match each topicFilter
		// e.g. topicFilter "foo/+" matches received topic "foo/bar/baz"
		MessageDataParserConfig dataParserConfig = m_topicDataParserMap.get(topic);
		if (dataParserConfig == null){
			boolean configFound=false;
			String topicFilter=null;
			Iterator<String> itr = m_topicDataParserMap.keySet().iterator();
			while(!configFound && itr.hasNext()){
				topicFilter= itr.next();
				configFound = MqttTopic.isMatched(topicFilter, topic);
			}
			if(configFound){
				dataParserConfig = m_topicDataParserMap.get(topicFilter);
			}
			if(LOG.isDebugEnabled()){
				if(configFound) LOG.debug("matched incoming topic {} against data topicFilter {}", topic, topicFilter);
				else LOG.debug("no data topicFilter match found against incoming topic {}", topic);
			} 
		}

		// see if this topic should be processed as an event
		MessageEventParserConfig eventParserConfig = m_topicEventParserMap.get(topic);
		if (eventParserConfig == null){
			boolean configFound=false;
			String topicFilter=null;
			Iterator<String> itr = m_topicEventParserMap.keySet().iterator();
			while(!configFound && itr.hasNext()){
				topicFilter= itr.next();
				configFound = MqttTopic.isMatched(topicFilter, topic);
			}
			if(configFound){
				eventParserConfig = m_topicEventParserMap.get(topicFilter);
			}
			if(LOG.isDebugEnabled()){
				if(configFound) LOG.debug("matched incoming topic {} against event topicFilter {}", topic, topicFilter);
				else LOG.debug("no event topicFilter match found against incoming topic {}", topic);
			} 
		}

		if (dataParserConfig==null && eventParserConfig==null){
			LOG.warn("Ignoring message recieved from unknown topic:"+topic);
			return;
		}

		// see if topic creates data
		if (dataParserConfig!=null) try{

			String defaultDataForeignSource = dataParserConfig.getForeignSource();
			
			String dataPayloadType = dataParserConfig.getPayloadType();
			Object dataPayloadObject = MessagePayloadTypeHandler.parsePayload(payload, dataPayloadType, dataParserConfig.getCompression());

			// if string returned simply log fact we cannot persist this data
			if(dataPayloadObject instanceof String){
				LOG.warn("cannot persist as data string message received from topic:"+topic+" message:"+dataPayloadObject);
			} else {

				XmlGroups dataSource = dataParserConfig.getXmlGroups();
				XmlRrd xmlRrd = dataParserConfig.getXmlRrd();
				OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(dataSource, xmlRrd, topic, defaultDataForeignSource, qos);

				List<OnmsCollectionAttributeMap> dataAttributeMap = onmsAttributeMessageHandler.payloadObjectToAttributeMap(dataPayloadObject);

				//add topic and qos from notification, foreign source from configuration //TODO REMOVE
//				for(OnmsCollectionAttributeMap dataAttribute:dataAttributeMap){
//					dataAttribute.setTopic(topic);
//					dataAttribute.setQos(qos);
//					dataAttribute.setForeignSource(defaultDataForeignSource);
//				}

				dataPersistor.persistAttributeMapList(dataAttributeMap);
			}

		} catch (Exception ex){
			LOG.error("unable to persist data message from topic:"+topic, ex);
		}

		// see if topic creates an event
		// event messages are saved as string params if they cannot be parsed
		if (eventParserConfig!=null) try{
			List<OnmsCollectionAttributeMap> eventAttributeMap;

			String defaultEventForeignSource = eventParserConfig.getForeignSource();
			
			String eventPayloadType = eventParserConfig.getPayloadType();
			
			String ueiRoot = eventParserConfig.getUeiRoot();
			
			Object eventPayloadObject = MessagePayloadTypeHandler.parsePayload(payload, eventPayloadType, eventParserConfig.getCompression());

			// if string returned simply create event with string value
			if(eventPayloadObject instanceof String){
				eventAttributeMap =  stringToAttributeMap((String)eventPayloadObject);
			} else try {
				// if an object returned try and parse object using jxpath
				XmlGroups eventSource = eventParserConfig.getXmlGroups();
				XmlRrd xmlRrd=null; // xmlRrd not defined for events
				OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(eventSource, xmlRrd, topic, defaultEventForeignSource, qos);
				eventAttributeMap = onmsAttributeMessageHandler.payloadObjectToAttributeMap(eventPayloadObject);
			} catch (Exception ex){
				LOG.error("saving event data as string because unable to use jxpath to convert message from topic:"+topic, ex);
				eventAttributeMap =  stringToAttributeMap(eventPayloadObject.toString());
			}

			//add topic and qos from notification //TODO REMOVE
//			for(OnmsCollectionAttributeMap eventAttribute:eventAttributeMap){
//				eventAttribute.setTopic(topic);
//				eventAttribute.setQos(qos);
//				eventAttribute.setForeignSource(defaultEventForeignSource);
//			}

			eventPersistor.persistAttributeMapList(eventAttributeMap, ueiRoot);

		} catch (Exception ex){
			LOG.error("unable to persist event message from topic:"+topic, ex);
		}
	}

	private static String PAYLOAD_STRING="PAYLOAD_STRING";
	public  List<OnmsCollectionAttributeMap> stringToAttributeMap(String payloadObject){
		OnmsCollectionAttributeMap attributeMap = new OnmsCollectionAttributeMap();

		OnmsCollectionAttribute collectionAttribute = new OnmsCollectionAttribute();
		collectionAttribute.setOnmsType("string");
		collectionAttribute.setValue((String) payloadObject);
		attributeMap.getAttributeMap().put(PAYLOAD_STRING,collectionAttribute );

		// adding timestamp because not parsed from string message
		attributeMap.setTimestamp(new Date());

		List<OnmsCollectionAttributeMap>  attributeMapList  = Arrays.asList(attributeMap);
		return attributeMapList ;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}



}
