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

package org.opennms.plugins.messagenotifier.eventnotifier;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sends mqtt events into opennms
 * 
 * @author admin
 *
 */
public class MqttEventNotificationClient implements NotificationClient {
	private static final Logger LOG = LoggerFactory.getLogger(MqttEventNotificationClient.class);

	public static final String MQTT_JSON_EVENT = "uei.opennms.org/plugin/MqttReceiver/jsonPayloadEvent";
	public static final String MQTT_TEXT_EVENT = "uei.opennms.org/plugin/MqttReceiver/stringPayloadEvent";
	public static final String MQTT_DEFAULT_EVENT_SEVERITY = "Normal";

	public static final String MQTT_PAYLOAD_STRING_PARAM = "mqtt-payload-string";
	public static final String MQTT_TOPIC_PARAM = "mqtt-topic";
	public static final String MQTT_QOS_PARAM = "mqtt-qos";
	

	private EventProxy eventProxy = null;
	
    private String foreignSource="mqtt";
    
    private String foreignIdKey="id";
	
	public EventProxy getEventProxy() {
		return eventProxy;
	}

	public void setEventProxy(EventProxy eventProxy) {
		this.eventProxy = eventProxy;
	}

	public void setForeignSource(String foreignSource) {
		this.foreignSource = foreignSource;
	}

	public void setForeignIdKey(String foreignIdKey) {
		this.foreignIdKey = foreignIdKey;
	}

	@Override
	public void sendMessageNotification(MessageNotification messageNotification) {
		try{
			String qosStr = Integer.toString(messageNotification.getQos());
			String topic = messageNotification.getTopic();

			// parse payload to see if json or just string
			byte[] payload = messageNotification.getPayload();
			String payloadString = new String(payload, "UTF8");

			JSONObject jsonObject=null;
			try {
				JSONParser parser = new JSONParser();
				Object obj;
				obj = parser.parse(payloadString);
				jsonObject = (JSONObject) obj;
				LOG.debug("payload JsonObject.toString():" + jsonObject.toString());
			} catch (ParseException e1) {
				LOG.debug("cannot parse notification payload to json object. payloadString="+ payloadString);
			}

			// if mqtt payload string is not parsable json just create a text event
			if(jsonObject==null){
				EventBuilder eb= new EventBuilder(MQTT_TEXT_EVENT, topic);
				eb.addParam(MQTT_TOPIC_PARAM,topic);
				eb.addParam(MQTT_QOS_PARAM,qosStr);
				eb.addParam(MQTT_PAYLOAD_STRING_PARAM,payloadString);
				sendEvent(eb.getEvent());
			} else {
				// if json then put each value in a param
				EventBuilder eb= new EventBuilder(MQTT_JSON_EVENT, topic);
				eb.setSeverity(MQTT_DEFAULT_EVENT_SEVERITY);
				eb.addParam(MQTT_TOPIC_PARAM,topic);
				eb.addParam(MQTT_QOS_PARAM,qosStr);
				eb.addParam(MQTT_PAYLOAD_STRING_PARAM,payloadString);
				
				// TODO get node from foreign source eb.setNode(node)

				//copy in all values as json in params
				for(Object key: jsonObject.keySet()){
					String paramKey = "mqtt_json_"+key.toString();
					String paramValue = jsonObject.get(key).toString();
					eb.addParam(paramKey,paramValue);
				}
				sendEvent(eb.getEvent());
			}
		} catch (Exception e){
			LOG.error("problem creating event from mqtt notification", e);
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
			throw new RuntimeException("event proxy problem sending AlarmChangeNotificationEvent to OpenNMS:",ex);
		}
	}




	@Override
	public void init() {
		LOG.debug("initialising MqttEventNotificationClient");
		if (eventProxy == null)
			LOG.debug("OpenNMS event proxy not set - cannot send events to opennms");
	}

	@Override
	public void destroy() {
	}

}
