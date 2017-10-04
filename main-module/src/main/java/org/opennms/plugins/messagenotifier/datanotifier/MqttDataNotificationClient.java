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

package org.opennms.plugins.messagenotifier.datanotifier;

import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts the received json payload to data for persisting .
 * @author admin
 *
 */
public class MqttDataNotificationClient implements NotificationClient {
	private static 	final Logger LOG = LoggerFactory.getLogger(MqttDataNotificationClient.class);

	ValuePersister valuePersister;

	public void setValuePersister(ValuePersister valuePersister) {
		this.valuePersister = valuePersister;
	}

	public MqttDataNotificationClient(){
		LOG.debug("MqttDataNotificationClient initialised");
	}

	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		String jsonStr = new String(messageNotification.getPayload());

		if(LOG.isDebugEnabled()) LOG.debug("Notification received by MqttDataNotificationClient :\n topic:"+messageNotification.getTopic()
				+ "\n qos:"+messageNotification.getQos()
				+ "\n payload:"+jsonStr);

		if(valuePersister==null) throw new RuntimeException("valuePersistor must be set not null");

		try{
			valuePersister.persistJsonAttributes(jsonStr);
		}catch (Exception e){
			LOG.error("problem persisting received mqtt json payload",e);
		}

	}


	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
