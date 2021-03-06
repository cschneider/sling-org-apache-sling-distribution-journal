/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.journal.impl.subscriber;

import org.apache.sling.distribution.journal.impl.shared.TestMessageInfo;
import org.apache.sling.distribution.journal.impl.shared.Topics;
import org.apache.sling.distribution.journal.messages.Messages.PackageStatusMessage;
import org.apache.sling.distribution.journal.messages.Messages.PackageStatusMessage.Status;
import org.apache.sling.distribution.journal.HandlerAdapter;
import org.apache.sling.distribution.journal.MessageHandler;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.Closeable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageStatusWatcherTest {


    final static String TOPIC_NAME = Topics.STATUS_TOPIC;

    private static final String SUB1_SLING_ID = "sub1sling";
    private static final String SUB1_AGENT_NAME = "sub1agent";

    private static final String PUB1_AGENT_NAME = "pub1agent";

    @Mock
    MessagingProvider provider;

    @Spy
    Topics topics = new Topics();

    @Captor
    private ArgumentCaptor<HandlerAdapter<PackageStatusMessage>> adapterCaptor;

    PackageStatusWatcher statusWatcher;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(provider.createPoller(
                eq(TOPIC_NAME),
                eq(Reset.earliest),
                adapterCaptor.capture()))
                .thenReturn(mock(Closeable.class));

        statusWatcher = new PackageStatusWatcher(provider, topics, SUB1_AGENT_NAME);

    }


    @Test
    public void testStatusWatcher() {

        generateMessages(10, 50);


        assertPackageStatus(1000, null);
        assertPackageStatus(1010, Status.REMOVED_FAILED);


        statusWatcher.clear(1011);

        assertPackageStatus(1010, null);
        assertPackageStatus(1011, Status.REMOVED_FAILED);
        assertPackageStatus(1049, Status.REMOVED_FAILED);
        assertPackageStatus(1050, null);


        generateMessages(50, 60);

        assertPackageStatus(1011, Status.REMOVED_FAILED);
        assertPackageStatus(1050, Status.REMOVED_FAILED);
        assertPackageStatus(1060, null);

        statusWatcher.clear(1100);

        assertPackageStatus(1050, null);





    }


    void generateMessages(int begin, int end) {
        MessageHandler<PackageStatusMessage> handler = adapterCaptor.getValue().getHandler();
        for (int i=begin; i<end; i++) {
            handler.handle(new TestMessageInfo(TOPIC_NAME, 0, i, 0l),
                    createStatusMessage(i));
        }
    }

    PackageStatusMessage createStatusMessage(int i) {
        PackageStatusMessage pkgStatMsg = PackageStatusMessage.newBuilder()
                .setSubSlingId(SUB1_SLING_ID)
                .setSubAgentName(SUB1_AGENT_NAME)
                .setPubAgentName(PUB1_AGENT_NAME)
                .setOffset(1000 + i)
                .setStatus(PackageStatusMessage.Status.REMOVED_FAILED)
                .build();

        return pkgStatMsg;

    }

    void assertPackageStatus(long pkgOffset, Status status) {
        if (status == null) {
            assertEquals(null, statusWatcher.getStatus(pkgOffset));
            assertEquals(null, statusWatcher.getStatusOffset(pkgOffset));
        } else {
            assertEquals(status, statusWatcher.getStatus(pkgOffset));
            assertEquals(pkgOffset-1000, (long) statusWatcher.getStatusOffset(pkgOffset));
        }
    }


}
