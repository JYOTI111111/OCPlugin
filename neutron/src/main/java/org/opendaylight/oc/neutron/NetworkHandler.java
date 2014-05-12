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
import java.util.UUID;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler implements INeutronNetworkAware {

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
    *            An instance of new Neutron Network object.
    *
    * @return A HTTP status code to the creation request.
     */
     @Override
     public int canCreateNetwork(NeutronNetwork network) {
         if (network == null){
              LOGGER.error("Network object can't be null..");
              return HttpURLConnection.HTTP_BAD_REQUEST;
         }
         LOGGER.debug("Network object " + network);

         apiConnector = Activator.apiConnector;

         if(network.getNetworkUUID() == null || network.getNetworkName() == null || network.getNetworkUUID().equals("") || network.getNetworkName().equals("")) {
             LOGGER.error("Network UUID and Network Name can't be null/empty...");
             return HttpURLConnection.HTTP_BAD_REQUEST;
         }
         try{
             return createNetwork(network);
         }
         catch(IOException ie){
             LOGGER.error("IOException :   "+ie);
             return HttpURLConnection.HTTP_INTERNAL_ERROR;
         }
         catch(Exception e){
             LOGGER.error("Exception :   "+e);
             return HttpURLConnection.HTTP_INTERNAL_ERROR;
         }

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
          try{
          virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
          if(virtualNetwork != null)
          {
               LOGGER.info("Network creation verified....");
          }
          }
          catch(Exception e){
               LOGGER.error("Exception :     "+e);
          }
      }

      /**
       * Invoked to create the specified Neutron Network.
       *
       * @param network
       *            An instance of new Neutron Network object.
       *
       * @return A HTTP status code to the creation request.
       */
      private int createNetwork(NeutronNetwork network) throws IOException{
           VirtualNetwork virtualNetwork = null;
           String networkUUID = null;
           try{
               networkUUID = UUID.fromString(network.getNetworkUUID()).toString();
           }catch(Exception ex){
               LOGGER.error("NetworkUUID incorrect format : ",ex);
               return HttpURLConnection.HTTP_BAD_REQUEST;
           }
           virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUUID);
           if(virtualNetwork != null)
           {
               LOGGER.warn("Network already exists..");
               return HttpURLConnection.HTTP_FORBIDDEN;
           }
           virtualNetwork = new VirtualNetwork();
           // map neutronNetwork to virtualNetwork
           virtualNetwork = mapNetworkProperties(network, virtualNetwork);
           boolean networkCreated = apiConnector.create(virtualNetwork);
           if (!networkCreated) {
               LOGGER.warn("Network creation failed..");
               return HttpURLConnection.HTTP_INTERNAL_ERROR;
           }
           LOGGER.info("Network : " + virtualNetwork.getName() +
                      "  having UUID : " + virtualNetwork.getUuid() +
                      "  sucessfully created...");
           return HttpURLConnection.HTTP_OK;
      }


   /**
   * Invoked to map the NeutronNetwork object properties to the virtualNetwork object.
   * @param neutronNetwork
   *        An instance of new Neutron Network object.
   * @param virtualNetwork
   *        An instance of new virtualNetwork object.
   * @return {@link VirtualNetwork}
   */
      private VirtualNetwork mapNetworkProperties(NeutronNetwork neutronNetwork,VirtualNetwork virtualNetwork) {
           String networkUUID = neutronNetwork.getNetworkUUID();
           String networkName = neutronNetwork.getNetworkName();
           virtualNetwork.setName(networkName);
           virtualNetwork.setUuid(networkUUID);
           virtualNetwork.setDisplayName(networkName);
           return virtualNetwork;
      }

  /**
    * Invoked when a network update is requested to indicate if the specified
    * network can be changed using the specified delta and then update the network.
    * @param delta
    *        Updates to the network object using patch semantics.
    * @param original
    *        An instance of the Neutron Network object to be updated.
    *  @return A HTTP status code to the update request.
    */
      @Override
      public int canUpdateNetwork(NeutronNetwork deltaNetwork, NeutronNetwork originalNetwork) {
          VirtualNetwork virtualnetwork = new VirtualNetwork();
          apiConnector = Activator.apiConnector;

          if(deltaNetwork==null || originalNetwork==null ) {
              LOGGER.error("Neutron Networks can't be null..");
              return HttpURLConnection.HTTP_BAD_REQUEST;
          }
          if(("").equals(deltaNetwork.getNetworkName())) {
              LOGGER.error("Neutron Networks name can't be empty..");
               return HttpURLConnection.HTTP_BAD_REQUEST;
          }
          try {
              virtualnetwork =(VirtualNetwork) apiConnector.findById(VirtualNetwork.class, originalNetwork.getNetworkUUID());

          } catch (IOException e) {
              LOGGER.error("Exception :     "+e);
              return HttpURLConnection.HTTP_INTERNAL_ERROR;
          }

          if(virtualnetwork==null){
              LOGGER.error("No network exists for the specified UUID...");
              return HttpURLConnection.HTTP_FORBIDDEN;
          }
          else{
              try{
                  return updateNetwork(deltaNetwork,virtualnetwork);
              }catch(IOException ie){
                  LOGGER.error("IOException:     "+ie);
                  return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }
              catch(Exception e){
                  LOGGER.error("Exception:     "+e);
                  return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }
          }
      }


      /**
       * Invoked to update the network
       *
       *  @param delta_network
       *            An instance of Neutron Network.
       *  @param virtualNetwork
       *            An instance of new virtualNetwork object.
       *
       * @return A HTTP status code to the creation request.
       */
      private int updateNetwork(NeutronNetwork deltaNetwork, VirtualNetwork virtualNetwork) throws IOException{
          String networkName = deltaNetwork.getNetworkName();
          virtualNetwork.setName(networkName);
          virtualNetwork.setDisplayName(networkName);
          {
              boolean networkUpdate= apiConnector.update(virtualNetwork);
              if(!networkUpdate)
              {
                  LOGGER.warn("Network Updation failed..");
                  return HttpURLConnection.HTTP_INTERNAL_ERROR;
              }
                  LOGGER.info("Network having UUID : " + virtualNetwork.getUuid() + "  has been sucessfully updated...");
                  return HttpURLConnection.HTTP_OK;
          }
      }

   /**
    * Invoked to take action after a network has been updated.
    * @param network
    *            An instance of modified Neutron Network object.
    */
      @Override
      public void neutronNetworkUpdated(NeutronNetwork network) {
          try{
              VirtualNetwork virtualnetwork = new VirtualNetwork();
              virtualnetwork =(VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());

              if (network.getNetworkName().equalsIgnoreCase(virtualnetwork.getDisplayName())) {
                  LOGGER.info("Network updatation verified....");
              }
              else {
                  LOGGER.info("Network updatation failed....");
              }
              }
          catch(Exception e) {
              LOGGER.error("Exception :"+e);
              }
          }

   /**
     * Invoked when a network deletion is requested to indicate if the specified
     * network can be deleted and then delete the network.
     *  @param network
     *         An instance of the Neutron Network object to be deleted.
     *  @return A HTTP status code to the deletion request.
     */
      @Override
     public int canDeleteNetwork(NeutronNetwork network) {
          apiConnector = Activator.apiConnector;
          VirtualNetwork virtualNetwork = null;
          try{
          virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
          if(virtualNetwork != null) {
              if(virtualNetwork.getVirtualMachineInterfaceBackRefs()!= null) {
                  LOGGER.info("Network with UUID :  "+ network.getNetworkUUID() +" cannot be deleted as it has port(s) associated with it....");
                  return HttpURLConnection.HTTP_FORBIDDEN;
              }
              else{
               apiConnector.delete(virtualNetwork);
               LOGGER.info("Network with UUID :  "+ network.getNetworkUUID() +"  has been deleted successfully....");
               return HttpURLConnection.HTTP_OK;
              }
          }
          else{
              LOGGER.info("No Network exists with UUID :  "+ network.getNetworkUUID() );
              return HttpURLConnection.HTTP_BAD_REQUEST;
          }
          }
          catch(Exception e){
               LOGGER.error("Exception : "+e);
               return HttpURLConnection.HTTP_INTERNAL_ERROR;
          }
}

    /**
     * Invoked to take action after a network has been deleted.
     * @param network
     *            An instance of deleted Neutron Network object.
     */
      @Override
      public void neutronNetworkDeleted(NeutronNetwork network) {
          VirtualNetwork virtualNetwork = null;
          try{
          virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getNetworkUUID());
          if(virtualNetwork == null) {
               LOGGER.info("Network deletion verified....");
          }
          else{
               LOGGER.info("Network deletion failed....");
          }
          }
          catch(Exception e){
               LOGGER.error("Exception :   "+e);
          }
      }
}