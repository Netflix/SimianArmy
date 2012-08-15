package com.netflix.simianarmy.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.netflix.simianarmy.aws.AWSClient;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VSphereClient extends AWSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereClient.class);

    public VSphereClient(BasicConfiguration config) {
        super(config.getStr("simianarmy.aws.accountKey"), config.getStr("simianarmy.aws.secretKey"), config.getStrOrElse("simianarmy.aws.region", "us-east-1"));
        this.vsphereUrl = config.getStr("client.vsphere.url");
        this.username = config.getStr("client.vsphere.username");
        this.password = config.getStr("client.vsphere.password");
    }
    
    private String username = null;
    private String password = null;
    private String vsphereUrl = null;

    @Override
    public List<AutoScalingGroup> describeAutoScalingGroups() {

        Map<String, AutoScalingGroup> groups = new HashMap<String, AutoScalingGroup>();
        try {
            ServiceInstance vmwareService = new ServiceInstance(new URL(vsphereUrl), username, password, true);

            Folder rootFolder = vmwareService.getRootFolder();
            ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
            if(mes==null || mes.length ==0)
            {
                LOGGER.info("### nichts von vmware gefunden");
                return new LinkedList<AutoScalingGroup>();
            }

            for (int i = 0; i < mes.length; i++) {
                VirtualMachine vm = (VirtualMachine) mes[i]; 

                String folderName = vm.getParent().getName();
                String instanceId = vm.getName();
                //String loctyp = instanceId.substring(0,6);
                
                LOGGER.debug("adding <"+instanceId+"> to group <"+folderName+">");
                addInstanceToGroup(groups, folderName, instanceId);
            }
            vmwareService.getServerConnection().logout();
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot connect to VMWare",e);
        } catch (MalformedURLException e) {
            throw new AmazonServiceException("cannot connect to VMWare",e);
        }
        
        return new ArrayList<AutoScalingGroup>(groups.values());
  }

    protected void addInstanceToGroup(Map<String, AutoScalingGroup> groups, String groupName, String instanceId) {
        AutoScalingGroup asg = groups.get(groupName);
        if (asg == null) {
            asg = new AutoScalingGroup();
            asg.setAutoScalingGroupName(groupName);
            groups.put(groupName, asg);
        }
        List<Instance> instances = asg.getInstances();
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
    }
    
    @Override
    public void terminateInstance(String instanceId) {
        LOGGER.info("IS24Client.terminateInstance() recreating "+instanceId);

    }
}
