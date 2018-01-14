/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
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

package org.opennms.plugins.json;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONObject;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.json.OnmsCollectionAttribute;
import org.opennms.protocols.xml.config.XmlGroup;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlObject;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 */
public class OnmsAttributeMessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeMessageHandler.class);

	private XmlGroups source=null;
	
	private XmlRrd xmlRrd=null;

	public OnmsAttributeMessageHandler(XmlGroups source, XmlRrd xmlRrd){
		if(source==null)throw new IllegalStateException("XmlGroups source must not be null");
		this.source = source;
		this.xmlRrd=xmlRrd;
	}


//	@SuppressWarnings("unchecked")
//	public List<OnmsCollectionAttributeMap> jsonToAttributeMap(String jsonStr){
//		JSONObject jsonObject=null;
//		JSONParser parser = new JSONParser();
//		Object obj;
//		try {
//			obj = parser.parse(new StringReader(jsonStr));
//
//			if (obj instanceof JSONObject) {
//				jsonObject = (JSONObject) obj;
//				return jsonToAttributeMap(jsonObject);
//			} else if (obj instanceof JSONArray) {
//				// handle json starting with unnamed array e.g. [{ "id": "monitorID", "PM10": 1000 },{ "id": "monitorID2", "PM10": 1000 }]
//				// creating a named array object to specifically parse
//				// e.g. {array: [{ "id": "monitorID", "PM10": 1000 },{ "id": "monitorID2", "PM10": 1000 }]}
//				JSONArray array = (JSONArray) obj;
//				jsonObject = new JSONObject();
//				jsonObject.put("array", array); 
//				return jsonToAttributeMap(jsonObject);
//			} else throw new RuntimeException("unexpected type returned from jsonsimple parser:"+ obj.getClass());
//		} catch (Exception ex) {
//			throw new RuntimeException("problem parsing attributemap from json message:"+jsonStr, ex);
//		}
//	}
	
	public List<OnmsCollectionAttributeMap> payloadObjectToAttributeMap(Object payloadObject){
		if(payloadObject instanceof JSONObject){
			return jsonToAttributeMap((JSONObject) payloadObject);
		} else if(payloadObject instanceof Document){
			return xmlToAttributeMap((Document) payloadObject);
			
		} else throw new UnsupportedOperationException("not yet implimented - parsing this object" +payloadObject.getClass().getName());
	}

	public List<OnmsCollectionAttributeMap> jsonToAttributeMap(JSONObject json){
		List<OnmsCollectionAttributeMap> attributeMapList = new ArrayList<OnmsCollectionAttributeMap>();
		try {
			fillAttributeMap(attributeMapList, source, json);
		} catch (Exception ex) {
			throw new RuntimeException("problem parsing attributeMap from json message:"+json.toJSONString(), ex);
		}
		return attributeMapList;
	}
	
	public List<OnmsCollectionAttributeMap> xmlToAttributeMap(Document document){
		List<OnmsCollectionAttributeMap> attributeMapList = new ArrayList<OnmsCollectionAttributeMap>();
		try {
			if(true) throw new UnsupportedOperationException("not yet implimented - parsing xml Document"); //TODO REMOVE
			//TODO fillAttributeMap(attributeMapList, source, document);
		} catch (Exception ex) {
			throw new RuntimeException("problem parsing attributeMap from json message:"+document.toString(), ex);
		}
		return attributeMapList;
	}


	public void fillAttributeMap(List<OnmsCollectionAttributeMap> attributeMapList, XmlGroups source, JSONObject json) throws ParseException {
		JXPathContext context = JXPathContext.newContext(json);
		for (XmlGroup group : source.getXmlGroups()) {
			LOG.debug("fillAttributeMap: getting resources for XML group '{}' using XPATH '{}'", group.getName(), group.getResourceXpath());

			@SuppressWarnings("unchecked")
			Iterator<Pointer> itr = context.iteratePointers(group.getResourceXpath());

			while (itr.hasNext()) {
				JXPathContext relativeContext = context.getRelativeContext(itr.next());

				Date timestamp = getTimeStamp(relativeContext, group);
				LOG.debug("fillAttributeMap: timestamp {}", timestamp);

				String resourceName = getResourceName(relativeContext, group);
				String foreignId= getForeignId(relativeContext, group);
				LOG.debug("fillAttributeMap: processing node foreignId '{}' json/xml resourceName '{}' of type '{}'", foreignId, resourceName, group.getResourceType());
				//final Resource collectionResource = getCollectionResource(agent, resourceName, group.getResourceType(), timestamp);
				//LOG.debug("fillCollectionSet: processing resource {}", collectionResource);
				OnmsCollectionAttributeMap onmsCollectionAttributeMap= new OnmsCollectionAttributeMap();
				onmsCollectionAttributeMap.setXmlRrd(xmlRrd);
				
				onmsCollectionAttributeMap.setForeignId(foreignId);
				onmsCollectionAttributeMap.setResourceName(resourceName);
				onmsCollectionAttributeMap.setTimestamp(timestamp);
				for (XmlObject object : group.getXmlObjects()) {
					LOG.debug("fillAttributeMap: XmlObject object.getXpath():"+ object.getXpath());
					try {
						Object valueObj = relativeContext.getValue(object.getXpath());
						if (valueObj != null) {
							String name=object.getName();
							OnmsCollectionAttribute attr = new OnmsCollectionAttribute();
							String type=object.getDataType().toString();
							attr.setOnmsType(type);
							String value=valueObj.toString();
							attr.setValue(value);
							onmsCollectionAttributeMap.getAttributeMap().put(name, attr);
							LOG.debug("fillAttributeMap: "
									+ " name:"+ name
									+ " type:"+ type
									+ " value:"+ value
									);

							//builder.withAttribute(collectionResource, group.getName(), object.getName(), obj.toString(), object.getDataType());
						}
					} catch (JXPathException ex) {
						LOG.warn("fillAttributeMap Unable to get value for {}: {}", object.getXpath(), ex.getMessage());
					}
				}
				attributeMapList.add(onmsCollectionAttributeMap);
				//processXmlResource(builder, collectionResource, resourceName, group.getName());
			}
		}
	}

	/**
	 * Gets the resource name.
	 * resource name is provided by MultipleResourceKey or is set to node
	 *
	 * @param context the JXpath context
	 * @param group the group
	 * @return the resource name
	 */
	private String getResourceName(JXPathContext context, XmlGroup group) {
		// Processing multiple-key resource name.
		// If XpathList doesn't exist or not found, a node resource will be assumed.
		String resourceName="node";
		if (group.hasMultipleResourceKey()) {
			List<String> keys = new ArrayList<String>();
			for (String key : group.getXmlResourceKey().getKeyXpathList()) {
				LOG.debug("getResourceName: getting key for resource's name using {}", key);
				Object val = context.getValue(key);
				String keyName = (val==null) ? null : val.toString(); // handles json Long and json string representation of long and other values
				keys.add(keyName);
			}
			resourceName =  StringUtils.join(keys, "_");
			LOG.debug("getResourceName: resource's constructed from KeyXpathList: '{}'", resourceName);
			return resourceName;
		} else {
			LOG.debug("getResourceName: no KeyXpathList using default resourceName '{}'", resourceName);
			return resourceName;
		}
	}

	/**
	 * Node foreignId is provided by keyXpath
	 * @param context
	 * @param group
	 * @return
	 */
	private String getForeignId(JXPathContext context, XmlGroup group) {
		// Processing single-key resource name.
		LOG.debug("getForeignId: getting key for node foreignId using {}", group.getKeyXpath());
		return (String)context.getValue(group.getKeyXpath());
	}


	/**
	 * Gets the time stamp.
	 * 
	 * @param context the JXPath context
	 * @param group the group
	 * @return the time stamp
	 */
	protected Date getTimeStamp(JXPathContext context, XmlGroup group) {
		if (group.getTimestampXpath() == null) {
			// if no timestampXpath defined use current date
			Date date = new Date();
			LOG.debug("getTimeStamp: getTimestampXpath() = null. Using current Date :"+date.getTime()+" ("+date+")");
			return date ; 
		}
		String pattern = group.getTimestampFormat() == null ? "yyyy-MM-dd HH:mm:ss" : group.getTimestampFormat();

		LOG.debug("getTimeStamp: retrieving custom timestamp to be used when updating RRDs using XPATH '{}' and pattern '{}'", group.getTimestampXpath(), pattern);
		Date date = null;
		Object val = context.getValue(group.getTimestampXpath());
		String value = (val==null) ? null : val.toString(); // handles json Long and json string representation of long and other values
		
		// if pattern is empty treat as ms long value
		if("".equals(pattern)){
			try {
				long datems = Long.parseLong(value);
				date = new Date(datems);
			} catch (Exception e) {
				LOG.warn("getTimeStamp: (Empty Pattern). Can't convert custom timestamp {} as long to new Date(long)", value);
			}
		}else try {
			DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
			DateTime dateTime = dtf.parseDateTime(value);
			date = dateTime.toDate();
		} catch (Exception e) {
			LOG.warn("getTimeStamp: can't convert custom timestamp {} using pattern {}", value, pattern);
		}
		return date;
	}

}