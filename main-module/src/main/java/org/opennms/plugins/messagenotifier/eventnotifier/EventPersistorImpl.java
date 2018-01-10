package org.opennms.plugins.messagenotifier.eventnotifier;

import java.util.List;

import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPersistorImpl implements EventPersistor {
	private static final Logger LOG = LoggerFactory.getLogger(EventPersistorImpl.class);
	
	/* (non-Javadoc)
	 * @see org.opennms.plugins.messagenotifier.eventnotifier.EventPersistor#persistAttributeMapList(java.util.List)
	 */
	@Override
	public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap){

		LOG.debug("eventPersistor persisting attributeMap: "+attributeMap.toString());
		//TODO
	}
}
