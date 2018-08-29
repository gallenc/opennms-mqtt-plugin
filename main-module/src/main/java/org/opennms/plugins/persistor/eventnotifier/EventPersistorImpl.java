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

package org.opennms.plugins.persistor.eventnotifier;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.plugins.messagehandler.OnmsCollectionAttribute;
import org.opennms.plugins.messagehandler.OnmsCollectionAttributeMap;
import org.opennms.plugins.persistor.NodeByForeignSourceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPersistorImpl implements EventPersistor {
	private static final Logger LOG = LoggerFactory.getLogger(EventPersistorImpl.class);

	public static final String MESSAGE_GENERIC_CONTENT_EVENT = "uei.opennms.org/plugin/MqttReceiver/MessageGenericContentEvent";
	public static final String MESSAGE_DEFAULT_EVENT_SEVERITY = "Normal";

	public static final String MESSAGE_PAYLOAD_STRING_PARAM = "message-payload-string";
	public static final String MESSAGE_TOPIC_PARAM = "message-topic";
	public static final String MESSAGE_QOS_PARAM = "message-qos";

    public static final String MESSAGE_RESOURCE_NAME = "message-resource-name";

	private static final String UEI_SUFFIX_KEY = "UEI_SUFFIX"; // used to find uei

	private NodeByForeignSourceCache m_nodeByForeignSourceCache;

	private EventProxy eventProxy = null;

	public NodeByForeignSourceCache getNodeByForeignSourceCache() {
		return m_nodeByForeignSourceCache;
	}

	public void setNodeByForeignSourceCache(NodeByForeignSourceCache nodeByForeignSourceCache) {
		this.m_nodeByForeignSourceCache = nodeByForeignSourceCache;
	}

	public EventProxy getEventProxy() {
		return eventProxy;
	}

	public void setEventProxy(EventProxy eventProxy) {
		this.eventProxy = eventProxy;
	}

	/* (non-Javadoc)
	 * @see org.opennms.plugins.persistor.eventnotifier.EventPersistor#persistAttributeMapList(java.util.List)
	 */
	@Override
	public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMapList, String ueiRoot){
		LOG.debug("eventPersistor persisting attributeMap: "+attributeMapList.toString());

		for(OnmsCollectionAttributeMap onmsCollectionAttributeMap: attributeMapList){
			try {
				String qosStr=Integer.toString(onmsCollectionAttributeMap.getQos());
				String topic = onmsCollectionAttributeMap.getTopic();
				Date timestamp = onmsCollectionAttributeMap.getTimestamp();

				String foreignSource=onmsCollectionAttributeMap.getForeignSource();
				String foreignId = onmsCollectionAttributeMap.getForeignId();
				String resourceName = onmsCollectionAttributeMap.getResourceName();
				Map<String, OnmsCollectionAttribute> attributeMap = onmsCollectionAttributeMap.getAttributeMap();
				
				EventBuilder eb=null;
				if (ueiRoot==null) {
					eb = new EventBuilder(MESSAGE_GENERIC_CONTENT_EVENT, topic);
				} else {
					OnmsCollectionAttribute value = attributeMap.get(UEI_SUFFIX_KEY);
					String uei=null;
					if (value!=null) {
						uei = ueiRoot +"/"+ value.getValue();
					} else uei= ueiRoot;
					
					eb = new EventBuilder(uei, topic);
				}

				 
				eb.setTime(timestamp);
				eb.addParam(MESSAGE_TOPIC_PARAM,topic);
				eb.addParam(MESSAGE_QOS_PARAM,qosStr);
				eb.addParam(MESSAGE_RESOURCE_NAME, resourceName);

				//find node id (if exists) from foreign source and foreign id
				String lookupCriteria= foreignSource+":"+foreignId;
				Map<String, String> nodeData = m_nodeByForeignSourceCache.createOrUpdateNode(lookupCriteria, null); //TODO add location?
				String nodeIdStr=null;
				if(nodeData==null) {
					LOG.debug("cannot find node for lookupCriteria="+lookupCriteria);
				} else try {
					nodeIdStr=nodeData.get("nodeId");
					Integer nodeId = Integer.parseInt(nodeIdStr);
					eb.setNodeid(nodeId);
				} catch (Exception e){
					LOG.error("cannot parse nodeId "+nodeIdStr+
							"from node cash data for lookupCriteria="+lookupCriteria,e);
				}
				
				for(String paramKey:attributeMap.keySet()){
					OnmsCollectionAttribute value = attributeMap.get(paramKey);
					eb.addParam(paramKey,value.getValue());
				}

				sendEvent(eb.getEvent());

			} catch (Exception e){
				LOG.error("problem creating event from onmsCollectionAttributeMap:"+onmsCollectionAttributeMap, e);
			}
		}
	}

	private void sendEvent(Event e){
		LOG.debug("sending event to opennms. event.tostring():" + e.toString());
		try {
			if (eventProxy != null) {
				eventProxy.send(e);
			} else {
				LOG.error("OpenNMS event proxy not set - not sending event to opennms");
			}
		} catch (EventProxyException ex) {
			throw new RuntimeException("event proxy problem sending event to OpenNMS:",ex);
		}
	}


	public void init() {
		LOG.debug("initialising MqttEventNotificationClient");
		if (eventProxy == null)
			LOG.debug("OpenNMS event proxy not set - cannot send events to opennms");
	}


	public void destroy() {

	}



}
