package jgrapes.test.statistics;

import java.io.PrintStream;

import jgrapes.JGrapes;
import jgrapes.JGrapesException;
import jgrapes.NodeID;
import jgrapes.test.TopologyNode;

class StatisticsTopologyNode extends TopologyNode {

  String lastCache = "";
  String psConf = null;

  public StatisticsTopologyNode(NodeID myNode, String nhConf, String psConf, String chConf,
                                int logTime, PrintStream out) throws JGrapesException
  {
    super(myNode, nhConf, psConf, chConf, logTime, out);
    this.psConf = psConf;
  }

  public void setActiveState(boolean state) {
    if (state == true && activeState == false) {
      try {
        this.ps = JGrapes.newPeerSamplerInstance(nh, null, psConf);
      } catch (JGrapesException e) { e.printStackTrace(); }
    }

    super.setActiveState(state);
  }

  protected void logCache() {
    if (this.logTime == 0) return;
    if (System.currentTimeMillis() > this.lastLogTime + this.logTime) {
      NodeID neighborhood[];
      String cacheDump="";

      neighborhood = ps.getCache();
      for (int i=0; i<neighborhood.length; i++) {
        NodeID neighbor = neighbor = neighborhood[i];
        cacheDump += String.format("%s:%d,", neighbor.getIpAddress(), neighbor.getPort());
      }

      if (!lastCache.equals(cacheDump)) {
        lastCache = cacheDump;
        this.lastLogTime = System.currentTimeMillis();
        print(String.format("@ cachedump time=%s node=%s:%d cachesize=%d cache='%s'",
                            (this.lastLogTime/1000),
                            myNodeID.getIpAddress(), myNodeID.getPort(),
                            neighborhood.length, cacheDump));
      }
    }
  }
}