package org.opennms.plugins.persistor.eventnotifier;

import java.util.List;

import org.opennms.plugins.messagehandler.OnmsCollectionAttributeMap;

public interface EventPersistor {

	public abstract void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap, String ueiRoot);

}