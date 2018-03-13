package org.opennms.plugins.messagenotifier.datanotifier;

import java.net.InetAddress;
import java.nio.file.Paths;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
import org.opennms.netmgt.model.OnmsGeolocation;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.model.ResourceTypeUtils;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.plugins.json.OnmsCollectionAttribute;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCache;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPersistorImpl implements DataPersistor {
	private static final Logger LOG = LoggerFactory.getLogger(DataPersistorImpl.class);

	private static final String LATITUDE_KEY = null;

	private static final Object LONGITUDE_KEY = null;

	private PersisterFactory m_persisterFactory=null;
	private NodeByForeignSourceCache m_nodeByForeignSourceCache=null;

	public void setPersisterFactory(PersisterFactory persisterFactory) {
		this.m_persisterFactory = persisterFactory;
	}

	public void setNodeByForeignSourceCache(NodeByForeignSourceCache nodeByForeignSourceCache) {
		this.m_nodeByForeignSourceCache = nodeByForeignSourceCache;
	}

	/* (non-Javadoc)
	 * @see org.opennms.plugins.messagenotifier.datanotifier.DataPersistor#persistAttributeMapList(java.util.List)
	 */
	@Override
	public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMapList){
		if(LOG.isDebugEnabled()) LOG.debug("dataPersistor persisting attributeMap: "+attributeMapList.toString());
		for(OnmsCollectionAttributeMap onmsCollectionAttributeMap: attributeMapList){
			String qosStr=null;
			String topic=null;
			Date timestamp=null;
			try {

				// get data from onmsCollectionAttributeMap
				qosStr=Integer.toString(onmsCollectionAttributeMap.getQos());
				topic = onmsCollectionAttributeMap.getTopic();
				timestamp = onmsCollectionAttributeMap.getTimestamp();

				String foreignSource=onmsCollectionAttributeMap.getForeignSource();
				String foreignId = onmsCollectionAttributeMap.getForeignId();
				String resourceName = onmsCollectionAttributeMap.getResourceName();
				Map<String, OnmsCollectionAttribute> attributeMap = onmsCollectionAttributeMap.getAttributeMap();
				XmlRrd xmlRrd = onmsCollectionAttributeMap.getXmlRrd();
				List<String> rras = xmlRrd.getXmlRras();
				Integer step = xmlRrd.getStep();

				// Setup persister and auxiliary objects needed by the m_persister
				ServiceParameters m_params = new ServiceParameters(Collections.emptyMap());
				RrdRepository m_repository = new RrdRepository();
				m_repository.setRraList(rras);
				m_repository.setStep(Math.max(step, 1));
				m_repository.setHeartBeat(m_repository.getStep() * 2);
				m_repository.setRrdBaseDir(Paths.get(System.getProperty("opennms.home"),"share","rrd","snmp").toFile());
				Persister m_persister = m_persisterFactory.createPersister(m_params, m_repository);	

				//find node id (if exists) from foreign source and foreign id
				String lookupCriteria= foreignSource+":"+foreignId;

				OnmsAssetRecord assetRecord=null;
				String latStr=null;
				String lonStr=null;

				if(attributeMap.containsKey(LATITUDE_KEY)) {
					try {

						latStr = attributeMap.get(LATITUDE_KEY).getValue();
						lonStr = attributeMap.get(LONGITUDE_KEY).getValue();

						Double latitude = Double.valueOf(latStr);
						Double longitude = Double.valueOf(lonStr);

						OnmsGeolocation geolocation = new OnmsGeolocation();
						geolocation.setLatitude(latitude); 
						geolocation.setLongitude(longitude);
						assetRecord= new OnmsAssetRecord();
						assetRecord.setGeolocation(geolocation);
					} catch (Exception e){
						LOG.warn("Could not parse geolocation latitudeKey:"+LATITUDE_KEY
								+ " latValue:"+latStr
								+ " LONGITUDE_KEY:"+LONGITUDE_KEY
								+ " lonValue:"+lonStr,e);
					}
				}

				Map<String, String> nodeData = m_nodeByForeignSourceCache.createOrUpdateNode(lookupCriteria, assetRecord);

				if(nodeData==null){
					LOG.warn("no node exists for foreignSource="+foreignSource+" foreignId:"+foreignId+" in received attribute map:"+attributeMapToString(attributeMap));
				} else {
					Integer nodeId;
					try {
						nodeId = Integer.parseInt(nodeData.get("nodeId"));
					} catch (Exception e){
						throw new RuntimeException("Cannot parse nodeId from node cash data for m_foreignSource="
								+foreignSource+" foreignId:"+foreignId+" in received attribute map:"+attributeMapToString(attributeMap),e);
					}

					NodeLevelResource nodelevelResource = new NodeLevelResource(nodeId);

					// Build the interface resource
					//TODO INTERFACE LEVEL RESOURCE
					InterfaceLevelResource interfaceLevelResource = new InterfaceLevelResource(nodelevelResource, "mqtt");

					// Generate the collection set
					CollectionAgent agent = new MockCollectionAgent(foreignSource, foreignId, nodeId);
					CollectionSetBuilder builder = new CollectionSetBuilder(agent);
					builder.withTimestamp(timestamp);

					// try and correctly parse the remaining attributes in json message
					for(String attributeName: attributeMap.keySet()){
						OnmsCollectionAttribute onmsCollectionAttribute = attributeMap.get(attributeName);
						String attributeValue = onmsCollectionAttribute.getValue();
						AttributeType attributeType=onmsCollectionAttribute.getOnmsType();

						String group="sniffy"; //TODO CHANGE TO DYNAMIC


						if(LOG.isDebugEnabled()) LOG.debug("DataPersistor "
								+" timeStamp:"+timestamp.getTime()+"  ("+timestamp.toString()
								+") adding attribute:"+attributeName
								+" attributeValue:"+attributeValue
								+" attributeType:"+attributeType.toString()
								+" group:"+group
								+" interfaceLevelResource:"+interfaceLevelResource.toString());

						builder.withAttribute(interfaceLevelResource , group, attributeName, attributeValue, attributeType);

					}


					CollectionSet collectionSet =  builder.build();

					// Persist
					collectionSet.visit(m_persister);

				}
			} catch (Exception e){
				LOG.error("problem persisting data from topic:"+topic+" qos:"+qosStr+" timestamp:"+timestamp.getTime()+" ("+timestamp.toString()
						+ ") "+ " from onmsCollectionAttributeMap:"+onmsCollectionAttributeMap, e);
			}
		}
	}

	// init method to be called by blueprint after all parameters set
	public void init(){
		if(this.m_persisterFactory==null) throw new IllegalStateException("m_persisterFactory should be set by blueprint");
		if(this.m_nodeByForeignSourceCache==null) throw new IllegalStateException("m_nodeByForeignSourceCache should be set by blueprint");
	}

	// destroy method to be called by blueprint
	public void destroy(){

	}

	private String attributeMapToString(Map<String,OnmsCollectionAttribute> attributeMap){
		StringBuffer msg = new StringBuffer("attributeMap[");
		for(String key:attributeMap.keySet()){
			msg.append("'"+key+":"+attributeMap.get(key)+"'  ");
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
