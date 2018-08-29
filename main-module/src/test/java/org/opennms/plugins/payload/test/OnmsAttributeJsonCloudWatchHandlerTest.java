package org.opennms.plugins.payload.test;

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
import org.opennms.plugins.messagehandler.CompressionMethods;
import org.opennms.plugins.messagehandler.MessagePayloadTypeHandler;
import org.opennms.plugins.messagehandler.OnmsAttributeMessageHandler;
import org.opennms.plugins.messagehandler.OnmsCollectionAttributeMap;
import org.opennms.protocols.xml.config.XmlGroup;
import org.opennms.protocols.xml.config.XmlGroups;
import org.opennms.protocols.xml.config.XmlRrd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnmsAttributeJsonCloudWatchHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeJsonCloudWatchHandlerTest.class);

	private static final String TEST_JSON_CLOUDWATCH = "src/test/resources/JsonParserTests/testJsonCloudWatch.json";
	private static final String TEST_XMLGROUP_CLOUDWATCH = "src/test/resources/JsonParserTests/testXmlGroupCloudWatch.xml";

	// test of JsonCloudwatch
	@Test
	public void test1() {
		LOG.debug("start OnmsAttributeJsonCloudWatchHandlerTest test1");

		String xmlGroupFile = TEST_XMLGROUP_CLOUDWATCH;
		String jsonFile = TEST_JSON_CLOUDWATCH;

		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile);

//		assertTrue(attributeMapList.size()==1);

//		assertTrue("d".equals(attributeMapList.get(0).getForeignId()));
//
//		assertEquals("462",attributeMapList.get(0).getAttributeMap().get("light").getValue() );
//		assertEquals("97.34025",attributeMapList.get(0).getAttributeMap().get("moisture").getValue() );
//		assertEquals("29.8125",attributeMapList.get(0).getAttributeMap().get("temperature").getValue() );
//		assertEquals("0.1875",attributeMapList.get(0).getAttributeMap().get("x_acc").getValue() );
//		assertEquals("0.375",attributeMapList.get(0).getAttributeMap().get("y_acc").getValue() );
//		assertEquals("0.8125",attributeMapList.get(0).getAttributeMap().get("z_acc").getValue() );

		LOG.debug("end OnmsAttributeJsonCloudWatchHandlerTest test1");
	}

	/*
	 * Test with no compression
	 */
	public List<OnmsCollectionAttributeMap> testMethod(String xmlGroupFile, String jsonFile){
		String topic=null;  // not used but needed by class declaration
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
		String defaultForeignSource=null;
		int qos=0;
		OnmsAttributeMessageHandler onmsAttributeMessageHandler = new OnmsAttributeMessageHandler(xmlGroups, xmlRrd, topic, defaultForeignSource, qos );
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
