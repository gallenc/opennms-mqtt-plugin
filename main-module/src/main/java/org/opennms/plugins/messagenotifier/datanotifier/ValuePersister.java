/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.plugins.messagenotifier.datanotifier;

import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opennms.netmgt.collection.api.AttributeType;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.Persister;
import org.opennms.netmgt.collection.api.PersisterFactory;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.collection.support.builder.CollectionSetBuilder;
import org.opennms.netmgt.collection.support.builder.InterfaceLevelResource;
import org.opennms.netmgt.collection.support.builder.NodeLevelResource;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.model.ResourceTypeUtils;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to persist values to OpenNMS persistence layer - rrd or newts
 *
 * @author cgallen
 */
public class ValuePersister  {
	private static final Logger LOG = LoggerFactory.getLogger(ValuePersister.class);

	private ServiceParameters params;

	private RrdRepository repository;

	private PersisterFactory persisterFactory;

	private Persister persister;

	private List<String> rras;

	private int intervalInSeconds;

	private String foreignSource;

	private ConfigDao configDao;

	private NodeByForeignSourceCache nodeByForeignSourceCache;

	public void setConfigDao(ConfigDao configDao) {
		this.configDao = configDao;
	}

	public void setPersisterFactory(PersisterFactory persisterFactory) {
		this.persisterFactory = persisterFactory;
	}

	public void setNodeByForeignSourceCache(NodeByForeignSourceCache nodeByForeignSourceCache) {
		this.nodeByForeignSourceCache = nodeByForeignSourceCache;
	}


	// init method to be called by blueprint after all parameters set
	public void init(){
		LOG.info("initialising value persitor with configDao values:"+configDao.toString());

		rras = configDao.getRras();
		intervalInSeconds=configDao.getIntervalInSeconds();
		foreignSource=configDao.getForeignSource();

		// Setup auxiliary objects needed by the persister
		params = new ServiceParameters(Collections.emptyMap());
		repository = new RrdRepository();
		repository.setRraList(rras);
		repository.setStep(Math.max(intervalInSeconds, 1));
		repository.setHeartBeat(repository.getStep() * 2);
		repository.setRrdBaseDir(Paths.get(System.getProperty("opennms.home"),"share","rrd","snmp").toFile());

		persister = persisterFactory.createPersister(params, repository);
	}

	// destroy method to be called by blueprint
	public void destroy(){

	}


