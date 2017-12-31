package org.opennms.plugins.messagenotifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.opennms.plugins.messagenotifier.datanotifier.MqttDataNotificationClient;
import org.opennms.plugins.messagenotifier.eventnotifier.MqttEventNotificationClient;
import org.opennms.plugins.mqtt.config.JsonEventParserConfig;
import org.opennms.plugins.mqtt.config.JsonDataParserConfig;
import org.opennms.plugins.mqtt.config.MQTTClientConfig;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private List<MessageNotifier> m_defaultMessageNotifiers = null;

	private MQTTReceiverConfig m_MQTTReceiverConfig = null;

	private Map<String,MQTTClientImpl> m_clientMap= new HashMap();

	private String m_configFile=null;

	private MessageNotificationClientQueueImpl m_messageNotificationClientQueueImpl = null;


	public List<MessageNotifier> getDefaultMessageNotifiers() {
		return m_defaultMessageNotifiers;
	}

	public void setDefaultMessageNotifiers(List<MessageNotifier> messageNotifiers) {
		this.m_defaultMessageNotifiers = messageNotifiers;
	}

	public String getConfigFile() {
		return m_configFile;
	}

	public void setConfigFile(String configFile) {
		this.m_configFile = configFile;
	}

	public void init(){

		m_MQTTReceiverConfig = loadConfigFile();

		// set up mqtt receivers
		for(MQTTClientConfig config:m_MQTTReceiverConfig.getMqttClients()){
			MQTTClientImpl client = new MQTTClientImpl(config);
			if (m_clientMap.containsKey(config.getClientInstanceId())) 
				throw new RuntimeException("duplicate ClientInstanceId '"+config.getClientInstanceId()
						+ "' in configuration");
			m_clientMap.put(config.getClientInstanceId(), client);
		}

		// set up message queue
		m_messageNotificationClientQueueImpl = new MessageNotificationClientQueueImpl();
		m_messageNotificationClientQueueImpl.setMaxQueueLength(m_MQTTReceiverConfig.getMaxQueueLength());
		
		// add m_defaultMessageNotifiers from context if defined
		// used to add rest interface
		List<MessageNotifier> messageNotifiers = new ArrayList<MessageNotifier>();
		if(m_defaultMessageNotifiers!=null){
			for(MessageNotifier messageNotifier:m_defaultMessageNotifiers){
				LOG.debug("adding default messageNotifier:"+messageNotifier.toString());
				messageNotifiers.add(messageNotifier);
			}
		}
		
		// add user defined mqtt receivers
		for(String clientInstanceId : m_clientMap.keySet()){
			LOG.debug("adding client:"+clientInstanceId);
			messageNotifiers.add(m_clientMap.get(clientInstanceId));
		}
		
		m_messageNotificationClientQueueImpl.setMessageNotifiers(messageNotifiers);

		Map<String, NotificationClient> topicHandlingClients = new LinkedHashMap<String, NotificationClient>();

		// set up event collectors
		for(JsonEventParserConfig jsonEventParserConfig : m_MQTTReceiverConfig.getJsonEventParsers()){
			MqttEventNotificationClient eventClient = new MqttEventNotificationClient();
			//TODO SET UP CONFIG
			
			for(String eventSubscriptionTopic:jsonEventParserConfig.getSubscriptionTopics()){
				LOG.debug("adding handler for eventSubscriptionTopic:"+eventSubscriptionTopic);
				topicHandlingClients.put(eventSubscriptionTopic, eventClient);
			}
		}

		// set up pm collectors
		for( JsonDataParserConfig jsonDataParserConfig : m_MQTTReceiverConfig.getJsonDataParsers()){
			MqttDataNotificationClient dataClient = new MqttDataNotificationClient();
			//TODO SET UP CONFIG
			
			for(String dataSubscriptionTopic:jsonDataParserConfig.getSubscriptionTopics()){
				LOG.debug("adding handler for performanceSubscriptionTopic:"+dataSubscriptionTopic);
				topicHandlingClients.put(dataSubscriptionTopic, dataClient);
			}
		}


		m_messageNotificationClientQueueImpl.setTopicHandlingClients(topicHandlingClients);
		// init receivers


	}

	public void destroy(){

		// disconnect and destroy mqtt receivers
		for (String clientInstanceId : m_clientMap.keySet()){
			try {
				MQTTClientImpl client = m_clientMap.get(clientInstanceId);
				client.destroy();
				m_clientMap.remove(clientInstanceId);
			} catch (Exception e){
				LOG.error("problem destroying client "+clientInstanceId, e);
			}
		}
	}

	public MQTTReceiverConfig loadConfigFile(){

		if (m_configFile==null) throw new RuntimeException("MQTTReceiverConfig fileUri must be set");

		MQTTReceiverConfig licenceMetadata =null;
		try {

			File mqttReceiverConfigFile = new File(m_configFile);
			LOG.debug("reading MQTTReceiverConfig file "+mqttReceiverConfigFile.getAbsolutePath());

			if (mqttReceiverConfigFile.exists()) {
				JAXBContext jaxbContext = JAXBContext.newInstance(MQTTReceiverConfig.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				licenceMetadata = (MQTTReceiverConfig) jaxbUnmarshaller.unmarshal(mqttReceiverConfigFile);

				System.out.println("MQTT Receiver Config successfully loaded from file="+mqttReceiverConfigFile.getAbsolutePath());
				LOG.info("MQTT Receiver Config successfully loaded from file="+mqttReceiverConfigFile.getAbsolutePath());
			} else {
				System.out.println("MQTT Receiver Config file="+mqttReceiverConfigFile.getAbsolutePath()+" does not exist.");
				LOG.info("MQTT Receiver Config file="+mqttReceiverConfigFile.getAbsolutePath()+" does not exist.");
			}
			return licenceMetadata;

		} catch (JAXBException e) {
			LOG.error("MQTT Receiver Config Problem loading configuration: "+ e.getMessage());
			throw new RuntimeException("MQTT Receiver Config Problem loading configuration",e);
		}


	}
}
