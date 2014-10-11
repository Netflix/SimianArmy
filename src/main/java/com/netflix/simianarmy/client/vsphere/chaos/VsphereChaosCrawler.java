package com.netflix.simianarmy.client.vsphere.chaos;

import com.amazonaws.services.autoscaling.model.Instance;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.vsphere.VSphereClient;
import com.netflix.simianarmy.client.vsphere.VSphereFolderGroup;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

/**
 *  The Class VSphereChaosCrawler. This will crawl for all available virtual machines under folders
 *  defined by configuration simianarmy.client.vsphere.crawler.groups.
 */
public class VSphereChaosCrawler implements ChaosCrawler{

    /** The vSphere client. */
    private final VSphereClient client;
    /** The config. */
    private final MonkeyConfiguration config;

    public VSphereChaosCrawler(VSphereClient client, MonkeyConfiguration config) {
        this.client = client;
        this.config = config;
    }

    /**
     * The group types Types.
     */
    public enum Types implements GroupType {

        /** only crawls Virtual Machines under target vSphere folders. */
        VFG;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

    @Override
    /**
     * Create a list of groups defined by simianarmy.client.vsphere.crawler.groups.
     * @return a list of groups
     * @throws RuntimeException If there is no fold specified by configuration simianarmy.client.vsphere.crawler.groups
     */
    public List<ChaosCrawler.InstanceGroup> groups() {
        String groups = config.getStrOrElse("simianarmy.client.vsphere.crawler.groups","");
        if ("".equals(groups.trim())) {
            throw new RuntimeException("simianarmy.client.vsphere.crawler.groups must be defined as" +
                    " Format: Datacenter/folder/../folder,Datacenter/folder/../folder");
        }
        return groups(groups.split(","));
    }

    @Override
    /**
     * Create a list of groups by group name. The group name is vSphere absolute folder path and this crawls all
     * virtual machines directly under this folder and welds them into a group.
     *
     * @param names the group names.
     * @return a list of groups
     */
    public List<ChaosCrawler.InstanceGroup> groups(String... names) {
        List<ChaosCrawler.InstanceGroup> list = new LinkedList<ChaosCrawler.InstanceGroup>();
        for (VSphereFolderGroup vSphereFolderGroup : client.describeVsphereGroups(names)) {
            ChaosCrawler.InstanceGroup ig = new BasicInstanceGroup(vSphereFolderGroup.getGroupName(), Types.VFG,
                    client.region());
            for (Instance inst : vSphereFolderGroup.getInstances()) {
                ig.addInstance(inst.getInstanceId());
            }
            list.add(ig);
        }
        return list;
    }
}
