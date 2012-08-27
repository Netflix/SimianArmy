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
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

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
 * This client describes the AutoScalingGroup's and can terminate Instance's that
 * are hosted in a VSphere Center.
 * 
 * @author ingmar.krusch@immobilienscout24.de
 */
public class VSphereClient extends AWSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereClient.class);
    private static final String ATTRIBUTE_CHAOS_MONKEY = "ChaosMonkey";

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
                VirtualMachine virtualMachine = (VirtualMachine) mes[i]; 
                String instanceId = virtualMachine.getName();
                String groupName = virtualMachine.getParent().getName();
                
//                if (! instanceId.equals("devmnk001"))
//                    continue;

                Boolean optIn = getOptInByAttribute(virtualMachine);
                if (optIn == null || optIn) {
                    groups.addInstance(instanceId, groupName);
                }
                else {
                    LOGGER.debug("VirtualMachine opted out by VM-Attribute: " + instanceId);
                }
            }
        } 
        finally {
            disconnectService();
        }

        return groups.asList();
    }

    private Boolean getOptInByAttribute(VirtualMachine virtualMachine) {
        String optInAttribute = getChaosMonkeyAttributeValue(virtualMachine);
        boolean optInAttributeIsSet = (optInAttribute != null && ! "".equals(optInAttribute.trim()));
        Boolean optIn = null;
        if (optInAttributeIsSet) {
            optIn = Boolean.valueOf(optInAttribute);
        }
        return optIn;
    }

    private String getChaosMonkeyAttributeValue(VirtualMachine virtualMachine) throws AmazonServiceException {
        try {
            for (CustomFieldDef fieldDef : virtualMachine.getAvailableField()) {
              if (ATTRIBUTE_CHAOS_MONKEY.equals(fieldDef.getName())) {
                CustomFieldValue[] customFieldValues = virtualMachine.getCustomValue();
                if (customFieldValues == null) {
                    continue;
                }
                for (CustomFieldValue customFieldValue : customFieldValues) {
                  if (customFieldValue.getKey() == fieldDef.getKey()) {
                    CustomFieldStringValue stringValue = (CustomFieldStringValue) customFieldValue;
                    return stringValue.getValue();
                  }
                }

                break;
              }
            }
        } catch (Exception e) {
            throw new AmazonServiceException("cannot read property from virtual machine " +virtualMachine.getName(),e);
        }

        return null;
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
            if (service == null) {
                service = new ServiceInstance(new URL(url), username, password, true);
            }
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

//        try {
//            connectService();
//            
//            this.service.getOptionManager().getPropertyByPath("TODO IK");
//            
            return;
//        } 
//        finally {
//            disconnectService();
//        }
    }
}
