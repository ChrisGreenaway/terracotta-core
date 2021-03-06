/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import org.slf4j.Logger;

import com.tc.async.api.StageManager;
import com.tc.util.ProductID;
import com.tc.management.TCClient;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForL1;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.cluster.ClusterInternalEventsGun;

import java.util.Map;


public class StandardClientBuilder implements ClientBuilder {
  
  private final ProductID typeOfClient;

  public StandardClientBuilder(ProductID product) {
    this.typeOfClient = product;
  }
  
  @Override
  public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                         SessionProvider sessionProvider, 
                                                         int socketConnectTimeout, TCClient client) {
    return commMgr.createClientChannel(typeOfClient, sessionProvider, socketConnectTimeout);
  }

  @Override
  public CommunicationsManager createCommunicationsManager(MessageMonitor monitor, TCMessageRouter messageRouter,
                                                           NetworkStackHarnessFactory stackHarnessFactory,
                                                           ConnectionPolicy connectionPolicy, int commThread,
                                                           HealthCheckerConfig aConfig,
                                                           Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping,
                                                           ReconnectionRejectedHandler reconnectionRejectedHandler,
                                                           TCSecurityManager securityManager) {
    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_CLIENT, monitor, messageRouter, stackHarnessFactory, null,
                                         connectionPolicy, 0, aConfig, new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping,
                                         reconnectionRejectedHandler, securityManager);
  }

  @Override
  public ClientHandshakeManager createClientHandshakeManager(Logger logger,
                                                             ClientHandshakeMessageFactory chmf, 
                                                             SessionManager sessionManager,
                                                             ClusterInternalEventsGun clusterEventsGun, 
                                                             String uuid, 
                                                             String name, 
                                                             String clientVersion,
                                                             ClientEntityManager entity) {
    return new ClientHandshakeManagerImpl(logger, chmf, sessionManager, clusterEventsGun, uuid, name, clientVersion, entity);
  }

  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel, StageManager stages) {
    return new ClientEntityManagerImpl(channel, stages);
  }

}
