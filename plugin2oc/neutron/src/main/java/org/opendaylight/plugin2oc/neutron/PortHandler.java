/*
 * Copyright (C) 2014 Juniper Networks, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plugin2oc.neutron;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
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
public class PortHandler implements INeutronPortAware, INeutronPortCRUD {
    /**
     * Logger instance.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(PortHandler.class);
    static ApiConnector apiConnector;
    private NeutronPort originalPort;

    public NeutronPort getOriginalPort() {
        return originalPort;
    }

    public void setOriginalPort(NeutronPort originalPort) {
        this.originalPort = originalPort;
    }

    /**
     * Invoked when a port creation is requested to check if the specified Port
     * can be created and then creates the port
     *
     * @param NeutronPort
     *            An instance of proposed new Neutron Port object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreatePort(NeutronPort neutronPort) {
        apiConnector = Activator.apiConnector;
        if (neutronPort == null) {
            LOGGER.error("NeutronPort object can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (neutronPort.getID().equals("")) {
            LOGGER.error("Port Device Id or Port Uuid can't be empty/null...");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (neutronPort.getTenantID() == null) {
            LOGGER.error("Tenant ID can't be null...");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        List<Neutron_IPs> ips = neutronPort.getFixedIPs();
        if (ips == null) {
            LOGGER.warn("Neutron Fixed Ips can't be null..");
            return HttpURLConnection.HTTP_FORBIDDEN;
        }
        Project project;
        try {
            project = (Project) apiConnector.findById(Project.class, neutronPort.getTenantID());
            if (project == null) {
                try {
                    Thread.currentThread();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException :      ", e);
                    return HttpURLConnection.HTTP_INTERNAL_ERROR;
                }
                project = (Project) apiConnector.findById(Project.class, neutronPort.getTenantID());
                if (project == null) {
                    LOGGER.error("Could not find projectUUID...");
                    return HttpURLConnection.HTTP_NOT_FOUND;
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to add the specified Neutron port.
     *
     * @param network
     *            An instance of new Neutron Port object.
     *
     * @return {@link Boolean} A boolean to the creation request.
     */
    @Override
    public boolean addPort(NeutronPort neutronPort) {
        apiConnector = Activator.apiConnector;
        String networkID = neutronPort.getNetworkUUID();
        String portID = neutronPort.getID();
        String portDesc = neutronPort.getID();
        String deviceID = neutronPort.getDeviceID();
        String projectID = neutronPort.getTenantID();
        String portMACAddress = neutronPort.getMacAddress();
        VirtualMachineInterface virtualMachineInterface = null;
        VirtualMachine virtualMachine = null;
        VirtualNetwork virtualNetwork = null;
        Project project = null;
        MacAddressesType macAddressesType = new MacAddressesType();
        try {
            networkID = UUID.fromString(neutronPort.getNetworkUUID()).toString();
            portID = UUID.fromString(neutronPort.getID()).toString();
            if (neutronPort.getDeviceID() != null && !(("").equals(neutronPort.getDeviceID()))) {
                if (!(deviceID.contains("-"))) {
                    deviceID = uuidFormater(deviceID);
                }
                deviceID = UUID.fromString(deviceID).toString();
            }
            if (!(projectID.contains("-"))) {
                projectID = uuidFormater(projectID);
            }
            projectID = UUID.fromString(projectID).toString();
        } catch (Exception ex) {
            LOGGER.error("exception :   ", ex);
            return false;
        }
        try {
            LOGGER.debug("portId:    " + portID);
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portID);
            if (deviceID != null && !(("").equals(deviceID))) {
                virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, deviceID);
                LOGGER.debug("virtualMachine:   " + virtualMachine);
                if (virtualMachine == null) {
                    virtualMachine = new VirtualMachine();
                    virtualMachine.setName(deviceID);
                    virtualMachine.setUuid(deviceID);
                    boolean virtualMachineCreated = apiConnector.create(virtualMachine);
                    LOGGER.debug("virtualMachineCreated: " + virtualMachineCreated);
                    if (!virtualMachineCreated) {
                        LOGGER.warn("virtualMachine creation failed..");
                        return false;
                    }
                    LOGGER.info("virtualMachine : " + virtualMachine.getName() + "  having UUID : " + virtualMachine.getUuid()
                            + "  sucessfully created...");
                }
            }
            project = (Project) apiConnector.findById(Project.class, projectID);
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkID);
            LOGGER.info("virtualNetwork: " + virtualNetwork);
            if (virtualNetwork == null) {
                LOGGER.warn("virtualNetwork does not exist..");
                return false;
            } else {
                virtualMachineInterface = new VirtualMachineInterface();
                virtualMachineInterface.addVirtualNetwork(virtualNetwork);
                virtualMachineInterface.setDisplayName(portDesc);
                virtualMachineInterface.setUuid(portID);
                virtualMachineInterface.setName(portDesc);
                virtualMachineInterface.setParent(project);
                macAddressesType.addMacAddress(portMACAddress);
                virtualMachineInterface.setMacAddresses(macAddressesType);
                if (deviceID != null && !(("").equals(deviceID))) {
                    virtualMachineInterface.setVirtualMachine(virtualMachine);
                }
                boolean virtualMachineInterfaceCreated = apiConnector.create(virtualMachineInterface);
                if (!virtualMachineInterfaceCreated) {
                    LOGGER.warn("actual virtualMachineInterface creation failed..");
                    return false;
                }
                LOGGER.info("virtualMachineInterface : " + virtualMachineInterface.getName() + "  having UUID : " + virtualMachineInterface.getUuid()
                        + "  sucessfully created...");
            }
            INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
            NeutronSubnet subnet = null;
            List<Neutron_IPs> ips = neutronPort.getFixedIPs();
            InstanceIp instanceIp = new InstanceIp();
            String instaneIpUuid = UUID.randomUUID().toString();
            for (Neutron_IPs ipValues : ips) {
                if (ipValues.getIpAddress() == null) {
                    subnet = systemCRUD.getSubnet(ipValues.getSubnetUUID());
                    instanceIp.setAddress(subnet.getLowAddr());
                } else {
                    instanceIp.setAddress(ipValues.getIpAddress());
                }
            }
            instanceIp.setName(instaneIpUuid);
            instanceIp.setUuid(instaneIpUuid);
            instanceIp.setParent(virtualMachineInterface);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setVirtualNetwork(virtualNetwork);
            boolean instanceIpCreated = apiConnector.create(instanceIp);
            if (!instanceIpCreated) {
                LOGGER.warn("instanceIp addition failed..");
                return false;
            }
            LOGGER.info("Instance IP added sucessfully...");
            return true;
        } catch (IOException ie) {
            LOGGER.error("IOException :    ", ie);
            return false;
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
        try {
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID());
            if (virtualMachineInterface != null) {
                LOGGER.info("Port creation verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :    " + e);
        }
    }

    /**
     * Invoked when a port deletion is requested to check if the specified Port
     * can be deleted and then delete the port
     *
     * @param NeutronPort
     *            An instance of proposed Neutron Port object.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeletePort(NeutronPort neutronPort) {
        if (neutronPort == null) {
            LOGGER.info("Port object can't be null...");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        apiConnector = Activator.apiConnector;
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to delete the specified Neutron port.
     *
     * @param network
     *            An instance of new Neutron Port object.
     *
     * @return boolean to the deletion request.
     */
    @Override
    public boolean removePort(String portUUID) {
        InstanceIp instanceIP = null;
        List<ObjectReference<ApiPropertyBase>> virtualMachineInterfaceBackRefs = null;
        try {
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector
                    .findById(VirtualMachineInterface.class, portUUID);
            List<ObjectReference<ApiPropertyBase>> instanceIPs = virtualMachineInterface.getInstanceIpBackRefs();
            if (instanceIPs != null) {
                for (ObjectReference<ApiPropertyBase> ref : instanceIPs) {
                    String instanceIPUUID = ref.getUuid();
                    if (instanceIPUUID != null) {
                        instanceIP = (InstanceIp) apiConnector.findById(InstanceIp.class, instanceIPUUID);
                        apiConnector.delete(instanceIP);
                    }
                }
            }
            apiConnector.delete(virtualMachineInterface);
            VirtualMachine virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, virtualMachineInterface.getVirtualMachine()
                    .get(0).getUuid());
            if (virtualMachine != null) {
                virtualMachineInterfaceBackRefs = virtualMachine.getVirtualMachineInterfaceBackRefs();
                if (virtualMachineInterfaceBackRefs == null) {
                    apiConnector.delete(virtualMachine);
                }
            }
            LOGGER.info("Specified port deleted sucessfully...");
            return true;
        } catch (IOException io) {
            LOGGER.error("Exception  :   " + io);
            return false;
        } catch (Exception e) {
            LOGGER.error("Exception  :   " + e);
            return false;
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
        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID());
            if (virtualMachineInterface == null) {
                LOGGER.info("Port deletion verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :    " + e);
        }
    }

    /**
     * Invoked when a port update is requested to indicate if the specified port
     * can be updated using the specified delta and update the port
     *
     * @param delta
     *            Updates to the port object using patch semantics.
     * @param original
     *            An instance of the Neutron Port object to be updated.
     *
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdatePort(NeutronPort deltaPort, NeutronPort port) {
        apiConnector = Activator.apiConnector;
        if (deltaPort == null || port == null) {
            LOGGER.error("Neutron Port objects can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (deltaPort.getMacAddress() != null) {
            LOGGER.error("MAC Address for the port can't be updated..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        originalPort = port;
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public boolean updatePort(String portUUID, NeutronPort deltaPort) {
        apiConnector = Activator.apiConnector;
        VirtualMachine virtualMachine = null;
        String deviceID = deltaPort.getDeviceID();
        String portName = deltaPort.getName();
        List<Neutron_IPs> fixedIPs = deltaPort.getFixedIPs();
        boolean instanceIpUpdate = false;
        String networkUUID = deltaPort.getNetworkUUID();
        VirtualNetwork virtualnetwork = null;
        VirtualMachineInterface virtualMachineInterface;
        try {
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portUUID);
            if (fixedIPs != null) {
                if (networkUUID == null) {
                    for (ObjectReference<ApiPropertyBase> networks : virtualMachineInterface.getVirtualNetwork()) {
                        networkUUID = networks.getUuid();
                    }
                }
                boolean subnetExist = false;
                virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
                if (virtualnetwork != null && virtualnetwork.getNetworkIpam() != null) {
                    for (Neutron_IPs fixedIp : fixedIPs) {
                        for (ObjectReference<VnSubnetsType> ref : virtualnetwork.getNetworkIpam()) {
                            VnSubnetsType vnSubnetsType = ref.getAttr();
                            if (vnSubnetsType != null) {
                                List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
                                if (subnets != null) {
                                    for (VnSubnetsType.IpamSubnetType subnetValue : subnets) {
                                        Boolean doesSubnetExist = subnetValue.getSubnetUuid().matches(fixedIp.getSubnetUUID());
                                        if (doesSubnetExist) {
                                            subnetExist = true;
                                            for (ObjectReference<ApiPropertyBase> instanceIp : virtualMachineInterface.getInstanceIpBackRefs()) {
                                                InstanceIp instanceIpLocal = (InstanceIp) apiConnector.findById(InstanceIp.class, instanceIp.getUuid());
                                                instanceIpLocal.setVirtualNetwork(virtualnetwork);
                                                INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
                                                NeutronSubnet subnet = null;
                                                for (Neutron_IPs ip : originalPort.getFixedIPs()) {
                                                    subnet = systemCRUD.getSubnet(ip.getSubnetUUID());
                                                    subnet.releaseIP(ip.getIpAddress());
                                                }
                                                if (fixedIp.getIpAddress() == null) {
                                                    subnet = systemCRUD.getSubnet(fixedIp.getSubnetUUID());
                                                    instanceIpLocal.setAddress(subnet.getLowAddr());
                                                } else {
                                                    instanceIpLocal.setAddress(fixedIp.getIpAddress());
                                                }
                                                instanceIpUpdate = apiConnector.update(instanceIpLocal);
                                                virtualMachineInterface.setVirtualNetwork(virtualnetwork);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!subnetExist) {
                    LOGGER.error("Subnet UUID must exist in the network..");
                    originalPort = null;
                    return false;
                }
            }
            if (deviceID != null) {
                if (("").equals(deviceID)) {
                    virtualMachineInterface.clearVirtualMachine();
                } else {
                    deviceID = UUID.fromString(deltaPort.getDeviceID()).toString();
                    try {
                        virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, deviceID);
                    } catch (Exception e) {
                        LOGGER.error("Exception:     " + e);
                        originalPort = null;
                        return false;
                    }
                    if (virtualMachine == null) {
                        virtualMachine = new VirtualMachine();
                        virtualMachine.setName(deviceID);
                        virtualMachine.setUuid(deviceID);
                        boolean virtualMachineCreated = apiConnector.create(virtualMachine);
                        LOGGER.debug("virtualMachineCreated: " + virtualMachineCreated);
                        if (!virtualMachineCreated) {
                            LOGGER.warn("virtualMachine creation failed..");
                            originalPort = null;
                            return false;
                        }
                        LOGGER.info("virtualMachine : " + virtualMachine.getName() + "  having UUID : " + virtualMachine.getUuid()
                                + "  sucessfully created...");
                    }
                    virtualMachineInterface.setVirtualMachine(virtualMachine);
                }
            }
            if (portName != null) {
                virtualMachineInterface.setDisplayName(portName);
            }
            if ((deviceID != null && !(("").equals(deviceID))) || portName != null || instanceIpUpdate) {
                if ((deviceID != null && !(("").equals(deviceID))) || portName != null) {
                    boolean portUpdate = apiConnector.update(virtualMachineInterface);
                    if (!portUpdate) {
                        LOGGER.warn("Port Updation failed..");
                        originalPort = null;
                        return false;
                    }
                }
                LOGGER.info("Port having UUID : " + virtualMachineInterface.getUuid() + "  has been sucessfully updated...");
                originalPort = null;
                return true;
            } else {
                LOGGER.info("Nothing to update...");
                originalPort = null;
                return false;
            }
        } catch (IOException e1) {
            LOGGER.warn("Exception    : " + e1);
            originalPort = null;
            return false;
        }
    }

    /**
     * Invoked to take action after a port has been updated.
     *
     * @param network
     *            An instance of modified Neutron Port object.
     */
    @Override
    public void neutronPortUpdated(NeutronPort neutronPort) {
        try {
            VirtualMachineInterface virtualMachineInterface;
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID());
            if (("").equals(neutronPort.getDeviceID())) { // TODO : Fix Port
                                                          // Update (Dependent
                                                          // on VM Refs issue)
                if (neutronPort.getName().matches(virtualMachineInterface.getDisplayName()) && virtualMachineInterface.getVirtualMachine() == null) {
                    LOGGER.info("Port updatation verified....");
                }
            } else if (neutronPort.getName().matches(virtualMachineInterface.getDisplayName())
                    && neutronPort.getDeviceID().matches(virtualMachineInterface.getVirtualMachine().get(0).getUuid())) {
                LOGGER.info("Port updatation verified....");
            } else {
                LOGGER.info("Port updatation failed....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :" + e);
        }
    }

    /**
     * Invoked to format the UUID if UUID is not in correct format.
     *
     * @param String
     *            An instance of UUID string.
     *
     * @return Correctly formated UUID string.
     */
    private String uuidFormater(String uuid) {
        String uuidPattern = null;
        try {
            String id1 = uuid.substring(0, 8);
            String id2 = uuid.substring(8, 12);
            String id3 = uuid.substring(12, 16);
            String id4 = uuid.substring(16, 20);
            String id5 = uuid.substring(20, 32);
            uuidPattern = (id1 + "-" + id2 + "-" + id3 + "-" + id4 + "-" + id5);
        } catch (Exception e) {
            LOGGER.error("UUID is not in correct format ");
            LOGGER.error("Exception :" + e);
        }
        return uuidPattern;
    }

    @Override
    public List<NeutronPort> getAllPorts() {
        return null;
    }

    @Override
    public NeutronPort getGatewayPort(String arg0) {
        return null;
    }

    @Override
    public NeutronPort getPort(String arg0) {
        return null;
    }

    @Override
    public boolean macInUse(String arg0) {
        return false;
    }

    @Override
    public boolean portExists(String arg0) {
        return false;
    }
}