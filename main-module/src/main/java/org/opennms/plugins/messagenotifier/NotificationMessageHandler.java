package org.opennms.plugins.messagenotifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opennms.plugins.json.OnmsAttributeMessageHandler;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.messagenotifier.datanotifier.DataPersistor;
import org.opennms.plugins.messagenotifier.eventnotifier.EventPersistor;
import org.opennms.plugins.mqtt.config.MessageDataParserConfig;
import org.opennms.plugins.mqtt.config.MessageEventParserConfig;
import org.opennms.plugins.mqtt.config.MessageParserConfig;
import org.opennms.protocols.xml.config.XmlGroups;
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
		byte[] payload = messageNotification.getPayload();

		MessageDataParserConfig dataParserConfig = m_topicDataParserMap.get(topic);
		
		MessageEventParserConfig eventParserConfig = m_topicEventParserMap.get(topic);
		
		if (dataParserConfig==null && eventParserConfig==null){
			LOG.warn("Ignoring message recieved for unknown topic:"+topic);
			return;
		}

		// see if topic creates data
		if (dataParserConfig!=null) try{
			String dataPayloadType = dataParserConfig.getPayloadType();
			Object dataPayloadObject = MessagePayloadTypeHandler.parsePayload(payload, dataPayloadType);

			XmlGroups dataSource = dataParserConfig.getXmlGroups();
			OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(dataSource);
			
			List<OnmsCollectionAttributeMap> dataAttributeMap = onmsAttributeMessageHandler.payloadObjectToAttributeMap(dataPayloadObject);

			dataPersistor.persistAttributeMapList(dataAttributeMap);
	
		} catch (Exception ex){
			LOG.error("unable to persist data message from topic:"+topic, ex);
		}
		// see if topic creates an event
		if (eventParserConfig!=null)try{
			String eventPayloadType = eventParserConfig.getPayloadType();
			Object eventPayloadObject = MessagePayloadTypeHandler.parsePayload(payload, eventPayloadType);

			XmlGroups eventSource = eventParserConfig.getXmlGroups();
			OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(eventSource);
			
			List<OnmsCollectionAttributeMap> eventAttributeMap = onmsAttributeMessageHandler.payloadObjectToAttributeMap(eventPayloadObject);

			eventPersistor.persistAttributeMapList(eventAttributeMap);
			
		} catch (Exception ex){
			LOG.error("unable to persist event message from topic:"+topic, ex);
		}
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
