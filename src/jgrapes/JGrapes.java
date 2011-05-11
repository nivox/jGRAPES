/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

import jgrapes.NetworkHelper;
import jgrapes.CloudHelper;
import jgrapes.PeerSampler;


/**
 * This class acts as the single access point to the GRAPES modules.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public abstract class JGrapes {
  /* Lets make sure the grapes library is loaded */
  static {
    System.loadLibrary("jgrapes");
  }


  /**
   * Initialize and returns a NetworkHelper instance
   *
   * @param ipAddr ip address
   * @param port port number
   * @param conf net_helper implementation dependant configuration
   */
  public static NetworkHelper newNetworkHelperInstance(String ipAddr, int port, String conf)
    throws JGrapesException
  {
    long id = -1;
    if (conf == null) conf = "";
    if (ipAddr == null) throw new JGrapesException("Null ip address");
    try{
      id = NetworkHelper.init(ipAddr, port, conf);
    } catch(JGrapesException e){
      throw e;
    } catch(Exception e) {
      JGrapesException ex = new JGrapesException("Error initializing network_helper");
      ex.initCause(e);
    }

    NetworkHelper nh = new NetworkHelper(id, ipAddr, port);
    return nh;
  }

  /**
   * Initialize and returns a CloudHelper instance
   *
   * @param nh NetworkHelper instance associated to this CloudHelper
   * @param conf cloud_helper implementation dependant configuration
   */
  public static CloudHelper newCloudHelperInstance(NetworkHelper nh, String conf)
    throws JGrapesException
  {
    long id = -1;
    if (conf == null) conf = "";
    try{
      id = CloudHelper.init(nh.getNodeIdRefenece(), conf);
    } catch(JGrapesException e){
      throw e;
    } catch(Exception e) {
      JGrapesException ex = new JGrapesException("Error initializing cloud_helper");
      ex.initCause(e);
    }

    CloudHelper ch = new CloudHelper(id);
    return ch;
  }

  /**
   * Initialize and returns a PeerSampler instance.
   *
   * @param nh NetworkHelper instance associated to this PeerSampler
   * @param myMetadata metadata associated to the local node
   * @param conf peers_sampler implementation dependant configuration
   */
  public static PeerSampler newPeerSamplerInstance(NetworkHelper nh, byte[] myMetadata,
                                                   String conf)
    throws JGrapesException
  {
    long id = -1;
    if (conf == null) conf = "";
    try {
      id = PeerSampler.init(nh.getNodeIdRefenece(), myMetadata, conf);
    } catch(JGrapesException e){
      throw e;
    } catch(Exception e) {
      JGrapesException ex = new JGrapesException("Error initializing peer_sampler");
      ex.initCause(e);
    }


   PeerSampler p = new PeerSampler(id);
    return p;
  }
}