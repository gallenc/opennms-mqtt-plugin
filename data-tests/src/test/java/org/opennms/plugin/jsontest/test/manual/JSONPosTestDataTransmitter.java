package org.opennms.plugin.jsontest.test.manual;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONPosTestDataTransmitter {
	private static final Logger LOG = LoggerFactory.getLogger(JSONPosTestDataTransmitter.class);

	//public static final String jsonTestFile="./src/test/resources/testData.json";
	public static final String TEST_JSON_FILE="./src/test/resources/TestData/testData2.json";

	public static final String MAIN_PROPERTIES_FILE="./src/test/resources/TestData/clienttest.properties";

	public static final String SERVER_URL = "http://localhost:8980/opennms/plugin/mqtt/v1-0/receive/mqtt-events/0/"; 

	public static final String OPENNMS_REST_USERNAME = "admin";
	public static final String OPENNMS_REST_PASSWORD = "admin";

	public static final String DEFAULT_PERSIST_INTERVAL = "30000";

	// set up transmitter
	private String serverUrl = SERVER_URL;
	private String userName =OPENNMS_REST_USERNAME;
	private String password =OPENNMS_REST_PASSWORD;

	private long persistInterval = 1000 * 60; // 1 minute // 5 * 1000 * 60; // 5 MINUTES
	private boolean useRepeatTimer=true;
	private boolean useJsonFile = true;
	private String jsonTestFile=TEST_JSON_FILE;

	public static AtomicInteger messagecount = new AtomicInteger(0);

	public void testJsonTransmitter(){
		   // read from file
		BufferedReader reader=null;
        try {
        	File f = new File(jsonTestFile);
            reader = new BufferedReader(new FileReader(f));
            String line = "";
            while((line = reader.readLine()) != null) {
            	LOG.debug("transmitting message no "+messagecount.incrementAndGet()
            			+ " :" + line);
            	transmitString(line);
            }
        }
        catch(IOException e) {
        	LOG.error("error reading data " + e);
        } finally{
        	if (reader!=null )
				try {
					reader.close();
				} catch (IOException e) {}
        }
	}

	public void transmitString(String message){
		LOG.debug("Transmitting:"+message);
		CloseableHttpClient client=null;
		try {
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(1000).build();
			
			
			//CredentialsProvider provider = new BasicCredentialsProvider();
			//UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
			//provider.setCredentials(AuthScope.ANY, credentials);
			
			//client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(provider).build();
			//CloseableHttpClient client = HttpClients.createDefault();
			
			client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

			HttpPost httpPost = new HttpPost(serverUrl);

			httpPost.setEntity(new StringEntity(message));

			// add authentication
			if (userName!=null){
				UsernamePasswordCredentials creds =  new UsernamePasswordCredentials(userName, password);
				httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
			}

			// json
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			HttpResponse response = client.execute(httpPost);

			LOG.debug("Response Status Code:"+response.getStatusLine().getStatusCode()+ " Reason: "+response.getStatusLine().getReasonPhrase());


		} catch (Exception e) {
			LOG.debug("problem sending message:",e);
		} finally{
			if (client!=null) try { client.close(); } catch (IOException e) { }
		}

		LOG.debug("Ending Json Test");
	}

	public void readProperties(String propFileName){
		File propFile = new File(propFileName);
		LOG.debug("reading properties from file:"+propFile.getAbsolutePath());
		if (!propFile.canRead()) throw new RuntimeException("cannot read file"+propFileName);
		readProperties(propFile);
	}

	public void readProperties(File propFile){
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(propFile);
			prop.load(input);

			jsonTestFile=prop.getProperty("org.opennms.plugin.jsontest.test.jsonFileName",TEST_JSON_FILE);

			persistInterval = Long.parseLong(prop.getProperty("org.opennms.plugin.jsontest.message.persist.interval",DEFAULT_PERSIST_INTERVAL));

			serverUrl= prop.getProperty("org.opennms.plugin.jsontest.serverUrl",SERVER_URL);

			userName = prop.getProperty("org.opennms.plugin.jsontest.userName",OPENNMS_REST_USERNAME);

			password = prop.getProperty("org.opennms.plugin.jsontest.password",OPENNMS_REST_PASSWORD);

		} catch (IOException ex) {
			throw new RuntimeException("problem loading properties file:"+propFile.getName(),ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) { }
			}
		}
	}

	@Override
	public String toString() {
		return "JsonTransmitterTest [serverUrl=" + serverUrl
				+ ", userName=" + userName
				+ ", password=" + password
				+ ", persistInterval=" + persistInterval + ", useRepeatTimer="
				+ useRepeatTimer + ", useJsonFile=" + useJsonFile
				+ ", jsonTestFile=" + jsonTestFile + "]";
	}
}
