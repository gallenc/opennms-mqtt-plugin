 /*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.mqtt.config;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="message-client")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageClientConfig {
	
	private String clientInstanceId=null;
	
	private Set<MQTTTopicSubscriptionXml> topicList=null;
	
	private String clientType=null;
	
	private Set<ConfigProperty> configuration=null;

	public String getClientType() {
		return clientType;
	}

	@XmlAttribute(required=true)
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}


	public Set<ConfigProperty> getConfiguration() {
		return configuration;
	}

	@XmlElementWrapper()
	@XmlElement(name="client-configuration")
	public void setConfiguration(Set<ConfigProperty> configuration) {
		this.configuration = configuration;
	}

	public String getClientInstanceId() {
		return clientInstanceId;
	}

	@XmlAttribute(required=true)
	public void setClientInstanceId(String clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public Set<MQTTTopicSubscriptionXml> getTopicList() {
		return topicList;
	}

	@XmlElementWrapper(required=true)
	@XmlElement(name="topic")
	public void setTopicList(Set<MQTTTopicSubscriptionXml> topicList) {
		this.topicList = topicList;
	}

	
}
