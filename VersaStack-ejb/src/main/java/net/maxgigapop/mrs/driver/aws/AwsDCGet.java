/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.AmazonServiceException;
import net.maxgigapop.mrs.driver.aws.AwsAuthenticateService;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.AmazonDirectConnectClient;
import com.amazonaws.services.directconnect.model.*;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author muzcategui
 */
public class AwsDCGet {

    private AmazonDirectConnectClient client = null;
    private List<VirtualGateway> virtualGateways = null;
    private List<VirtualInterface> virtualInterfaces = null;
    private List<Connection> connections = null;

    public AwsDCGet(String access_key_id, String secret_access_key, Regions region) {
        AwsAuthenticateService authenticate = new AwsAuthenticateService(access_key_id, secret_access_key);
        this.client = authenticate.AwsAuthenticateDCService(Region.getRegion(region));

        //get all the privateGateways gateways under the account
        DescribeVirtualGatewaysResult virtualGatewaysResult = this.client.describeVirtualGateways();
        virtualGateways = virtualGatewaysResult.getVirtualGateways();

        //get all the virtual interfaces under the account
        DescribeVirtualInterfacesResult virtualInterfacesResult = this.client.describeVirtualInterfaces();
        virtualInterfaces = virtualInterfacesResult.getVirtualInterfaces();

        //get all the connections under the account
        DescribeConnectionsResult connectionsResult = this.client.describeConnections();
        connections = connectionsResult.getConnections();

    }

    //get all the virtual Interfaces 
    public List<VirtualInterface> getVirtualInterfaces() {
        return virtualInterfaces;
    }

    //get a list of vitual interfaces from a gateway
    public List<VirtualInterface> getVirtualInterfaces(String id) {
        List<VirtualInterface> vis = new ArrayList();
        for (VirtualInterface v : virtualInterfaces) {
            String virtualGatewayId = v.getVirtualGatewayId();
            if (virtualGatewayId == null) {
                continue;
            }
            if (virtualGatewayId.equals(id)) {
                vis.add(v);
            }
        }
        return vis;
    }

    //get the list of all the virtual interfaces that do not have a gateway
    public List<VirtualInterface> getNoAssocVirtualInterface() {
        List<VirtualInterface> vis = new ArrayList();
        for (VirtualInterface v : virtualInterfaces) {
            String virtualGatewayId = v.getVirtualGatewayId();
            if (virtualGatewayId == null) {
                vis.add(v);
            }
        }
        return vis;
    }

    //get all the direct connect connections
    public List<Connection> getConnections() {
        return connections;
    }

    //public Connection from a Virtual interface
    public Connection getConnection(VirtualInterface v) {
        for (Connection c : connections) {
            if (c.getConnectionId().equals(v.getConnectionId()));
            return c;
        }
        return null;
    }

    //get all the private gateways 
    public List<VirtualGateway> getVirtualGateways() {
        return virtualGateways;
    }

    //get the client  for AWS DC
    public AmazonDirectConnectClient getClient() {
        return client;
    }

    //get the VirtualInterface by Vlan
    public String getVirtualInterfaceByVlan(String vlan) {
        for (VirtualInterface vif: virtualInterfaces) {
            if (vif.getVlan().toString().equals(vlan)) {
                return vif.getVirtualInterfaceId();
            }
        }
        return null;
    }
    
    /**
     * ****************************************************************
     * function to wait for directconnect virtual interface deletion
     * ****************************************************************
     */
    public void dxvifDeletionCheck(String id) {
        DescribeVirtualInterfacesRequest request = new DescribeVirtualInterfacesRequest();
        request.withVirtualInterfaceId(id);
        long delay = 1000L;
        long delayMax = 16000L;
        while (true) {
            delay *= 2;
            try {
                VirtualInterface resource = client.describeVirtualInterfaces(request).getVirtualInterfaces().get(0);
                if (resource.getVirtualInterfaceState().equals(VirtualInterfaceState.Deleted.toString())) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                }
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }
  
}
