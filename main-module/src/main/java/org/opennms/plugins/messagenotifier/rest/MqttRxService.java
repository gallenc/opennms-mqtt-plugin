package org.opennms.plugins.messagenotifier.rest;

import java.util.Set;

import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.MessageNotificationClient;
import org.opennms.plugins.messagenotifier.MessageNotifier;
import org.opennms.plugins.mqtt.config.MQTTTopicSubscription;

public interface MqttRxService extends MessageNotifier {
	
	public void messageArrived(MessageNotification messageNotification) throws Exception;
	
	public Set<MQTTTopicSubscription> getTopicList();

	public void setTopicList(Set<MQTTTopicSubscription> topicList);

	@Override
	public void addMessageNotificationClient( MessageNotificationClient messageNotificationClient);

	@Override
	public void removeMessageNotificationClient( MessageNotificationClient messageNotificationClient) ;

}
