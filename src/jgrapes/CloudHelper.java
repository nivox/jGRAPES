/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

/**
 * This class implements the bindings for the cloud_helper module of GRAPES.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public class CloudHelper {

  protected long nativeInstanceID;
  protected NodeID cloudNode;

  /**
   * Initialize an instance of cloud_helper using the specified native nodeID reference and the
   * supplied configuration.
   *
   * @param myIdReference the memory location of the native local nodeID instance
   * @param conf cloud helper configuration
   * @return memory location of the native cloud_helper instance
   */
  protected static native long init(long myIdReference, String conf) throws JGrapesException;

  /**
   * Builds a CloudHelper instance binding it to the specified native instance
   *
   * @param nativeInstance memory location of the native cloud_helper instance
   */
  protected CloudHelper(long nativeInstance) {
    this.nativeInstanceID = nativeInstance;
    this.cloudNode = null;
  }

  /**
   * Checks whether the supplied node represent the cloud
   *
   * @param node node instance to check
   * @return true if node represents the cloud
   */
  public native boolean isCloudNode(NodeID node);

  /**
   * Return the NodeID representing the cloud.
   *
   * @return cloud node
   */
  protected native NodeID getCloudNodeImpl();

  /**
   * Return the NodeID representing the cloud.
   *
   * @return cloud node
   */
  public NodeID getCloudNode() {
    if (cloudNode == null)
      cloudNode = getCloudNodeImpl();

    return cloudNode;
  }

  /**
   * Issue a get request for the specified key. If header is non null it will be prefixed to the
   * associated response.
   *
   * @param key key to retrieve from the cloud
   * @param header header of the associated response or null
   * @throws JGrapesException error performing the request
   */
  public native void get(String key, byte[] header) throws JGrapesException;

  /**
   * Issue a put request for the specified key and data.
   *
   * @param key key to update/set
   * @param data new value of key
   * @throws JGrapesException error performing the request
   */
  public native void put(String key, byte[] data) throws JGrapesException;

  /**
   * Return the timestamp associated to the last GET operation
   *
   * @return seconds since 00:00 hours, Jan 1, 1970 UTC.
   */
  public native long getLastTimestamp() throws JGrapesException;

  /**
   * Wait for a response from the cloud
   *
   * @param seconds time to wait before returning
   * @return true if there is data ready to be read
   * @throws JGrapesException the last request resulted failed (unknown key?)
   */
  public native boolean waitForCloud(long seconds) throws JGrapesException;

  /**
   * Receive data from the cloud
   *
   * @param maxBytes size of the returned buffer
   * @throw JGrapesException error reading from the cloud
   */
  public native byte[] recvFromCloud(int maxBytes) throws JGrapesException;
}