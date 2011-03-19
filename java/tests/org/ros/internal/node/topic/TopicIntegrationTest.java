/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.topic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Before;
import org.junit.Test;
import org.ros.MessageListener;
import org.ros.internal.node.Node;
import org.ros.internal.node.NodeSocketAddress;
import org.ros.internal.node.RemoteException;
import org.ros.internal.node.server.MasterServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TopicIntegrationTest {

  private MasterServer masterServer;

  @Before
  public void setUp() throws URISyntaxException, XmlRpcException, IOException {
    masterServer = new MasterServer(NodeSocketAddress.createDefault(0));
    masterServer.start();
  }

  @Test
  public void testOnePublisherToOneSubscriber() throws URISyntaxException, RemoteException,
      IOException, InterruptedException, XmlRpcException {
    TopicDefinition topicDefinition =
        new TopicDefinition("/foo",
            MessageDefinition.createFromMessage(new org.ros.message.std_msgs.String()));

    Node publisherNode = Node.createPrivate("/publisher", masterServer.getUri(), 0, 0);
    Publisher<org.ros.message.std_msgs.String> publisher =
        publisherNode.createPublisher(topicDefinition, org.ros.message.std_msgs.String.class);

    Node subscriberNode = Node.createPrivate("/subscriber", masterServer.getUri(), 0, 0);
    Subscriber<org.ros.message.std_msgs.String> subscriber =
        publisherNode.createSubscriber(topicDefinition, org.ros.message.std_msgs.String.class);

    final org.ros.message.std_msgs.String helloMessage = new org.ros.message.std_msgs.String();
    helloMessage.data = "Hello, ROS!";

    final CountDownLatch messageReceived = new CountDownLatch(1);
    subscriber.addMessageListener(new MessageListener<org.ros.message.std_msgs.String>() {
      @Override
      public void onNewMessage(org.ros.message.std_msgs.String message) {
        assertEquals(helloMessage, message);
        messageReceived.countDown();
      }
    });

    // TODO(damonkohler): Ugly hack because we can't currently detect when the
    // servers have settled into their connections.
    Thread.sleep(100);

    publisher.publish(helloMessage);
    assertTrue(messageReceived.await(1, TimeUnit.SECONDS));
  }
}
