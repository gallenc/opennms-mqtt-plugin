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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.dao.api.AssetRecordDao;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.dao.api.MonitoringLocationDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventIpcManager;
import org.opennms.netmgt.events.api.EventListener;
import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsGeolocation;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNode.NodeType;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionOperations;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created: User: cgallen
 */
public class NodeByForeignSourceCacheImpl implements NodeByForeignSourceCache, EventListener {
	private static final Logger LOG = LoggerFactory.getLogger(NodeByForeignSourceCacheImpl.class);

	private static final String MQTT_UNKNOWN_NODE_EVENT = "uei.opennms.org/plugin/MqttReceiver/unknownNodeEvent";

	private long MAX_SIZE = 10000;
	private long MAX_TTL = 0; // Minutes

	private volatile NodeDao m_nodeDao;
	private volatile MonitoringLocationDao m_monitoringLocationDao;
	private volatile IpInterfaceDao m_ipInterfaceDao;
	private volatile AssetRecordDao m_assetRecordDao;
	private volatile TransactionOperations m_transactionOperations;
	private volatile EventIpcManager m_eventIpcManager;

	private volatile EventProxy m_eventProxy;

	private boolean m_createMissingNodes = true;
	private boolean m_createDummyInterfaces = true;
	private boolean m_createNodeAssetData = true;

	// contains list of nodes pending persisting
	private ConcurrentHashMap<String, String> m_persistMap = new ConcurrentHashMap<String, String>();

	private LoadingCache<String, Map<String, String>> m_nodeDataCache = null;

	public NodeByForeignSourceCacheImpl() {
	}

	class NodeNotInDatabaseException extends Exception {
		private static final long serialVersionUID = 1L;	
	}

	public void init() {
		if (m_nodeDataCache == null) {
			LOG.warn("initializing m_nodeDataCache (" + ", TTL="+ MAX_TTL + "m, MAX_SIZE=" + MAX_SIZE + ")");

			CacheBuilder cacheBuilder = CacheBuilder.newBuilder();

			if (MAX_TTL > 0) {
				cacheBuilder.expireAfterWrite(MAX_TTL, TimeUnit.MINUTES);
			}
			if (MAX_SIZE > 0) {
				cacheBuilder.maximumSize(MAX_SIZE);
			}

			CacheLoader<String, Map<String, String>> loader = new CacheLoader<String, Map<String, String>>() {
				@Override
				public Map<String, String> load(String key)	throws Exception {
					Map<String, String> result = getNodeAndCategoryInfo(key);

					// throws exception if value not in database
					// google cache cannot handle null values 
					// and we do not want to cache negative results
					if(result==null || result.isEmpty()) {
						LOG.debug("CacheLoader cannot find node in database for key="+key);
						throw new NodeNotInDatabaseException();
					}
					return result;
				}
			};

			m_nodeDataCache = cacheBuilder.build(loader);
		}

		// register for node events
		if(m_eventIpcManager== null) throw new RuntimeException("m_eventIpcManager cannot be null"); 
		m_eventIpcManager.addEventListener(this);

	}

	public void destroy(){
		if (m_eventIpcManager!=null) m_eventIpcManager.removeEventListener(this);

	}

