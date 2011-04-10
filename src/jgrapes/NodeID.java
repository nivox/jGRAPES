package jgrapes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


public class NodeID {
  protected String ip;
  protected int port;

  protected NodeID() {}

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
      throw new IllegalArgumentException("FUUUUUUUUUUCK!");
    }

    return n;
  }


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

  public InetSocketAddress getSocketAddress() {
    return InetSocketAddress.createUnresolved(ip, port);
  }

  public String getIpAddress() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  public String toString() {
    return String.format("%s:%d", ip, port);
  }
}