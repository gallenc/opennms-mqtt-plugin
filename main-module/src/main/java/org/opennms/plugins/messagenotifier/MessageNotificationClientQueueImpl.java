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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * This is a notification client which receives the notification and adds it to a m_queue for processing
 * @author admin
 *
 */
public class MessageNotificationClientQueueImpl implements MessageNotificationClient{

	private static final Logger LOG = LoggerFactory.getLogger(MessageNotificationClientQueueImpl.class);

	//private MessageNotifier m_messageNotifier;

	private Integer m_maxMessageQueueLength=1000;

	private Integer m_maxMessageQueueThreads=1;

	private ExecutorService executorService=null;

	private LinkedBlockingQueue<MessageNotification> m_queue=null;

	private AtomicBoolean m_clientRunning = new AtomicBoolean(false);

	private List<NotificationClient> m_notificationHandlingClients = new ArrayList<NotificationClient>();

	private List<MessageNotifier> m_messageNotifiers = null;

	@Override
	public List<MessageNotifier> getIncommingMessageNotifiers() {
		return m_messageNotifiers;
	}

	@Override
	public void setIncommingMessageNotifiers(List<MessageNotifier> messageNotifiers) {
		this.m_messageNotifiers = messageNotifiers;
	}

	public void setOutgoingNotificationHandlingClients(
			List<NotificationClient> notificationHandlingClients) {
		this.m_notificationHandlingClients = notificationHandlingClients;
	}


	public Integer getMaxMessageQueueLength() {
		return m_maxMessageQueueLength;
	}

	public void setMaxMessageQueueLength(Integer maxMessageQueueLength) {
		this.m_maxMessageQueueLength = maxMessageQueueLength;
	}

	public Integer getMaxMessageQueueThreads() {
		return m_maxMessageQueueThreads;
	}

	public void setMaxMessageQueueThreads(Integer maxMessageQueueThreads) {
		this.m_maxMessageQueueThreads = maxMessageQueueThreads;
	}

	public void init(){
		LOG.debug("initialising messageNotificationClientQueue with maxMessageQueueThreads:"+m_maxMessageQueueThreads
				+ " and maxMessageQueueLength "+m_maxMessageQueueLength);

		if (m_messageNotifiers==null) throw new IllegalStateException("m_messageNotifiers list cannot be null");
		if (m_maxMessageQueueThreads==null) throw new IllegalStateException("maxMessageQueueThreads list cannot be null");

		m_queue= new LinkedBlockingQueue<MessageNotification>(m_maxMessageQueueLength);

		executorService = Executors.newFixedThreadPool(m_maxMessageQueueThreads);

		// start consuming threads
		m_clientRunning.set(true);

		for(int i=0; i<m_maxMessageQueueThreads; i++){
			executorService.execute(new RemovingConsumer());
		}

		// start listening for notifications
		for(MessageNotifier messageNotifier: m_messageNotifiers){
			messageNotifier.addMessageNotificationClient(this);
		}


	}

	public void destroy(){
		LOG.debug("shutting down blockingQueue");

		// stop listening for notifications
		for(MessageNotifier messageNotifier: m_messageNotifiers){
			messageNotifier.removeMessageNotificationClient(this);
		}

		// signal consuming threads to stop
		m_clientRunning.set(false);
		if(executorService!=null) synchronized(this) {
			if(executorService!=null){
				executorService.shutdown();
				try {
					executorService.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					LOG.warn("executor service still has threads running at termination");
				}
			}
		}
	}

	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		if(LOG.isDebugEnabled()) LOG.debug("client received notification - adding notification to m_queue");

		if (! m_queue.offer(messageNotification)){
			LOG.warn("Cannot m_queue any more messageNotifications. messageNotification m_queue full. size="+m_queue.size());
		};

	}


	/*
	 * Class run in separate thread to remove and process notifications from the m_queue 
	 */
	private class RemovingConsumer implements Runnable {

		@Override
		public void run() {

			if(LOG.isDebugEnabled()) LOG.debug("starting notification RemovingConsumer in thread");
			// we remove elements from the m_queue until interrupted and m_clientRunning==false.
			while (m_clientRunning.get()) {
				try {
					MessageNotification messageNotification = m_queue.take();

					if(LOG.isDebugEnabled()) LOG.debug("Notification received from m_queue by consumer thread :\n topic:"+messageNotification.getTopic()
							+ "\n qos:"+messageNotification.getQos()
							+ "\n payload:"+new String(messageNotification.getPayload()));

					// we look in list for notification handling clients to handle this received notification
					if(m_notificationHandlingClients.isEmpty()) { 
						LOG.warn("no topic handing clients have been set to receive notification");
					} else {
						for(NotificationClient notificationClient:m_notificationHandlingClients){

							try {
								notificationClient.sendMessageNotification(messageNotification);
							} catch (Exception e){
								LOG.error("problem processing messageNotification:",e);
							}
						}
					}

				} catch (InterruptedException e) { }

			}

			LOG.debug("shutting down notification RemovingConsumer in thread");
		}
	}


}





