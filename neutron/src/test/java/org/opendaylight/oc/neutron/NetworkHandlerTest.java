package org.opendaylight.oc.neutron;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;

public class NetworkHandlerTest {

    NetworkHandler NetworkHandlerObject;
    NeutronNetwork mockedNeutronNetwork = mock(NeutronNetwork.class);
    ApiConnector mockedApiConnector = mock (ApiConnector.class);
    VirtualNetwork mockedVirtualNetwork =mock (VirtualNetwork.class);
    NeutronNetwork neutron = new NeutronNetwork();

    @Before
    public void beforeTest(){
        NetworkHandlerObject = new NetworkHandler();
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedNeutronNetwork);
        assertNotNull(mockedVirtualNetwork);
    }

    @After
    public void AfterTest(){
        NetworkHandlerObject = null;
        Activator.apiConnector = null;
    }

    /* dummy params for Neutron Network  */

    public NeutronNetwork defaultNeutronObject(){
        neutron.setNetworkName("Virtual Network");
        neutron.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29a");
        neutron.setShared(false);
        return neutron;
    }

    /* Test method to check if neutron network is null */
    @Test
    public void testCanCreateNetworkNull() {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandlerObject.canCreateNetwork(null));
    }


    /* Test method to check if neutron network shared is null */
    @Test
    public void testCanCreateNetworkGetSharedNull() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        when(mockedNeutronNetwork.getShared()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandlerObject.canCreateNetwork(mockedNeutronNetwork));
    }


    /* Test method to check if neutron network is shared or not */
    @Test
    public void testCanCreateNetworkIsShared() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        when(mockedNeutronNetwork.isShared()).thenReturn(true);
        assertEquals(HttpURLConnection.HTTP_NOT_ACCEPTABLE, NetworkHandlerObject.canCreateNetwork(mockedNeutronNetwork));
    }


    /* Test method to check if api connector is null */
    @Test
    public void testCanCreateNetworkApiConnectorNull() {
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, NetworkHandlerObject.canCreateNetwork(neutronNetwork));
    }


    /* Test method to check if neutron network uuid or name is null */
    @Test
    public void testCanCreateNetworkUuidNameNull() {
        Activator.apiConnector = mockedApiConnector;
        neutron.setNetworkUUID(null);
        neutron.setNetworkName(null);
        neutron.setShared(false);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandlerObject.canCreateNetwork(neutron));
    }


    /* Test method to check if neutron network uuid is empty or name is null */
    @Test
    public void testCanCreateNetworkUuidEmpty() {
        Activator.apiConnector = mockedApiConnector;
        neutron.setNetworkUUID("");
        neutron.setNetworkName(null);
        neutron.setShared(false);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandlerObject.canCreateNetwork(neutron));
    }


    /* Test method to check neutron network creation is successful */
    @Test
    public void testCanCreateNetwork() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        when(mockedApiConnector.create(mockedVirtualNetwork)).thenReturn(true);
        assertEquals(HttpURLConnection.HTTP_OK, NetworkHandlerObject.canCreateNetwork(neutronNetwork));
    }

    
//    @Test
//    public void testCanCreateNetworkCreate() throws Exception {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronNetwork neutronNetwork = defaultNeutronObject();
//        VirtualNetwork virtualNetwork = new VirtualNetwork();
//        virtualNetwork.setName("Virtual Network");
//        virtualNetwork.setUuid("6b9570f2-17b1-4fc399ec-1b7f7778a29a");
//        when(mockedApiConnector.create(virtualNetwork)).thenReturn(true);
//        assertEquals(HttpURLConnection.HTTP_OK, NetworkHandlerObject.createNetwork(neutronNetwork));
//    }
    

    /* Test method to check neutron network creation fails with Internal Server Error */
    @Test
    public void testCanCreateNetworkInternalError() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        when(mockedApiConnector.create(mockedVirtualNetwork)).thenReturn(false);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, NetworkHandlerObject.canCreateNetwork(neutronNetwork));
    }
}