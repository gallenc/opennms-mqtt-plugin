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

package org.opennms.plugins.messagenotifier;

/**
 * message class used to transport message notifications
 * @author admin
 *
 */
public class MessageNotification {

	private String topic;
	private byte[] payload;
	private int qos;

	public MessageNotification( String topic, int qos, byte[] payload){
		this.topic=topic;
		this.payload=payload;
		this.qos=qos;
	}

	public String getTopic() {
		return topic;
	}

	public int getQos() {
		return qos;
	}

	public byte[] getPayload() {
		return payload;
	}
}
