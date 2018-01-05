/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OnmsCollectionAttributeMap {
	
	private String foreignId=null;
	
	private String resourceName=null;
	
	private Date timestamp=null;
	
	private Map<String,OnmsCollectionAttribute> attributeMap = new HashMap<String, OnmsCollectionAttribute>();
	
	public Map<String,OnmsCollectionAttribute> getAttributeMap() {
		return attributeMap;
	}

	public void setAttributeMap(Map<String,OnmsCollectionAttribute> attributeMap) {
		this.attributeMap = attributeMap;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getForeignId() {
		return foreignId;
	}

	public void setForeignId(String foreignId) {
		this.foreignId = foreignId;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	@Override
	public String toString() {
		
		String ts = (timestamp==null) ? null : Long.toString(timestamp.getTime());
		return "OnmsCollectionAttributeMap [foreignId=" + foreignId
				+ ", resourceName=" + resourceName + ", timestamp=" + ts +" ("+timestamp+ ")"
				+ ", attributeMap=" + attributeMap + "]";
	}

}
