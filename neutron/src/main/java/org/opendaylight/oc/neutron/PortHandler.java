/*
 * Copyright (C) 2014 Juniper Networks, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
*/

package org.opendaylight.opencontrail.neutron;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Port.
 */

public class PortHandler implements INeutronPortAware {
/**
* Logger instance.
*/
   static final Logger LOGGER = LoggerFactory.getLogger(PortHandler.class);
   static ApiConnector apiConnector;

   /**
    * Invoked when a port creation is requested to check if the specified
    * Port can be created and then creates the port
    *
    * @param NeutronPort
    *             An instance of new Neutron Port object.
    *
    * @return A HTTP status code to the creation request.
    */
   @Override
   public int canCreatePort(NeutronPort neutronPort) {
      if (neutronPort == null ){
           LOGGER.error("NeutronPort object can't be null..");
           return HttpURLConnection.HTTP_BAD_REQUEST;
      }

      if(neutronPort.getDeviceID()==null || neutronPort.getDeviceID().equals("")|| neutronPort.getID().equals("")) {
          LOGGER.error("Port Device Id or Port Uuid can't be empty/null...");
          return HttpURLConnection.HTTP_BAD_REQUEST;
      }

      if(neutronPort.getName() == null || neutronPort.getName().equals("")) {
          LOGGER.error("Port name can't be empty/null...");
          return HttpURLConnection.HTTP_BAD_REQUEST;
      }

      List<Neutron_IPs> ips = neutronPort.getFixedIPs();
      if(ips==null) {
          LOGGER.warn("Neutron Fixed Ips can't be null..");
          return HttpURLConnection.HTTP_FORBIDDEN;
      }

      apiConnector = Activator.apiConnector;
      try{
         return createPort(neutronPort);
      } catch (Exception e) {
          LOGGER.error("exception :   ",e);
          return HttpURLConnection.HTTP_INTERNAL_ERROR;
      }
   }


   /**
    * Invoked to create the specified Neutron port.
    *
    * @param network
            *      An instance of new Neutron Port object.
            *
    * @return A HTTP status code to the creation request.
    */
   private int createPort(NeutronPort neutronPort) {
        String networkID = neutronPort.getNetworkUUID();
        String portID = neutronPort.getID();
        String portDesc = neutronPort.getName();
        String deviceID = neutronPort.getDeviceID();
        String portMACAddress = neutronPort.getMacAddress();
        VirtualMachineInterface virtualMachineInterface=null;
        VirtualMachine virtualMachine=null;
        VirtualNetwork virtualNetwork=null;
        MacAddressesType macAddressesType=new MacAddressesType();
        try {
        networkID=UUID.fromString(neutronPort.getNetworkUUID()).toString();
        portID = UUID.fromString(neutronPort.getID()).toString();
        deviceID = UUID.fromString(neutronPort.getDeviceID()).toString();
        }catch(Exception ex) {
            LOGGER.error("exception :   ",ex);
            return HttpURLConnection.HTTP_BAD_REQUEST;
            }
        try {
            LOGGER.info("portId:    "+portID);
            virtualMachineInterface= (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portID);
            if(virtualMachineInterface != null){
                LOGGER.error("Port already exist...");
                return HttpURLConnection.HTTP_FORBIDDEN;
                }
            else {
                virtualMachine=(VirtualMachine)apiConnector.findById(VirtualMachine.class,deviceID);
                if(virtualMachine == null){
                    virtualMachine =new VirtualMachine();
                    virtualMachine.setName(deviceID);
                    virtualMachine.setUuid(deviceID);
                    boolean virtualMachineCreated=apiConnector.create(virtualMachine);
                    if(!virtualMachineCreated){
                        LOGGER.warn("virtualMachine creation failed..");
                        return HttpURLConnection.HTTP_INTERNAL_ERROR;
                    }
                    LOGGER.info("virtualMachine : " + virtualMachine.getName() +
                            "  having UUID : " + virtualMachine.getUuid() +
                            "  sucessfully created...");
                }
                virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkID);
                if(virtualNetwork == null){
                     LOGGER.warn("Specfied Network does not exist for port creation..");
                     return HttpURLConnection.HTTP_BAD_REQUEST;
                }
                else{
                     virtualMachineInterface=new VirtualMachineInterface();
                     virtualMachineInterface.setUuid(portID);
                     virtualMachineInterface.setName(portDesc);
                     virtualMachineInterface.setParent(virtualMachine);
                     virtualMachineInterface.setVirtualNetwork(virtualNetwork);
                     macAddressesType.addMacAddress(portMACAddress);
                     virtualMachineInterface.setMacAddresses(macAddressesType);
                     boolean virtualMachineInterfaceCreated=apiConnector.create(virtualMachineInterface);
                     if(!virtualMachineInterfaceCreated){
                         LOGGER.warn("virtualMachineInterface creation failed..");
                         return HttpURLConnection.HTTP_INTERNAL_ERROR;
                     }
                     LOGGER.info("virtualMachineInterface : " + virtualMachineInterface.getName() +
                        "  having UUID : " + virtualMachineInterface.getUuid() +
                        "  sucessfully created...");
                }
            }
            INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
            NeutronSubnet subnet = null;
            List<Neutron_IPs> ips = neutronPort.getFixedIPs();
            InstanceIp instanceIp=new InstanceIp();
            String instaneIpUuid = UUID.randomUUID().toString();
            for(Neutron_IPs ipValues : ips ) {
                if(ipValues.getIpAddress() == null){
            subnet = systemCRUD.getSubnet(ipValues.getSubnetUUID());
            instanceIp.setAddress(subnet.getLowAddr());
                }
                else{
                    instanceIp.setAddress(ipValues.getIpAddress());
                }
                }
            instanceIp.setName(instaneIpUuid);
            instanceIp.setUuid(instaneIpUuid);
            instanceIp.setParent(virtualMachineInterface);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setVirtualNetwork(virtualNetwork);
            boolean instanceIpCreated = apiConnector.create(instanceIp);
            if(!instanceIpCreated){
                LOGGER.warn("instanceIp addition failed..");
                return HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
            LOGGER.info("Instance IP added sucessfully...");
            return HttpURLConnection.HTTP_OK;
        }catch(IOException ie){
            LOGGER.error("IOException :    ",ie);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
   }
   /**
    * Invoked to take action after a port has been created.
    *
    * @param network
    *            An instance of new Neutron port object.
    */
   @Override
   public void neutronPortCreated(NeutronPort neutronPort) {
       VirtualMachineInterface virtualMachineInterface = null;
       try{
          virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID());
          if(virtualMachineInterface != null) {
            LOGGER.info("Port creation verified....");
           }
          else{
            LOGGER.info("Port creation failed....");
          }
       }
       catch(Exception e) {
            LOGGER.error("Exception :    "+e);
       }
   }


