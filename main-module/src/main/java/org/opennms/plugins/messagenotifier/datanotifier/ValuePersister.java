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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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
import org.opennms.netmgt.model.OnmsAssetRecord;
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

	private ServiceParameters m_params;

	private RrdRepository m_repository;

	private PersisterFactory m_persisterFactory;

	private Persister m_persister;

	// works with 2017-10-19 10:15:02.854888";
	private String m_dateTimeFormatPattern="yyyy-MM-dd HH:mm:ss.SSSSSS";

	// default 5 minutes
	private List<String> m_rras = Arrays.asList("RRA:AVERAGE:0.5:1:2016",
			"RRA:AVERAGE:0.5:12:1488",
			"RRA:AVERAGE:0.5:288:366",
			"RRA:MAX:0.5:288:366",
			"RRA:MIN:0.5:288:366");

	private int m_intervalInSeconds = 300;

	private String m_foreignSource;

	private ConfigDao m_configDao;

	private NodeByForeignSourceCache m_nodeByForeignSourceCache;

	// default to local time offset
	private ZoneOffset m_zoneOffset = OffsetDateTime.now().getOffset(); 

	public void setTimeZoneOffset(String timeZoneOffset) {
		ZoneOffset zo = OffsetDateTime.now().getOffset();
		if(timeZoneOffset==null || "".equals(timeZoneOffset.trim())){
			LOG.info("mqtt zone offset not supplied. Using local default zone offset "+zo);
		} else {
			try {
				zo = ZoneOffset.of(timeZoneOffset);
			} catch (Exception e){
				LOG.warn("Cannot parse supplied timeZoneOffset="+timeZoneOffset+" Using local default zone offset "+zo, e);
			}
		}
		m_zoneOffset=zo;
	}

	public void setConfigDao(ConfigDao configDao) {
		this.m_configDao = configDao;
	}

	public void setPersisterFactory(PersisterFactory persisterFactory) {
		this.m_persisterFactory = persisterFactory;
	}

	public void setNodeByForeignSourceCache(NodeByForeignSourceCache nodeByForeignSourceCache) {
		this.m_nodeByForeignSourceCache = nodeByForeignSourceCache;
	}


	// init method to be called by blueprint after all parameters set
	public void init(){
		LOG.info("initialising value persitor with m_configDao values:"+m_configDao.toString());

		m_rras = m_configDao.getRras();
		m_intervalInSeconds=m_configDao.getIntervalInSeconds();
		m_foreignSource=m_configDao.getForeignSource();
		m_dateTimeFormatPattern=m_configDao.getDateTimeFormatPattern();
		setTimeZoneOffset(m_configDao.getTimeZoneOffset());

		// Setup auxiliary objects needed by the m_persister
		m_params = new ServiceParameters(Collections.emptyMap());
		m_repository = new RrdRepository();
		m_repository.setRraList(m_rras);
		m_repository.setStep(Math.max(m_intervalInSeconds, 1));
		m_repository.setHeartBeat(m_repository.getStep() * 2);
		m_repository.setRrdBaseDir(Paths.get(System.getProperty("opennms.home"),"share","rrd","snmp").toFile());

		m_persister = m_persisterFactory.createPersister(m_params, m_repository);	
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

		String foreignIdKey = m_configDao.getForeignIdKey();
		String timeStampKey = m_configDao.getTimeStampKey();
		String group = m_configDao.getGroup();
		Map<String,AttributeType> dataDefinition = m_configDao.getDataDefinition();

		Date timeStamp;
		String foreignId;

		// try to parse timestamp from json
		// or create a timestamp from current date if timestamp key not set
		String timeStampValue="not set";
		if(timeStampKey==null || "".equals(timeStampKey.trim())){
			timeStamp = new Date();
		} else {
			if (!attributeMap.containsKey(timeStampKey)) {
				throw new RuntimeException("no time stamp value for timeStampKey:"+timeStampKey+" in received attribute map:"+objectMapToString(attributeMap));
			} else try {
				timeStampValue = attributeMap.get(timeStampKey).toString();
				timeStamp = parseJsonTimestampToDate(timeStampValue);
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
		String lookupCriteria= m_foreignSource+":"+foreignId;
		
		OnmsAssetRecord assetRecord=null; //TODO ADD ASSET RECORD 
		Map<String, String> nodeData = m_nodeByForeignSourceCache.createOrUpdateNode(lookupCriteria, assetRecord);

		if(nodeData==null){
			LOG.warn("no node exists for m_foreignSource="+m_foreignSource+" foreignId:"+foreignId+" in received attribute map:"+objectMapToString(attributeMap));
		} else {
			Integer nodeId;
			try {
				nodeId = Integer.parseInt(nodeData.get("nodeId"));
			} catch (Exception e){
				throw new RuntimeException("Cannot parse nodeId from node cash data for m_foreignSource="
						+m_foreignSource+" foreignId:"+foreignId+" in received attribute map:"+objectMapToString(attributeMap),e);
			}

			NodeLevelResource nodelevelResource = new NodeLevelResource(nodeId);

			// Build the interface resource
			InterfaceLevelResource interfaceLevelResource = new InterfaceLevelResource(nodelevelResource, "mqtt");

			// Generate the collection set
			CollectionAgent agent = new MockCollectionAgent(m_foreignSource, foreignId, nodeId);
			CollectionSetBuilder builder = new CollectionSetBuilder(agent);
			builder.withTimestamp(timeStamp);

			// try and correctly parse the remaining attributes in json message
			for(Object key: attributeMap.keySet()){
				String attributeName = key.toString();
				String attributeValue = attributeMap.get(key).toString();
				if(dataDefinition.containsKey(attributeName)){
					AttributeType attributeType = dataDefinition.get(attributeName);
					if(LOG.isDebugEnabled()) LOG.debug("ValuePersistor at json timeStampValue:"+timeStampValue
							+" timeStamp:"+timeStamp.getTime()+"  "+parseDatetoJsonTimestamp(timeStamp)
							+" adding attribute:"+attributeName
							+" attributeValue:"+attributeValue
							+" attributeType:"+attributeType.toString()
							+" group:"+group
							+" interfaceLevelResource:"+interfaceLevelResource.toString());
					builder.withAttribute(interfaceLevelResource , group, attributeName, attributeValue, attributeType);
				} else {
					LOG.warn("no data definition for parameter:"+attributeName+" in received attribute map:"+objectMapToString(attributeMap));
				}
			}

			CollectionSet collectionSet =  builder.build();

			// Persist
			collectionSet.visit(m_persister);
		}
	}

	public Date parseJsonTimestampToDate(String dateStr){
		Date date;

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_dateTimeFormatPattern);
			LocalDateTime localDateTime= LocalDateTime.parse(dateStr, formatter);
			Instant instant = localDateTime.toInstant(m_zoneOffset);
			date = Date.from(instant);
		} catch (Exception e) {
			LOG.warn("using current date because cannot parse supplied json date string :"+dateStr
					+" with m_zoneOffset"+m_zoneOffset
					+ " and supplied m_dateTimeFormatPattern: "+m_dateTimeFormatPattern, e);
			date = new Date();
		} 

		return date;
	}

	public String parseDatetoJsonTimestamp(Date date){
		String dateStr;
		
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_dateTimeFormatPattern);
			Instant instantFromDate = date.toInstant();
			LocalDateTime endLocalDateTime = LocalDateTime.ofInstant(instantFromDate,m_zoneOffset);
			dateStr = endLocalDateTime.format(formatter);
		} catch (Exception e) {
			throw new RuntimeException("cannot format supplied date :"+date
					+" with supplied m_dateTimeFormatPattern: "+m_dateTimeFormatPattern, e);
		}
		return dateStr;
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


		// use for store by m_foreignSource
		public MockCollectionAgent(String foreignSource, String foreignId, int nodeId) {
			this.nodeId = nodeId; //0 ?
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
				dir = ResourcePath.get(ResourceTypeUtils.FOREIGN_SOURCE_DIRECTORY,foreignSource,foreignId);
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
