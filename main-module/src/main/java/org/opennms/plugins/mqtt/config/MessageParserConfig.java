package org.opennms.plugins.mqtt.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.protocols.xml.config.XmlGroups;

@XmlRootElement(name="messageParser")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageParserConfig {

	private List<String> subscriptionTopics = null;
	private String payloadType = null;
	private XmlGroups xmlGroups = null;

	public MessageParserConfig() {
		super();
	}

	public List<String> getSubscriptionTopics() {
		return subscriptionTopics;
	}

	@XmlElementWrapper
	@XmlElement(name = "topic")
	public void setSubscriptionTopics(List<String> subscriptionTopics) {
		this.subscriptionTopics = subscriptionTopics;
	}

	public String getPayloadType() {
		return payloadType;
	}

	@XmlAttribute(required = true)
	public void setPayloadType(String payloadType) {
		this.payloadType = payloadType;
	}

	public XmlGroups getXmlGroups() {
		return xmlGroups;
	}

	@XmlElement(name = "xml-groups")
	public void setXmlGroups(XmlGroups xmlGroups) {
		this.xmlGroups = xmlGroups;
	}

}