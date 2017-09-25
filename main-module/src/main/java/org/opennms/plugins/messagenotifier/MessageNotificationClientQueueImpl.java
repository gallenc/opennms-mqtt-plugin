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

package org.opennms.plugins.messagenotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * This is a notification client which receives the notification and adds it to a queue for processing
 * @author admin
 *
 */
public class MessageNotificationClientQueueImpl implements MessageNotificationClient{

	private static final Logger LOG = LoggerFactory.getLogger(MessageNotificationClientQueueImpl.class);

	private MessageNotifier messageNotifier;

	private Integer maxQueueLength=1000;

	private LinkedBlockingQueue<MessageNotification> queue=null;
	private AtomicBoolean clientRunning = new AtomicBoolean(false);

	private RemovingConsumer removingConsumer = new RemovingConsumer();
	private Thread removingConsumerThread = new Thread(removingConsumer);

	private Map<String,NotificationClient> topicHandlingClients = new HashMap<String, NotificationClient>();


	/**
	 * @param topicHandlingClients the topicHandlingClients to set
	 */
	public void setTopicHandlingClients(Map<String,NotificationClient> topicHandlingClients) {
		this.topicHandlingClients.putAll(topicHandlingClients);
		if(LOG.isDebugEnabled()){
			StringBuffer sb = new StringBuffer("registering clients for topics: " );
			for(String topic: this.topicHandlingClients.keySet()){
				sb.append("topic:"+topic+" client:"+topicHandlingClients.get(topic)+", ");
			}
			LOG.debug(sb.toString());
		}
	}

	/**
	 * @param messageNotifier
	 */
	public void setMessageNotifier(MessageNotifier messageNotifier) {
		this.messageNotifier = messageNotifier;
	}

	/**
	 * @return the databaseChangeNotifier
	 */
	public MessageNotifier getMessageNotifier() {
		return messageNotifier;
	}

	public Integer getMaxQueueLength() {
		return maxQueueLength;
	}

	public void setMaxQueueLength(Integer maxQueueLength) {
		this.maxQueueLength = maxQueueLength;
	}

	public void init(){
		LOG.debug("initialising messageNotificationClientQueue with queue size "+maxQueueLength);
		if (messageNotifier==null) throw new IllegalStateException("messageNotifier cannot be null");

		queue= new LinkedBlockingQueue<MessageNotification>(maxQueueLength);

		// start consuming thread
		clientRunning.set(true);
		removingConsumerThread.start();

		// start listening for notifications
		messageNotifier.addMessageNotificationClient(this);

	}

	public void destroy(){
		LOG.debug("shutting down client");

		// stop listening for notifications
		if (messageNotifier!=null) messageNotifier.removeMessageNotificationClient(this);

		// signal consuming thread to stop
		clientRunning.set(false);
		removingConsumerThread.interrupt();
	}

	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		if(LOG.isDebugEnabled()) LOG.debug("client received notification - adding notification to queue");

		if (! queue.offer(messageNotification)){
			LOG.warn("Cannot queue any more messageNotifications. messageNotification queue full. size="+queue.size());
		};

	}


	/*
	 * Class run in separate thread to remove and process notifications from the queue 
	 */
	private class RemovingConsumer implements Runnable {

		@Override
		public void run() {

			// we remove elements from the queue until interrupted and clientRunning==false.
			while (clientRunning.get()) {
				try {
					MessageNotification messageNotification = queue.take();

					if(LOG.isDebugEnabled()) LOG.debug("Notification received from queue by consumer thread :\n topic:"+messageNotification.getTopic()
							+ "\n qos:"+messageNotification.getQos()
							+ "\n payload:"+new String(messageNotification.getPayload()));

					// we look in hashtable for topic handling clients to handle this received notification
					if(topicHandlingClients.isEmpty()) { 
						LOG.warn("no topic handing clients have been set to receive notification");
					} else {
						NotificationClient topicHandlingClient = topicHandlingClients.get(messageNotification.getTopic());
						if (topicHandlingClient==null){
							LOG.warn("no topic handing client has been set for topic:"+messageNotification.getTopic());
						} else
							try {
								topicHandlingClient.sendMessageNotification(messageNotification);
							} catch (Exception e){
								LOG.error("problem processing messageNotification:",e);
							}
					}

				} catch (InterruptedException e) { }

			}

			LOG.debug("shutting down notification consumer thread");
		}
	}

}





