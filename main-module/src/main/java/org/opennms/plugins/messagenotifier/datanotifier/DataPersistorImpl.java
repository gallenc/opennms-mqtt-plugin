package org.opennms.plugins.messagenotifier.datanotifier;

import java.util.List;

import org.opennms.netmgt.collection.api.PersisterFactory;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.mqttclient.NodeByForeignSourceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPersistorImpl implements DataPersistor {
	private static final Logger LOG = LoggerFactory.getLogger(DataPersistorImpl.class);
	
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
	public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap){
		LOG.debug("dataPersistor persisting attributeMap: "+attributeMap.toString());
		//TODO
	}
	
}
