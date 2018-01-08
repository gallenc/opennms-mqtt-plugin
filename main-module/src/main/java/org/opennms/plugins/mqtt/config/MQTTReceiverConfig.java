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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="mqtt-receiver")
@XmlAccessorType(XmlAccessType.NONE)
public class MQTTReceiverConfig {

	private Set<MQTTClientConfig> mqttClients=null;
	
	private Set<MessageClientConfig> messageClients=null;

	private Set<MessageDataParserConfig> messageDataParsers=null;

	private Set<MessageEventParserConfig> messageEventParsers=null;

	private Integer maxMessageQueueLength = 1000; // default message queue length

	private Boolean createMissingNodes=true;

	private Boolean createDummyInterfaces=true;

	private Boolean createNodeAssetData=true;

	private Integer nodeCacheMaxTtl=null; // default  0 (Set to zero to disable TTL)

	private Integer nodeCacheMaxSize=null; // default 10000 (Set to zero to disable max size)


	public Set<MQTTClientConfig> getMqttClients() {
		return mqttClients;
	}

	@XmlElementWrapper
	@XmlElement(name="client")
	public void setMqttClients(Set<MQTTClientConfig> mqttClients) {
		this.mqttClients = mqttClients;
	}

	@XmlElementWrapper
	@XmlElement(name="message-client")
	public Set<MessageClientConfig> getMessageClients() {
		return messageClients;
	}

	public void setMessageClients(Set<MessageClientConfig> messageClients) {
		this.messageClients = messageClients;
	}

	public Set<MessageDataParserConfig> getMessageDataParsers() {
		return messageDataParsers;
	}

	@XmlElementWrapper
	@XmlElement(name="messageDataParser")
	public void setMessageDataParsers(Set<MessageDataParserConfig> messageDataParsers) {
		this.messageDataParsers = messageDataParsers;
	}

	public Set<MessageEventParserConfig> getMessageEventParsers() {
		return messageEventParsers;
	}

	@XmlElementWrapper
	@XmlElement(name="messageEventParser")
	public void setMessageEventParsers(Set<MessageEventParserConfig> messageEventParsers) {
		this.messageEventParsers = messageEventParsers;
	}

	public Integer getMaxMessageQueueLength() {
		return maxMessageQueueLength;
	}

	@XmlElement
	public void setMaxMessageQueueLength(Integer macQueueLength) {
		this.maxMessageQueueLength = macQueueLength;
	}

	public Boolean getCreateMissingNodes() {
		return createMissingNodes;
	}

	@XmlElement
	public void setCreateMissingNodes(Boolean createMissingNodes) {
		this.createMissingNodes = createMissingNodes;
	}

	public Boolean getCreateDummyInterfaces() {
		return createDummyInterfaces;
	}

	@XmlElement
	public void setCreateDummyInterfaces(Boolean createDummyInterfaces) {
		this.createDummyInterfaces = createDummyInterfaces;
	}

	public Boolean getCreateNodeAssetData() {
		return createNodeAssetData;
	}

	@XmlElement
	public void setCreateNodeAssetData(Boolean createNodeAssetData) {
		this.createNodeAssetData = createNodeAssetData;
	}

	public Integer getNodeCacheMaxTtl() {
		return nodeCacheMaxTtl;
	}

	@XmlElement
	public void setNodeCacheMaxTtl(Integer nodeCacheMaxTtl) {
		this.nodeCacheMaxTtl = nodeCacheMaxTtl;
	}

	public Integer getNodeCacheMaxSize() {
		return nodeCacheMaxSize;
	}

	@XmlElement
	public void setNodeCacheMaxSize(Integer nodeCacheMaxSize) {
		this.nodeCacheMaxSize = nodeCacheMaxSize;
	}

}
