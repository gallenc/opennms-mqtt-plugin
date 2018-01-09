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

@XmlRootElement(name="mqtt-client")
@XmlAccessorType(XmlAccessType.NONE)
public class MQTTClientConfig {
	
	private String clientInstanceId=null;
	private String brokerUrl=null;
	private String clientId=null;
	private String userName=null;
	private String password=null;
	private String connectionRetryInterval=null;
	private String clientConnectionMaxWait=null;
	
	private Set<MQTTTopicSubscription> topicList=null;

	public String getClientInstanceId() {
		return clientInstanceId;
	}

	@XmlAttribute(required=true)
	public void setClientInstanceId(String clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	@XmlElement(required=true)
	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	public String getClientId() {
		return clientId;
	}

	@XmlElement(required=true)
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUserName() {
		return userName;
	}

	@XmlElement
	public void setUserName(String userName) {
		this.userName = userName;
	}


	public String getPassword() {
		return password;
	}

	@XmlElement
	public void setPassword(String password) {
		this.password = password;
	}

	public String getConnectionRetryInterval() {
		return connectionRetryInterval;
	}

	@XmlElement
	public void setConnectionRetryInterval(String connectionRetryInterval) {
		this.connectionRetryInterval = connectionRetryInterval;
	}

	@XmlElement
	public String getClientConnectionMaxWait() {
		return clientConnectionMaxWait;
	}

	public void setClientConnectionMaxWait(String clientConnectionMaxWait) {
		this.clientConnectionMaxWait = clientConnectionMaxWait;
	}

	public Set<MQTTTopicSubscription> getTopicList() {
		return topicList;
	}

	@XmlElementWrapper(required=true)
	@XmlElement(name="topic")
	public void setTopicList(Set<MQTTTopicSubscription> topicList) {
		this.topicList = topicList;
	}
	
}
