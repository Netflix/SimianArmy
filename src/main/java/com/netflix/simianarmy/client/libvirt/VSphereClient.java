package com.netflix.simianarmy.client.libvirt;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VSphereClient extends AWSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereClient.class);

    public VSphereClient(BasicConfiguration config) {
        super(config.getStr("simianarmy.aws.accountKey"), config.getStr("simianarmy.aws.secretKey"), config.getStrOrElse("simianarmy.aws.region", "us-east-1"));
        this.url = config.getStr("client.vsphere.url");
        this.username = config.getStr("client.vsphere.username");
        this.password = config.getStr("client.vsphere.password");
    }
    
    private String username = null;
    private String password = null;
    private String url = null;
    private ServiceInstance service = null;

    @Override
    public List<AutoScalingGroup> describeAutoScalingGroups() {
        final VSphereGroups groups = new VSphereGroups();

        try {
            connectService();
            ManagedEntity[] mes = describeVirtualMachines();
            
            for (int i = 0; i < mes.length; i++) {
                VirtualMachine vm = (VirtualMachine) mes[i]; 
                String instanceId = vm.getName();
                String groupName = vm.getParent().getName();

                groups.addInstance(instanceId, groupName);
            }
        } 
        finally {
            disconnectService();
        }

        return groups.asList();
    }

    private ManagedEntity[] describeVirtualMachines() {
        ManagedEntity[] mes = null;

        Folder rootFolder = service.getRootFolder();
        try {
            mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        } catch (InvalidProperty e) {
            throw new AmazonServiceException("cannot query VSphere",e);
        } catch (RuntimeFault e) {
            throw new AmazonServiceException("cannot query VSphere",e);
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot query VSphere",e);
        }

        if(mes == null || mes.length == 0) {
            throw new AmazonServiceException("vsphere returned zero entities of type \"VirtualMachine\"");
        }
        else {
            return mes;
        }
    }

    private void disconnectService() {
        if (service != null) {
            service.getServerConnection().logout();
            service = null;
        }
    }

    private void connectService() throws AmazonServiceException {
        try {
            service = new ServiceInstance(new URL(url), username, password, true);
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot connect to VSphere",e);
        } catch (MalformedURLException e) {
            throw new AmazonServiceException("cannot connect to VSphere",e);
        }
    }
        
    @Override
    public void terminateInstance(String instanceId) {
        LOGGER.info("VSphereClient.terminateInstance() recreating "+instanceId);
        // TODO IK
    }
}
