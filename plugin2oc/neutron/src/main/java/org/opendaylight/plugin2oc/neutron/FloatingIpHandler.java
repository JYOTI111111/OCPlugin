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
import net.juniper.contrail.api.types.FloatingIp;
import net.juniper.contrail.api.types.FloatingIpPool;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Router.
 */
public class FloatingIpHandler implements INeutronFloatingIPAware, INeutronFloatingIPCRUD {
    /**
     * Logger instance.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(PortHandler.class);
    static ApiConnector apiConnector;

    /**
     * Invoked when a floating ip creation is requested to check if the specified
     * floating ip can be created.
     *
     * @param floatingip
     *            An instance of proposed new Neutron Floating ip object.
     *
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreateFloatingIP(NeutronFloatingIP fip) {
        if (fip == null) {
            LOGGER.error("Neutron Floating Ip can not be null ");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (("").equals(fip.getFloatingIPUUID())) {
            LOGGER.error("Floating Ip UUID can not be null ");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (fip.getTenantUUID() == null || ("").equals(fip.getTenantUUID())) {
            LOGGER.error(" Floating Ip tenant Id can not be null");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (fip.getFloatingIPAddress() == null) {
            LOGGER.error(" Floating Ip address can not be null");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        apiConnector = Activator.apiConnector;
        return HttpURLConnection.HTTP_OK;
    }
    
    /**
     * Invoked when a floating ip deletion is requested to indicate if the specified
     * floating ip can be deleted.
     *
     * @param floatingip
     *            An instance of the Neutron floating ip object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteFloatingIP(NeutronFloatingIP neutronFloatingIp) {
        if (neutronFloatingIp == null) {
            LOGGER.error("Neutron Floating Ip can not be null.. ");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked when a floating ip update is requested to indicate if the specified
     * floating ip can be changed using the specified delta.
     *
     * @param delta
     *            Updates to the floating ip object using patch semantics.
     * @param original
     *            An instance of the Neutron floating ip object to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateFloatingIP(NeutronFloatingIP deltaFloatingIp, NeutronFloatingIP originalFloatingIp) {
        FloatingIp floatingIP = null;
        apiConnector = Activator.apiConnector;
        if (deltaFloatingIp == null || originalFloatingIp == null) {
            LOGGER.error("Neutron Floating Ip can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        try {
            floatingIP = (FloatingIp) apiConnector.findById(FloatingIp.class, originalFloatingIp.getFloatingIPUUID());
        } catch (IOException e) {
            LOGGER.error("Exception :     " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        if (floatingIP == null) {
            LOGGER.error("No network exists for the specified UUID...");
            return HttpURLConnection.HTTP_FORBIDDEN;
        }
        return HttpURLConnection.HTTP_OK;
    }
    
    /**
     * Invoked to take action after a floating ip has been created.
     *
     * @param neutronFloatingIp
     *            An instance of new Neutron floating ip object.
     */
    @Override
    public void neutronFloatingIPCreated(NeutronFloatingIP neutronFloatingIp) {
        FloatingIp floatingIp = null;
        try {
            floatingIp = (FloatingIp) apiConnector.findById(FloatingIp.class, neutronFloatingIp.getFloatingIPUUID());
            if (floatingIp != null) {
                LOGGER.info("Floating Ip creation verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :    " + e);
        }
    }

    /**
     * Invoked to take action after a floating ip has been deleted.
     *
     * @param floatingip
     *            An instance of deleted Neutron floating ip object.
     */
    @Override
    public void neutronFloatingIPDeleted(NeutronFloatingIP neutronFloatingIp) {
        FloatingIp fip = null;
        try {
            fip = (FloatingIp) apiConnector.findById(FloatingIp.class, neutronFloatingIp.getFloatingIPUUID());
            if (fip == null) {
                LOGGER.info("Floating ip deletion verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :   " + e);
        }
    }
    /**
     * Invoked to take action after a floating ip has been updated.
     *
     * @param floatingIp
     *            An instance of modified Neutron floating ip object.
     */
    @Override
    public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIp) {
        LOGGER.info("Floating Ip with floating UUID " + floatingIp.getFloatingIPUUID() + " is Updated");
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
        String id1 = uuid.substring(0, 8);
        String id2 = uuid.substring(8, 12);
        String id3 = uuid.substring(12, 16);
        String id4 = uuid.substring(16, 20);
        String id5 = uuid.substring(20, 32);
        uuidPattern = (id1 + "-" + id2 + "-" + id3 + "-" + id4 + "-" + id5);
        return uuidPattern;
    }

    /**
     * Invoked to add the specified Neutron floating ip.
     *
     * @param floating ip
     *            An instance of new Neutron floating ip object.
     *
     * @return A boolean to the creation request.
     */
    @Override
    public boolean addFloatingIP(NeutronFloatingIP neutronFloatingIp) {
        String projectUUID = null;
        String floatingPoolNetworkId = null;
        String fipId = neutronFloatingIp.getID();
        String floatingIpaddress = neutronFloatingIp.getFloatingIPAddress();
        try {
            floatingPoolNetworkId = neutronFloatingIp.getFloatingNetworkUUID();
            projectUUID = neutronFloatingIp.getTenantUUID().toString();
            if (!(projectUUID.contains("-"))) {
                projectUUID = uuidFormater(projectUUID);
            }
            projectUUID = UUID.fromString(projectUUID).toString();
        } catch (Exception ex) {
            LOGGER.error("UUID input incorrect", ex);
            return false;
        }
        Project project;
        try {
            project = (Project) apiConnector.findById(Project.class, projectUUID);
            if (project == null) {
                try {
                    Thread.currentThread();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException :    ", e);
                    return false;
                }
                project = (Project) apiConnector.findById(Project.class, projectUUID);
                if (project == null) {
                    LOGGER.error("Could not find projectUUID...");
                    return false;
                }
            }
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, floatingPoolNetworkId);
            if (virtualNetwork == null) {
                LOGGER.error("Could not find Virtual network...");
                return false;
            }
            String floatingPoolId = virtualNetwork.getFloatingIpPools().get(0).getUuid();
            FloatingIpPool floatingIpPool = (FloatingIpPool) apiConnector.findById(FloatingIpPool.class, floatingPoolId);
            if (floatingIpPool == null) {
                LOGGER.error("Could not find Floating ip pool...");
                return false;
            }
            FloatingIp floatingIp = new FloatingIp();
            floatingIp.setUuid(fipId);
            floatingIp.setName(fipId);
            floatingIp.setDisplayName(fipId);
            floatingIp.setAddress(floatingIpaddress);
            floatingIp.setParent(floatingIpPool);
            floatingIp.setProject(project);
            if (neutronFloatingIp.getPortUUID() != null) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                        neutronFloatingIp.getPortUUID());
                if (virtualMachineInterface != null) {
                    floatingIp.addVirtualMachineInterface(virtualMachineInterface);
                }
            }
            boolean floatingIpCreaterd = apiConnector.create(floatingIp);
            if (!floatingIpCreaterd) {
                LOGGER.warn("Floating Ip creation failed..");
                return false;
            }
            LOGGER.info("Floating Ip : " + floatingIp.getName() + "  having UUID : " + floatingIp.getUuid() + "  sucessfully created...");
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean floatingIPExists(String arg0) {
        return false;
    }

