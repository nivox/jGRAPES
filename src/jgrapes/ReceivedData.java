/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

/**
 * This class serves as a container for the received data and a reference to the remote node
 */
public class ReceivedData {
  protected String remoteNodeAddr;
  protected byte[] data;

  /**
   * Initialize a ReceivedData object for the specified remote node and data
   *
   * @param remoteAddr ip:port string describing remote node
   * @param data received data
   */
  protected ReceivedData(String remoteAddr, byte[] data) {
    this.remoteNodeAddr = remoteAddr;
    this.data = data;
  }

  /**
   * Return the remote node
   *
   * @return remote node
   */
  public NodeID getRemotePeerNodeID() {
    return NodeID.getUnresolvedNodeID(remoteNodeAddr);
  }

  /**
   * Return the data
   *
   * @return data
   */
  public byte[] getData() {
    return data;
  }
}