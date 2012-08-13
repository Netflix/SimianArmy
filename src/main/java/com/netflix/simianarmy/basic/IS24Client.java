package com.netflix.simianarmy.basic;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.netflix.simianarmy.aws.AWSClient;
import com.vmware.vim25.VirtualMachineCapability;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class IS24Client extends AWSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(IS24Client.class);

    public IS24Client(String accessKey, String secretKey, String region) {
		super(accessKey, secretKey, region);
	}
    
    private int count = 0;
    
    @Override
    public List<AutoScalingGroup> describeAutoScalingGroups() {
		LOGGER.info("IS24Client.describeAutoScalingGroups()");
        List<AutoScalingGroup> groups = new LinkedList<AutoScalingGroup>();
        
        count++;
        
		if (count > 0) createGroup(groups, "devranZ", 2, 3);
		if (count > 0) createGroup(groups, "devbonZ", 1, 2);
		if (count > 1) createGroup(groups, "devapiZ", 2, 3);
		if (count > 1) createGroup(groups, "devikrZ", 3, 4);
		if (count > 2) createGroup(groups, "devwebZ", 4, 5);
		if (count > 2) createGroup(groups, "devappZ", 5, 6);
		if (count > 3) createGroup(groups, "devmplZ", 6, 7);
        
        return groups;
  }

	protected void createGroup(List<AutoScalingGroup> groups, String groupName,
			int from, int till) {
		AutoScalingGroup asg = new AutoScalingGroup();
		List<Instance> instances = new LinkedList<Instance>();
		asg.setAutoScalingGroupName(groupName);
		for (int i = from; i <= till; i++) {
			Instance instance = new Instance();
			String instanceId = groupName+"00"+i;
			LOGGER.info(instanceId);
			instance.setInstanceId(instanceId);
			instances.add(instance);
		}
		asg.setInstances(instances);
		groups.add(asg);
	}
	
	@Override
	public void terminateInstance(String instanceId) {
		LOGGER.info("IS24Client.terminateInstance() sprengen....");

		String username = "devvcs01_lm";
		String password = "BfMCr$FL9C12";
		String vsphereUrl = "https://devvcs01.iscout.local/sdk";
		try {
			ServiceInstance si = new ServiceInstance(new URL(vsphereUrl), username, password, true);

			Folder rootFolder = si.getRootFolder();
			String name = rootFolder.getName();
			System.out.println("root:" + name);
			ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
			if(mes==null || mes.length ==0)
			{
				return;
			}
			
			VirtualMachine vm = (VirtualMachine) mes[0]; 
			
			VirtualMachineConfigInfo vminfo = vm.getConfig();
			VirtualMachineCapability vmc = vm.getCapability();

			vm.getResourcePool();
			System.out.println("Hello " + vm.getName());
			System.out.println("GuestOS: " + vminfo.getGuestFullName());
			System.out.println("Multiple snapshot supported: " + vmc.isMultipleSnapshotsSupported());

			si.getServerConnection().logout();

		
		} catch (RemoteException e) {
			LOGGER.error("terminateInstance: cannot connect");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			LOGGER.error("terminateInstance: cannot connect");
			e.printStackTrace();
		}

	}
}
