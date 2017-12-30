package org.opennms.plugins.messagenotifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.opennms.plugins.mqtt.config.MQTTClientConfig;
import org.opennms.plugins.mqtt.config.MQTTReceiverConfig;
import org.opennms.plugins.mqttclient.MQTTClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private MQTTReceiverConfig m_MQTTReceiverConfig = null;
	
	private Map<String,MQTTClientImpl> m_clientMap= new HashMap();

	private String m_configFile=null;

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
		
		// set up performance
		
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
