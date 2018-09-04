/* ***************************************************************************
 * Copyright 2018 OpenNMS Group Inc, Entimoss Ltd. Or their affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ****************************************************************************/

package org.opennms.plugins.messagenotifier.mqttclient;

import java.util.Set;

import org.opennms.plugins.messagenotifier.mqttclient.MQTTTopicSubscription;

public class MQTTClientConfig {

	public MQTTClientConfig(){
		super();
	}

	public MQTTClientConfig(String clientInstanceId, String brokerUrl,
			String clientId, String userName, String password,
			String connectionRetryInterval, String clientConnectionMaxWait,
			Set<MQTTTopicSubscription> topicList) {
		super();
		this.clientInstanceId = clientInstanceId;
		this.brokerUrl = brokerUrl;
		this.clientId = clientId;
		this.userName = userName;
		this.password = password;
		this.connectionRetryInterval = connectionRetryInterval;
		this.clientConnectionMaxWait = clientConnectionMaxWait;
		this.topicList = topicList;
	}

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

	public void setClientInstanceId(String clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConnectionRetryInterval() {
		return connectionRetryInterval;
	}

	public void setConnectionRetryInterval(String connectionRetryInterval) {
		this.connectionRetryInterval = connectionRetryInterval;
	}

	public String getClientConnectionMaxWait() {
		return clientConnectionMaxWait;
	}

	public void setClientConnectionMaxWait(String clientConnectionMaxWait) {
		this.clientConnectionMaxWait = clientConnectionMaxWait;
	}

	public Set<MQTTTopicSubscription> getTopicList() {
		return topicList;
	}

	public void setTopicList(Set<MQTTTopicSubscription> topicList) {
		this.topicList = topicList;
	}

}