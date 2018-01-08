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

	// map of topic / parser config
	private Map<String,MessageParserConfig> m_topicParserMap = new ConcurrentHashMap<String,MessageParserConfig>(); 
	
	private DataPersistor dataPersistor=null;
	
	private EventPersistor eventPersistor=null;
	
	public void setTopicParserMap(Map<String,MessageParserConfig> topicParserMap){
		m_topicParserMap.clear();
		m_topicParserMap.putAll(topicParserMap);
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

		MessageParserConfig parserConfig = m_topicParserMap.get(topic);
		if (parserConfig==null){
			LOG.warn("Ignoring message recieved for unknown topic:"+topic);
			return;
		}

		try{
			String payloadType = parserConfig.getPayloadType();
			Object payloadObject = MessagePayloadTypeHandler.parsePayload(payload, payloadType);

			XmlGroups source = parserConfig.getXmlGroups();
			OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(source);
			
			List<OnmsCollectionAttributeMap> attributeMap = onmsAttributeMessageHandler.payloadObjectToAttributeMap(payloadObject);
			
			if(parserConfig instanceof MessageDataParserConfig) {
				dataPersistor.persistAttributeMapList(attributeMap);
			} else if(parserConfig instanceof MessageEventParserConfig) {
				eventPersistor.persistAttributeMapList(attributeMap);
			} else throw new RuntimeException("unable to process parserconfig type:"+parserConfig.getClass().getName());
			
		} catch (Exception ex){
			LOG.error("unable to handle message from topic:"+topic, ex);
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
