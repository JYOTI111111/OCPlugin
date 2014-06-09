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
import java.util.Iterator;
import java.util.List;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet_IPAllocationPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Subnet.
 */
public class SubnetHandler implements INeutronSubnetAware {
    /**
     * Logger instance.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(SubnetHandler.class);
    static ApiConnector apiConnector = Activator.apiConnector;

    /**
     * Invoked when a subnet creation is requested to check if the specified
     * subnet can be created and then creates the subnet.
     *
     * @param subnet
     *            An instance of proposed new Neutron Subnet object.
     *
     * @return A HTTP status code to the creation request.
     **/
    @Override
    public int canCreateSubnet(NeutronSubnet subnet) {
        VirtualNetwork virtualnetwork = new VirtualNetwork();
        apiConnector = Activator.apiConnector;
        if (subnet == null) {
            LOGGER.error("Neutron Subnet can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (subnet.getCidr() == null || ("").equals(subnet.getCidr())) {
            LOGGER.info("Subnet Cidr can not be empty or null...");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        boolean isvalidGateway = validGatewayIP(subnet, subnet.getGatewayIP());
        if (!isvalidGateway) {
            LOGGER.error("Incorrect gateway IP....");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        try {
            virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
        } catch (IOException e) {
            LOGGER.error("Exception : " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        if (virtualnetwork == null) {
            LOGGER.error("No network exists for the specified UUID...");
            return HttpURLConnection.HTTP_FORBIDDEN;
        } else {
            try {
                boolean ifSubnetExist = subnetExists(virtualnetwork.getNetworkIpam(), subnet);
                if (ifSubnetExist) {
                    LOGGER.error("The subnet already exists..");
                    return HttpURLConnection.HTTP_FORBIDDEN;
                }
                return createSubnet(subnet, virtualnetwork);
            } catch (IOException ie) {
                LOGGER.error("IOException:     " + ie);
                return HttpURLConnection.HTTP_INTERNAL_ERROR;
            } catch (Exception e) {
                LOGGER.error("Exception:  " + e);
                return HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
        }
    }

    private boolean subnetExists(List<ObjectReference<VnSubnetsType>> ipamRefs, NeutronSubnet subnet) {
        if (ipamRefs != null) {
            for (ObjectReference<VnSubnetsType> ref : ipamRefs) {
                VnSubnetsType vnSubnetsType = ref.getAttr();
                if (vnSubnetsType != null) {
                    List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
                    if (subnets != null) {
                        for (VnSubnetsType.IpamSubnetType subnetValue : subnets) {
                            String[] ipPrefix = getIpPrefix(subnet);
                            Boolean doesSubnetExist = subnetValue.getSubnet().getIpPrefix().matches(ipPrefix[0]);
                            if (doesSubnetExist) {
                                return doesSubnetExist;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Invoked to create the subnet
     *
     * @param subnet
     *            An instance of new Subnet Type object.
     */
    @Override
    public void neutronSubnetCreated(NeutronSubnet subnet) {
        VirtualNetwork virtualNetwork = null;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
            boolean ifSubnetExists = subnetExists(virtualNetwork.getNetworkIpam(), subnet);
            if (ifSubnetExists) {
                LOGGER.info("Subnet creation verified...");
            }
        } catch (Exception e) {
            LOGGER.error("Exception :    " + e);
        }
    }

    /**
     * Invoked to create the subnet
     *
     * @param subnet
     *            An instance of new Subnet Type object.
     * @param virtualNetwork
     *            An instance of new virtualNetwork object.
     *
     * @return A HTTP status code to the creation request.
     */
    private int createSubnet(NeutronSubnet subnet, VirtualNetwork virtualNetwork) throws IOException {
        // add subnet properties to the virtual-network object
        VirtualNetwork virtualnetwork = mapSubnetProperties(subnet, virtualNetwork);
        boolean subnetCreate = apiConnector.update(virtualnetwork);
        if (!subnetCreate) {
            LOGGER.warn("Subnet creation failed..");
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        } else {
            LOGGER.info("Subnet " + subnet.getCidr() + "sucessfully added to the network having UUID : " + virtualnetwork.getUuid());
            return HttpURLConnection.HTTP_OK;
        }
    }

    /**
     * Invoked to add the NeutronSubnet properties to the virtualNetwork object.
     *
     * @param subnet
     *            An instance of new Neutron Subnet object.
     * @param virtualNetwork
     *            An instance of new virtualNetwork object.
     *
     * @return {@link VirtualNetwork}
     */
    private VirtualNetwork mapSubnetProperties(NeutronSubnet subnet, VirtualNetwork vn) {
        String[] ipPrefix = null;
        NetworkIpam ipam = null;
        VnSubnetsType vnSubnetsType = new VnSubnetsType();
        SubnetType subnetType = new SubnetType();
        try {
            if (subnet.getCidr().contains("/")) {
                ipPrefix = subnet.getCidr().split("/");
            } else {
                throw new IllegalArgumentException("String " + subnet.getCidr() + " not in correct format..");
            }
            // Find default-network-ipam
            String ipamId = apiConnector.findByName(NetworkIpam.class, null, "default-network-ipam");
            ipam = (NetworkIpam) apiConnector.findById(NetworkIpam.class, ipamId);
        } catch (IOException ex) {
            LOGGER.error("IOException :     " + ex);
        } catch (Exception ex) {
            LOGGER.error("Exception :      " + ex);
        }
        if (ipPrefix != null) {
            subnetType.setIpPrefix(ipPrefix[0]);
            subnetType.setIpPrefixLen(Integer.valueOf(ipPrefix[1]));
            if (vn.getNetworkIpam() != null) {
                for (ObjectReference<VnSubnetsType> ref : vn.getNetworkIpam()) {
                    vnSubnetsType = ref.getAttr();
                }
            } else {
                vnSubnetsType.addIpamSubnets(subnetType, subnet.getGatewayIP(), subnet.getSubnetUUID());
            }
            vn.setNetworkIpam(ipam, vnSubnetsType);
        }
        return vn;
    }

    /**
     * Invoked to get the IP Prefix from the Neutron Subnet object.
     *
     * @param subnet
     *            An instance of new Neutron Subnet object.
     *
     * @return IP Prefix
     * @throws Exception
     */
    String[] getIpPrefix(NeutronSubnet subnet) {
        String[] ipPrefix = null;
        String cidr = subnet.getCidr();
        if (cidr.contains("/")) {
            ipPrefix = cidr.split("/");
        } else {
            throw new IllegalArgumentException("String " + cidr + " not in correct format..");
        }
        return ipPrefix;
    }

    /**
     * Invoked when a subnet update is requested to indicate if the specified
     * subnet can be changed using the specified delta.
     *
     * @param delta
     *            Updates to the subnet object using patch semantics.
     * @param original
     *            An instance of the Neutron Subnet object to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateSubnet(NeutronSubnet deltaSubnet, NeutronSubnet originalSubnet) {
        if (deltaSubnet == null || originalSubnet == null) {
            LOGGER.error("Neutron Subnets can't be null..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        if (deltaSubnet.getGatewayIP() == null || ("").equals(deltaSubnet.getGatewayIP().toString())) {
            LOGGER.error("Gateway IP can't be empty/null`..");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        boolean isvalidGateway = validGatewayIP(originalSubnet, deltaSubnet.getGatewayIP());
        if (!isvalidGateway) {
            LOGGER.error("Incorrect gateway IP....");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        apiConnector = Activator.apiConnector;
        try {
            boolean ifSubnetExist = false;
            VirtualNetwork virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, originalSubnet.getNetworkUUID());
            List<ObjectReference<VnSubnetsType>> ipamRefs = virtualnetwork.getNetworkIpam();
            if (ipamRefs != null) {
                for (ObjectReference<VnSubnetsType> ref : ipamRefs) {
                    VnSubnetsType vnSubnetsType = ref.getAttr();
                    if (vnSubnetsType != null) {
                        List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
                        for (VnSubnetsType.IpamSubnetType subnetValue : subnets) {
                            boolean doesSubnetExist = subnetValue.getSubnetUuid().matches(originalSubnet.getSubnetUUID());
                            if (doesSubnetExist) {
                                subnetValue.setDefaultGateway(deltaSubnet.getGatewayIP());
                                ifSubnetExist = true;
                            }
                        }
                    }
                }
            }
            if (ifSubnetExist) {
                boolean subnetUpdate = apiConnector.update(virtualnetwork);
                if (!subnetUpdate) {
                    LOGGER.warn("Subnet upadtion failed..");
                    return HttpURLConnection.HTTP_INTERNAL_ERROR;
                } else {
                    LOGGER.info(" Subnet " + originalSubnet.getCidr() + " sucessfully updated with gateway IP : " + deltaSubnet.getGatewayIP());
                    return HttpURLConnection.HTTP_OK;
                }
            } else {
                LOGGER.warn("Subnet upadtion failed..");
                return HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
        } catch (IOException e) {
            LOGGER.error("Exception :     " + e);
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
    }

    /**
     * Invoked to take action after a subnet has been updated.
     *
     * @param subnet
     *            An instance of modified Neutron Subnet object.
     */
    @Override
    public void neutronSubnetUpdated(NeutronSubnet subnet) {
        try {
            boolean ifSubnetExist = false;
            VirtualNetwork virtualnetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
            List<ObjectReference<VnSubnetsType>> ipamRefs = virtualnetwork.getNetworkIpam();
            if (ipamRefs != null) {
                for (ObjectReference<VnSubnetsType> ref : ipamRefs) {
                    VnSubnetsType vnSubnetsType = ref.getAttr();
                    if (vnSubnetsType != null) {
                        List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
                        for (VnSubnetsType.IpamSubnetType subnetValue : subnets) {
                            boolean doesSubnetExist = subnetValue.getDefaultGateway().matches(subnet.getGatewayIP());
                            if (doesSubnetExist) {
                                ifSubnetExist = true;
                            }
                        }
                    }
                }
            }
            if (ifSubnetExist) {
                LOGGER.info("Subnet upadtion verified..");
            } else {
                LOGGER.warn("Subnet upadtion failed..");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception :     " + ex);
        }
    }

    /**
     * Invoked when a subnet deletion is requested to indicate if the specified
     * subnet can be deleted and then delete the subnet.
     *
     * @param subnet
     *            An instance of the Neutron Subnet object to be deleted.
     *
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteSubnet(NeutronSubnet subnet) {
        apiConnector = Activator.apiConnector;
        VirtualNetwork virtualNetwork = null;
        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
            boolean subnetDelete = deleteSubnet(subnet, virtualNetwork);
            if (!subnetDelete) {
                LOGGER.error("Subnet deletion failed..");
                return HttpURLConnection.HTTP_INTERNAL_ERROR;
            } else {
                LOGGER.info("Subnet " + subnet.getCidr() + " sucessfully deleted from network  : " + virtualNetwork.getUuid());
                return HttpURLConnection.HTTP_NO_CONTENT;
            }
        } catch (Exception e) {
            LOGGER.error("Exception :     ", e.getMessage());
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
    }

    boolean validGatewayIP(NeutronSubnet subnet, String ipAddress) {
        try {

            SubnetUtils util = new SubnetUtils(subnet.getCidr());
            SubnetInfo info = util.getInfo();
            boolean inRange = info.isInRange(ipAddress);
            if (!inRange) {
                return false;
            } else {
                // ip available in allocation pool
                Iterator<NeutronSubnet_IPAllocationPool> i = subnet.getAllocationPools().iterator();
                while (i.hasNext()) {
                    NeutronSubnet_IPAllocationPool pool = i.next();
                    if (pool.contains(ipAddress)) {
                        return true;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Exception  :  " + e);
            return false;
        }
    }

    /**
     * Invoked to delete a specified subnet.
     *
     * @param subnet
     *            An instance of the Neutron Subnet object to be deleted.
     *
     * @param virtualNetwork
     *            An instance of the Virtual network object.
     *
     * @return A HTTP status code to the deletion request.
     */
    private boolean deleteSubnet(NeutronSubnet subnet, VirtualNetwork virtualNetwork) {
        try {
            VnSubnetsType.IpamSubnetType subnetVmType = null;
            VnSubnetsType vnSubnetsType = null;
            List<VnSubnetsType.IpamSubnetType> subnets = null;
            List<ObjectReference<VnSubnetsType>> ipamRefs = virtualNetwork.getNetworkIpam();
            if (ipamRefs != null) {
                for (ObjectReference<VnSubnetsType> ref : ipamRefs) {
                    vnSubnetsType = ref.getAttr();
                    if (vnSubnetsType != null) {
                        subnets = vnSubnetsType.getIpamSubnets();
                        for (VnSubnetsType.IpamSubnetType subnetValue : subnets) {
                            String[] ipPrefix = getIpPrefix(subnet);
                            boolean doesSubnetExist = subnetValue.getSubnet().getIpPrefix().matches(ipPrefix[0]);
                            if (doesSubnetExist) {
                                subnetVmType = subnetValue;
                            }
                        }
                    }
                }
                vnSubnetsType.clearIpamSubnets();
                for (VnSubnetsType.IpamSubnetType subnetVal : subnets) {
                    if (!subnetVal.getSubnet().getIpPrefix().matches(subnetVmType.getSubnet().getIpPrefix())) {
                        vnSubnetsType.addIpamSubnets(subnetVal);
                    }
                }
                if (vnSubnetsType.getIpamSubnets() != null) {
                    virtualNetwork.clearNetworkIpam();
                    String ipamId = apiConnector.findByName(NetworkIpam.class, null, "default-network-ipam");
                    NetworkIpam ipam = (NetworkIpam) apiConnector.findById(NetworkIpam.class, ipamId);
                    virtualNetwork.addNetworkIpam(ipam, vnSubnetsType);
                } else {
                    virtualNetwork.clearNetworkIpam();
                }
                return apiConnector.update(virtualNetwork);
            } else {
                LOGGER.error("Subnet deletion failed...");
                return false;
            }
        } catch (IOException ioEx) {
            LOGGER.error("Exception     : " + ioEx);
            return false;
        } catch (Exception ex) {
            LOGGER.error("Exception     : " + ex);
            return false;
        }
    }

    /**
     * Invoked to take action after a subnet has been deleted.
     *
     * @param subnet
     *            An instance of deleted Neutron Subnet object.
     */
    @Override
    public void neutronSubnetDeleted(NeutronSubnet subnet) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
            boolean ifSubnetExist = subnetExists(virtualNetwork.getNetworkIpam(), subnet);
            if (!ifSubnetExist) {
                LOGGER.info("Subnet deletion verified..");
            } else {
                LOGGER.warn("Subnet deletion failed..");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception :    " + ex);
        }
    }
}