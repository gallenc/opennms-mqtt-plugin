package org.opennms.plugins.messagenotifier.datanotifier;

import java.util.List;

import org.opennms.plugins.json.OnmsCollectionAttributeMap;

public interface DataPersistor {

	public abstract void persistAttributeMapList(
			List<OnmsCollectionAttributeMap> attributeMap);

}