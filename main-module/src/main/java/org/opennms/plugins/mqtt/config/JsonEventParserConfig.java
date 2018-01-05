package org.opennms.plugins.mqtt.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.protocols.xml.config.XmlGroups;


@XmlRootElement(name="jsonEventParser")
@XmlAccessorType(XmlAccessType.NONE)
public class JsonEventParserConfig {
	
	private List<String> subscriptionTopics=null;
	
	private XmlGroups xmlGroups=null;
	
	public List<String> getSubscriptionTopics() {
		return subscriptionTopics;
	}

	@XmlElementWrapper
	@XmlElement(name="topic")
	public void setSubscriptionTopics(List<String> subscriptionTopics) {
		this.subscriptionTopics = subscriptionTopics;
	}

	public XmlGroups getXmlGroups() {
		return xmlGroups;
	}

	@XmlElement
	public void setXmlGroups(XmlGroups xmlGroups) {
		this.xmlGroups = xmlGroups;
	}


}
