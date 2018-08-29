package org.opennms.plugins.messagenotifier.mqttclient;


public class MQTTTopicSubscription {

	private String topic=null;
	private String qos=null;
	
	public MQTTTopicSubscription(){};
	
	public MQTTTopicSubscription(String topic,String qos ){
		this.topic=topic;
		this.qos=qos;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getQos() {
		return qos;
	}

	public void setQos(String qos) {
		this.qos = qos;
	}

}