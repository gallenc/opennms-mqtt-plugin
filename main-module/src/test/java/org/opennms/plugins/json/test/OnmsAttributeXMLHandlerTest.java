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
import java.util.Iterator;
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
import org.w3c.dom.Document;

public class OnmsAttributeXMLHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeXMLHandlerTest.class);

	// complex parsing - based upon opennms xml collector example 
	// https://wiki.opennms.org/wiki/XML_Collector
	private static final String TEST_XML_1 = "src/test/resources/XmlParserTests/testXml1.xml";
	private static final String TEST_XMLGROUP_XML_1 = "src/test/resources/XmlParserTests/testXmlGroup_Xml1.xml";

	/*
	 * using kura test xml at
	 * https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	 */
	@Test
	public void test1() {
		LOG.debug("start OnmsAttributeCsvHandlerTest test1");

		// tests that file can be parsed into csv
		String csvFile = TEST_XML_1;

		String csvString = readFile(csvFile);
		LOG.debug("csvString:"+csvString);

		// convert csv string as byte array to attributeMapList
		byte[] payload;
		try {
			payload = csvString.getBytes(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String compression=CompressionMethods.UNCOMPRESSED;
		Document receivedObject = (Document) MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.XML,compression);
		assertNotNull(receivedObject);
		

		LOG.debug("receivedObject(Document):"+receivedObject.toString());

		LOG.debug("end OnmsAttributeCsvHandlerTest test1");
	}

	/*
	 * using kura test xml at
	 * https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	 */
    @Test
	public void test2() {
		LOG.debug("start OnmsAttributeCsvHandlerTest test2");

		String xmlGroupFile = TEST_XMLGROUP_XML_1;
		String csvFile = TEST_XML_1;

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, csvFile);

		
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
		
		String compression=CompressionMethods.UNCOMPRESSED;
		Object payloadObj = MessagePayloadTypeHandler.parsePayload(payload , MessagePayloadTypeHandler.XML,compression);

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

}
