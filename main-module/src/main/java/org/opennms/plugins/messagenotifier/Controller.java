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

import org.opennms.plugins.json.OnmsAttributeJsonHandler;
import org.opennms.plugins.messagenotifier.datanotifier.MqttDataNotificationClient;
import org.opennms.plugins.messagenotifier.eventnotifier.MqttEventNotificationClient;
import org.opennms.plugins.messagenotifier.rest.MqttRxService;
import org.opennms.plugins.mqtt.config.ConfigProperty;
import org.opennms.plugins.mqtt.config.JsonEventParserConfig;
import org.opennms.plugins.mqtt.config.JsonDataParserConfig;
import org.opennms.plugins.mqtt.config.MQTTClientConfig;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqtt.config.MessageClientConfig;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCacheImpl;
import org.opennms.protocols.xml.config.XmlGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private NodeByForeignSourceCacheImpl m_nodeByForeignSourceCacheImpl=null;

	// m_messageReceiverServices are used to define message notifiers 
	// within the initial blueprint. Used for ReST interface
	private List<MqttRxService> m_messageReceiverServices = null;

	private MQTTReceiverConfig m_MQTTReceiverConfig = null;

	private Map<String,MQTTClientImpl> m_clientMap= new HashMap<String, MQTTClientImpl>();

	private Map<String,MqttRxService>  m_rxServiceMap= new HashMap<String, MqttRxService>();

	private String m_configFile=null;

	private MessageNotificationClientQueueImpl m_messageNotificationClientQueueImpl = null;


	public NodeByForeignSourceCacheImpl getNodeByForeignSourceCacheImpl() {
		return m_nodeByForeignSourceCacheImpl;
	}

	public void setNodeByForeignSourceCacheImpl(
			NodeByForeignSourceCacheImpl nodeByForeignSourceCacheImpl) {
		this.m_nodeByForeignSourceCacheImpl = nodeByForeignSourceCacheImpl;
	}

	public List<MqttRxService> getMessageReceiverServices() {
		return m_messageReceiverServices;
	}

	public void setMessageReceiverServices(List<MqttRxService> messageReceiverServices) {
		m_messageReceiverServices = messageReceiverServices;
	}

	public String getConfigFile() {
		return m_configFile;
	}

	public void setConfigFile(String configFile) {
		this.m_configFile = configFile;
	}

	public void init(){

		m_MQTTReceiverConfig = loadConfigFile();

		//set up node cache
		if (m_nodeByForeignSourceCacheImpl==null) throw new RuntimeException("nodeByForeignSourceCacheImpl must be set");
		if(m_MQTTReceiverConfig.getNodeCacheMaxSize()!=null) m_nodeByForeignSourceCacheImpl.setMAX_SIZE(m_MQTTReceiverConfig.getNodeCacheMaxSize());
		if(m_MQTTReceiverConfig.getNodeCacheMaxTtl()!=null) m_nodeByForeignSourceCacheImpl.setMAX_TTL(m_MQTTReceiverConfig.getNodeCacheMaxTtl());
		if(m_MQTTReceiverConfig.getCreateDummyInterfaces()!=null) m_nodeByForeignSourceCacheImpl.setCreateDummyInterfaces(m_MQTTReceiverConfig.getCreateDummyInterfaces());
		if(m_MQTTReceiverConfig.getCreateMissingNodes()!=null) m_nodeByForeignSourceCacheImpl.setCreateMissingNodes(m_MQTTReceiverConfig.getCreateMissingNodes());
		if(m_MQTTReceiverConfig.getCreateNodeAssetData()!=null) m_nodeByForeignSourceCacheImpl.setcreateNodeAssetData(m_MQTTReceiverConfig.getCreateNodeAssetData());
		//m_nodeByForeignSourceCacheImpl.init(); // TODO
		//m_nodeByForeignSourceCacheImpl.destroy(); // TODO

		// Set Up mqtt clients and message receivers
		// set up mqtt receivers
		for(MQTTClientConfig mqttConfig:m_MQTTReceiverConfig.getMqttClients()){
			if (m_clientMap.containsKey(mqttConfig.getClientInstanceId())) 
				throw new RuntimeException("duplicate ClientInstanceId '"+mqttConfig.getClientInstanceId()
						+ "' in configuration");
			MQTTClientImpl client = new MQTTClientImpl(mqttConfig);
			m_clientMap.put(mqttConfig.getClientInstanceId(), client);
		}

		// set up other receivers
		Map<String, MqttRxService> messageReceivers = new HashMap<String,MqttRxService>();
		if(m_messageReceiverServices!=null){
			for(MqttRxService messageReceiver: m_messageReceiverServices){
				messageReceivers.put(messageReceiver.getServiceName(), messageReceiver);
			}
		}

		for(MessageClientConfig receiverConfig:m_MQTTReceiverConfig.getMessageClients()){
			if (m_clientMap.containsKey(receiverConfig.getClientInstanceId())) 
				throw new RuntimeException("duplicate ClientInstanceId '"+receiverConfig.getClientInstanceId()
						+ "' in configuration");
			MqttRxService rxService = messageReceivers.get(receiverConfig.getClientInstanceId());
			if(rxService!=null && rxService.getServiceType().equals(receiverConfig.getClientType())){
				if(receiverConfig.getConfiguration()!=null) {
					Map<String, String> properties = new HashMap<String, String>();
					for(ConfigProperty prop:receiverConfig.getConfiguration()){
						properties.put(prop.getName(), prop.getValue());
					}
					rxService.setConfiguration(properties);
				}
				if(receiverConfig.getTopicList()!=null) rxService.setTopicList(receiverConfig.getTopicList());
				m_rxServiceMap.put(receiverConfig.getClientInstanceId(), rxService);
			} else {
				throw new RuntimeException("receiver configId "+receiverConfig.getClientInstanceId()
						+ " clientType "+receiverConfig.getClientType()
						+ " defines unavailable service");
			}
		}

		// set up message queue
		m_messageNotificationClientQueueImpl = new MessageNotificationClientQueueImpl();
		m_messageNotificationClientQueueImpl.setMaxMessageQueueLength(m_MQTTReceiverConfig.getMaxMessageQueueLength());

		// add m_messageReceiverServices to message queue
		// used to add rest interface
		List<MessageNotifier> messageNotifiers = new ArrayList<MessageNotifier>();
		for(String clientInstanceId : m_rxServiceMap.keySet()){
			LOG.debug("adding rxService client:"+clientInstanceId);
			messageNotifiers.add(m_rxServiceMap.get(clientInstanceId));
		}

		// add user defined mqtt receivers to message queue
		for(String clientInstanceId : m_clientMap.keySet()){
			LOG.debug("adding mqtt client:"+clientInstanceId);
			messageNotifiers.add(m_clientMap.get(clientInstanceId));
		}

		m_messageNotificationClientQueueImpl.setMessageNotifiers(messageNotifiers);

		Map<String, NotificationClient> topicHandlingClients = new LinkedHashMap<String, NotificationClient>();

		// set up event collectors
		for(JsonEventParserConfig jsonEventParserConfig : m_MQTTReceiverConfig.getJsonEventParsers()){
			// create json parser
			XmlGroups xmlGroups = jsonEventParserConfig.getXmlGroups();
			OnmsAttributeJsonHandler onmsAttributeJsonHandler = new OnmsAttributeJsonHandler(xmlGroups);

			MqttEventNotificationClient eventClient = new MqttEventNotificationClient();
			//TODO SET UP CONFIG 
			//eventClient.setOnmsAttributeJsonHandler(onmsAttributeJsonHandler);

			for(String eventSubscriptionTopic:jsonEventParserConfig.getSubscriptionTopics()){
				LOG.debug("adding handler for eventSubscriptionTopic:"+eventSubscriptionTopic);
				if(topicHandlingClients.containsKey(eventSubscriptionTopic)) {
					LOG.error("duplicate handler for eventSubscriptionTopic:"+eventSubscriptionTopic);
				} else 	topicHandlingClients.put(eventSubscriptionTopic, eventClient);
			}
		}

		// set up pm collectors
		for( JsonDataParserConfig jsonDataParserConfig : m_MQTTReceiverConfig.getJsonDataParsers()){
			// create json parser
			XmlGroups xmlGroups = jsonDataParserConfig.getXmlGroups();
			OnmsAttributeJsonHandler onmsAttributeJsonHandler = new OnmsAttributeJsonHandler(xmlGroups);

			MqttDataNotificationClient dataClient = new MqttDataNotificationClient();
			//TODO SET UP CONFIG 
			//dataClient.setOnmsAttributeJsonHandler(onmsAttributeJsonHandler);

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
		
		// disconnect and destroy MqttRxService receivers
		for (String clientInstanceId : m_rxServiceMap.keySet()){
			try {
				MqttRxService client = m_rxServiceMap.get(clientInstanceId);
				client.destroy();
				m_rxServiceMap.remove(clientInstanceId);
			} catch (Exception e){
				LOG.error("problem destroying m_rxServiceMap client "+clientInstanceId, e);
			}
		}

		//m_nodeByForeignSourceCacheImpl.destroy(); // TODO

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
