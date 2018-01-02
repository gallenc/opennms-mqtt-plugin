package org.opennms.plugins.json.test.manual;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.opennms.plugins.json.OnmsAttributeJsonHandler;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.protocols.xml.config.XmlGroup;
import org.opennms.protocols.xml.config.XmlGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnmsAttributeJsonHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(OnmsAttributeJsonHandlerTest.class);

	private static final String TEST_JSON_1 = "src/test/resources/JsonParserTests/testJson1.json";
	private static final String TEST_XMLGROUP_1 = "src/test/resources/JsonParserTests/testXmlGroup1.xml";
	
	private static final String TEST_JSON_2 = "src/test/resources/JsonParserTests/testJson2.json";
	private static final String TEST_XMLGROUP_2 = "src/test/resources/JsonParserTests/testXmlGroup2.xml";


	@Test
	public void test1() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test1");
		
		String xmlGroupFile = TEST_XMLGROUP_1;
		String jsonFile = TEST_JSON_1;
		
		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile);
		
		LOG.debug("end OnmsAttributeJsonHandlerTest test1");
	}
	
	@Test
	public void test2() {
		LOG.debug("start OnmsAttributeJsonHandlerTest test2");
		
		String xmlGroupFile = TEST_XMLGROUP_2;
		String jsonFile = TEST_JSON_2;
		
		List<OnmsCollectionAttributeMap> attributeMapList = testMethod(xmlGroupFile, jsonFile);
		
		LOG.debug("end OnmsAttributeJsonHandlerTest test2");
	}
	
	public List<OnmsCollectionAttributeMap> testMethod(String xmlGroupFile, String jsonFile){
		// read xpath configuration
		XmlGroups xmlGroups = unmarshalXmlGroups(xmlGroupFile);
		// read json string
		String jsonString = readFile(jsonFile);
		LOG.debug("jsonString"+jsonString);

		// convert json string to attributeMapList
		OnmsAttributeJsonHandler onmsAttributeJsonHandler = new OnmsAttributeJsonHandler(xmlGroups);
		List<OnmsCollectionAttributeMap> attributeMapList = onmsAttributeJsonHandler.jsonToAttributeMap(jsonString);

		LOG.debug("attributeMap.size: "+attributeMapList.size()+" attributeMap.toString: "+attributeMapList.toString().replaceAll("],", "],\n"));

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

	//	public void testExtractJson() {
	//		LOG.debug("start testExtractJson()");
	//		try {
	//
	//			File pluginManagerFile = new File(fileUri);
	//
	//			JAXBContext jaxbContext = JAXBContext.newInstance(XmlGroups.class);
	//			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	//
	//			XmlGroups xmlGroups = (XmlGroups) jaxbUnmarshaller.unmarshal(pluginManagerFile);
	//
	//			LOG.debug("test loaded historic data from file="+pluginManagerFile.getAbsolutePath());
	//
	//			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	//			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
	//			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	//			StringWriter stringWriter = new StringWriter();
	//			jaxbMarshaller.marshal(xmlGroups ,stringWriter);
	//			LOG.debug("output string xmlgroups: \n"+stringWriter.toString());
	//
	//			OnmsAttributeJsonHandler waj = new OnmsAttributeJsonHandler();
	//
	//			JSONObject json=null;
	//			try {
	//				json = parseJson(jsonUri);
	//			} catch (Exception e1) {
	//				// TODO Auto-generated catch block
	//				e1.printStackTrace();
	//			} 
	//			LOG.debug("output string json: \n"+json.toJSONString());
	//
	//			List<OnmsCollectionAttributeMap> attributeMapList = new ArrayList<OnmsCollectionAttributeMap>();
	//
	//			try {
	//				waj.fillAttributeMap(attributeMapList , xmlGroups, json);
	//			} catch (java.text.ParseException e) {
	//				e.printStackTrace();
	//			}
	//
	//			LOG.debug("attributeMap.size: "+attributeMapList.size());
	//
	//			for( OnmsCollectionAttributeMap attributeMap: attributeMapList){
	//				for(String key: attributeMap.getAttributeMap().keySet()){
	//					LOG.debug("key:"+key + " Value:"+attributeMap.getAttributeMap().get(key).toString());
	//				}
	//			}
	//
	//
	//
	//
	//		} catch (JAXBException e) {
	//			throw new RuntimeException("Problem loading Plugin Manager Data",e);
	//		}
	//		LOG.debug("end testExtractJson()");
	//	}
}
