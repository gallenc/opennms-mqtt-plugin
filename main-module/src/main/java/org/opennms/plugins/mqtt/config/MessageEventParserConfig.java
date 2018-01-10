package org.opennms.plugins.mqtt.config;



import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name="messageEventParser")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageEventParserConfig extends MessageParserConfig {

	public MessageEventParserConfig(){
		super();
	}

}
