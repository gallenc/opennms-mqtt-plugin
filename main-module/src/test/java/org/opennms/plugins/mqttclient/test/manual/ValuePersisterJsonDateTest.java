package org.opennms.plugins.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.junit.Test;
import org.opennms.netmgt.collection.api.AttributeGroup;
import org.opennms.netmgt.collection.api.CollectionAttribute;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.Persister;
import org.opennms.netmgt.collection.api.PersisterFactory;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.plugins.messagenotifier.datanotifier.ConfigDao;
import org.opennms.plugins.messagenotifier.datanotifier.ValuePersister;

public class ValuePersisterJsonDateTest {

	@Test
	public void testDate() {
		
		String startDateString="2017-10-18T15:01:29UTC";
		String dateTimeFormatPattern="yyyy-mm-dd'T'HH:mm:ssz";
		
		ValuePersister vp = new ValuePersister();
		PersisterFactory persisterFactory = new MockPersisterFactory();
		vp.setPersisterFactory(persisterFactory );
		
		ConfigDao configDao = new ConfigDao();
		configDao.setDateTimeFormatPattern(dateTimeFormatPattern);
		vp.setConfigDao(configDao);
		
		Date date = vp.parseJsonTimestampToDate(startDateString);
		
		String newDateString = vp.parseDatetoJsonTimestamp(date);
		
		System.out.println("startDateString "+startDateString );
		System.out.println("newDateString "+newDateString );

		assertEquals(startDateString,newDateString );

	}
	
	private class MockPersisterFactory implements PersisterFactory{

		@Override
		public Persister createPersister(ServiceParameters params,
				RrdRepository repository) {
			
			return new MockPersister();
		}

		@Override
		public Persister createPersister(ServiceParameters params,
				RrdRepository repository, boolean dontPersistCounters,
				boolean forceStoreByGroup, boolean dontReorderAttributes) {
			
			return new MockPersister();
		}
		
	}
	
	private class MockPersister implements Persister {

		@Override
		public void visitCollectionSet(CollectionSet set) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visitResource(CollectionResource resource) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visitGroup(AttributeGroup group) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visitAttribute(CollectionAttribute attribute) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void completeAttribute(CollectionAttribute attribute) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void completeGroup(AttributeGroup group) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void completeResource(CollectionResource resource) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void completeCollectionSet(CollectionSet set) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void persistNumericAttribute(CollectionAttribute attribute) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void persistStringAttribute(CollectionAttribute attribute) {
			// TODO Auto-generated method stub
			
		}
		
		 
	}

}
