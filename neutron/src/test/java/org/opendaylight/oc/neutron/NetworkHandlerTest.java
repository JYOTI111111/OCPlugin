package org.opendaylight.oc.neutron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;

/**
 * Test Class for Neutron Network.
 */
public class NetworkHandlerTest {

    NetworkHandler NetworkHandler;
    NeutronNetwork mockedNeutronNetwork = mock(NeutronNetwork.class);
    ApiConnector mockedApiConnector = mock (ApiConnector.class);
    VirtualNetwork mockedVirtualNetwork =mock (VirtualNetwork.class);
    NeutronNetwork neutron = new NeutronNetwork();

    @Before
    public void beforeTest(){
        NetworkHandler = new NetworkHandler();
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedNeutronNetwork);
        assertNotNull(mockedVirtualNetwork);
    }

    @After
    public void AfterTest(){
        NetworkHandler = null;
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
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandler.canCreateNetwork(null));
    }


    /* Test method to check if neutron network shared is null */
    @Test
    public void testCanCreateNetworkGetSharedNull() {
        Activator.apiConnector = mockedApiConnector;
        when(mockedNeutronNetwork.getShared()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandler.canCreateNetwork(mockedNeutronNetwork));
    }


    /* Test method to check if neutron network is shared or not */
    @Test
    public void testCanCreateNetworkIsShared() {
        Activator.apiConnector = mockedApiConnector;
        when(mockedNeutronNetwork.isShared()).thenReturn(true);
        assertEquals(HttpURLConnection.HTTP_NOT_ACCEPTABLE, NetworkHandler.canCreateNetwork(mockedNeutronNetwork));
    }


    /* Test method to check if api connector is null */
    @Test
    public void testCanCreateNetworkApiConnectorNull() {
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, NetworkHandler.canCreateNetwork(neutronNetwork));
    }


    /* Test method to check if neutron network uuid or name is null */
    @Test
    public void testCanCreateNetworkUuidNameNull() {
        Activator.apiConnector = mockedApiConnector;
        neutron.setNetworkUUID(null);
        neutron.setNetworkName(null);
        neutron.setShared(false);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandler.canCreateNetwork(neutron));
    }


    /* Test method to check if neutron network uuid is empty or name is null */
    @Test
    public void testCanCreateNetworkUuidEmpty() {
        Activator.apiConnector = mockedApiConnector;
        neutron.setNetworkUUID("");
        neutron.setNetworkName(null);
        neutron.setShared(false);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, NetworkHandler.canCreateNetwork(neutron));
    }


    /* Test method to check neutron network creation is successful
    @Test
    public void testCanCreateNetwork() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        when(mockedApiConnector.create(mockedVirtualNetwork)).thenReturn(true);
        assertEquals(HttpURLConnection.HTTP_OK, NetworkHandler.canCreateNetwork(neutronNetwork));
    }*/


    /* Test method to check neutron network creation fails with Internal Server Error */
    @Test
    public void testCanCreateNetworkInternalError() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronNetwork neutronNetwork = defaultNeutronObject();
        when(mockedApiConnector.create(mockedVirtualNetwork)).thenReturn(false);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, NetworkHandler.canCreateNetwork(neutronNetwork));
    }
}