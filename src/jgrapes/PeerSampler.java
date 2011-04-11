/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

/**
 * This class implements the bindings for the peer_sampler module of GRAPES
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public class PeerSampler {
  private long nativeInstanceID;

  /**
   * Initialize an instance of peer_sampler using the specified nodeID native reference, metadata
   * and configuration.
   *
   * @param myIdReference memory location of the native nodeID instance
   * @param myMetadata metadata associated to the local node
   * @param conf implementation dependant configuration
   * @return memory location of native peer_sampler instance
   */
  protected static native long init(long myIdReference, byte[] myMetadata, String conf) throws JGrapesException;

  /**
   * Builds a PeerSampler instance mapped to the specified native instance
   *
   * @param nativeInstanceID memory location of the native peer_sampler instance
   */
  protected PeerSampler(long nativeInstanceID) {
    this.nativeInstanceID = nativeInstanceID;
  }

  /**
   * Return the peer sampler cache
   *
   * @return array of node in cache
   */
  public native NodeID[] getCache();

  /**
   * Try to increase the cache size
   *
   * @param n desired size
   * @return new size
   */
  public native int growCache(int n);

  /**
   * Try to decrease the cache size
   *
   * @param n desired size
   * @return new size
   */
  public native int shrinkCache(int n);

  /**
   * Remove the specified peer from the cache
   *
   * @param neighbor node to remove from cache
   * @throws JGrapesException error removing peer
   */
  public native void removePeer(NodeID neighbor) throws JGrapesException;

  /**
   * Add the specified peer to the cache with the supplied metadata
   *
   * @param neighbor node to add to the cache
   * @param metadata metadata for the new node
   * @throws JGrapesException error adding peer
   */
  public native void addPeer(NodeID neighbor, byte[] metadata) throws JGrapesException;

  /**
   * Parse the specified data.
   * Call with null data will cause the active thread of the protocol to be performed
   *
   * @param data peer sampler protocol data or null
   * @throws JGrapesException error parsing the data
   */
  public native void parseData(byte[] data) throws JGrapesException;

  /**
   * Change the metadata for the local node
   *
   * @param new metadata
   * @throws JGrapesException error changing the metadata (size mismatch?)
   */
  public native void changeMetadata(byte[] metadata) throws JGrapesException;

  /**
   * Return the metadata of the nodes currently in cache (the order of the metadatas is the one
   * specified by getCache).
   *
   * @return byte[] array of node's metadata
   */
  public native byte[][] getMetadata();
}