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
import net.juniper.contrail.api.types.FloatingIpPool;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler implements INeutronNetworkAware, INeutronNetworkCRUD {
    /**
     * Logger instance.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    static ApiConnector apiConnector;

    /**
     * Invoked when a network creation is requested to check if the specified
     * network can be created and then creates the network
     *
     * @param network
     *            An instance of proposed new Neutron Network object.
     *
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNetwork(NeutronNetwork network) {
        if (network == null) {
            LOGGER.error("Network object can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        LOGGER.debug("Network object " + network);
        apiConnector = Activator.apiConnector;
        if (network.getNetworkUUID() == null || network.getNetworkName() == null || network.getNetworkUUID().equals("")
                || network.getNetworkName().equals("")) {
            LOGGER.error("Network UUID and Network Name can't be null/empty...");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (network.getTenantID() == null) {
            LOGGER.error("Network tenant Id can not be null");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        try {
            VirtualNetwork virtualNetwork = null;
            String networkUUID = null;
            String projectUUID = null;
            try {
                networkUUID = UUID.fromString(network.getNetworkUUID()).toString();
                projectUUID = network.getTenantID().toString();
                if (!(projectUUID.contains("-"))) {
                    projectUUID = uuidFormater(projectUUID);
                }
                projectUUID = UUID.fromString(projectUUID).toString();
                LOGGER.info("projectUUID 2  " + projectUUID);
            } catch (Exception ex) {
                LOGGER.error("UUID input incorrect", ex);
                return HttpURLConnection.HTTP_BAD_REQUEST;
            }
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
            Project project = (Project) apiConnector.findById(Project.class, projectUUID);
            if (project == null) {
                try {
                    Thread.currentThread();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException :    ", e);
                    return HttpURLConnection.HTTP_BAD_REQUEST;
                }
                project = (Project) apiConnector.findById(Project.class, projectUUID);
                if (project == null) {
                    LOGGER.error("Could not find projectUUID...");
                    return HttpURLConnection.HTTP_NOT_FOUND;
                }
            }
            if (virtualNetwork != null) {
                LOGGER.warn("Network already exists..");
                return HttpURLConnection.HTTP_FORBIDDEN;
            }
        } catch (Exception e) {
            LOGGER.error("Exception :   " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return 200;
    }

    /**
     * Invoked to take action after a network has been created.
     *
     * @param network
     *            An instance of new Neutron Network object.
     */
    @Override
    public void neutronNetworkCreated(NeutronNetwork network) {
        VirtualNetwork virtualNetwork = null;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
            if (virtualNetwork != null) {
                LOGGER.info("Network creation verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :     " + e);
        }
    }

    /**
     * Invoked to map the NeutronNetwork object properties to the virtualNetwork
     * object.
     *
     * @param neutronNetwork
     *            An instance of new Neutron Network object.
     * @param virtualNetwork
     *            An instance of new virtualNetwork object.
     * @return {@link VirtualNetwork}
     */
    private VirtualNetwork mapNetworkProperties(NeutronNetwork neutronNetwork, VirtualNetwork virtualNetwork) {
        boolean routerExternal = false;
        boolean ishared = false;
        String networkUUID = neutronNetwork.getNetworkUUID();
        String networkName = neutronNetwork.getNetworkName();
        if (neutronNetwork.getRouterExternal() != null) {
            routerExternal = neutronNetwork.getRouterExternal();
        }
        if (neutronNetwork.getShared() != null) {
            ishared = neutronNetwork.getShared();
        }
        virtualNetwork.setName(networkName);
        virtualNetwork.setUuid(networkUUID);
        virtualNetwork.setDisplayName(networkName);
        virtualNetwork.setRouterExternal(routerExternal);
        virtualNetwork.setIsShared(ishared);
        return virtualNetwork;
    }

    /**
     * Invoked when a network update is requested to indicate if the specified
     * network can be changed using the specified delta.
     *
     * @param delta
     *            Updates to the network object using patch semantics.
     * @param original
     *            An instance of the Neutron Network object to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateNetwork(NeutronNetwork deltaNetwork, NeutronNetwork originalNetwork) {
        VirtualNetwork virtualnetwork;
        apiConnector = Activator.apiConnector;
        if (deltaNetwork == null || originalNetwork == null) {
            LOGGER.error("Neutron Networks can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (("").equals(deltaNetwork.getNetworkName())) {
            LOGGER.error("Neutron Networks name to be update can't be empty..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        try {
            virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, originalNetwork.getNetworkUUID());
        } catch (IOException e) {
            LOGGER.error("Exception :     " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        if (virtualnetwork == null) {
            LOGGER.error("No network exists for the specified UUID...");
            return HttpURLConnection.HTTP_FORBIDDEN;
        }
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network
     *            An instance of modified Neutron Network object.
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
        try {
            VirtualNetwork virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
            if (network.getNetworkName().equalsIgnoreCase(virtualnetwork.getDisplayName())) {
                LOGGER.info("Network updatation verified....");
            } else {
                LOGGER.info("Network updatation failed....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :" + e);
        }
    }

    /**
     * Invoked when a network deletion is requested to indicate if the specified
     * network can be deleted.
     *
     * @param network
     *            An instance of the Neutron Network object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteNetwork(NeutronNetwork network) {
        apiConnector = Activator.apiConnector;
        VirtualNetwork virtualNetwork = null;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
            if (virtualNetwork != null) {
                if (virtualNetwork.getVirtualMachineInterfaceBackRefs() != null) {
                    LOGGER.info("Network with UUID :  " + network.getNetworkUUID() + " cannot be deleted as it has port(s) associated with it....");
                    return HttpURLConnection.HTTP_FORBIDDEN;
                }
            } else {
                LOGGER.info("No Network exists with UUID :  " + network.getNetworkUUID());
                return HttpURLConnection.HTTP_BAD_REQUEST;
            }
        } catch (Exception e) {
            LOGGER.error("Exception : " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been deleted.
     *
     * @param network
     *            An instance of deleted Neutron Network object.
     */
    @Override
    public void neutronNetworkDeleted(NeutronNetwork network) {
        VirtualNetwork virtualNetwork = null;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
            if (virtualNetwork == null) {
                LOGGER.info("Network deletion verified....");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :   " + e);
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
        String id1 = uuid.substring(0, 8);
        String id2 = uuid.substring(8, 12);
        String id3 = uuid.substring(12, 16);
        String id4 = uuid.substring(16, 20);
        String id5 = uuid.substring(20, 32);
        uuidPattern = (id1 + "-" + id2 + "-" + id3 + "-" + id4 + "-" + id5);
        return uuidPattern;
    }

    /**
     * Invoked to add the specified Neutron Network.
     *
     * @param network
     *            An instance of new Neutron Network object.
     *
     * @return A boolean to the creation request.
     */
    @Override
    public boolean addNetwork(NeutronNetwork network) {
        apiConnector = Activator.apiConnector;
        VirtualNetwork virtualNetwork = null;
        Project project;
        virtualNetwork = new VirtualNetwork();
        try {
            project = (Project) apiConnector.findById(Project.class, network.getTenantID());
            virtualNetwork.setParent(project);
        } catch (IOException e2) {
            e2.printStackTrace();
            return false;
        }
        // map neutronNetwork to virtualNetwork
        virtualNetwork = mapNetworkProperties(network, virtualNetwork);
        boolean networkCreated;
        try {
            networkCreated = apiConnector.create(virtualNetwork);
            LOGGER.debug("networkCreated:   " + networkCreated);
            if (!networkCreated) {
                LOGGER.warn("Network creation failed..");
                return false;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        }
        LOGGER.info("Network : " + virtualNetwork.getName() + "  having UUID : " + virtualNetwork.getUuid() + "  sucessfully created...");
        if (virtualNetwork.getRouterExternal() == true) {
            FloatingIpPool floatingIpPool = null;
            String fipId = UUID.randomUUID().toString();
            floatingIpPool = new FloatingIpPool();
            floatingIpPool.setName(fipId);
            floatingIpPool.setDisplayName(fipId);
            floatingIpPool.setUuid(fipId);
            floatingIpPool.setParent(virtualNetwork);
            boolean createFloatingIpPool;
            try {
                createFloatingIpPool = apiConnector.create(floatingIpPool);
                if (!createFloatingIpPool) {
                    LOGGER.info("Floating Ip pool creation failed..");
                    return false;
                } else {
                    LOGGER.info("Floating Ip pool created with UUID  : " + floatingIpPool.getUuid());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public List<NeutronNetwork> getAllNetworks() {
        return null;
    }

    @Override
    public NeutronNetwork getNetwork(String arg0) {
        return null;
    }

    @Override
    public boolean networkExists(String arg0) {
        return false;
    }

    @Override
    public boolean networkInUse(String arg0) {
        return false;
    }

    /**
     * Invoked to delete the specified Neutron Network.
     *
     * @param string
     *            An instance of network UUID.
     *
     * @return A boolean to the delete request.
     */
    @Override
    public boolean removeNetwork(String networkUUID) {
        VirtualNetwork virtualNetwork;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
            apiConnector.delete(virtualNetwork);
            VirtualNetwork virtualNetwork1 = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
            if (virtualNetwork1 != null) {
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Network with UUID :" + networkUUID + "  failed to delete.... ");
            return false;
        }
        LOGGER.info("Network with UUID :  " + networkUUID + "  has been deleted successfully....");
        return true;
    }

    /**
     * Invoked to update the network
     *
     * @param string
     *             An instance of network UUID.
     * @param delta_network
     *            An instance of Network.
     *
     * @return A boolean to the update request.
     */
    @Override
    public boolean updateNetwork(String networkUUID, NeutronNetwork deltaNetwork) {
        apiConnector = Activator.apiConnector;
        VirtualNetwork virtualNetwork = null;
        VirtualNetwork originalNetwork = null;
        try {
            originalNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
        } catch (IOException e) {
            LOGGER.warn("Network does not exist..");
            return false;
        }
        String networkName = deltaNetwork.getNetworkName();
        if (deltaNetwork.getShared() != null) {
            virtualNetwork.setIsShared(deltaNetwork.getShared());
        }
        if (deltaNetwork.getRouterExternal() != null) {
            virtualNetwork.setRouterExternal(deltaNetwork.getRouterExternal());
        }
        if (deltaNetwork.getRouterExternal() == null) {
        }
        virtualNetwork.setName(networkName);
        virtualNetwork.setDisplayName(networkName);
        boolean networkUpdate;
        try {
            networkUpdate = apiConnector.update(virtualNetwork);
            System.out.println(networkUpdate);
            if (!networkUpdate) {
                LOGGER.warn("Network Updation failed..");
                return false;
            }
        } catch (IOException e) {
            LOGGER.warn("Network Updation failed..");
            return false;
        }
        LOGGER.info("Network having UUID : " + virtualNetwork.getUuid() + "  has been sucessfully updated...");
        if (deltaNetwork.getRouterExternal() != null) {
            if (!(originalNetwork.getRouterExternal()) && deltaNetwork.getRouterExternal()) {
                VirtualNetwork UpdatedVirtualnetwork;
                try {
                    UpdatedVirtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, originalNetwork.getUuid());
                    FloatingIpPool floatingIpPool = null;
                    String fipId = UUID.randomUUID().toString();
                    floatingIpPool = new FloatingIpPool();
                    floatingIpPool.setName(fipId);
                    floatingIpPool.setDisplayName(fipId);
                    floatingIpPool.setUuid(fipId);
                    floatingIpPool.setParent(UpdatedVirtualnetwork);
                    boolean createFloatingIpPool = apiConnector.create(floatingIpPool);
                    if (!createFloatingIpPool) {
                        LOGGER.info("Floating Ip pool creation failed..");
                        return false;
                    } else {
                        LOGGER.info("Floating Ip pool created with UUID  : " + floatingIpPool.getUuid());
                    }
                } catch (IOException e) {
                    LOGGER.info("Floating Ip pool creation failed..");
                    return false;
                }
            } else if (originalNetwork.getRouterExternal() && !(deltaNetwork.getRouterExternal())) {
                String floatingPoolId = virtualNetwork.getFloatingIpPools().get(0).getUuid();
                FloatingIpPool floatingIpPool;
                try {
                    floatingIpPool = (FloatingIpPool) apiConnector.findById(FloatingIpPool.class, floatingPoolId);
                    if (floatingIpPool != null) {
                        apiConnector.delete(floatingIpPool);
                    }
                    floatingIpPool = (FloatingIpPool) apiConnector.findById(FloatingIpPool.class, floatingPoolId);
                    if (floatingIpPool == null) {
                        LOGGER.info("Floating Ip pool is removed after update network..");
                    } else {
                        LOGGER.info("Floating Ip pool is failed to removed after update network..");
                        return false;
                    }
                } catch (IOException e) {
                    LOGGER.info("Floating Ip pool is failed to removed after update network..");
                    return false;
                }
            }
        }
        return true;
    }
}