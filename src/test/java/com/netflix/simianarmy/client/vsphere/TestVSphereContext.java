/*
 *  Copyright 2012 Immobilien Scout GmbH
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
 */
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.client.vsphere;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestVSphereContext {
    @Test
    public void shouldSetClientOfCorrectType() {
        VSphereContext context = new VSphereContext();
        AWSClient awsClient = context.awsClient();
        assertNotNull(awsClient);
        assertTrue(awsClient instanceof VSphereClient);
    }
}
