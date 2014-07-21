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
import static org.junit.Assert.assertArrayEquals;

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
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet_IPAllocationPool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Class for Subnet.
 */
public class SubnetHandlerTest {
    SubnetHandler subnetHandler;
    VirtualNetwork mockedVirtualNetwork = mock(VirtualNetwork.class);
    SubnetHandler mockedSubnetHandler = mock(SubnetHandler.class);
    ApiConnector mockedApiConnector = mock(ApiConnector.class);
    NetworkIpam mockedNetworkIpam = mock(NetworkIpam.class);

    @Before
    public void beforeTest() {
        subnetHandler = new SubnetHandler();
        assertNotNull(mockedApiConnector);
        assertNotNull(mockedVirtualNetwork);
        assertNotNull(mockedSubnetHandler);
    }

    @After
    public void afterTest() {
        subnetHandler = null;
        Activator.apiConnector = null;
    }

    /* dummy params for Neutron Subnet */
    public NeutronSubnet defaultSubnetObject() {
        NeutronSubnet subnet = new NeutronSubnet();
        List<NeutronSubnet_IPAllocationPool> allocationPools = new ArrayList<NeutronSubnet_IPAllocationPool>();
        NeutronSubnet_IPAllocationPool neutronSubnet_IPAllocationPool = new NeutronSubnet_IPAllocationPool();
        subnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setCidr("10.0.0.0/24");
        subnet.setGatewayIP("10.0.0.254");
        neutronSubnet_IPAllocationPool.setPoolStart("10.0.0.1");
        neutronSubnet_IPAllocationPool.setPoolEnd("10.0.0.254");
        allocationPools.add(neutronSubnet_IPAllocationPool);
        subnet.setAllocationPools(allocationPools);
        return subnet;
    }

    /* dummy params for Neutron Delta Subnet */
    public NeutronSubnet defaultDeltaSubnet() {
        NeutronSubnet subnet = new NeutronSubnet();
        subnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        subnet.setCidr("10.0.0.0/24");
        subnet.setGatewayIP("10.0.0.254");
        return subnet;
    }

    public NeutronSubnet defaultOriginalSubnet() {
        NeutronSubnet subnet = new NeutronSubnet();
        return subnet;
    }

