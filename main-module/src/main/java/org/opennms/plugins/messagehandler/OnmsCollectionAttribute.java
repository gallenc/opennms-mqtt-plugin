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

package org.opennms.plugins.messagehandler;

import org.opennms.netmgt.collection.api.AttributeType;

public class OnmsCollectionAttribute {
	private String value;
	private AttributeType onmsType;

	public AttributeType getOnmsType() {
		return onmsType;
	}
	
	public void setOnmsType(AttributeType onmsType) {
		this.onmsType = onmsType;
	}
	
	public void setOnmsType(String typeAsString) {
		this.onmsType = AttributeType.parse(typeAsString);
		if(onmsType==null) throw new IllegalArgumentException(typeAsString +" cannot be parsed to AttributeType.");
		
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "OnmsCollectionAttribute [value=" + value + ", onmsType="
				+ onmsType + "]";
	}



}
