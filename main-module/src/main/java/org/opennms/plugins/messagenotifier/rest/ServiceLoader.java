/*
 * Copyright 2014 OpenNMS Group Inc., Entimoss ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opennms.plugins.messagenotifier.rest;


import java.util.concurrent.atomic.AtomicReference;

import org.opennms.plugins.messagenotifier.rest.MqttRxService;



/** 
 * Used to statically pass service references to Jersey ReST classes.
 * 
 * @author cgallen
 */
public class ServiceLoader {

	private static AtomicReference<MqttRxService> m_mqttRxService = new AtomicReference<>();
	

	public ServiceLoader(){
		super();
	}

	public ServiceLoader(MqttRxService mqttRxService){
		super();
		setMqttRxService(mqttRxService);
	}

	/**
	 * @return the mqttRxService
	 */
	public static MqttRxService getMqttRxService() {
		return m_mqttRxService.get();
	}

	/**
	 * @param mqttRxService the mqttRxService to set
	 */
	public static void setMqttRxService(MqttRxService mqttRxService) {
		m_mqttRxService.set(mqttRxService);
	}
	

}
