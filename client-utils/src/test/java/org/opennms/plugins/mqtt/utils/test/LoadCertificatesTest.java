/* ***************************************************************************
 * Copyright 2018 OpenNMS Group Inc, Entimoss Ltd. Or their affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ****************************************************************************/

package org.opennms.plugins.mqtt.utils.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;

import org.junit.Test;
import org.opennms.plugins.mqtt.utils.SampleUtil;
import org.opennms.plugins.mqtt.utils.SampleUtil.KeyStorePasswordPair;

import com.amazonaws.services.iot.client.util.AwsIotTlsSocketFactory;
import com.amazonaws.services.iot.client.util.IotConnectionException;

public class LoadCertificatesTest {

	@Test
	public void test() {
		String certificateFile ="src/test/resources/testcerts/opennmsclient1.cert.pem";
		String privateKeyFile = "src/test/resources/testcerts/opennmsclient1.private.key";
		String algorithm = null;

		KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

		String keyPassword = pair.keyPassword;
		KeyStore keyStore = pair.keyStore;
		
		// super(client, new AwsIotTlsSocketFactory(keyStore, keyPassword), "ssl://" + client.getClientEndpoint() + ":8883");

		try {
			
			AwsIotTlsSocketFactory socketFactory = new AwsIotTlsSocketFactory(keyStore, keyPassword);
			
			// see https://github.com/eclipse/paho.mqtt.java/blob/75594f0b226b14911f397b69059c18a50eaabb54/org.eclipse.paho.client.mqttv3/src/main/java/org/eclipse/paho/client/mqttv3/internal/TCPNetworkModule.java
			
			Socket socket = socketFactory.createSocket();
		} catch (IotConnectionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


