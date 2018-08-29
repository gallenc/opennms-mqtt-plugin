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

package org.opennms.plugins.mqttclient;

import java.util.Map;

import org.opennms.netmgt.model.OnmsAssetRecord;

public interface NodeByForeignSourceCache {
	
	/**
	 * returns cache or database entry for node
	 * or returns null if node not in cache or database
	 * @param nodeCriteria nodeid or foreignSource:foreignId
	 */
    public Map<String,String> getEntry(String nodeCriteria);


	/**
	 * Try to retrieve node entry for nodeCriteria from cache or database
	 * If not in cache or database creation of new node is attempted
	 * @param nodeCriteria nodeid or foreignSource:foreignId
	 * @param assetRecord optional asset record entry to create with this node. Can be null.
	 */
    Map<String,String> createOrUpdateNode(String nodeCriteria, OnmsAssetRecord assetRecord);

	/**
	 * removes the entry for a given nodeCriteria
	 * does nothing if the node entry a given nodeCriteria does not exist
	 */
	void removeEntry(String nodeCriteria);
	
	/**
	 * refresh the entry for a given nodeCriteria
	 */
	void refreshEntry(String nodeCriteria);
	
	/**
	 * removes the entry for a given nodeId
	 * does nothing if the node entry a given node id does not exist
	 */
	public void removeEntryForNodeId(String nodeId);
	
	/**
	 * refreshes the entry for a given nodeId
	 * does nothing if the node entry a given node id does not exist
	 */
	public void refreshEntryForNodeId(String nodeId);

}