	@SuppressWarnings("unchecked")
	public void persistJsonAttributes(String jsonStr){
		JSONObject jsonObject=null;
		jsonObject=null;
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonStr);
			jsonObject = (JSONObject) obj;
			LOG.debug("received payload JsonObject.toString():" + jsonObject.toString());
		} catch (ParseException e1) {
			throw new RuntimeException("cannot parse notification payload to json object. payloadString="+ jsonStr,e1);
		}
		persistAttributeMap(jsonObject);
	}


	public void persistAttributeMap(Map<String,Object> attributeMap){

		String foreignIdKey = configDao.getForeignIdKey();
		String timeStampKey = configDao.getTimeStampKey();
		String group = configDao.getGroup();
		Map<String,AttributeType> dataDefinition = configDao.getDataDefinition();

		Date timeStamp;
		String foreignId;

		// try to parse timestamp from json
		// or create a timestamp if timestamp key not set
		if(timeStampKey==null || "".equals(timeStampKey.trim())){
			timeStamp = new Date();
		} else {
			if (!attributeMap.containsKey(timeStampKey)) {
				throw new RuntimeException("no time stamp value for timeStampKey:"+timeStampKey+" in received attribute map:"+objectMapToString(attributeMap));
			} else try {
				String timeStampValue = attributeMap.get(timeStampKey).toString();
				timeStamp = parseJsonTimestamp(timeStampValue);
			} catch (Exception e){
				throw new RuntimeException("cannot parse timeStampValue for timeStampKey:"+timeStampKey+" in received attribute map:"+objectMapToString(attributeMap),e);
			}
			// remove timestamp if present as not needed for Attributes
			attributeMap.remove(timeStampKey);
		}

		// find foreignId from json message
		if(!attributeMap.containsKey(foreignIdKey)){
			throw new RuntimeException("no foreignId value for foreignIdKey:"+foreignIdKey+" in received attribute map:"+objectMapToString(attributeMap));
		}
		foreignId = attributeMap.get(foreignIdKey).toString();
		// remove foreignId if present as not needed for Attributes
		attributeMap.remove(foreignIdKey);

		//find node id (if exists) from foreign source and foreign id
		String lookupCriteria= foreignSource+":"+foreignId;
		Map<String, String> nodeData = nodeByForeignSourceCache.createOrUpdateNode(lookupCriteria);

		if(nodeData==null){
			LOG.warn("no node exists for foreignSource="+foreignSource+" foreignId:"+foreignId+" in received attribute map:"+objectMapToString(attributeMap));
		} else {
			Integer nodeId;
			try {
				nodeId = Integer.parseInt(nodeData.get("nodeId"));
			} catch (Exception e){
				throw new RuntimeException("cannot parse nodeId from node cash data for foreignSource="
						+foreignSource+" foreignId:"+foreignId+" in received attribute map:"+objectMapToString(attributeMap),e);
			}

			CollectionAgent agent = new MockCollectionAgent(foreignSource, foreignId, nodeId);
			NodeLevelResource nodelevelResource = new NodeLevelResource(nodeId);
			
			// Build the interface resource
			InterfaceLevelResource interfaceLevelResource = new InterfaceLevelResource(nodelevelResource, "mqtt");
			
			// Generate the collection set
			CollectionSetBuilder builder = new CollectionSetBuilder(agent);
			builder.withTimestamp(timeStamp);

			// try and correctly parse the remaining attributes in json message
			for(Object key: attributeMap.keySet()){
				String attributeName = key.toString();
				String attributeValue = attributeMap.get(key).toString();
				if(dataDefinition.containsKey(attributeName)){
					AttributeType attributeType = dataDefinition.get(attributeName);
					builder.withAttribute(interfaceLevelResource , group, attributeName, attributeValue, attributeType);
				} else {
					LOG.warn("no data definition for parameter:"+attributeName+" in received attribute map:"+objectMapToString(attributeMap));
				}
			}

			CollectionSet collectionSet =  builder.build();

			// Persist
			collectionSet.visit(persister);
		}
	}

	public Date parseJsonTimestamp(String dateStr){
		//TODO
		return new Date();
	}

	private String objectMapToString(Map<String,Object> jsonObject){
		StringBuffer msg = new StringBuffer("[");
		for(String key:jsonObject.keySet()){
			msg.append("'"+key+":"+jsonObject.get(key)+"'  ");
		}
		msg.append("]");
		return msg.toString();
	}

	private static class MockCollectionAgent implements CollectionAgent {

		private final int nodeId;

		private final String foreignId;

		private final String nodeLabel;

		private final String foreignSource;


		// use for store by foreignSource
		public MockCollectionAgent(String foreignSource, String foreignId, int nodeId) {
			this.nodeId = 0;
			this.foreignId =foreignId;
			this.nodeLabel=foreignId;
			this.foreignSource=foreignSource;
		}

		@Override
		public int getType() {
			return 0;
		}

		@Override
		public InetAddress getAddress() {
			return null;
		}

		@Override
		public Set<String> getAttributeNames() {
			return Collections.emptySet();
		}

		@Override
		public <V> V getAttribute(String property) {
			return null;
		}

		@Override
		public Object setAttribute(String property, Object value) {
			return null;
		}

		@Override
		public Boolean isStoreByForeignSource() {
			return ResourceTypeUtils.isStoreByForeignSource();
		}

		@Override
		public String getHostAddress() {
			return null;
		}

		@Override
		public void setSavedIfCount(int ifCount) {
			// pass
		}

		@Override
		public int getNodeId() {
			return this.nodeId;
		}

		@Override
		public String getNodeLabel() {
			return this.nodeLabel;
		}

		@Override
		public String getForeignSource() {
			return this.foreignSource;
		}

		@Override
		public String getForeignId() {
			return this.foreignId;
		}

		@Override
		public String getLocationName() {
			return null;
		}

		@Override
		public ResourcePath getStorageResourcePath() {
			// Copied from org.opennms.netmgt.collectd.org.opennms.netmgt.collectd#getStorageDir
			final String foreignSource = getForeignSource();
			final String foreignId = getForeignId();

			final ResourcePath dir;
			if(isStoreByForeignSource() && foreignSource != null && foreignId != null) {
				dir = ResourcePath.get(ResourceTypeUtils.FOREIGN_SOURCE_DIRECTORY,
						foreignSource,
						foreignId);
			} else {
				dir = ResourcePath.get(String.valueOf(getNodeId()));
			}

			return dir;
		}

		@Override
		public String getSysObjectId() {
			return null;
		}

		@Override
		public long getSavedSysUpTime() {
			return 0;
		}

		@Override
		public void setSavedSysUpTime(long sysUpTime) {
			// pass
		}
	}


}
