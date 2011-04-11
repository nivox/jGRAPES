/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

/**
 * This class implements the bindings for the net_helper module of GRAPES
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public class NetworkHelper {
  protected long nativeInstanceID;
  protected NodeID myNodeID;

  /**
   * Initialize an instance of net_helper using the specified ip and port for the local node.
   *
   * @param ipAddr ip address of the local node
   * @param port port number of the local node
   * @param conf extended configuration
   */
  protected static native long init(String ipAddr, int port, String conf) throws JGrapesException;

  /**
   * Builds a NetworkHelper instance mapped to the specified native instance
   *
   * @param nativeInstanceID memory location of the native net_helper instance
   * @param ip ip address of the local node
   * @param port port number of the local node
   */
  protected NetworkHelper(long nativeInstanceID, String ip, int port) {
    this.nativeInstanceID = nativeInstanceID;
    this.myNodeID = NodeID.getUnresolvedNodeID(String.format("%s:%d", ip, port));
  }


  /**
   * Wait for incoming data from another node
   *
   * @param seconds time to wait before returning
   * @return true if there is data to be read
   */
  public native boolean waitForData(long seconds);

  /**
   * Receive data from a node
   *
   * @param maxBytes size of the returned buffer
   * @throw JGrapesException error receiving data from peer
   */
  public native ReceivedData recvFromPeer(int maxBytes) throws JGrapesException;


  /**
   * Return the memory location of the associated native instance
   *
   * @return memory location of native instance
   */
  protected long getNodeIdRefenece() {
    return nativeInstanceID;
  }

  /**
   * Return the NodeID describing the local node
   *
   * @return local node
   */
  public NodeID getLocalNodeID() {
    return myNodeID;
  }
}