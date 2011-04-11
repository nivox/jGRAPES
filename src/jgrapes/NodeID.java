/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


/**
 * Class that describe a node
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public class NodeID {
  protected String ip;
  protected int port;

  /**
   * Creates an empty node
   */
  protected NodeID() {}

  /**
   * Crates a node from the specified address without checking the validity of the address
   *
   * @param address node address (string of the form ip:port)
   * @return NodeID instance
   * @throw IllegalArgumentException invalid port number
   */
  protected static NodeID getUnresolvedNodeID(String address) {
    String[] addrComponents;
    NodeID n;

    if (address == null)
      throw new IllegalArgumentException("Null address");

    addrComponents = address.split(":", 2);
    n = new NodeID();
    try {
      n.ip = addrComponents[0];
      n.port = Integer.parseInt(addrComponents[1]);
    } catch (NumberFormatException e) {
      System.err.println("getUnresolvedNodeID: " + address);
      throw new IllegalArgumentException("Invalid port number: out of range: " + addrComponents[1]);
    } catch (Exception e) {
      System.err.println("getUnresolvedNodeID: " + address);
      e.printStackTrace();
    }

    return n;
  }

  /**
   * Builds a NodeID representation of the specified address
   *
   * @param nodeAddr address of the node (string of the form ip:port)
   * @throws UnknownHostException invalid ip address
   * @throws IllegalArgumentException invalid port number
   */
  public NodeID(String nodeAddr) throws UnknownHostException {
    String[] addrComponents;
    InetAddress ipAddr;
    addrComponents = nodeAddr.split(":");

    if (addrComponents.length != 2) {
      throw new IllegalArgumentException("Invalid node address");
    }

    InetAddress.getByName(addrComponents[0]);

    this.ip = addrComponents[0];

    try {
      this.port = Integer.parseInt(addrComponents[1]);

      if (this.port < 0 || this.port > 65535) {
        throw new IllegalArgumentException("Invalid port number: out of range");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid port number: not a number");
    }
  }

  /**
   * Return the InetSocketAddress for the current node
   *
   * @return InetSocketAddress of current node
   */
  public InetSocketAddress getSocketAddress() {
    return InetSocketAddress.createUnresolved(ip, port);
  }

  /**
   * Return the ip address of the current node
   *
   * @return ip address of current node
   */
  public String getIpAddress() {
    return ip;
  }

  /**
   * Return the port number of the current node
   *
   * @return port number of current node
   */
  public int getPort() {
    return port;
  }

  public String toString() {
    return String.format("%s:%d", ip, port);
  }
}