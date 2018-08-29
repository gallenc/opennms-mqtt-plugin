package org.opennms.plugins.persistor.datanotifier;

import java.util.List;

import org.opennms.plugins.messagehandler.OnmsCollectionAttributeMap;

public interface DataPersistor {

	public abstract void persistAttributeMapList(
			List<OnmsCollectionAttributeMap> attributeMap);

}