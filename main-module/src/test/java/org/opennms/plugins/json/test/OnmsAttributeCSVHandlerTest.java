package org.opennms.plugins.json.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.opennms.plugins.json.OnmsAttributeMessageHandler;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.messagenotifier.MessagePayloadTypeHandler;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnmsAttributeCSVHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeCSVHandlerTest.class);

	// complex parsing - based upon opennms xml collector example 
	// https://wiki.opennms.org/wiki/XML_Collector
	private static final String TEST_CSV_1 = "src/test/resources/CsvParserTests/testCsv1.txt";
	private static final String TEST_XMLGROUP_CSV_1 = "src/test/resources/CsvParserTests/testXmlGroup_csv1.xml";

	/*
	 * http://www.hantsair.org.uk/hampshire/asp/DataSite.asp?CBXSpecies1=NOm&CBXSpecies2=NO2m&CBXSpecies3=NOXm&CBXSpecies4=PM10m&day1=1&month1=jan&year1=2018&day2=2&month2=jan&year2=2018&period=15min&site=ES1&la=Southampton&res=6&Submit.x=32&Submit.y=9
	 */
	@Test
	public void test1() {
		LOG.debug("start OnmsAttributeCsvHandlerTest test1");

		// tests that file can be parsed into csv
		String csvFile = TEST_CSV_1;

		String csvString = readFile(csvFile);
		LOG.debug("csvString"+csvString);

		// convert csv string as byte array to attributeMapList
		byte[] payload;
		try {
			payload = csvString.getBytes(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		boolean compressed=false;
		List<List<String>> receivedObject = (List<List<String>>) MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.TEXT_CSV,compressed);
		assertNotNull(receivedObject);
		
		String receivedString = listToString(receivedObject);
		LOG.debug("receivedObject: SIZE"+receivedObject.size()
				+ " STRING:"+receivedString);

		String[] lines =  csvString.split("\\R+"); // split at line separator ignore empty lines
		LOG.debug("lines: SIZE"+lines.length);
		
		assertEquals(lines.length,receivedObject.size());
		
		List<List<String>> receivedObject2 = (List<List<String>>) MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.TEXT_CSV_HEADER,compressed);
		assertNotNull(receivedObject2);
		
		String receivedString2 = listToString(receivedObject2);
		LOG.debug("receivedString2: "+receivedString2);

		
		assertEquals(lines.length-1,receivedObject2.size());
	


		//		assertTrue(attributeMapList.size()==2);
		//
		//		// message 1
		//		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		//		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		//		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		//		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		//		
		//		// message 2
		//		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		//		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		//		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		//		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeCsvHandlerTest test1");
	}

	/*
	 * http://www.hantsair.org.uk/hampshire/asp/DataSite.asp?CBXSpecies1=NOm&CBXSpecies2=NO2m&CBXSpecies3=NOXm&CBXSpecies4=PM10m&day1=1&month1=jan&year1=2018&day2=2&month2=jan&year2=2018&period=15min&site=ES1&la=Southampton&res=6&Submit.x=32&Submit.y=9
	 */
    @Test
	public void test2() {
		LOG.debug("start OnmsAttributeCsvHandlerTest test2");

		String xmlGroupFile = TEST_XMLGROUP_CSV_1;
		String csvFile = TEST_CSV_1;

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, csvFile);

		
		//		assertTrue(attributeMapList.size()==2);
		//
		//		// message 1
		//		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		//		assertTrue("global".equals(attributeMapList.get(0).getForeignId()));
		//		assertTrue(new Long(1299258888).equals(new Long(attributeMapList.get(0).getTimestamp().getTime()))); 
		//		assertEquals("245",attributeMapList.get(0).getAttributeMap().get("nproc").getValue() );
		//		
		//		// message 2
		//		assertTrue("node".equals(attributeMapList.get(0).getResourceName()));
		//		assertTrue("zone1".equals(attributeMapList.get(1).getForeignId()));
		//		assertTrue(new Long(1299259999).equals(new Long(attributeMapList.get(1).getTimestamp().getTime()))); 
		//		assertEquals("24",attributeMapList.get(1).getAttributeMap().get("nproc").getValue() );

		LOG.debug("end OnmsAttributeCsvHandlerTest test2");
	}


	public List<OnmsCollectionAttributeMap> testMethod(String xmlGroupFile, String csvFile){
		// read xpath configuration
		XmlGroups xmlGroups = unmarshalXmlGroups(xmlGroupFile);
		// read csv string
		String csvString = readFile(csvFile);
		//LOG.debug("csvString"+csvString);

		// convert csv string as byte array to attributeMapList
		byte[] payload;
		try {
			payload = csvString.getBytes(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		boolean compressed=false;
		Object payloadObj = MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.TEXT_CSV_HEADER, compressed);

		XmlRrd xmlRrd = null; // not used but needed by class declaration
		OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(xmlGroups, xmlRrd );
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

	public String listToString(List<List<String>> receivedObject ){
		StringBuffer sb = new StringBuffer();

		Iterator<List<String>> i1 = receivedObject.iterator();
		while (i1.hasNext()){
			Iterator<String> i = i1.next().iterator();
			while (i.hasNext()){
				sb.append(i.next());
				if(i.hasNext()) sb.append(",");
			}
			if(i1.hasNext()) sb.append("\n");
		}
		return sb.toString();
	}



}
