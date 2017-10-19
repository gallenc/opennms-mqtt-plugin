package org.opennms.plugins.mqttclient.test.manual;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValuePersisterJsonDateTest {
	private static final Logger LOG = LoggerFactory.getLogger(ValuePersisterJsonDateTest.class);
	
	@Test
	public void simpleTestDate(){
		LOG.debug("start of simpleTestDate()");
		
		//ZoneOffset zoneOffset = ZoneOffset.of("Z");
		ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();
		LOG.debug("zoneOffset    : "+zoneOffset);
		
		
		String inputtimeStr ="2017-10-19 10:15:02.854888";
		String formatStr="yyyy-MM-dd HH:mm:ss.SSSSSS";
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatStr);
		
		LocalDateTime localDateTime= LocalDateTime.parse(inputtimeStr, formatter);
		String outputStr = localDateTime.format(formatter);
		
		Instant instant = localDateTime.toInstant(zoneOffset);
		Date dateFromInstant = Date.from(instant);
		Instant instantFromDate = dateFromInstant.toInstant();
		
		LocalDateTime endLocalDateTime = LocalDateTime.ofInstant(instantFromDate,zoneOffset);
		String outputStr2 = endLocalDateTime.format(formatter);

		LOG.debug("formatStr    : "+formatStr);
		LOG.debug("inputtimeStr : "+inputtimeStr);
		LOG.debug("outputStr    : "+outputStr);
		LOG.debug("outputStr2   : "+outputStr2);
		LOG.debug("end of simpleTestDate()");

	}


	@Test
	public void testValuePersisterDate() {
		LOG.debug("start of testValuePersisterDate()");
		
		String startDateString="2017-10-19 10:15:02.854888";
		String dateTimeFormatPattern="yyyy-MM-dd HH:mm:ss.SSSSSS";
		
		ValuePersister vp = new ValuePersister();
		PersisterFactory persisterFactory = new MockPersisterFactory();
		vp.setPersisterFactory(persisterFactory );
		
		ConfigDao configDao = new ConfigDao();
		configDao.setDateTimeFormatPattern(dateTimeFormatPattern);
		configDao.setTimeZoneOffset(ZoneOffset.UTC.toString());
		
		vp.setConfigDao(configDao);
		
		Date date = vp.parseJsonTimestampToDate(startDateString);
		
		String endDateString = vp.parseDatetoJsonTimestamp(date);
		
		LOG.debug("startDateString "+startDateString );
		LOG.debug("endDateString   "+endDateString );

		// ignore last 3 characters because of truncated nanoseconds in Date object
		int compareLength="yyyy-MM-dd HH:mm:ss.SSS".length();
		assertEquals(startDateString.substring(0, compareLength-1),
				endDateString.substring(0, compareLength-1) );
		LOG.debug("endof testValuePersisterDate()");

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
