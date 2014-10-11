// CHECKSTYLE IGNORE Javadoc
/*
 *
 *  Copyright 2012 Netflix, Inc.
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
 *
 */
package com.netflix.simianarmy.client.vsphere.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.vsphere.VSphereClient;
import com.netflix.simianarmy.client.vsphere.VSphereFolderGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.EnumSet;
import java.util.Properties;
import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class TestVFGChaosCrawler {
    private VSphereFolderGroup mkVfg(String vfgName, String instanceId) {
        VSphereFolderGroup vfg = new VSphereFolderGroup(vfgName);
        vfg.addInstance(instanceId);
        return vfg;
    }

    @Test
    public void testGroupTypes() {
        VFGChaosCrawler crawler = new VFGChaosCrawler(null, null);
        EnumSet<?> types = crawler.groupTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "VFG");
    }

    @Test
    public void testGroups() {
        VSphereClient clientMock = mock(VSphereClient.class);
        Properties properties = new Properties();
        properties.setProperty("simianarmy.client.vsphere.crawler.groups", "DC1,DC2");
        MonkeyConfiguration config = new BasicConfiguration(properties);
        VFGChaosCrawler crawler = new VFGChaosCrawler(clientMock, config);
        List<VSphereFolderGroup> vfgList = new ArrayList<VSphereFolderGroup>();
        vfgList.add(mkVfg("DC1", "vm-0001"));
        vfgList.add(mkVfg("DC2", "vm-0002"));

        when(clientMock.describeVsphereGroups("DC1", "DC2")).thenReturn(vfgList);

        List<InstanceGroup> groups = crawler.groups();

        verify(clientMock, times(1)).describeVsphereGroups("DC1", "DC2");

        Assert.assertEquals(groups.size(), 2);

        Assert.assertEquals(groups.get(0).type(), VFGChaosCrawler.Types.VFG);
        Assert.assertEquals(groups.get(0).name(), "DC1");
        Assert.assertEquals(groups.get(0).instances().size(), 1);
        Assert.assertEquals(groups.get(0).instances().get(0), "vm-0001");

        Assert.assertEquals(groups.get(1).type(), VFGChaosCrawler.Types.VFG);
        Assert.assertEquals(groups.get(1).name(), "DC2");
        Assert.assertEquals(groups.get(1).instances().size(), 1);
        Assert.assertEquals(groups.get(1).instances().get(0), "vm-0002");
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = ".*simianarmy.client.vsphere.crawler.groups must be defined as.*")
    public void testGroupsNotConfigured() {
        VSphereClient clientMock = mock(VSphereClient.class);
        Properties properties = new Properties();
        MonkeyConfiguration config = new BasicConfiguration(properties);
        VFGChaosCrawler crawler = new VFGChaosCrawler(clientMock, config);
        crawler.groups();
    }
}
