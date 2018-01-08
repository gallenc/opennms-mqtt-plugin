package org.opennms.plugins.mqtt.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.protocols.xml.config.XmlRrd;

@XmlRootElement(name="messageDataParser")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageDataParserConfig extends MessageParserConfig {
	
	private XmlRrd xmlRrd=null;
	
	public XmlRrd getXmlRrd() {
		return xmlRrd;
	}

	@XmlElement
	public void setXmlRrd(XmlRrd xmlRrd) {
		this.xmlRrd = xmlRrd;
	}



}
