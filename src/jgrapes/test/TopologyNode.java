/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes.test;

import java.io.PrintStream;

import jgrapes.CloudHelper;
import jgrapes.NetworkHelper;
import jgrapes.NodeID;
import jgrapes.PeerSampler;
import jgrapes.ReceivedData;
import jgrapes.JGrapes;
import jgrapes.JGrapesException;
import jgrapes.JGrapesHelper;


public class TopologyNode extends Thread {

  private static final Boolean sync = new Boolean(true);
  protected volatile boolean terminated = false;
  protected volatile boolean activeState = false;

  protected NetworkHelper nh;
  protected PeerSampler ps;
  protected CloudHelper ch;

  protected NodeID myNodeID;
  protected PrintStream out;
  protected int logTime;
  protected long lastLogTime;

  public TopologyNode(NodeID myNode, String nhConf, String psConf, String chConf, int logTime,
                      PrintStream out) throws JGrapesException
  {
    this.myNodeID = myNode;
    synchronized (sync) {
      if (nhConf == null) nhConf = "";
      this.nh = JGrapes.newNetworkHelperInstance(myNode.getIpAddress(), myNode.getPort(), nhConf);

      if (chConf != null) {
        this.ch = JGrapes.newCloudHelperInstance(nh, chConf);
      } else this.ch = null;

      if (psConf == null) psConf = "";
      this.ps = JGrapes.newPeerSamplerInstance(nh, null, psConf);
    }

    this.out = out;
    this.logTime = logTime * 1000;
    this.lastLogTime = 0;
    this.terminated = false;
    this.activeState = true;
  }

  public void addNode(NodeID node) throws JGrapesException {
    ps.addPeer(node, null);
  }

  public NodeID getNodeID() {
    return myNodeID;
  }

  public void terminate() {
    print("# setting teminated state");
    this.terminated = true;
  }

  public void setActiveState(boolean state) {
    this.activeState = state;
  }

  protected void print(String line) {
    if (out != null) {
      out.println(line);
      out.flush();
    }
  }

  protected void logCache() {
    if (this.logTime == 0) return;
    if (System.currentTimeMillis() > this.lastLogTime + this.logTime) {
      NodeID neighborhood[];
      String cacheDump="";

      this.lastLogTime = System.currentTimeMillis();
      neighborhood = ps.getCache();
      for (int i=0; i<neighborhood.length; i++) {
        NodeID neighbor = neighbor = neighborhood[i];
        cacheDump += String.format("%s:%d,", neighbor.getIpAddress(), neighbor.getPort());
      }
      print(String.format("@ cachedump time=%s node=%s:%d cachesize=%d cache='%s'",
                          (this.lastLogTime/1000),
                          myNodeID.getIpAddress(), myNodeID.getPort(),
                          neighborhood.length, cacheDump));
    }
  }

  protected boolean actActive(boolean stateTransition) {
    NetworkHelper netHelpers[] = new NetworkHelper[]{nh};
    CloudHelper cloudHelpers[] = new CloudHelper[]{ch};

    if (stateTransition) print("# activate");

    try{
      Object resource;
      resource = JGrapesHelper.waitForAny(netHelpers, cloudHelpers, 1);
      if (resource != null) {
        byte[] data = null;

        if (resource instanceof NetworkHelper) {
          ReceivedData rec;
          rec = nh.recvFromPeer(1024);

          data = rec.getData();
        } else if (resource instanceof CloudHelper) {
          data = ch.recvFromCloud(1024);
        } else throw new JGrapesException("wait4any returned unknown resource");


        ps.parseData(data);
      } else {
        logCache();
        ps.parseData(null);
      }
    } catch (JGrapesException e) {
      print(String.format("# Catched exception", myNodeID.getIpAddress(),
                          myNodeID.getPort()));
      e.printStackTrace();
    }

    return false;
  }

  protected boolean actInactive(boolean stateTransition) {
    NodeID cache[] = this.ps.getCache();

    if (stateTransition) print("# deactivate");

    try {
      if (stateTransition) {
        // If the node has just been deactivated, clean the cache ...
        for (NodeID node: cache) {
          ps.removePeer(node);
        }

        // ... and then consume incoming cloud data if any
        if (ch.waitForCloud(10))
          ch.recvFromCloud(1024);
      }

      // Periodically consume incoming data from active peers
      if (nh.waitForData(1))
        nh.recvFromPeer(1024);
    } catch (JGrapesException e) {}

    return false;
  }

  public void run() {
    NetworkHelper netHelpers[] = new NetworkHelper[]{nh};
    CloudHelper cloudHelpers[] = new CloudHelper[]{ch};

    boolean stop = false;
    boolean lastState = false;
    boolean stateTransition = false;

    print("# started");
    while (!this.terminated) {
      stateTransition = (lastState != this.activeState);
      lastState = this.activeState;

      if (this.activeState) {
        stop = actActive(stateTransition);
      } else {
        stop = actInactive(stateTransition);
      }
      if (stop) break;
    }

    print("# finished");
    if (this.out != null) this.out.close();
  }

}