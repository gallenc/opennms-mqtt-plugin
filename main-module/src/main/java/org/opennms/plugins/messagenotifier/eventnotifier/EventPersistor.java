package org.opennms.plugins.messagenotifier.eventnotifier;

import java.util.List;

import org.opennms.plugins.json.OnmsCollectionAttributeMap;

public interface EventPersistor {

	public abstract void persistAttributeMapList(List<OnmsCollectionAttributeMap> attributeMap, String ueiRoot);

}