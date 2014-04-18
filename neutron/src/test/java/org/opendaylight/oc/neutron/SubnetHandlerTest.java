package org.opendaylight.oc.neutron;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

public class SubnetHandlerTest {

    SubnetHandler SubnetHandlerObject;
    NeutronSubnet mockedNeutronSubnet = mock(NeutronSubnet.class);
    NeutronNetwork mockedNeutronNetwork = mock(NeutronNetwork.class);
    VirtualNetwork mockedVirtualNetwork = mock(VirtualNetwork.class);
    ApiConnector mockedApiConnector = mock(ApiConnector.class);
    SubnetType mockedSubnetType = mock(SubnetType.class);
    NetworkIpam mockedNetworkIpam = mock(NetworkIpam.class);
    SubnetHandler subnetMock = mock(SubnetHandler.class);


    @Before
    public void beforeTest(){
        SubnetHandlerObject = new SubnetHandler();
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedNeutronNetwork);
        assertNotNull(mockedVirtualNetwork);
    }

    @After
    public void afterTest(){
        SubnetHandlerObject = null;
        Activator.apiConnector = null;
    }

    /* dummy params for Neutron Subnet  */

    public NeutronSubnet defaultSubnetObject(){
        NeutronSubnet subnet = new NeutronSubnet();
        subnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setCidr("10.0.0.1/24");
        subnet.setGatewayIP("10.0.0.0");
        return subnet;
    }

    /* Test method to check if neutron subnet is null */
    @Test
    public void testCanCreateSubnetNull() {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, SubnetHandlerObject.canCreateSubnet(null));
    }


    /*  Test method to check if Api server is available */
    @Test
    public void testcanCreateSubnetApiConnectorNull() {
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, SubnetHandlerObject.canCreateSubnet(neutronSubnet));
    }


    /* Test method to check if virtual network is null */
    @Test
    public void testCanCreateSubnetVirtualNetworkNull() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class,neutronSubnet.getNetworkUUID())).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, SubnetHandlerObject.canCreateSubnet(neutronSubnet));
    }


    /* Test method to check if subnet can be created  */
    @Test
    public void testCanCreateSubnet() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class,neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.update(mockedVirtualNetwork)).thenReturn(true);
        assertEquals(HttpURLConnection.HTTP_OK, SubnetHandlerObject.canCreateSubnet(neutronSubnet));
    }


    /* Test method to check if subnet creation returns Internal Server Error  */
    @Test
    public void testCanCreateSubnetException() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class,neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.update(mockedVirtualNetwork)).thenReturn(false);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, SubnetHandlerObject.canCreateSubnet(neutronSubnet));
    }


    /* Test method to check ipPrefix  */
    @Test
    public void testGetIpPrefix() {
        SubnetHandlerObject.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        String cidr = "10.0.0.1/24";
        String[] ipPrefix = cidr.split("/");
        assertArrayEquals(ipPrefix, SubnetHandlerObject.getIpPrefix(neutronSubnet));
    }


    /* Test method to check if ipPrefix is null  */
    @Test(expected=NullPointerException.class)
    public void testGetIpPrefixNull() {
        SubnetHandlerObject.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = new NeutronSubnet();
        neutronSubnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        neutronSubnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        SubnetHandlerObject.getIpPrefix(neutronSubnet);
    }


    /* Test method to check if ipPrefix is valid  */
    @Test(expected=IllegalArgumentException.class)
    public void testGetIpPrefixInvalid() {
        SubnetHandlerObject.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = new NeutronSubnet();
        neutronSubnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        neutronSubnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        neutronSubnet.setCidr("10.0.0.1");
        SubnetHandlerObject.getIpPrefix(neutronSubnet);
    }


    /* Test method to check if network is available  */
    @Test
    public void testGetNetwork() throws IOException {
        SubnetHandlerObject.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class,neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        assertNotNull(SubnetHandlerObject.getNetwork(neutronSubnet));
    }
}