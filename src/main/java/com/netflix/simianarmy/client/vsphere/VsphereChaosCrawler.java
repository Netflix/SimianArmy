package com.netflix.simianarmy.client.vsphere;

import com.amazonaws.services.autoscaling.model.Instance;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

/**
 *  The Class VsphereChaosCrawler. This will crawl for all available virtual machines under folders
 *  defined with simianarmy.client.vsphere.crawler.groups.
 */
public class VsphereChaosCrawler implements ChaosCrawler{

    /** The vSphere client. */
    private final VSphereClient vSphereClient;
    /** The config. */
    private final MonkeyConfiguration config;

    public VsphereChaosCrawler(VSphereClient vSphereClient, MonkeyConfiguration config) {
        this.vSphereClient = vSphereClient;
        this.config = config;
    }

    /**
     * The group types Types.
     */
    public enum Types implements GroupType {

        /** only crawls vSphere folders. */
        VSPHERE;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<ChaosCrawler.InstanceGroup> groups() {
        String groups = config.getStrOrElse("simianarmy.client.vsphere.crawler.groups","");
        if ("".equals(groups)) {
            throw new RuntimeException("simianarmy.client.vsphere.crawler.groups must be defined as" +
                    " Format: Datacenter/folder/../folder,Datacenter/folder/../folder");
        }
        return groups(groups.split(","));
    }

    @Override
    public List<ChaosCrawler.InstanceGroup> groups(String... names) {
        List<ChaosCrawler.InstanceGroup> list = new LinkedList<ChaosCrawler.InstanceGroup>();
        for (VSphereGroup vSphereGroup : vSphereClient.describeVsphereGroups(names)) {
            ChaosCrawler.InstanceGroup ig = new BasicInstanceGroup(vSphereGroup.getGroupNme(), Types.VSPHERE, vSphereClient.region());
            for (Instance inst : vSphereGroup.getInstances()) {
                ig.addInstance(inst.getInstanceId());
            }
            list.add(ig);
        }
        return list;
    }
}
