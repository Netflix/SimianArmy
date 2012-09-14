// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.client.vsphere;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.netflix.simianarmy.basic.BasicConfiguration;

/*
 *  Copyright 2012 Immobilienscout GmbH
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestVSphereServiceConnection {
    // private ServiceInstance serviceMock = mock(ServiceInstance.class);
    private BasicConfiguration configMock = mock(BasicConfiguration.class);

    @Test
    public void shouldReturnConfiguredPropertiesAfterConstructedFromConfig() {
        when(configMock.getStr("client.vsphere.username")).thenReturn("configured username");
        when(configMock.getStr("client.vsphere.password")).thenReturn("configured password");
        when(configMock.getStr("client.vsphere.url")).thenReturn("configured url");

        VSphereServiceConnection service = new VSphereServiceConnection(configMock);

        assertEquals(service.getUsername(), "configured username");
        assertEquals(service.getPassword(), "configured password");
        assertEquals(service.getUrl(), "configured url");
    }
}