	private Map<String, String> getNodeAndCategoryInfo(String nodeCriteria) {
		// always returns a linked hashmap but will be empty if the node not found
		final Map<String, String> result = new LinkedHashMap<>();

		// safety check
		if (nodeCriteria != null) {
			LOG.debug("Fetching node data from database into m_nodeDataCache");

			// wrap in a transaction so that Hibernate session is bound and
			// getCategories works
			m_transactionOperations.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					OnmsNode node = m_nodeDao.get(nodeCriteria);
					if (node != null) {
						// the node is created so no longer need to block concurrent creations
						m_persistMap.remove(nodeCriteria);
						populateBodyWithNodeInfo(result, node);
					}
				}
			});

		}
		if(LOG.isDebugEnabled()){
			StringBuffer sb= new StringBuffer("Node Data fetched from database:");
			for(String key : result.keySet()){
				sb.append("\n   "+key+" : "+result.get(key));
			}
			LOG.debug(sb.toString());
		}

		return result;
	}

	/**
	 * utility method to populate a Map with the most import node attributes
	 *
	 * @param body
	 *            the map
	 * @param node
	 *            the node object
	 */
	private static void populateBodyWithNodeInfo(Map<String, String> body,OnmsNode node) {
		body.put("nodeId", node.getNodeId());
		body.put("nodelabel", node.getLabel());
		body.put("nodesysname", node.getSysName());
		body.put("nodesyslocation", node.getSysLocation());
		body.put("foreignsource", node.getForeignSource());
		body.put("foreignid", node.getForeignId());
		//body.put("operatingsystem", node.getOperatingSystem());
		StringBuilder categories = new StringBuilder();
		for (Iterator<OnmsCategory> i = node.getCategories().iterator(); i
				.hasNext();) {
			categories.append(((OnmsCategory) i.next()).getName());
			if (i.hasNext()) {
				categories.append(",");
			}
		}
		body.put("categories", categories.toString());

		// if(archiveAssetData){
		//
		// // parent information
		// OnmsNode parent = node.getParent();
		// if (parent!=null){
		// if (parent.getLabel()!=null)body.put("parent-nodelabel",
		// parent.getLabel());
		// if (parent.getNodeId() !=null)body.put("parent-nodeid",
		// parent.getNodeId());
		// if (parent.getForeignSource() !=null)body.put("parent-foreignsource",
		// parent.getForeignSource());
		// if (parent.getForeignId() !=null)body.put("parent-foreignid",
		// parent.getForeignId());
		// }
		//
		//assetRecord.
		OnmsAssetRecord assetRecord= node.getAssetRecord() ;
		if(assetRecord!=null){

			//geolocation
			OnmsGeolocation gl = assetRecord.getGeolocation();
			if (gl !=null){
				if (gl.getLatitude() !=null)body.put("asset-latitude",
						gl.getLatitude().toString());
				if (gl.getLongitude()!=null)body.put("asset-longitude",
						gl.getLongitude().toString());
			}
			//
			// //assetRecord
			// if (assetRecord.getRegion() !=null && !
			// "".equals(assetRecord.getRegion())) body.put("asset-region",
			// assetRecord.getRegion());
			// if (assetRecord.getBuilding() !=null && !
			// "".equals(assetRecord.getBuilding())) body.put("asset-building",
			// assetRecord.getBuilding());
			// if (assetRecord.getFloor() !=null && !
			// "".equals(assetRecord.getFloor())) body.put("asset-floor",
			// assetRecord.getFloor());
			// if (assetRecord.getRoom() !=null && !
			// "".equals(assetRecord.getRoom())) body.put("asset-room",
			// assetRecord.getRoom());
			// if (assetRecord.getRack() !=null && !
			// "".equals(assetRecord.getRack())) body.put("asset-rack",
			// assetRecord.getRack());
			// if (assetRecord.getSlot() !=null && !
			// "".equals(assetRecord.getSlot())) body.put("asset-slot",
			// assetRecord.getSlot());
			// if (assetRecord.getPort() !=null && !
			// "".equals(assetRecord.getPort())) body.put("asset-port",
			// assetRecord.getPort());
			// if (assetRecord.getCategory() !=null && !
			// "".equals(assetRecord.getCategory())) body.put("asset-category",
			// assetRecord.getCategory());
			// if (assetRecord.getDisplayCategory() !=null && !
			// "".equals(assetRecord.getDisplayCategory()))
			// body.put("asset-displaycategory", assetRecord.getDisplayCategory());
			// if (assetRecord.getNotifyCategory() !=null && !
			// "".equals(assetRecord.getNotifyCategory()))
			// body.put("asset-notifycategory", assetRecord.getNotifyCategory());
			// if (assetRecord.getPollerCategory() !=null && !
			// "".equals(assetRecord.getPollerCategory()))
			// body.put("asset-pollercategory", assetRecord.getPollerCategory());
			// if (assetRecord.getThresholdCategory() !=null && !
			// "".equals(assetRecord.getThresholdCategory()))
			// body.put("asset-thresholdcategory",
			// assetRecord.getThresholdCategory());
			// if (assetRecord.getManagedObjectType() !=null && !
			// "".equals(assetRecord.getManagedObjectType()))
			// body.put("asset-managedobjecttype",
			// assetRecord.getManagedObjectType());
			// if (assetRecord.getManagedObjectInstance() !=null && !
			// "".equals(assetRecord.getManagedObjectInstance()))
			// body.put("asset-managedobjectinstance",
			// assetRecord.getManagedObjectInstance());
			// if (assetRecord.getManufacturer() !=null && !
			// "".equals(assetRecord.getManufacturer()))
			// body.put("asset-manufacturer", assetRecord.getManufacturer());
			// if (assetRecord.getVendor() !=null && !
			// "".equals(assetRecord.getVendor())) body.put("asset-vendor",
			// assetRecord.getVendor());
			// if (assetRecord.getModelNumber() !=null && !
			// "".equals(assetRecord.getModelNumber()))
			// body.put("asset-modelnumber", assetRecord.getModelNumber());
			// }
		}

	}

	/**
	 * returns cache or database entry for node
	 * or returns null if node not in cache or database
	 * @param nodeCriteria nodeid or foreignSource:foreignId
	 */
	@Override
	public Map<String, String> getEntry(String nodeCriteria) {
		try {
			// if the cache loader cannot find a value an exception will be thrown 
			// this means a null value was returned by the nodeDao 
			// and we need to create a new value in the database
			return m_nodeDataCache.get(nodeCriteria);
		} catch (ExecutionException e) {
			if (e.getCause()  instanceof NodeNotInDatabaseException){
				LOG.debug("CacheLoader threw NodeNotInDatabaseException because cannot find node in database for key nodeCriteria="+nodeCriteria);
				return null;
			}
			else {
				throw new RuntimeException("problem loading node for cache:",e);
			}
		}
	}


	/**
	 * refreshes the entry for a given nodeId
	 * does nothing if the node entry a given node id does not exist
	 */
	@Override
	public void refreshEntryForNodeId(String nodeId) {
		LOG.debug("refreshing node m_nodeDataCache entry: " + nodeId);

		for(Entry<String, Map<String, String>> entry :m_nodeDataCache.asMap().entrySet()){
			String entryNodeId= entry.getValue().get("nodeId");
			if(nodeId.equals(entryNodeId)){
				String nodeCriteria=entry.getKey();
				m_nodeDataCache.refresh(nodeCriteria);
			}
		}
	}

	/**
	 * removes the entry for a given node id
	 * does nothing if the node entry a given node id does not exist
	 */
	@Override
	public void removeEntryForNodeId(String nodeId) {
		LOG.debug("removing node m_nodeDataCache entry for nodeId: " + nodeId);

		for(Entry<String, Map<String, String>> entry :m_nodeDataCache.asMap().entrySet()){
			String entryNodeId= entry.getValue().get("nodeId");
			if(nodeId.equals(entryNodeId)){
				String nodeCriteria=entry.getKey();
				m_nodeDataCache.invalidate(nodeCriteria);
			};
		}
	}

	@Override
	public void removeEntry(String nodeCriteria) {
		m_nodeDataCache.invalidate(nodeCriteria);
	}

	@Override
	public void refreshEntry(String nodeCriteria) {
		m_nodeDataCache.refresh(nodeCriteria);
	}


	/**
	 * Try to retrieve node entry for nodeCriteria from cache or database
	 * If not in cache or database creation of new node is attempted
	 * @param nodeCriteria nodeid or foreignSource:foreignId
	 */
	@Override
	public Map<String, String> createOrUpdateNode(String nodeCriteria, OnmsAssetRecord assetRecord) {

		Map<String, String> nodeData = getEntry(nodeCriteria);
		if (nodeData != null) return nodeData;

		EventBuilder eb= new EventBuilder(MQTT_UNKNOWN_NODE_EVENT, "mqtt-client");
		eb.addParam(new Parm("nodeCriteria", nodeCriteria));
		sendEvent(eb.getEvent());

		if(!this.m_createMissingNodes) return null;

		// try to create new node if not already scheduled for creation by another thread
		// this stops rapid messages from same new node trying to create several new nodes in a race
		// m_persistMap prevents attempts to create the node if already requested.
		//  The m_persistMap entry is cleared next time the node is actually loaded from the database by the cache
		if (!m_persistMap.containsKey(nodeCriteria)) {
			synchronized (this) {
				// check another mqtt thread is not trying to create this node
				if (! m_persistMap.containsKey(nodeCriteria)) {
					// check again if node has just been created
					// if it has then just return the data
					nodeData = getEntry(nodeCriteria);
					if(nodeData!=null) return nodeData;

					// telling other mqtt threads that node is already being added for nodeCriteria
					m_persistMap.put(nodeCriteria, nodeCriteria);

					OnmsNode onmsNode = new OnmsNode();
					onmsNode.setType(NodeType.ACTIVE);
					OnmsMonitoringLocation defaultLocation= m_monitoringLocationDao.getDefaultLocation();
					onmsNode.setLocation(defaultLocation);
					if (nodeCriteria.contains(":")) {
						String[] criteria = nodeCriteria.split(":");
						String foreignSource = criteria[0];
						String foreignId = criteria[1];
						onmsNode.setForeignSource(foreignSource);
						onmsNode.setForeignId(foreignId);
						onmsNode.setLabel(foreignId);
					} else {
						onmsNode.setLabel(nodeCriteria);
					}

					LOG.debug("Mqtt cache Adding node {}", onmsNode);

					// wrap in a transaction so that Hibernate session is bound and
					// getIpInterfaces works
					m_transactionOperations.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

							//using save or update in case the node has just been created by an external process
							m_nodeDao.saveOrUpdate(onmsNode);

							//retrieve saved node
							OnmsNode node = m_nodeDao.get(nodeCriteria);
							if(node==null) throw new RuntimeException("node should not be null"); //should not happen

							// create a dummy interface for node if required
							if(m_createDummyInterfaces){
								if(node.getIpInterfaces()==null || node.getIpInterfaces().isEmpty()) {
									LOG.debug("Mqtt cache creating new dummy interface");
									// use illegal ip address 254.0.0.1 and set as unmanaged
									// issnmpprimary='N'  Not eligible (does not support SNMP or or has no ifIndex)
									// ipstatus=1 Up 9 if node is responding
									// ismanaged 'U' - Unmanaged
									// iphostname - set this to the mqtt device name
									// ifindex null
									// nodeid ( not nullable ) set to node id  of the mqtt node
									OnmsIpInterface ipIf = new OnmsIpInterface();
									try {
										ipIf.setIpAddress(InetAddress.getByName("254.0.0.1"));
									} catch (UnknownHostException e) {
										throw new RuntimeException("problem creating IP address",e); // should never happen
									}
									ipIf.setIsSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
									ipIf.setIsManaged("U");
									ipIf.setIpHostName(onmsNode.getLabel());
									ipIf.setNode(node);
									Integer ipIfId = m_ipInterfaceDao.save(ipIf);
									OnmsIpInterface createdipIf=m_ipInterfaceDao.get(ipIfId);
									if(createdipIf==null) throw new RuntimeException("createdipIf should not be null"); //should not happen
									node.addIpInterface(createdipIf);
									m_nodeDao.update(node);
									LOG.debug("Mqtt cache added dummy interface {} to node {}",createdipIf,node);
								}
							}

							// only updating geolocation for asset data
							if(m_createNodeAssetData){
								if(assetRecord!=null){
									LOG.debug("Mqtt cache creating geolocation for nodeid:"+node.getNodeId());
									OnmsAssetRecord currentAssetRecord = node.getAssetRecord();
									currentAssetRecord.setGeolocation(assetRecord.getGeolocation());		
					                m_assetRecordDao.saveOrUpdate(currentAssetRecord);
					                LOG.debug("Mqtt cache updated asset record {} to node {}",currentAssetRecord,node);
								}
							}

							LOG.debug("Mqtt cache node added {}", node);
						}
					});

					m_nodeDao.flush();
					m_ipInterfaceDao.flush();
					m_assetRecordDao.flush();

					// retrieve added node to and sends node added event 
					// this updates the cache which clears entry in m_persistMap 
					nodeData = getEntry(nodeCriteria);
					if(nodeData==null){
						throw new RuntimeException("problem creating and retreiving new node for nodeCriteria="+nodeCriteria);
					}

					eb= new EventBuilder(EventConstants.NODE_ADDED_EVENT_UEI, "mqtt-client");
					eb.addParam("nodelabel", nodeData.get("nodelabel"));
					eb.addParam(new Parm("nodeCriteria", nodeCriteria));

					String nidStr =nodeData.get("nodeId");
					Long nodeId = Long.parseLong(nidStr);
					eb.setNodeid(nodeId);

					sendEvent(eb.getEvent());
				}
			}
		}

		return nodeData;
	}

	private void sendEvent(Event e){
		LOG.debug("sending event to opennms. event.tostring():" + e.toString());
		try {
			if (m_eventProxy != null) {
				m_eventProxy.send(e);
			} else {
				LOG.error("OpenNMS event proxy not set - not sending event to opennms");
			}
		} catch (EventProxyException ex) {
			throw new RuntimeException("event proxy problem sending event to OpenNMS:",ex);
		}
	}

	// EventListener
	@Override
	public String getName() {
		return "MQTTNodeByForeignSourceCache";
	}

	@Override
	public void onEvent(Event event) {
		// check for node change events
		String uei=event.getUei();
		if(uei!=null && uei.startsWith("uei.opennms.org/nodes/")) {
			Long nodeId=event.getNodeid();
			if(EventConstants.NODE_DELETED_EVENT_UEI.equals(uei)){
				LOG.debug("node cache removing entry for nodeId="+nodeId);
				if(nodeId!=null) removeEntryForNodeId(Long.toString(nodeId));
			} else if (
					// uei.endsWith("Added") || only cashing nodes which are in MQTT messags
					uei.endsWith("Updated") || 
					uei.endsWith("Changed")
					) {
				LOG.debug("node cache updating entry for nodeId="+nodeId);
				if(nodeId!=null) refreshEntryForNodeId(Long.toString(nodeId));
			}
		}
	}

	/* getters and setters */

	public void setCreateMissingNodes(boolean createMissingNodes) {
		this.m_createMissingNodes = createMissingNodes;
	}

	public void setCreateDummyInterfaces(boolean createDummyInterfaces) {
		this.m_createDummyInterfaces = createDummyInterfaces;
	}

	public void setcreateNodeAssetData(boolean createNodeAssetData) {
		this.m_createNodeAssetData = createNodeAssetData;
	}

	public void setNodeDao(NodeDao nodeDao) {
		this.m_nodeDao = nodeDao;
	}

	public void setTransactionOperations(
			TransactionOperations transactionOperations) {
		this.m_transactionOperations = transactionOperations;
	}

	public void setMAX_SIZE(long MAX_SIZE) {
		this.MAX_SIZE = MAX_SIZE;
	}

	public void setMAX_TTL(long MAX_TTL) {
		this.MAX_TTL = MAX_TTL;
	}

	public void setEventProxy(EventProxy eventProxy) {
		this.m_eventProxy = eventProxy;
	}

	public void setEventIpcManager(EventIpcManager eventIpcManager) {
		this.m_eventIpcManager = eventIpcManager;
	}

	public void setMonitoringLocationDao(MonitoringLocationDao monitoringLocationDao) {
		this.m_monitoringLocationDao = monitoringLocationDao;
	}

	public void setIpInterfaceDao(IpInterfaceDao ipInterfaceDao) {
		this.m_ipInterfaceDao = ipInterfaceDao;
	}

	public void setAssetRecordDao(AssetRecordDao assetRecordDao) {
		this.m_assetRecordDao = assetRecordDao;
	}


}