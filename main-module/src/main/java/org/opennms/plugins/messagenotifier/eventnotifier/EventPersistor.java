package org.opennms.plugins.messagenotifier.eventnotifier;

import java.util.List;

import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPersistor {
	private static final Logger LOG = LoggerFactory.getLogger(EventPersistor.class);
	
	public void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap){

		LOG.debug("eventPersistor persisting attributeMap: "+attributeMap.toString());
		//TODO
	}
}
