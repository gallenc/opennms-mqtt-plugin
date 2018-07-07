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


import java.util.List;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

//TODO WEB REST SERVICE NEEDS FIXED FOR PATH PARAMETERS
/**
 * REST service to post mqtt data to opennms
 */
@Path("/postmessage")
public class MqttRxRestImpl {
	private static final Logger LOG = LoggerFactory.getLogger(MqttRxRestImpl.class);


	/**
	 * Allows ReST interface to post mqtt message directly into mqtt message queue
	 * url in form /postmessage/<QOS>/TOPIC
	 * where qos is an integer 0,1,2 etc
	 * topic is the topic path e.g. 
	 * so full path is postmessage/0/foo/bar/xxx
	 * 
	 * @param uriInfo 
	 * @param qosStr qos value as integer 0,1,2
	 * @param payloadStr json payload of topic
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{qos}/")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response mqttPostMessage(@Context UriInfo uriInfo, @PathParam("qos") String qosStr, String payloadStr) throws Exception {
		
		// decode path segments and extract topic
		List<PathSegment> segments = uriInfo.getPathSegments(true);
		
		StringBuilder topicBuilder = new StringBuilder();
		for(int i=1 ; i< segments.size(); i++){
			topicBuilder.append(segments.get(i).getPath());
		}
		String topic = topicBuilder.toString();

		LOG.debug("received POST mqtt message: /postmessage/-QOS-/-topic...- : /postmessage/"+qosStr+"/"+topic
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
			LOG.error("Problem receiving message: POST mqtt message: /postmessage/-QOS-/-topic...- : /postmessage/"+qosStr+"/"+topic
				+ "  json payload "+payloadStr,ex);
			return Response.status(500).entity("Problem receiving message: POST mqtt message: /postmessage/-QOS-/-topic...- : /postmessage/"+qosStr+"/"+topic
				+ "  json payload "+payloadStr+"  Exception:"+ex).build();  
		}
		return Response.status(200).build();  

	} 

	
}