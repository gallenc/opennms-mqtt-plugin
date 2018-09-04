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

package org.opennms.plugins.messagenotifier;

import java.util.List;

/**
 * Client interface used to register with list of MessageChangeNotifiers to receive MessageNotifications
 * @author admin
 *
 */
public interface MessageNotificationClient extends NotificationClient {

	public List<MessageNotifier> getIncommingMessageNotifiers();

	public void setIncommingMessageNotifiers(List<MessageNotifier> messageNotifiers);
	
}
