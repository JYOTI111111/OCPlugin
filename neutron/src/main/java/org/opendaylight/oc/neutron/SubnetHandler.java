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
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
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
   *            An instance of new Neutron Subnet object.
   *
   * @return A HTTP status code to the creation request.
   **/
  @Override
  public int canCreateSubnet(NeutronSubnet subnet) {
      VirtualNetwork virtualnetwork = new VirtualNetwork();
      apiConnector = Activator.apiConnector;

      if(subnet==null) {
          LOGGER.error("Neutron Subnet can't be null..");
          return HttpURLConnection.HTTP_BAD_REQUEST;
          }

      try {
          virtualnetwork=getNetwork(subnet);
          } catch (IOException e) {
              LOGGER.error("Exception :     "+e);
              return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }

      if(virtualnetwork==null) {
          LOGGER.error("No network exists for the specified UUID...");
          return HttpURLConnection.HTTP_FORBIDDEN;
          }
      else {
          try {
              List<ObjectReference <VnSubnetsType>> ipamRefs = virtualnetwork.getNetworkIpam();
              if (ipamRefs != null) {
                  int result=subnetExists(ipamRefs,subnet);
                  if(result !=0) {
                      return result;
                      }
                  }
              } catch (Exception e) {
                  LOGGER.error("Exception :     "+e);
                  return HttpURLConnection.HTTP_INTERNAL_ERROR;
                  }
          try {
              return createSubnet(subnet,virtualnetwork);
              }
          catch(IOException ie) {
              LOGGER.error("IOException:     "+ie);
              return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }
          catch(Exception e) {
              LOGGER.error("Exception:     "+e);
              return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }
          }
      }

  private int subnetExists(List<ObjectReference <VnSubnetsType>> ipamRefs,NeutronSubnet subnet) {
      for (ObjectReference <VnSubnetsType> ref : ipamRefs) {
          VnSubnetsType vnSubnetsType = ref.getAttr();
          if(vnSubnetsType != null) {
              List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
              if(subnets != null) {
                  if(subnet.getCidr()==null) {
                      LOGGER.error("CIDR can't be null");
                      return HttpURLConnection.HTTP_BAD_REQUEST;
                      }
                  for(VnSubnetsType.IpamSubnetType subnetValue: subnets) {
                      String[] ipPrefix=getIpPrefix(subnet);
                      Boolean doesSubnetExist = subnetValue.getSubnet().getIpPrefix().matches(ipPrefix[0]);
                      if(doesSubnetExist) {
                          LOGGER.error("The subnet already exists..");
                          return HttpURLConnection.HTTP_FORBIDDEN;
                          }
                      }
                  }
              }
          }
      return 0;
      }


  /**
   * Invoked to create the subnet
   *
   *  @param subnet
   *            An instance of new Subnet Type object.
   */
  @Override
  public void neutronSubnetCreated(NeutronSubnet subnet) {
      VirtualNetwork virtualNetwork = null;
      try {
          virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, subnet.getNetworkUUID());
          List<ObjectReference <VnSubnetsType>> ipamRefs = virtualNetwork.getNetworkIpam();
          if (ipamRefs != null) {
              for (ObjectReference <VnSubnetsType> ref : ipamRefs) {
                  VnSubnetsType vnSubnetsType = ref.getAttr();
                  if(vnSubnetsType != null) {
                      List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetsType.getIpamSubnets();
                      for(VnSubnetsType.IpamSubnetType subnetValue: subnets) {
                          String[] ipPrefix=getIpPrefix(subnet);
                          boolean doesSubnetExist = subnetValue.getSubnet().getIpPrefix().matches(ipPrefix[0]);
                          if(doesSubnetExist) {
                              LOGGER.info("Subnet creation verified...");
                              }
                          else{
                              LOGGER.info("Subnet creation failed...");
                          }
                          }
                      }
                  }
              }
          } catch (Exception e) {
              LOGGER.error("Exception :     "+e);
              }
      }


  /**
   * Invoked to create the subnet
   *
   *  @param subnet
   *            An instance of new Subnet Type object.
   *  @param virtualNetwork
   *            An instance of new virtualNetwork object.
   *
   * @return A HTTP status code to the creation request.
   */
  private int createSubnet(NeutronSubnet subnet, VirtualNetwork virtualNetwork) throws IOException {
      //add subnet properties to the virtual-network object
      VirtualNetwork virtualnetwork = mapSubnetProperties(subnet, virtualNetwork);
      boolean subnetCreate= apiConnector.update(virtualnetwork);
      if(!subnetCreate) {
          LOGGER.warn("Subnet creation failed..");
          return HttpURLConnection.HTTP_INTERNAL_ERROR;
          }
      else {
          LOGGER.info("Subnet " + subnet.getCidr() + "  sucessfully added to the network having UUID : " + virtualnetwork.getUuid() );
          return HttpURLConnection.HTTP_OK;
          }
      }

  /**
   * Invoked to add the NeutronSubnet properties to the virtualNetwork object.
   *
   * @param subnet
   *        An instance of new Neutron Subnet object.
   * @param virtualNetwork
   *       An instance of new virtualNetwork object.
   *
   * @return {@link VirtualNetwork}
   */
   private VirtualNetwork mapSubnetProperties(NeutronSubnet subnet, VirtualNetwork vn) {
       String[] ipPrefix=null;
       NetworkIpam ipam = null;
       VnSubnetsType vnSubnetsType=new VnSubnetsType();
       SubnetType subnetType = new SubnetType();

   try {
       ipPrefix=getIpPrefix(subnet);
       //   Find default-network-ipam
       String ipamId = apiConnector.findByName(NetworkIpam.class, null, "default-network-ipam");
       ipam = (NetworkIpam)apiConnector.findById(NetworkIpam.class, ipamId);
       }
   catch (IOException ex) {
       LOGGER.error("IOException :    "+ex);
       }
   catch (Exception ex) {
       LOGGER.error("Exception :   "+ex);
       }
   if(ipPrefix != null) {
       subnetType.setIpPrefix(ipPrefix[0]);
       subnetType.setIpPrefixLen(Integer.valueOf(ipPrefix[1]));
       if(vn.getNetworkIpam() !=null) {
           for (ObjectReference <VnSubnetsType> ref : vn.getNetworkIpam()) {
               vnSubnetsType = ref.getAttr();
               }
           }
       vnSubnetsType.addIpamSubnets(subnetType, subnet.getGatewayIP());
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
 * subnet can be changed using the specified delta and then update the subnet.
 * @param delta
 *            Updates to the subnet object using patch semantics.
 * @param original
 *            An instance of the Neutron Subnet object to be updated.
 * @return A HTTP status code to the update request.
 */
   @Override
   public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
       return HttpURLConnection.HTTP_CREATED;
       }


/**
 * Invoked to take action after a subnet has been updated.
 * @param subnet
 *            An instance of modified Neutron Subnet object.
 */
  @Override
  public void neutronSubnetUpdated(NeutronSubnet subnet) {
      // TODO Auto-generated method stub
      }

  /**
   * Invoked when a subnet deletion is requested to indicate if the specified
   * subnet can be deleted.
   *  @param subnet
   *       An instance of the Neutron Subnet object to be deleted.
   * @return A HTTP status code to the deletion request.
   */
   @Override
   public int canDeleteSubnet(NeutronSubnet subnet) {
       // TODO Auto-generated method stub
       return HttpURLConnection.HTTP_CREATED;
       }


  /**
   * Invoked to take action after a subnet has been deleted.
   * @param subnet
   *            An instance of deleted Neutron Subnet object.
   */
   @Override
   public void neutronSubnetDeleted(NeutronSubnet subnet) {
       // TODO Auto-generated method stub
       }



  /**
   * Invoked to get the virtualNetwork object.
   *
   * @param subnet
   *       An instance of new Neutron Subnet object.
   * @param virtualNetwork
   *       An instance of new virtualNetwork object.
   *
   * @return {@link VirtualNetwork}
   */
  VirtualNetwork getNetwork(NeutronSubnet subnet) throws IOException {
      VirtualNetwork virtualNetwork= new VirtualNetwork();
      String networkUuid=subnet.getNetworkUUID();
      virtualNetwork =(VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
      return virtualNetwork;
      }
  }