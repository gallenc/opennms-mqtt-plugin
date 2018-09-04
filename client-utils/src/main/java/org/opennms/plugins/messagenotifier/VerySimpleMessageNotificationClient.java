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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simply prints out the received notification. Used primarily for testing.
 * @author admin
 *
 */
public class VerySimpleMessageNotificationClient implements NotificationClient {
	private static 	final Logger LOG = LoggerFactory.getLogger(MessageNotificationClientQueueImpl.class);

	public VerySimpleMessageNotificationClient(){
		
	}
	
	
	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		if(LOG.isDebugEnabled()) LOG.debug("Notification received by VerySimpleMessageNotificationClient :\n topic:"+messageNotification.getTopic()
				+ "\n qos:"+messageNotification.getQos()
				+ "\n payload:"+new String(messageNotification.getPayload()));

	}


	@Override
	public void init() {
		LOG.debug("VerySimpleMessageNotificationClient initialised");

	}

	@Override
	public void destroy() {
		LOG.debug("VerySimpleMessageNotificationClient destroyed");

	}

}