    @Override
    public List<NeutronFloatingIP> getAllFloatingIPs() {
        return null;
    }

    @Override
    public NeutronFloatingIP getFloatingIP(String arg0) {
        return null;
    }
    /**
     * Invoked to delete the specified Neutron floating ip.
     *
     * @param String
     *             An instance of floating ip UUID.
     *
     * @return A boolean to the delete request.
     */
    @Override
    public boolean removeFloatingIP(String neutronFloatingIp) {
        apiConnector = Activator.apiConnector;
        try {
            FloatingIp floatingIp = (FloatingIp) apiConnector.findById(FloatingIp.class, neutronFloatingIp);
            if (floatingIp != null) {
                apiConnector.delete(floatingIp);
                LOGGER.info("Floating Ip with UUID :  " + floatingIp.getUuid() + "  has been deleted successfully....");
                return true;
            } else {
                LOGGER.info("No Floating Ip exists with UUID :  " + neutronFloatingIp);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception : " + e);
            return false;
        }
    }
    /**
     * Invoked to update the floating ip
     *
     * @param string
     *             An instance of floating ip UUID.
     * @param delta_floatingip
     *            An instance of delta floating ip.
     *
     * @return A boolean to the update request.
     */
    @Override
    public boolean updateFloatingIP(String floatingIpUUID, NeutronFloatingIP deltaFloatingIp) {
        FloatingIp floatingIP;
        try {
            floatingIP = (FloatingIp) apiConnector.findById(FloatingIp.class, floatingIpUUID);
            String virtualMachineInterfaceUUID = deltaFloatingIp.getPortUUID();
            if (deltaFloatingIp.getPortUUID() != null) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                        virtualMachineInterfaceUUID);
                if (virtualMachineInterface != null) {
                    floatingIP.setVirtualMachineInterface(virtualMachineInterface);
                }
            }
            if (virtualMachineInterfaceUUID == null) {
                floatingIP.clearVirtualMachineInterface();
            }
            boolean floatingIpUpdate = apiConnector.update(floatingIP);
            if (!floatingIpUpdate) {
                LOGGER.warn("Floating Ip Updation failed..");
                return false;
            }
            LOGGER.info("Floating Ip  having UUID : " + floatingIP.getUuid() + "  has been sucessfully updated...");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