  /**
   * Invoked when a port deletion is requested to check if the specified
   * Port can be deleted and then deletes the port
   *
   * @param NeutronPort
   *           An instance of proposed Neutron Port object.
   *
   * @return A HTTP status code to the deletion request.
   */
  @Override
  public int canDeletePort(NeutronPort neutronPort) {
      if (neutronPort == null) {
          LOGGER.info("Port object can't be null...");
          return HttpURLConnection.HTTP_BAD_REQUEST;
      }
      apiConnector = Activator.apiConnector;
      try {
          return deletePort(neutronPort);
      } catch (Exception e) {
           LOGGER.error("exception :   ",e);
           return HttpURLConnection.HTTP_INTERNAL_ERROR;
      }
  }


  /**
   * Invoked to delete the specified Neutron port.
   *
   * @param network
   *         An instance of new Neutron Port object.
   *
   * @return A HTTP status code to the deletion request.
   */
  private int deletePort(NeutronPort neutronPort) {
     String portID = neutronPort.getID();
     VirtualMachineInterface virtualMachineInterface=null;
     InstanceIp instanceIP=null;
     try {
         virtualMachineInterface= (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portID);
         if(virtualMachineInterface == null) {
             LOGGER.info("Specified port does not exist...");
             return HttpURLConnection.HTTP_BAD_REQUEST;
         }
         else {
             List<ObjectReference<ApiPropertyBase>> instanceIPs= virtualMachineInterface.getInstanceIpBackRefs();
            if(instanceIPs !=null){
                for (ObjectReference <ApiPropertyBase> ref : instanceIPs) {
                    String instanceIPUUID = ref.getUuid();
                    if(instanceIPUUID != null) {
                        instanceIP=(InstanceIp)apiConnector.findById(InstanceIp.class,instanceIPUUID);
                        apiConnector.delete(instanceIP);
                    }
                }
            }
            apiConnector.delete(virtualMachineInterface);
            LOGGER.info("Specified port deleted sucessfully...");
            return HttpURLConnection.HTTP_OK;
         }
     } catch (IOException io) {
         LOGGER.error("Exception  :   "+io);
         return HttpURLConnection.HTTP_INTERNAL_ERROR;
     }catch (Exception e) {
        LOGGER.error("Exception  :   "+e);
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
     }
  }


  /**
   * Invoked to take action after a port has been deleted.
   *
   * @param network
   *            An instance of new Neutron port object.
   */
  @Override
  public void neutronPortDeleted(NeutronPort neutronPort) {
     VirtualMachineInterface virtualMachineInterface = null;
     try {
          virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID());
       if(virtualMachineInterface == null) {
         LOGGER.info("Port deletion verified....");
        }
       else{
         LOGGER.info("Port deletion failed....");
       }
     }
    catch(Exception e) {
         LOGGER.error("Exception :    "+e);
    }
  }


  @Override
  public int canUpdatePort(NeutronPort arg0, NeutronPort arg1) {
      return 0;
  }


  @Override
  public void neutronPortUpdated(NeutronPort arg0) {
  }
}