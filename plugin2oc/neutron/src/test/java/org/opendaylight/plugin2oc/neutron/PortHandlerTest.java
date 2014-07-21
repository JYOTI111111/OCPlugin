/*
d * Copyright (C) 2014 Juniper Networks, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plugin2oc.neutron;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.easymock.PowerMock.expectNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PortHandler.class, VirtualMachineInterface.class, InstanceIp.class })
public class PortHandlerTest {
    PortHandler porthandler;
    PortHandler mockedporthandler = mock(PortHandler.class);
    NeutronNetwork mockedNeutronNetwork = mock(NeutronNetwork.class);
    ApiConnector mockedApiConnector = mock(ApiConnector.class);
    VirtualNetwork mockedVirtualNetwork = mock(VirtualNetwork.class);
    NeutronPort mockedNeutronPort = mock(NeutronPort.class);
    VirtualMachineInterface mockedVirtualMachineInterface = mock(VirtualMachineInterface.class);
    VirtualMachine mockedvirtualMachine = mock(VirtualMachine.class);
    Project mockedProject = mock(Project.class);
    Neutron_IPs mockNeutron_IPs = mock(Neutron_IPs.class);
    InstanceIp mockInstanceIp = mock(InstanceIp.class);
    NeutronCRUDInterfaces mockNeutronCRUDInterfaces = mock(NeutronCRUDInterfaces.class);

    @Before
    public void beforeTest() {
        porthandler = new PortHandler();
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedNeutronNetwork);
        assertNotNull(mockedVirtualNetwork);
        assertNotNull(mockedNeutronPort);
        assertNotNull(mockedVirtualMachineInterface);
        assertNotNull(mockedvirtualMachine);
        assertNotNull(mockNeutron_IPs);
        assertNotNull(mockInstanceIp);
        assertNotNull(mockedProject);
    }

    @After
    public void AfterTest() {
        porthandler = null;
        Activator.apiConnector = null;
    }

    /* dummy params for Neutron Port */
    public NeutronPort defaultNeutronPortObject() {
        NeutronPort neutronPort = new NeutronPort();
        neutronPort.setPortUUID("64a271fe-0216-46bc-a3e6-1ff582fbd324");
        neutronPort.setNetworkUUID("54a271fe-0216-46bc-a3e6-1ff582fbd324");
        neutronPort.setMacAddress("02:70:72:93:4d:d6");
        neutronPort.setName("port12");
        neutronPort.setDeviceID("100071fe-0216-46bc-a3e6-1ff582fbd324");
        neutronPort.setTenantID("100071fe-0216-46bc-a3e6-1ff582fbd324");
        List<Neutron_IPs> ips = new ArrayList<Neutron_IPs>();
        ips.add(mockNeutron_IPs);
        neutronPort.setFixedIPs(ips);
        return neutronPort;
    }

    /* dummy params for Update Neutron Port */
    public NeutronPort detaNeutronPort() {
        NeutronPort dummyNeutronPort = new NeutronPort();
        dummyNeutronPort.setPortUUID("64a271fe-0216-46bc-a3e6-1ff582fbd324");
        dummyNeutronPort.setNetworkUUID("54a271fe-0216-46bc-a3e6-1ff582fbd324");
        dummyNeutronPort.setMacAddress("00:70:72:93:4d:d6");
        dummyNeutronPort.setName("port01");
        dummyNeutronPort.setDeviceID("100071fe-0216-46bc-a3e6-1ff582fbd324");
        dummyNeutronPort.setTenantID("100071fe-0216-46bc-a3e6-1ff582fbd324");
        List<Neutron_IPs> ips = new ArrayList<Neutron_IPs>();
        ips.add(mockNeutron_IPs);
        dummyNeutronPort.setFixedIPs(ips);
        return dummyNeutronPort;
    }

    /* Test method to check if neutron port is null */
    @Test
    public void testCanCreatePortNull() {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canCreatePort(null));
    }

    /* Test method to check if neutron port device ID is empty */
    @Test
    public void testCanCreateIdEmtpy() {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = new NeutronPort();
        neutronPort.setPortUUID("");
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canCreatePort(neutronPort));
    }

    /* Test method to check if neutron port tenant ID is empty */
    @Test
    public void testCanCreateTenantIdEmtpy() {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = new NeutronPort();
        neutronPort.setPortUUID("64a271fe-0216-46bc-a3e6-1ff582fbd324");
        neutronPort.setTenantID(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canCreatePort(neutronPort));
    }

    /* Test method to check if neutron port fixed IP is null */
    @Test
    public void testCanCreateFixedIPNull() {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        neutronPort.setFixedIPs(null);
        when(mockedNeutronPort.getFixedIPs()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, porthandler.canCreatePort(neutronPort));
    }

    /* Test method to check if neutron port Project is null */
    @Test
    public void testCanCreateProjectNull() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(null);
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, porthandler.canCreatePort(neutronPort));
    }

    /* Test method to check if neutron port canCreate returns status Ok */
    @Test
    public void testCanCreateOk() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(mockedProject);
        assertEquals(HttpURLConnection.HTTP_OK, porthandler.canCreatePort(neutronPort));
    }

    /* Test method to check if neutron port add port VM creation failed */
    @Test
    public void testAddPortVMFailed() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        VirtualMachine mockedvirtualMachine = PowerMock.createNiceMock(VirtualMachine.class);
        expectNew(VirtualMachine.class).andReturn(mockedvirtualMachine);
        when(mockedApiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID())).thenReturn(null);
        when(mockedApiConnector.findById(VirtualMachine.class, neutronPort.getDeviceID())).thenReturn(null);
        when(mockedApiConnector.create(mockedvirtualMachine)).thenReturn(false);
        PowerMock.replay(mockedvirtualMachine, VirtualMachine.class);
        assertEquals(false, porthandler.addPort(neutronPort));
    }

    /*
     * Test method to check if neutron port add port failed when virtual network
     * is null
     */
    @Test
    public void testAddPortVIrtualNetworkNull() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        when(mockedApiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID())).thenReturn(null);
        when(mockedApiConnector.findById(VirtualMachine.class, neutronPort.getDeviceID())).thenReturn(mockedvirtualMachine);
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(mockedProject);
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronPort.getNetworkUUID())).thenReturn(null);
        assertEquals(false, porthandler.addPort(neutronPort));
    }

    /*
     * Test method to check if neutron port add port failed when VMI is not
     * created
     */
    @Test
    public void testAddPortVMIcreateFailed() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        VirtualMachineInterface mockedVirtualMachineInterface = PowerMock.createNiceMock(VirtualMachineInterface.class);
        expectNew(VirtualMachineInterface.class).andReturn(mockedVirtualMachineInterface);
        when(mockedApiConnector.findById(VirtualMachineInterface.class, neutronPort.getID())).thenReturn(null);
        when(mockedApiConnector.findById(VirtualMachine.class, neutronPort.getDeviceID())).thenReturn(mockedvirtualMachine);
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(mockedProject);
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronPort.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.create(mockedVirtualMachineInterface)).thenReturn(false);
        PowerMock.replay(mockedVirtualMachineInterface, VirtualMachineInterface.class);
        assertEquals(false, porthandler.addPort(neutronPort));
    }

    /*
     * Test method to check if neutron port add port failed when instanceIp is
     * not created
     */
    @Test
    public void testAddPortInstanceIpCreateFailed() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        VirtualMachineInterface mockedVirtualMachineInterface = PowerMock.createNiceMock(VirtualMachineInterface.class);
        expectNew(VirtualMachineInterface.class).andReturn(mockedVirtualMachineInterface);
        InstanceIp mockedInstanceIp = PowerMock.createNiceMock(InstanceIp.class);
        expectNew(InstanceIp.class).andReturn(mockedInstanceIp);
        when(mockedApiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID())).thenReturn(null);
        when(mockedApiConnector.findById(VirtualMachine.class, neutronPort.getDeviceID())).thenReturn(mockedvirtualMachine);
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(mockedProject);
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronPort.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.create(mockedVirtualMachineInterface)).thenReturn(true);
        PowerMock.replay(mockedVirtualMachineInterface, VirtualMachineInterface.class);
        when(neutronPort.getFixedIPs().get(0).getIpAddress()).thenReturn("10.0.0.1");
        when(mockedApiConnector.create(mockedInstanceIp)).thenReturn(false);
        PowerMock.replay(mockedInstanceIp, InstanceIp.class);
        assertEquals(false, porthandler.addPort(neutronPort));
    }

    /* Test method to check port is created */
    @Test
    public void testAddPortOk() throws Exception {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        VirtualMachineInterface mockedVirtualMachineInterface = PowerMock.createNiceMock(VirtualMachineInterface.class);
        expectNew(VirtualMachineInterface.class).andReturn(mockedVirtualMachineInterface);
        InstanceIp mockedInstanceIp = PowerMock.createNiceMock(InstanceIp.class);
        expectNew(InstanceIp.class).andReturn(mockedInstanceIp);
        when(mockedApiConnector.findById(VirtualMachineInterface.class, neutronPort.getPortUUID())).thenReturn(null);
        when(mockedApiConnector.findById(VirtualMachine.class, neutronPort.getDeviceID())).thenReturn(mockedvirtualMachine);
        when(mockedApiConnector.findById(Project.class, neutronPort.getTenantID())).thenReturn(mockedProject);
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronPort.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.create(mockedVirtualMachineInterface)).thenReturn(true);
        PowerMock.replay(mockedVirtualMachineInterface, VirtualMachineInterface.class);
        when(neutronPort.getFixedIPs().get(0).getIpAddress()).thenReturn("10.0.0.1");
        when(mockedApiConnector.create(mockedInstanceIp)).thenReturn(true);
        PowerMock.replay(mockedInstanceIp, InstanceIp.class);
        assertEquals(true, porthandler.addPort(neutronPort));
    }

    /* Test method to check if neutron port is null for delete */
    @Test
    public void testcanDeletePortNull() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canDeletePort(null));
    }

    /* Test method to update port with null neutron port and delta port */
    @Test
    public void testcanUpdatePortNull() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canUpdatePort(null, null));
    }

    /* Test method to update port with delta port MacAddress can't be updated */
    @Test
    public void testcanUpdateMacAddress() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronPort neutronPort = defaultNeutronPortObject();
        NeutronPort dummyNeutronPort = detaNeutronPort();
        dummyNeutronPort.setMacAddress("00:70:72:93:4d:d6");
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, porthandler.canUpdatePort(dummyNeutronPort, neutronPort));
    }
    // /* Test method to canUpdate port OK */
    // @Test
    // public void testcanUpdateOK() throws IOException {
    // Activator.apiConnector = mockedApiConnector;
    // NeutronPort neutronPort = defaultNeutronPortObject();
    // NeutronPort dummyNeutronPort = detaNeutronPort();
    // dummyNeutronPort.setMacAddress(null);
    // neutronPort.setNetworkUUID(null);
    // dummyNeutronPort.setNetworkUUID(null);
    // assertEquals(HttpURLConnection.HTTP_OK,
    // porthandler.canUpdatePort(dummyNeutronPort, neutronPort));
    // }
    // /*Test method to update port OK*/
    // @Test
    // public void testUpdateOK() throws IOException {
    // Activator.apiConnector = mockedApiConnector;
    // NeutronPort neutronPort = defaultNeutronPortObject();
    // NeutronPort dummyNeutronPort = detaNeutronPort();
    // porthandler.setOriginalPort(neutronPort);
    // VnSubnetsType vnSubnetType = new VnSubnetsType();
    // ObjectReference<VnSubnetsType> ref = new ObjectReference<>();
    // List<ObjectReference<VnSubnetsType>> ipamRefs = new
    // ArrayList<ObjectReference<VnSubnetsType>>();
    // List<VnSubnetsType.IpamSubnetType> subnets = new
    // ArrayList<VnSubnetsType.IpamSubnetType>();
    // VnSubnetsType.IpamSubnetType subnetType = new
    // VnSubnetsType.IpamSubnetType();
    // SubnetType type = new SubnetType();
    // List<String> temp = new ArrayList<String>();
    // List<Neutron_IPs> fixedIp = new ArrayList<Neutron_IPs>();
    // Neutron_IPs neutronIps = new Neutron_IPs();
    // neutronIps.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
    // fixedIp.add(neutronIps);
    // dummyNeutronPort.setFixedIPs(fixedIp);
    //
    // for (int i = 0; i < 1; i++) {
    // subnetType.setSubnet(type);
    //
    // subnetType.getSubnet().setIpPrefix("10.0.0.0");
    // subnetType.getSubnet().setIpPrefixLen(24);
    // subnetType.setSubnetUuid("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
    // subnets.add(subnetType);
    // vnSubnetType.addIpamSubnets(subnetType);
    // ref.setReference(temp, vnSubnetType, "", "");
    // ipamRefs.add(ref);
    // }
    // when(mockedApiConnector.findById(VirtualMachineInterface.class,
    // neutronPort.getPortUUID())).thenReturn(mockedVirtualMachineInterface);
    // when(mockedApiConnector.findById(VirtualNetwork.class,
    // neutronPort.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
    // when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(ipamRefs);
    // // List<ObjectReference<ApiPropertyBase>> value = new
    // ArrayList<ObjectReference<ApiPropertyBase>>();
    // // VirtualMachineInterface vmInterface = new VirtualMachineInterface();
    // // vmInterface.setUuid("000570f2-17b1-4fc399ec-1b7f7778a29b");
    // // vmInterface.setDisplayName("VMI");
    // // vmInterface.setVirtualNetwork(mockedVirtualNetwork);
    // // InstanceIp instanceIp = new InstanceIp();
    // // instanceIp.setVirtualMachineInterface(vmInterface);
    //
    // assertEquals(true, porthandler.updatePort(neutronPort.getPortUUID(),
    // dummyNeutronPort));
    // }
}
