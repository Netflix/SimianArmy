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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.chaos;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext.Notification;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext.SshAction;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestChaosMonkeyArmy {
    private File sshKey;

    @BeforeTest
    public void createSshKey() throws IOException {
        sshKey = File.createTempFile("tmp", "key");
        Files.write("fakekey", sshKey, Charsets.UTF_8);
        sshKey.deleteOnExit();
    }

    private TestChaosMonkeyContext runChaosMonkey(String key) {
        return runChaosMonkey(key, true);
    }

    private TestChaosMonkeyContext runChaosMonkey(String key, boolean burnMoney) {
        Properties properties = new Properties();
        properties.setProperty("simianarmy.chaos.enabled", "true");
        properties.setProperty("simianarmy.chaos.leashed", "false");
        properties.setProperty("simianarmy.chaos.TYPE_A.enabled", "true");
        properties.setProperty("simianarmy.chaos.notification.global.enabled", "true");

        properties.setProperty("simianarmy.chaos.burnmoney", Boolean.toString(burnMoney));

        properties.setProperty("simianarmy.chaos.shutdowninstance.enabled", "false");
        properties.setProperty("simianarmy.chaos." + key.toLowerCase() + ".enabled", "true");

        properties.setProperty("simianarmy.chaos.ssh.key", sshKey.getAbsolutePath());

        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(properties);

        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        return ctx;
    }

    private void checkSelected(TestChaosMonkeyContext ctx) {
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
    }

    private void checkNotifications(TestChaosMonkeyContext ctx, String key) {
        List<Notification> notifications = ctx.getGloballyNotifiedList();
        Assert.assertEquals(notifications.size(), 2);
        Assert.assertEquals(notifications.get(0).getInstance(), "0:i-123456789012345670");
        Assert.assertEquals(notifications.get(0).getChaosType().getKey(), key);
        Assert.assertEquals(notifications.get(1).getInstance(), "1:i-123456789012345671");
        Assert.assertEquals(notifications.get(1).getChaosType().getKey(), key);
    }

    private void checkSshActions(TestChaosMonkeyContext ctx, String key) {
        List<SshAction> sshActions = ctx.getSshActions();
        Assert.assertEquals(sshActions.size(), 4);

        Assert.assertEquals(sshActions.get(0).getMethod(), "put");
        Assert.assertEquals(sshActions.get(0).getInstanceId(), "0:i-123456789012345670");

        // We require that each script include the name of the chaos type
        // This makes testing easier, and also means the scripts show where they came from
        Assert.assertTrue(sshActions.get(0).getContents().toLowerCase().contains(key.toLowerCase()));

        Assert.assertEquals(sshActions.get(1).getMethod(), "exec");
        Assert.assertEquals(sshActions.get(1).getInstanceId(), "0:i-123456789012345670");

        Assert.assertEquals(sshActions.get(2).getMethod(), "put");
        Assert.assertEquals(sshActions.get(2).getInstanceId(), "1:i-123456789012345671");
        Assert.assertTrue(sshActions.get(2).getContents().contains(key));

        Assert.assertEquals(sshActions.get(3).getMethod(), "exec");
        Assert.assertEquals(sshActions.get(3).getInstanceId(), "1:i-123456789012345671");
    }

    @Test
    public void testShutdownInstance() {
        String key = "ShutdownInstance";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);

        checkNotifications(ctx, key);

        List<String> terminated = ctx.terminated();
        Assert.assertEquals(terminated.size(), 2);
        Assert.assertEquals(terminated.get(0), "0:i-123456789012345670");
        Assert.assertEquals(terminated.get(1), "1:i-123456789012345671");
    }

    @Test
    public void testBlockAllNetworkTraffic() {
        String key = "BlockAllNetworkTraffic";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);

        checkNotifications(ctx, key);

        List<String> cloudActions = ctx.getCloudActions();
        Assert.assertEquals(cloudActions.size(), 3);
        Assert.assertEquals(cloudActions.get(0), "createSecurityGroup:0:i-123456789012345670:blocked-network");
        Assert.assertEquals(cloudActions.get(1), "setInstanceSecurityGroups:0:i-123456789012345670:sg-1");
        Assert.assertEquals(cloudActions.get(2), "setInstanceSecurityGroups:1:i-123456789012345671:sg-1");
    }

    @Test
    public void testDetachVolumes() {
        String key = "DetachVolumes";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);

        checkNotifications(ctx, key);

        List<String> cloudActions = ctx.getCloudActions();
        Assert.assertEquals(cloudActions.size(), 4);
        Assert.assertEquals(cloudActions.get(0), "detach:0:i-123456789012345670:volume-1");
        Assert.assertEquals(cloudActions.get(1), "detach:0:i-123456789012345670:volume-2");
        Assert.assertEquals(cloudActions.get(2), "detach:1:i-123456789012345671:volume-1");
        Assert.assertEquals(cloudActions.get(3), "detach:1:i-123456789012345671:volume-2");
    }

    @Test
    public void testBurnCpu() {
        String key = "BurnCpu";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testBurnIo() {
        String key = "BurnIO";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testBurnIoWithoutBurnMoney() {
        String key = "BurnIO";

        TestChaosMonkeyContext ctx = runChaosMonkey(key, false);

        checkSelected(ctx);

        List<Notification> notifications = ctx.getGloballyNotifiedList();
        Assert.assertEquals(notifications.size(), 0);

        List<SshAction> sshActions = ctx.getSshActions();
        Assert.assertEquals(sshActions.size(), 0);
    }

    @Test
    public void testFillDisk() {
        String key = "FillDisk";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testFillDiskWithoutBurnMoney() {
        String key = "FillDisk";

        TestChaosMonkeyContext ctx = runChaosMonkey(key, false);

        checkSelected(ctx);

        List<Notification> notifications = ctx.getGloballyNotifiedList();
        Assert.assertEquals(notifications.size(), 0);

        List<SshAction> sshActions = ctx.getSshActions();
        Assert.assertEquals(sshActions.size(), 0);
    }


    @Test
    public void testFailDns() {
        String key = "FailDns";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testFailDynamoDb() {
        String key = "FailDynamoDb";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testFailEc2() {
        String key = "FailEc2";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testFailS3() {
        String key = "FailS3";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testKillProcess() {
        String key = "KillProcesses";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testNetworkCorruption() {
        String key = "NetworkCorruption";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testNetworkLatency() {
        String key = "NetworkLatency";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testNetworkLoss() {
        String key = "NetworkLoss";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

    @Test
    public void testNullRoute() {
        String key = "NullRoute";

        TestChaosMonkeyContext ctx = runChaosMonkey(key);

        checkSelected(ctx);
        checkNotifications(ctx, key);
        checkSshActions(ctx, key);
    }

}
