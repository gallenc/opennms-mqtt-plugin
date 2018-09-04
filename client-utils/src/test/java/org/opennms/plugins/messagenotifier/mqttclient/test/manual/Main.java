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

package org.opennms.plugins.messagenotifier.mqttclient.test.manual;

import java.io.File;

public class Main {

	// if running in eclipse use vm argument ./src/test/resources/TestData/mqttclienttest.properties";
	public static void main(String[] args) {
		System.out.println("mqtt tester starting up");
		if (args.length==0) {
			System.out.println("please supply path and name of properties file");
			System.exit(0);
		}
		MQTTJsonTransmitterTest mqttTest = new MQTTJsonTransmitterTest();

		String filename=null;
		try {
			filename = args[0];
			File f = new File(filename);
			System.out.println("loading properties file: "+f.getAbsolutePath());
			mqttTest.readProperties(filename);

		} catch (Exception ex){
			System.out.println("cannot load properties file: "+filename);
			ex.printStackTrace(System.out);
			System.exit(0);
		}
		System.out.println("loaded the following properties from supplied filename: "+filename+"\n"+mqttTest.toString());
		System.out.println("starting transmitter use ctrl-c to exit");
		mqttTest.testMqttJsonTransmitter();
		System.out.println("mqtt tester shutting down");
	}

}
