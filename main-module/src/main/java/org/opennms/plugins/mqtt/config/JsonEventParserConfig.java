package org.opennms.plugins.mqtt.config;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.protocols.xml.config.XmlGroup;


@XmlRootElement(name="jsonEventParser")
@XmlAccessorType(XmlAccessType.NONE)
public class JsonEventParserConfig {
	
	private List<String> subscriptionTopics=null;
	
	private XmlGroup xmlGroup=null;
	
	public List<String> getSubscriptionTopics() {
		return subscriptionTopics;
	}

	@XmlElementWrapper
	@XmlElement(name="topic")
	public void setSubscriptionTopics(List<String> subscriptionTopics) {
		this.subscriptionTopics = subscriptionTopics;
	}

	public XmlGroup getXmlGroup() {
		return xmlGroup;
	}
	
	@XmlElement
	public void setXmlGroup(XmlGroup xmlGroup) {
		this.xmlGroup = xmlGroup;
	}


}
