package org.opennms.plugins.json.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.opennms.plugins.json.OnmsAttributeMessageHandler;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.messagenotifier.CompressionMethods;
import org.opennms.plugins.messagenotifier.MessagePayloadTypeHandler;
import org.opennms.protocols.xml.config.XmlGroup;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnmsAttributeJsonHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeJsonHandlerTest.class);

	// complex parsing - based upon opennms xml collector example 
	// https://wiki.opennms.org/wiki/XML_Collector
	private static final String TEST_JSON_1 = "src/test/resources/JsonParserTests/testJson1.json";
	private static final String TEST_XMLGROUP_1 = "src/test/resources/JsonParserTests/testXmlGroup1.xml";

	// sniffy format parsing
	private static final String TEST_JSON_2 = "src/test/resources/JsonParserTests/testJson2.json";
	private static final String TEST_XMLGROUP_2 = "src/test/resources/JsonParserTests/testXmlGroup2.xml";

	// array of sniffy format data parsing
	private static final String TEST_JSON_3 = "src/test/resources/JsonParserTests/testJson3.json";
	private static final String TEST_XMLGROUP_3 = "src/test/resources/JsonParserTests/testXmlGroup3.xml";

	// SEE Kura format https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	// typed json encoding
	private static final String TEST_JSON_4 = "src/test/resources/JsonParserTests/testJson4.json";
	private static final String TEST_XMLGROUP_4 = "src/test/resources/JsonParserTests/testXmlGroup4.xml";

	// SEE Kura format https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	// simple json encoding
	private static final String TEST_JSON_5 = "src/test/resources/JsonParserTests/testJson5.json";
	private static final String TEST_XMLGROUP_5 = "src/test/resources/JsonParserTests/testXmlGroup5.xml";
	
	// SEE Kura format https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	// simple json encoding
	private static final String TEST_JSON_6 = "src/test/resources/JsonParserTests/testJson6.json";
	private static final String TEST_XMLGROUP_6 = "src/test/resources/JsonParserTests/testXmlGroup6.xml";
	
	// Laurawan test - Herb Garcea
	private static final String TEST_JSON_7 = "src/test/resources/JsonParserTests/testJson7.json";
	private static final String TEST_XMLGROUP_7 = "src/test/resources/JsonParserTests/testXmlGroup7.xml";
	private static final String TEST_XMLGROUP_7b = "src/test/resources/JsonParserTests/testXmlGroup7b.xml";
	
	@Test
	public void test1() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test1");

		String xmlGroupFile = TEST_XMLGROUP_1;
		String jsonFile = TEST_JSON_1;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);

		assertTrue(attributeMapList.size()==2);

		// message 1
		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		
		// message 2
		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test1");
	}

	@Test
	public void test2() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test2");

		String xmlGroupFile = TEST_XMLGROUP_2;
		String jsonFile = TEST_JSON_2;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);

		assertTrue(attributeMapList.size()==1);

		assertTrue("monitorID".equals(attributeMapList.get(0).getForeignId()));

		DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
		DateTime dateTime = dtf.parseDateTime("2017-10-19 10:15:02.854888");
		long time = dateTime.toDate().getTime();
		assertTrue(new Long(time).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 

		assertEquals("Common#1",attributeMapList.get(0).getAttributeMap().get("stationName").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test2");
	}

	@Test
	public void test3() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test3");

		String xmlGroupFile = TEST_XMLGROUP_3;
		String jsonFile = TEST_JSON_3;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);
		
		assertTrue(attributeMapList.size()==2); 
		
		// message 1
		assertTrue("monitorID".equals(attributeMapList.get(0).getForeignId()));

		DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
		DateTime dateTime = dtf.parseDateTime("2017-10-19 10:15:02.844444");
		long time = dateTime.toDate().getTime();
		assertTrue(new Long(time).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 

		assertEquals("Common#1",attributeMapList.get(0).getAttributeMap().get("stationName").getValue() );
		
		// message 2
		assertTrue("monitorID2".equals(attributeMapList.get(1).getForeignId()));

		DateTime dateTime2 = dtf.parseDateTime("2017-10-19 10:15:02.855555");
		long time2 = dateTime2.toDate().getTime();
		assertTrue(new Long(time2).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 

		assertEquals("Common#2",attributeMapList.get(1).getAttributeMap().get("stationName").getValue() );



		LOG.debug("end OnmsAttributeJsonHandlerTest test3");
	}

	@Test
	public void test4() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test4");

		String xmlGroupFile = TEST_XMLGROUP_4;
		String jsonFile = TEST_JSON_4;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);
		
		assertTrue(attributeMapList.size()==1); 
		
		// message 1
		assertTrue("A23D44567Q".equals(attributeMapList.get(0).getForeignId()));

		long time = 1491298822;
		assertTrue(new Long(time).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 

		assertEquals("45.234",attributeMapList.get(0).getAttributeMap().get("latitude").getValue() );
		assertEquals("-7.3456",attributeMapList.get(0).getAttributeMap().get("longitude").getValue() );
		Double d1 = Double.parseDouble("0.26456E+4");
		Double d2 = Double.parseDouble(attributeMapList.get(0).getAttributeMap().get("distance").getValue());
		assertEquals(d1, d2);

		LOG.debug("end OnmsAttributeJsonHandlerTest test4");
	}

	@Test
	public void test5() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test5");

		String xmlGroupFile = TEST_XMLGROUP_5;
		String jsonFile = TEST_JSON_5;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);
		
		// message 1
		assertTrue("A23D44567Q".equals(attributeMapList.get(0).getForeignId()));

		long time = 1491298822;
		assertTrue(new Long(time).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 

		assertEquals("45.234",attributeMapList.get(0).getAttributeMap().get("latitude").getValue() );
		assertEquals("-7.3456",attributeMapList.get(0).getAttributeMap().get("longitude").getValue() );
		Double d1 = Double.parseDouble("0.26456E+4");
		Double d2 = Double.parseDouble(attributeMapList.get(0).getAttributeMap().get("distance").getValue());
		assertEquals(d1, d2);

		LOG.debug("end OnmsAttributeJsonHandlerTest test5");
	}
	
	
	@Test
	public void test6() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test6");

		String xmlGroupFile = TEST_XMLGROUP_6;
		String jsonFile = TEST_JSON_6;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);

		assertTrue(attributeMapList.size()==2);

		// message 1
		assertTrue("global_0".equals(attributeMapList.get(0).getResourceName()));
		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		
		// message 2
		assertTrue("zone1_871".equals(attributeMapList.get(1).getResourceName()));
		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test6");
	}
	
	/*
	 * Test of compression
	 */
	@Test
	public void test6b() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test6b (compression)");

		String xmlGroupFile = TEST_XMLGROUP_6;
		String jsonFile = TEST_JSON_6;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, CompressionMethods.GZIP, topic);

		assertTrue(attributeMapList.size()==2);

		// message 1
		assertTrue("global_0".equals(attributeMapList.get(0).getResourceName()));
		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		
		// message 2
		assertTrue("zone1_871".equals(attributeMapList.get(1).getResourceName()));
		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test6b (compression)");
	}
	
	
	/*
	 * Test of automatic compression
	 */
	@Test
	public void test6c() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test6c (automatic compression)");

		String xmlGroupFile = TEST_XMLGROUP_6;
		String jsonFile = TEST_JSON_6;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, CompressionMethods.AUTOMATIC_GZIP, topic);

		assertTrue(attributeMapList.size()==2);

		// message 1
		assertTrue("global_0".equals(attributeMapList.get(0).getResourceName()));
		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		
		// message 2
		assertTrue("zone1_871".equals(attributeMapList.get(1).getResourceName()));
		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test6c (automatic compression)");
	}
	
	// test of Herb's Laurawan
	// with id in message as 'd'
	@Test
	public void test7() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test7");

		String xmlGroupFile = TEST_XMLGROUP_7;
		String jsonFile = TEST_JSON_7;
		String topic=null;  // not used but needed by class declaration

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);

		assertTrue(attributeMapList.size()==1);

		assertTrue("d".equals(attributeMapList.get(0).getForeignId()));

		assertEquals("462",attributeMapList.get(0).getAttributeMap().get("light").getValue() );
		assertEquals("97.34025",attributeMapList.get(0).getAttributeMap().get("moisture").getValue() );
		assertEquals("29.8125",attributeMapList.get(0).getAttributeMap().get("temperature").getValue() );
		assertEquals("0.1875",attributeMapList.get(0).getAttributeMap().get("x_acc").getValue() );
		assertEquals("0.375",attributeMapList.get(0).getAttributeMap().get("y_acc").getValue() );
		assertEquals("0.8125",attributeMapList.get(0).getAttributeMap().get("z_acc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test7");
	}
	
	// test of Herb's Laurawan
	// with id in topic
	@Test
	public void test7b() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test7b");

		String xmlGroupFile = TEST_XMLGROUP_7b;
		String jsonFile = TEST_JSON_7;
		
		// laurawan topic parsed by xpath	PMc178e3c2.6637eiot-2/type/mosquitto/id/00-08-00-4A-4F-F6/evt/datapoint/fmt/json
		//           $topicLevels[1]       /[2] /[3]      /[4]/[5]             /[6] etc...
		String topic="PMc178e3c2.6637eiot-2/type/mosquitto/id/00-08-00-4A-4F-F6/evt/datapoint/fmt/json"; 

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile, topic);

		assertTrue(attributeMapList.size()==1);

		assertTrue("00-08-00-4A-4F-F6".equals(attributeMapList.get(0).getForeignId()));

		assertEquals("462",attributeMapList.get(0).getAttributeMap().get("light").getValue() );
		assertEquals("97.34025",attributeMapList.get(0).getAttributeMap().get("moisture").getValue() );
		assertEquals("29.8125",attributeMapList.get(0).getAttributeMap().get("temperature").getValue() );
		assertEquals("0.1875",attributeMapList.get(0).getAttributeMap().get("x_acc").getValue() );
		assertEquals("0.375",attributeMapList.get(0).getAttributeMap().get("y_acc").getValue() );
		assertEquals("0.8125",attributeMapList.get(0).getAttributeMap().get("z_acc").getValue() );

		LOG.debug("end OnmsAttributeJsonHandlerTest test7b");
	}

	/*
	 * Test with no compression
	 */
	public List<OnmsCollectionAttributeMap> testMethod(String xmlGroupFile, String jsonFile, String topic){
		return testMethod(xmlGroupFile, jsonFile, CompressionMethods.UNCOMPRESSED, topic);
	}

	/*
	 * Test with compression selection
	 */
	public List<OnmsCollectionAttributeMap> testMethod(String xmlGroupFile, String jsonFile, String compression, String topic){
		// read xpath configuration
		XmlGroups xmlGroups = unmarshalXmlGroups(xmlGroupFile);
		// read json string
		String jsonString = readFile(jsonFile);
		LOG.debug("jsonString"+jsonString);
		
		// convert json string as byte array to attributeMapList
		byte[] payload;
		try {
			payload = jsonString.getBytes(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		if (CompressionMethods.GZIP.equals(compression)||CompressionMethods.AUTOMATIC_GZIP.equals(compression) ) {
			try {
				payload = CompressionMethods.compressGzip(payload);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		Object payloadObj = MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.JSON,compression);

		XmlRrd xmlRrd = null; // not used but needed by class declaration
		OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(xmlGroups, xmlRrd, topic );
		List<OnmsCollectionAttributeMap> attributeMapList = onmsAttributeMessageHandler.payloadObjectToAttributeMap(payloadObj);

		LOG.debug("attributeMap: \n    attributeMap.size: "+attributeMapList.size()+"\n    attributeMap.toString: "+attributeMapList.toString().replaceAll("],", "],\n    "));

		return attributeMapList;
	}


	public XmlGroups unmarshalXmlGroups(String fileUrl){
		try {
			File xmlGroupsFile = new File(fileUrl);
			LOG.debug("loading test xmlgroups from file="+xmlGroupsFile.getAbsolutePath());
			JAXBContext jaxbContext = JAXBContext.newInstance(XmlGroups.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			XmlGroups xmlGroups = (XmlGroups) jaxbUnmarshaller.unmarshal(xmlGroupsFile);
			return xmlGroups;
		} catch (JAXBException e) {
			throw new RuntimeException("Problem loading xmlgroups file",e);
		}
	}

	public String readFile(String fileUrl) {
		String fileString=null;
		try {
			File file = new File(fileUrl);
			LOG.debug("loading test data from file="+file.getAbsolutePath());
			fileString = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Problem loading file "+fileUrl,e);
		}
		return fileString;
	}

}
