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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Abstract Class JSON Collection Handler.
 * <p>All JsonCollectionHandler should extend this class.</p>
 * 
 * @author <a href="mailto:ronald.roskens@gmail.com">Ronald Roskens</a>
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class OnmsAttributeJsonHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeJsonHandler.class);

    
    public void fillAttributeMap(List<OnmsCollectionAttributeMap> attributeMapList, XmlGroups source, JSONObject json) throws ParseException {
        JXPathContext context = JXPathContext.newContext(json);
        for (XmlGroup group : source.getXmlGroups()) {
            LOG.debug("fillAttributeMap: getting resources for XML group {} using XPATH {}", group.getName(), group.getResourceXpath());
            
            @SuppressWarnings("unchecked")
			Iterator<Pointer> itr = context.iteratePointers(group.getResourceXpath());
            
            while (itr.hasNext()) {
                JXPathContext relativeContext = context.getRelativeContext(itr.next());
                
                Date timestamp = getTimeStamp(relativeContext, group);
                LOG.debug("fillAttributeMap: timestamp {}", timestamp);

                String resourceName = getResourceName(relativeContext, group);
                LOG.debug("fillAttributeMap: processing XML resource {} of type {}", resourceName, group.getResourceType());
                //final Resource collectionResource = getCollectionResource(agent, resourceName, group.getResourceType(), timestamp);
                //LOG.debug("fillCollectionSet: processing resource {}", collectionResource);
                OnmsCollectionAttributeMap onmsCollectionAttributeMap= new OnmsCollectionAttributeMap(); 
                for (XmlObject object : group.getXmlObjects()) {
                	LOG.debug("fillAttributeMap: XmlObject object.getXpath():"+ object.getXpath());
                    try {
                        Object obj = relativeContext.getValue(object.getXpath());
                        if (obj != null) {
                        	LOG.debug("fillAttributeMap: obj:"+ obj.toString());
                        	
                        	OnmsCollectionAttribute attr = new OnmsCollectionAttribute();
                        	attr.setOnmsType(object.getDataType().toString());
                        	attr.setValue(obj.toString());
                        	onmsCollectionAttributeMap.getAttributeMap().put(object.getName(), attr);

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
     *
     * @param context the JXpath context
     * @param group the group
     * @return the resource name
     */
    private String getResourceName(JXPathContext context, XmlGroup group) {
        // Processing multiple-key resource name.
        if (group.hasMultipleResourceKey()) {
            List<String> keys = new ArrayList<String>();
            for (String key : group.getXmlResourceKey().getKeyXpathList()) {
                LOG.debug("getResourceName: getting key for resource's name using {}", key);
                String keyName = (String)context.getValue(key);
                keys.add(keyName);
            }
            return StringUtils.join(keys, "_");
        }
        // If key-xpath doesn't exist or not found, a node resource will be assumed.
        if (group.getKeyXpath() == null) {
            return "node";
        }
        // Processing single-key resource name.
        LOG.debug("getResourceName: getting key for resource's name using {}", group.getKeyXpath());
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
            LOG.debug("getTimeStamp: getTimestampXpath() = null");
            return null;
        }
        String pattern = group.getTimestampFormat() == null ? "yyyy-MM-dd HH:mm:ss" : group.getTimestampFormat();
        LOG.debug("getTimeStamp: retrieving custom timestamp to be used when updating RRDs using XPATH {} and pattern {}", group.getTimestampXpath(), pattern);
        Date date = null;
        String value = (String)context.getValue(group.getTimestampXpath());
        try {
            DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
            DateTime dateTime = dtf.parseDateTime(value);
            date = dateTime.toDate();
        } catch (Exception e) {
            LOG.warn("getTimeStamp: can't convert custom timestamp {} using pattern {}", value, pattern);
        }
        return date;
    }

}