    /* Test method to check if neutron subnet is null */
    @Test
    public void testCanCreateSubnetNull() {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, subnetHandler.canCreateSubnet(null));
    }

    /* Test method to check if neutron subnet is null */
    @Test
    public void testCanCreateSubnetCidrNull() {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet subnet = new NeutronSubnet();
        subnet.setCidr(null);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, subnetHandler.canCreateSubnet(subnet));
    }

    /* Test method to check if Gateway Ip is invalid */
    @Test
    public void testCanCreateSubnetInvalidGatewayIp() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        neutronSubnet.setGatewayIP("20.0.0.250");
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, subnetHandler.canCreateSubnet(neutronSubnet));
    }

    /* Test method to check if virtual network is null */
    @Test
    public void testCanCreateSubnetVirtualNetworkNull() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, subnetHandler.canCreateSubnet(neutronSubnet));
    }

    /* Test method to check if subnet already exists */
    @Test
    public void testCanCreateSubnetAlreadyExist() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        VnSubnetsType vnSubnetType = new VnSubnetsType();
        ObjectReference<VnSubnetsType> ref = new ObjectReference<>();
        List<ObjectReference<VnSubnetsType>> ipamRefs = new ArrayList<ObjectReference<VnSubnetsType>>();
        List<VnSubnetsType.IpamSubnetType> subnets = new ArrayList<VnSubnetsType.IpamSubnetType>();
        VnSubnetsType.IpamSubnetType subnetType = new VnSubnetsType.IpamSubnetType();
        SubnetType type = new SubnetType();
        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < 1; i++) {
            subnetType.setSubnet(type);
            subnetType.getSubnet().setIpPrefix("10.0.0.0");
            subnetType.getSubnet().setIpPrefixLen(24);
            subnets.add(subnetType);
            vnSubnetType.addIpamSubnets(subnetType);
            ref.setReference(temp, vnSubnetType, "", "");
            ipamRefs.add(ref);
        }
        when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(ipamRefs);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, subnetHandler.canCreateSubnet(neutronSubnet));
    }

    /* Test method to check if subnet can create Ok */
    @Test
    public void testCanCreateSubnetOK() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_OK, subnetHandler.canCreateSubnet(neutronSubnet));
    }

    /* Test method to check if add subnet fails */
    @Test
    public void testAddSubnetFalse() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.update(mockedVirtualNetwork)).thenReturn(false);
        assertEquals(false, subnetHandler.addSubnet(neutronSubnet));
    }

    /* Test method to check if subnet is added successfully */
    @Test
    public void testAddSubnetTrue() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedApiConnector.update(mockedVirtualNetwork)).thenReturn(true);
        assertEquals(true, subnetHandler.addSubnet(neutronSubnet));
    }

    /* Test method to check ipPrefix */
    @Test
    public void testGetIpPrefix() {
        SubnetHandler.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        String cidr = "10.0.0.0/24";
        String[] ipPrefix = cidr.split("/");
        assertArrayEquals(ipPrefix, subnetHandler.getIpPrefix(neutronSubnet));
    }

    /* Test method to check if ipPrefix is valid */
    @Test(expected = IllegalArgumentException.class)
    public void testGetIpPrefixInvalid() {
        SubnetHandler.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = new NeutronSubnet();
        neutronSubnet.setNetworkUUID("6b9570f2-17b1-4fc399ec-1b7f7778a29b");
        neutronSubnet.setSubnetUUID("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
        neutronSubnet.setCidr("10.0.0.0");
        subnetHandler.getIpPrefix(neutronSubnet);
    }

    /* Test method to check if neutron subnets are null for update */
    @Test
    public void testcanUpdateSubnetNull() {
        Activator.apiConnector = mockedApiConnector;
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, subnetHandler.canUpdateSubnet(null, null));
    }

    /* Test method to check if subnets do not exist */
    @Test
    public void testcanUpdateSubnetNotFound() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        NeutronSubnet deltaSubnet = defaultDeltaSubnet();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(null);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, subnetHandler.canUpdateSubnet(deltaSubnet, neutronSubnet));
    }

    /*
     * Test method to check if subnet already exists and update subnet
     * successfully
     */
    @Test
    public void testcanUpdateSubnetOK() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        NeutronSubnet deltaSubnet = defaultDeltaSubnet();
        when(mockedApiConnector.findById(VirtualNetwork.class, neutronSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        VnSubnetsType vnSubnetType = new VnSubnetsType();
        ObjectReference<VnSubnetsType> ref = new ObjectReference<>();
        List<ObjectReference<VnSubnetsType>> ipamRefs = new ArrayList<ObjectReference<VnSubnetsType>>();
        List<VnSubnetsType.IpamSubnetType> subnets = new ArrayList<VnSubnetsType.IpamSubnetType>();
        VnSubnetsType.IpamSubnetType subnetType = new VnSubnetsType.IpamSubnetType();
        SubnetType type = new SubnetType();
        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < 1; i++) {
            subnetType.setSubnet(type);
            subnetType.setSubnetUuid("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
            subnetType.getSubnet().setIpPrefix("10.0.0.0");
            subnetType.getSubnet().setIpPrefixLen(24);
            subnets.add(subnetType);
            vnSubnetType.addIpamSubnets(subnetType);
            ref.setReference(temp, vnSubnetType, "", "");
            ipamRefs.add(ref);
        }
        when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(ipamRefs);
        assertEquals(HttpURLConnection.HTTP_OK, subnetHandler.canUpdateSubnet(deltaSubnet, neutronSubnet));
    }

    /*
     * Test method to check if subnet already exists and update subnet
     * successfully
     */
    @Test
    public void testUpdateSubnet() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        NeutronSubnet deltaSubnet = defaultDeltaSubnet();
        NeutronSubnet originalSubnet = neutronSubnet;
        subnetHandler.setOriginalSubnet(originalSubnet);
        // when(originalSubnet.getNetworkUUID()).thenReturn(value);
        when(mockedApiConnector.findById(VirtualNetwork.class, originalSubnet.getNetworkUUID())).thenReturn(mockedVirtualNetwork);
        VnSubnetsType vnSubnetType = new VnSubnetsType();
        ObjectReference<VnSubnetsType> ref = new ObjectReference<>();
        List<ObjectReference<VnSubnetsType>> ipamRefs = new ArrayList<ObjectReference<VnSubnetsType>>();
        List<VnSubnetsType.IpamSubnetType> subnets = new ArrayList<VnSubnetsType.IpamSubnetType>();
        VnSubnetsType.IpamSubnetType subnetType = new VnSubnetsType.IpamSubnetType();
        SubnetType type = new SubnetType();
        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < 1; i++) {
            subnetType.setSubnet(type);
            subnetType.setSubnetUuid("7b9570f2-17b1-4fc399ec-1b7f7778a29b");
            subnetType.getSubnet().setIpPrefix("10.0.0.0");
            subnetType.getSubnet().setIpPrefixLen(24);
            subnets.add(subnetType);
            vnSubnetType.addIpamSubnets(subnetType);
            ref.setReference(temp, vnSubnetType, "", "");
            ipamRefs.add(ref);
        }
        when(mockedVirtualNetwork.getNetworkIpam()).thenReturn(ipamRefs);
        when(mockedApiConnector.update(mockedVirtualNetwork)).thenReturn(true);
        assertEquals(true, subnetHandler.updateSubnet(neutronSubnet.getSubnetUUID(), deltaSubnet));
    }

    /* Tets method to check if a subnet deletion terminate with Internal_Error */
    @Test
    public void testCanDeleteSubnet() throws IOException {
        Activator.apiConnector = mockedApiConnector;
        NeutronSubnet neutronSubnet = defaultSubnetObject();
        assertEquals(HttpURLConnection.HTTP_OK, subnetHandler.canDeleteSubnet(neutronSubnet));
    }
}
