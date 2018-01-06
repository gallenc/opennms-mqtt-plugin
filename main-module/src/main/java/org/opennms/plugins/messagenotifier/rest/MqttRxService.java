package org.opennms.plugins.messagenotifier.rest;

import java.util.Map;
import java.util.Set;

import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.MessageNotificationClient;
import org.opennms.plugins.messagenotifier.MessageNotifier;
import org.opennms.plugins.mqtt.config.MQTTTopicSubscription;

public interface MqttRxService extends MessageNotifier {
	
	public void init();
	
	public void destroy();
	
	public void setServiceName(String serviceName);
	
	public String getServiceName();
	
	public void setServiceType(String serviceType);
	
	public String getServiceType();
	
	public void setConfiguration(Map<String,String> config);
	
	public Map<String,String> getConfiguration();
	
	public void messageArrived(MessageNotification messageNotification) throws Exception;
	
	public Set<MQTTTopicSubscription> getTopicList();

	public void setTopicList(Set<MQTTTopicSubscription> topicList);

	@Override
	public void addMessageNotificationClient( MessageNotificationClient messageNotificationClient);

	@Override
	public void removeMessageNotificationClient( MessageNotificationClient messageNotificationClient) ;

}
