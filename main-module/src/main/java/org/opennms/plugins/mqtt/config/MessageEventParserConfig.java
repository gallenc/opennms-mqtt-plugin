package org.opennms.plugins.mqtt.config;



import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name="messageEventParser")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageEventParserConfig extends MessageParserConfig {

	String ueiRoot=null;
	
	public MessageEventParserConfig(){
		super();
	}

	public String getUeiRoot() {
		return ueiRoot;
	}

	@XmlElement
	public void setUeiRoot(String ueiRoot) {
		this.ueiRoot = ueiRoot;
	}

}
