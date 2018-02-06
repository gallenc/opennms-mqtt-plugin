package org.opennms.plugins.messagenotifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.opennms.plugins.messagenotifier.rest.MqttRxService;
import org.opennms.plugins.mqtt.config.ConfigProperty;
import org.opennms.plugins.mqtt.config.MessageEventParserConfig;
import org.opennms.plugins.mqtt.config.MessageDataParserConfig;
import org.opennms.plugins.mqtt.config.MQTTClientConfig;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqtt.config.MessageClientConfig;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	
	AtomicBoolean configLoaded= new AtomicBoolean(false);
	
	// internal map of client name / mqtt client
	private Map<String,MQTTClientImpl> m_clientMap= new HashMap<String, MQTTClientImpl>();

	// internal map of client name / mqttrx service
	private Map<String,MqttRxService>  m_rxServiceMap= new HashMap<String, MqttRxService>();
	
	// internal parsed xml configuration
	private MQTTReceiverConfig m_MQTTReceiverConfig = null;
	
	/*
	 * externally set class properties
	 */
	
	// url to load config
	private String m_configFile=null;
	
	// notification message handler - convert messages to events or data
	private NotificationMessageHandler m_notificationMessageHandler=null;
	
	// node cache for opennms nodes
    private NodeByForeignSourceCacheImpl m_nodeByForeignSourceCacheImpl=null;
	
	// m_messageReceiverServices are used to define message notifiers 
	// within the initial blueprint. Used for ReST interface
	private List<MqttRxService> m_messageReceiverServices = null;

	// message queue and consumer thread pool
	private MessageNotificationClientQueueImpl m_messageNotificationClientQueueImpl = null;


	/*
	 * getters and setters for class properties
	 */
	
	public MQTTReceiverConfig getM_MQTTReceiverConfig() {
		return m_MQTTReceiverConfig;
	}

	public void setM_MQTTReceiverConfig(MQTTReceiverConfig m_MQTTReceiverConfig) {
		this.m_MQTTReceiverConfig = m_MQTTReceiverConfig;
	}
	
	public NotificationMessageHandler getNotificationMessageHandler() {
		return m_notificationMessageHandler;
	}

	public void setNotificationMessageHandler(NotificationMessageHandler notificationMessageHandler) {
		this.m_notificationMessageHandler = notificationMessageHandler;
	}

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

	public MessageNotificationClientQueueImpl getMessageNotificationClientQueueImpl() {
		return m_messageNotificationClientQueueImpl;
	}

	public void setMessageNotificationClientQueueImpl(
			MessageNotificationClientQueueImpl messageNotificationClientQueueImpl) {
		this.m_messageNotificationClientQueueImpl = messageNotificationClientQueueImpl;
	}

	/*
	 * business methods
	 */
	
	/**
	 * called by init to load configuration and set up controlled classes before start();
	 */
	public void loadConfig(){

		m_MQTTReceiverConfig = loadConfigFile();

		//set up node cache
		if (m_nodeByForeignSourceCacheImpl==null) throw new IllegalStateException("nodeByForeignSourceCacheImpl must be set");
		if(m_MQTTReceiverConfig.getNodeCacheMaxSize()!=null) m_nodeByForeignSourceCacheImpl.setMAX_SIZE(m_MQTTReceiverConfig.getNodeCacheMaxSize());
		if(m_MQTTReceiverConfig.getNodeCacheMaxTtl()!=null) m_nodeByForeignSourceCacheImpl.setMAX_TTL(m_MQTTReceiverConfig.getNodeCacheMaxTtl());
		if(m_MQTTReceiverConfig.getCreateDummyInterfaces()!=null) m_nodeByForeignSourceCacheImpl.setCreateDummyInterfaces(m_MQTTReceiverConfig.getCreateDummyInterfaces());
		if(m_MQTTReceiverConfig.getCreateMissingNodes()!=null) m_nodeByForeignSourceCacheImpl.setCreateMissingNodes(m_MQTTReceiverConfig.getCreateMissingNodes());
		if(m_MQTTReceiverConfig.getCreateNodeAssetData()!=null) m_nodeByForeignSourceCacheImpl.setcreateNodeAssetData(m_MQTTReceiverConfig.getCreateNodeAssetData());


		// Set Up mqtt clients and message receivers
		
		// set up mqtt receivers
		// m_clientMap will contain a list of all the mqtt clients 
		for(MQTTClientConfig mqttConfig:m_MQTTReceiverConfig.getMqttClients()){
			
			LOG.debug("adding mqtt receiver client:"+mqttConfig.getClientInstanceId());
			
			if(mqttConfig.getClientInstanceId()==null || "".equals(mqttConfig.getClientInstanceId())) 
				throw new IllegalArgumentException("clientInstanceId value is not defined for a MQTTClientConfig");
			if (m_clientMap.containsKey(mqttConfig.getClientInstanceId())) 
				throw new IllegalArgumentException("duplicate mqtt receiver ClientInstanceId '"+mqttConfig.getClientInstanceId()
						+ "' in configuration");
			
			MQTTClientImpl client = new MQTTClientImpl(mqttConfig);
			
			m_clientMap.put(mqttConfig.getClientInstanceId(), client);
		}
		
		// set up message receivers
		// m_rxServiceMap will contain a list of all the message clients defined in the config

		// set up message receivers from types defined in blueprint
		Map<String, MqttRxService> messageReceivers = new HashMap<String,MqttRxService>();
		if(m_messageReceiverServices!=null){
			for(MqttRxService messageReceiver: m_messageReceiverServices){
				
				if(messageReceiver.getClientInstanceId()==null || "".equals(messageReceiver.getClientInstanceId())) 
					throw new IllegalArgumentException("clientInstanceId value is not defined for a Receiver in blueprint");
				
				if (messageReceivers.containsKey(messageReceiver.getClientInstanceId())) 
					throw new IllegalArgumentException("duplicate messageReceiver ClientInstanceId in blueprint '"+messageReceiver.getClientInstanceId()
							+ "' in configuration");
				messageReceivers.put(messageReceiver.getClientInstanceId(), messageReceiver);
			}
		}

		// find the client in the blueprint corresponding to the configuration file and configure it
		for(MessageClientConfig receiverConfig:m_MQTTReceiverConfig.getMessageClients()){
			
			LOG.debug("adding message receiver client:"+receiverConfig.getClientInstanceId());
			
			if(receiverConfig.getClientInstanceId()==null || "".equals(receiverConfig.getClientInstanceId())) 
				throw new IllegalArgumentException("clientInstanceId value is not defined for a Receiver in configuration");
			
			if (m_rxServiceMap.containsKey(receiverConfig.getClientInstanceId())) 
				throw new IllegalArgumentException("duplicate ClientInstanceId '"+receiverConfig.getClientInstanceId()
						+ "' in message client configuration.");
			
			MqttRxService rxService = messageReceivers.get(receiverConfig.getClientInstanceId());
			
			if(rxService!=null && rxService.getClientType().equals(receiverConfig.getClientType())){
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
				throw new IllegalArgumentException("receiver configId "+receiverConfig.getClientInstanceId()
						+ " clientType "+receiverConfig.getClientType()
						+ " defines an unavailable service");
			}
		}

		// set up message queue
		if(m_messageNotificationClientQueueImpl==null) throw new IllegalStateException("m_messageNotificationClientQueueImpl has not been defined");
		m_messageNotificationClientQueueImpl.setMaxMessageQueueLength(m_MQTTReceiverConfig.getMaxMessageQueueLength());
		m_messageNotificationClientQueueImpl.setMaxMessageQueueThreads(m_MQTTReceiverConfig.getMaxMessageQueueThreads());
		

		// add m_messageReceiverServices to message queue
		// used to add rest interface
		List<MessageNotifier> incomingMessageNotifiers = new ArrayList<MessageNotifier>();
		for(String clientInstanceId : m_rxServiceMap.keySet()){
			LOG.debug("adding rxService to message queue client:"+clientInstanceId);
			incomingMessageNotifiers.add(m_rxServiceMap.get(clientInstanceId));
		}

		// add user defined mqtt receivers to message queue
		for(String clientInstanceId : m_clientMap.keySet()){
			LOG.debug("adding mqtt to message queue client:"+clientInstanceId);
			incomingMessageNotifiers.add(m_clientMap.get(clientInstanceId));
		}

		// add incoming message notifiers to the client queue
		m_messageNotificationClientQueueImpl.setIncommingMessageNotifiers(incomingMessageNotifiers);
		
		// NOTE notification handing clients are set up directly in the blueprint and not here
		// List<NotificationClient> notificationHandlingClients = Arrays.asList(m_notificationMessageHandler);
		// m_messageNotificationClientQueueImpl.setOutgoingNotificationHandlingClients(notificationHandlingClients);
		
		// set up m_notificationMessageHandler
		// with configuration for parsing messages into OnmsCollectionAttributeMap's

		Map<String, MessageDataParserConfig> topicDataParserMap = new HashMap<String, MessageDataParserConfig>();
		for( MessageDataParserConfig messageDataParserConfig:m_MQTTReceiverConfig.getMessageDataParsers()){
			for(String subscription:messageDataParserConfig.getSubscriptionTopics()){
				if (topicDataParserMap.containsKey(subscription)) 
					throw new IllegalArgumentException("duplicate data topic subscription '"+subscription
							+ "' in configuration");
				topicDataParserMap.put(subscription, messageDataParserConfig);
			}
		}
		
		m_notificationMessageHandler.setTopicDataParserMap(topicDataParserMap);
		
		Map<String, MessageEventParserConfig> topicEventParserMap = new HashMap<String, MessageEventParserConfig>();
		for( MessageEventParserConfig messageEventParserConfig:m_MQTTReceiverConfig.getMessageEventParsers()){
			for(String subscription:messageEventParserConfig.getSubscriptionTopics()){
				if (topicEventParserMap.containsKey(subscription)) 
					throw new IllegalArgumentException("duplicate event topic subscription '"+subscription
							+ "' in configuration");
				topicEventParserMap.put(subscription, messageEventParserConfig);
			}
		}
		
		m_notificationMessageHandler.setTopicEventParserMap(topicEventParserMap);
		
		configLoaded.set(true);

	}
	


	/**
	 * init method called by blueprint. Loads the configuration and then calls start()
	 */
	public void init(){
		// load configuration and set up all classes
		loadConfig();
		
		start();
	}

	/**
	 * Starts the controller. Must be called after load config
	 */
	public void start(){
		
		if(! configLoaded.get()) throw new IllegalStateException("loadConfig() must be called and succeed before start()");

		// initialise nodeCache
		m_nodeByForeignSourceCacheImpl.init();

		// initialise messageQueue
		m_messageNotificationClientQueueImpl.init();

		// initialise mqtt receivers
		for(String clientId: m_clientMap.keySet()){
			LOG.info("initialising"+clientId);
			try{
			m_clientMap.get(clientId).init();
			}catch (Exception ex){
				LOG.error("problem initialising clientId:"+clientId,ex);
			}
		}
		
		// initialise other receivers
		for(String clientId: m_rxServiceMap.keySet()){
			LOG.info("initialising"+clientId);
			try{
				m_rxServiceMap.get(clientId).init();
			}catch (Exception ex){
				LOG.error("problem initialising clientId:"+clientId,ex);
			}
		}

	}

	public void destroy(){

		// disconnect and destroy all mqtt receivers
		for (String clientInstanceId : m_clientMap.keySet()){
			try {
				MQTTClientImpl client = m_clientMap.get(clientInstanceId);
				if(client!=null) client.destroy();
			} catch (Exception e){
				LOG.error("problem destroying client "+clientInstanceId, e);
			}
			m_clientMap.remove(clientInstanceId);
		}

		// disconnect and destroy MqttRxService receivers
		for (String clientInstanceId : m_rxServiceMap.keySet()){
			try {
				MqttRxService client = m_rxServiceMap.get(clientInstanceId);
				if(client!=null) client.destroy();
			} catch (Exception e){
				LOG.error("problem destroying m_rxServiceMap client "+clientInstanceId, e);
			}
			m_rxServiceMap.remove(clientInstanceId);
		}

		// destroy message queue (shuts down consumer threads)
		try {
			m_messageNotificationClientQueueImpl.destroy();
		} catch (Exception e){
			LOG.error("problem destroying m_messageNotificationClientQueueImpl", e);
		}

		// destroy node cache
		try{
			m_nodeByForeignSourceCacheImpl.destroy();
		} catch (Exception e){
			LOG.error("problem destroying m_nodeByForeignSourceCacheImpl", e);
		}

	}

	public MQTTReceiverConfig loadConfigFile(){

		if (m_configFile==null) throw new RuntimeException("MQTTReceiverConfig fileUri must be set");

		MQTTReceiverConfig receiverConfig =null;
		try {

			File mqttReceiverConfigFile = new File(m_configFile);
			LOG.debug("reading MQTTReceiverConfig file "+mqttReceiverConfigFile.getAbsolutePath());

			if (mqttReceiverConfigFile.exists()) {
				JAXBContext jaxbContext = JAXBContext.newInstance(MQTTReceiverConfig.class);
				
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				receiverConfig = (MQTTReceiverConfig) jaxbUnmarshaller.unmarshal(mqttReceiverConfigFile);

				System.out.println("MQTT Receiver Config successfully loaded from file="+mqttReceiverConfigFile.getAbsolutePath());
				LOG.info("MQTT Receiver Config successfully loaded from file="+mqttReceiverConfigFile.getAbsolutePath());
			} else {
				System.out.println("MQTT Receiver Config file="+mqttReceiverConfigFile.getAbsolutePath()+" does not exist.");
				LOG.info("MQTT Receiver Config file="+mqttReceiverConfigFile.getAbsolutePath()+" does not exist.");
			}
			return receiverConfig;

		} catch (JAXBException e) {
			LOG.error("MQTT Receiver Config Problem loading configuration: "+ e.getMessage());
			throw new IllegalArgumentException("MQTT Receiver Config Problem loading configuration",e);
		}


	}


}
