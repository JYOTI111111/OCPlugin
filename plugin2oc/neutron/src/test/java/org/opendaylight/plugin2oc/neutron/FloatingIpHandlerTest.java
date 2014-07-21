//package org.opendaylight.plugin2oc.neutron;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import java.io.IOException;
//import java.net.HttpURLConnection;
//
//import net.juniper.contrail.api.ApiConnector;
//import net.juniper.contrail.api.types.FloatingIpPool;
//import net.juniper.contrail.api.types.Project;
//import net.juniper.contrail.api.types.VirtualNetwork;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
//
///**
// * Test Class for Floating Ip.
// */
//public class FloatingIpHandlerTest {
//    FloatingIpHandler floatingIpHandler;
//    NeutronFloatingIP mockedNeutronFloatingIP = mock(NeutronFloatingIP.class);
//    ApiConnector mockedApiConnector = mock(ApiConnector.class);
//    VirtualNetwork mockedVirtualNetwork = mock(VirtualNetwork.class);
//    Project mockProject = mock(Project.class);
//    FloatingIpPool mockFloatingIpPool = mock(FloatingIpPool.class);
//
//    @Before
//    public void beforeTest() {
//        floatingIpHandler = new FloatingIpHandler();
//        assertNotNull(mockedApiConnector);
//        assertNotNull(mockedNeutronFloatingIP);
//        assertNotNull(mockedVirtualNetwork);
//        assertNotNull(mockProject);
//        assertNotNull(mockFloatingIpPool);
//    }
//
//    @After
//    public void AfterTest() {
//        floatingIpHandler = null;
//        Activator.apiConnector = null;
//    }
//
//    /* dummy params for Neutron Floating Ip */
//    public NeutronFloatingIP defaultNeutronObject() {
//        NeutronFloatingIP neutronFloatingIP = new NeutronFloatingIP();
//        neutronFloatingIP.setFloatingIPUUID("000570f2-0000-0000-0000-1b7f7778a29a");
//        neutronFloatingIP.setFloatingNetworkUUID("6b9570f2-17b1-4fc3-99ec-1b7f7778a29a");
//        neutronFloatingIP.setTenantUUID("35ac67aa-d812-4f24-818f-c3ab10e865cb");
//        neutronFloatingIP.setPortUUID("111c67aa-0000-0000-818f-c3ab10e865cb");
//        neutronFloatingIP.setFixedIPAddress("10.0.0.10");
//        neutronFloatingIP.setFloatingIPAddress("10.0.0.90");
//        return neutronFloatingIP;
//    }
//
//    /* Test method to check if neutron floating ip is null */
//    @Test
//    public void testCanCreateFloatingIpNull() {
//        Activator.apiConnector = mockedApiConnector;
//        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, floatingIpHandler.canCreateFloatingIP(null));
//    }
//
//    @Test
//    public void testCanCreateFloatingIpUUIDEmpty() {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        neutronFloatingIP.setFloatingIPUUID("");
//        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//
//    @Test
//    public void testCanCreateFloatingIpTenantIdEmpty() {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        neutronFloatingIP.setTenantUUID(null);
//        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//
//    @Test
//    public void testCanCreateFloatingIpFloatingIpNull() {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        neutronFloatingIP.setFloatingIPAddress(null);
//        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//
//    /* Test method to check if project is not found */
//    @Test
//    public void testCanCreateFlaotingIpProjectNotFound() throws IOException {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        when(mockedApiConnector.findById(Project.class, neutronFloatingIP.getTenantUUID())).thenReturn(null);
//        when(mockedApiConnector.findById(Project.class, neutronFloatingIP.getTenantUUID())).thenReturn(null);
//        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//
//    /* Test method to check if Virtual network is not found */
//    @Test
//    public void testCanCreateFlaotingIpVirtualNetworkNotFound() throws IOException {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        when(mockedApiConnector.findById(Project.class, neutronFloatingIP.getTenantUUID())).thenReturn(mockProject);
//        when(mockedApiConnector.findById(VirtualNetwork.class, neutronFloatingIP.getFloatingNetworkUUID())).thenReturn(null);
//        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//
//    /* Test method to check if floating ip pool is not found */
//    @Test
//    public void testCanCreateFlaotingIpFloatingIpPoolNotFound() throws IOException {
//        Activator.apiConnector = mockedApiConnector;
//        NeutronFloatingIP neutronFloatingIP = defaultNeutronObject();
//        VirtualNetwork vn = new VirtualNetwork();
//        vn.setName("Virtual network");
//        vn.setRouterExternal(true);
//        vn.setUuid("111c67aa-0000-0000-818f-c3ab10e86000");
//        FloatingIpPool fipool = new FloatingIpPool();
//        fipool.setParent(vn);
//        fipool.setName("Floating Ip pool");
//        fipool.setUuid("111c67aa-1212-2222-818f-c3ab10e86000");
//        String floatingIpPoolUUID = vn.getFloatingIpPools().get(0).getUuid();
//        System.out.println(floatingIpPoolUUID);
//        when(mockedApiConnector.findById(Project.class, neutronFloatingIP.getTenantUUID())).thenReturn(mockProject);
//        when(mockedApiConnector.findById(VirtualNetwork.class, neutronFloatingIP.getFloatingNetworkUUID())).thenReturn(mockedVirtualNetwork);
//        when(mockedApiConnector.findById(FloatingIpPool.class, floatingIpPoolUUID)).thenReturn(null);
//        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, floatingIpHandler.canCreateFloatingIP(neutronFloatingIP));
//    }
//}
