/*
 * Copyright 2014 OpenNMS Group Inc., Entimoss ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opennms.plugins.messagenotifier.rest;


import org.opennms.plugins.messagenotifier.MessageNotification;
import org.opennms.plugins.messagenotifier.rest.MqttRxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * REST service to post mqtt data to opennms
 */
@Path("/receive")
public class MqttRxRestImpl {
	private static final Logger LOG = LoggerFactory.getLogger(MqttRxRestImpl.class);

	/**
	 * Allows ReST interface to post mqtt message directly into mqtt message queue
	 */
	@POST
	@Path("/{topic}/{qos}/")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response mqttPostMessage(@PathParam("topic") String topic,@PathParam("qos") String qosStr, String payloadStr) throws Exception {
		LOG.debug("received POST mqtt message: /receive/-topic-/-qos-/ : /receive/"+topic+ "/"+qosStr
				+ "  json payload "+payloadStr);

		MqttRxService mqttRxService = ServiceLoader.getMqttRxService();
		if (mqttRxService == null) throw new RuntimeException("ServiceLoader.getMqttRxService() cannot be null.");

		try{
			if (topic == null || "".equals(topic)) throw new RuntimeException("topic cannot be null or empty.");
			if (qosStr == null || "".equals(qosStr)) throw new RuntimeException("qos cannot be null or empty.");
			int qos = Integer.parseInt(qosStr);

			if (payloadStr == null) throw new RuntimeException("payloadStr cannot be null.");
			byte[] payload = payloadStr.getBytes("UTF-8");
			MessageNotification messageNotification = new MessageNotification(topic, qos, payload);
			mqttRxService.messageArrived(messageNotification);
		} catch(Exception ex){
			LOG.error("Problem receiving message: POST mqtt message: /receive/-topic-/-qos-/ : /receive/"+topic+ "/"+qosStr
					+ "  json payload "+payloadStr,ex);
			return Response.status(500).entity("Problem receiving message: POST mqtt message /receive/-topic-/-qos-/ : /receive/"+topic+ "/"+qosStr
					+ "  json payload "+payloadStr+"  Exception:"+ex).build();  
		}
		return Response.status(200).build();  

	} 

	
}