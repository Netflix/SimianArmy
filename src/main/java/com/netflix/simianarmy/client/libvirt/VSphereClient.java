package com.netflix.simianarmy.client.libvirt;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.client.aws.AWSClient;
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

        final VSphereGroups groups = new VSphereGroups();
        ManagedEntity[] mes = describeVirtualMachines();
        
        if(mes==null || mes.length ==0) {
            LOGGER.info("vsphere returned zero entities of type \"VirtualMachine\"");
            return groups.emptyList();
        }

        for (int i = 0; i < mes.length; i++) {
            VirtualMachine vm = (VirtualMachine) mes[i]; 

            String folderName = vm.getParent().getName();
            String instanceId = vm.getName();
            
            groups.addInstance(instanceId, folderName);
        }
        return groups.asList();
    }

    protected ManagedEntity[] describeVirtualMachines() {
        ManagedEntity[] mes = null;
        try {
            ServiceInstance vsphereService = new ServiceInstance(new URL(vsphereUrl), username, password, true);

            Folder rootFolder = vsphereService.getRootFolder();
            mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
            vsphereService.getServerConnection().logout();
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot connect to VSphere",e);
        } catch (MalformedURLException e) {
            throw new AmazonServiceException("cannot connect to VSphere",e);
        }
        return mes;
    }
        
    @Override
    public void terminateInstance(String instanceId) {
        LOGGER.info("VSphereClient.terminateInstance() recreating "+instanceId);
        // TODO IK
    }
}
