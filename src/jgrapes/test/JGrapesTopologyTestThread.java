/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jgrapes.CloudHelper;
import jgrapes.JGrapes;
import jgrapes.JGrapesException;
import jgrapes.JGrapesHelper;
import jgrapes.NetworkHelper;
import jgrapes.NodeID;
import jgrapes.PeerSampler;
import jgrapes.ReceivedData;

class TopologyNode extends Thread {

  protected NetworkHelper nh;
  protected PeerSampler ps;
  protected CloudHelper ch;

  protected PrintStream out;

  public TopologyNode(NodeID node, String nhConf, String psConf, String chConf,
                      NodeID bootstrapNode, PrintStream out, Object sync) throws JGrapesException
  {
    synchronized(sync) {
      System.out.format("Starting node %s:%d\n", node.getIpAddress(), node.getPort());
      if (nhConf == null) nhConf = "";
      this.nh = JGrapes.newNetworkHelperInstance(node.getIpAddress(), node.getPort(), nhConf);

      if (chConf != null) {
        this.ch = JGrapes.newCloudHelperInstance(nh, chConf);
      } else this.ch = null;

      if (psConf == null) psConf = "";
      this.ps = JGrapes.newPeerSamplerInstance(nh, null, psConf);
      this.out = out;
    }
  }

  protected void print(String line) {
    if (this.out != null) this.out.println(line);
  }

  public void run() {
    NetworkHelper netHelpers[] = new NetworkHelper[]{nh};
    CloudHelper cloudHelpers[] = new CloudHelper[]{ch};
    NodeID myNodeID;
    int iter = 0;
    myNodeID = nh.getLocalNodeID();
    while (true) {
      try {
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
          NodeID neighborhood[];

          ps.parseData(null);
          neighborhood = ps.getCache();

          if (iter++ % 10 == 0) {
            iter = 1;
            print(String.format("\n%s:%d - I have %d neighbors:",
                                myNodeID.getIpAddress(),
                                myNodeID.getPort(),
                                neighborhood.length));

            for (int i=0; i<neighborhood.length; i++) {
              NodeID neighbor = neighbor = neighborhood[i];
              print(String.format("%s:%d -\t %d: %s:%d",
                                  myNodeID.getIpAddress(),
                                  myNodeID.getPort(),
                                  i,
                                  neighbor.getIpAddress(),
                                  neighbor.getPort()));
            }
          }
        }
      } catch (JGrapesException e) {
        System.err.println(String.format("%s:%d - Catched exception",
                                         myNodeID.getIpAddress(),
                                         myNodeID.getPort()));
        e.printStackTrace();
      }
    }
  }
}


class Parameters {
  NodeID addr = null;
  NodeID bootstrap = null;
  int localNodeNr = 0;
  String nhConf = null;
  String psConf = null;
  String chConf = null;
  PrintStream out = null;
  boolean wait = false;
  public Parameters() {}
}

public class JGrapesTopologyTestThread {
  /**
   * Print help message
   */
  public static void printHelp(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("JGrapesTopologyTestThread", opts, true);
  }

  /**
   * Parse the command line
   */
  public static Parameters parseCommandLine(String args[]) {
    CommandLineParser cmdParser = new GnuParser();
    Options opts = new Options();

    Parameters params = new Parameters();

    // Setting defaults
    params.out = System.out;

    // Declaring supported options
    opts.addOption(null, "help", false, "print this help message");
    opts.addOption("h", true, "specify the local address to use (ip:port)");
    opts.addOption("b", true, "specify a remote node to bootstrap the local node");
    opts.addOption(null, "nh-conf", true, "specify the net_helper configuration");
    opts.addOption(null, "ps-conf", true, "specify the peer_sampler configuration");
    opts.addOption(null, "ch-conf", true, "specify the cloud_helper configuration. Causes the cloud helper to be initialized.");
    opts.addOption("n", true, "number of peer to instantiate. Cause formation of local topology (bypass -b)");
    opts.addOption("q", true, "be quiet");
    opts.addOption("o", true, "log file");
    opts.addOption("w", false, "wait for user input before starting nodes");

    // Parse the command line
    try {
      CommandLine line = cmdParser.parse(opts, args);

      if (line.hasOption("help")) printHelp(opts);

      if (line.hasOption('h')) {
        try {
          params.addr = new NodeID(line.getOptionValue('h'));
        } catch (UnknownHostException e) {
          throw new ParseException("Local node address is not valid");
        }
      } else throw new ParseException("Missing mandatory option -h");

      if (line.hasOption('b')) {
        try {
          params.bootstrap = new NodeID(line.getOptionValue('h'));
        } catch (UnknownHostException e) {
          throw new ParseException("Bootstrap node address is not valid");
        }
      }

      if (line.hasOption('n')) {
        try {
          params.localNodeNr = Integer.parseInt(line.getOptionValue('n'));
        } catch (NumberFormatException e) {
          throw new ParseException("Illegal argument for option -n");
        }
      }

      if (line.hasOption('q')) params.out = null;
      if (line.hasOption('o')) params.out = new PrintStream(line.getOptionValue('o'));
      if (line.hasOption('w')) params.wait = true;

      if(line.hasOption("nh-conf")) params.nhConf = line.getOptionValue("nh-conf");
      if(line.hasOption("ps-conf")) params.psConf = line.getOptionValue("ps-conf");
      if(line.hasOption("ch-conf")) params.chConf = line.getOptionValue("ch-conf");

      if (params.bootstrap == null && params.localNodeNr <= 0 && params.chConf == null) {
        throw new ParseException("Error: you must specify either a bootstrap address, a cloud " +
                                 "configuration or the number of local peer to setup");
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      printHelp(opts);
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return params;
  }

  private static void waitFor() {
    int tout = 30;

    while (tout > 0) {
      System.out.format("Starting in %d seconds...\n", tout--);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
    }

  }

  /**
   * Topology test thread entry point
   */
  public static void main(String args[]) {
    TopologyNode localTNodes[] = null;
    Parameters params = parseCommandLine(args);
    Boolean syncObject = new Boolean(true);

    try {
      // Setup base node
      System.out.println("Setup peer");
      TopologyNode baseTNode = new TopologyNode(params.addr,
                                                params.nhConf,
                                                params.psConf,
                                                params.chConf,
                                                params.bootstrap,
                                                params.out,
                                                syncObject);

      // Setup local nodes
      if (params.localNodeNr > 0) {
        System.out.format("Setup %d local peers\n", params.localNodeNr);
        localTNodes = new TopologyNode[params.localNodeNr];

        for (int i=0; i<localTNodes.length; i++) {
          NodeID localTNode = new NodeID(String.format("%s:%d",
                                                       params.addr.getIpAddress(),
                                                       (params.addr.getPort() + i + 1)));

          localTNodes[i] = new TopologyNode(localTNode,
                                            params.nhConf,
                                            params.psConf,
                                            params.chConf,
                                            params.addr,
                                            null,
                                            syncObject);
        }

        if (params.wait) {
          waitFor();
        }
        System.out.println("Starting peers...");
        baseTNode.start();
        if (localTNodes != null)
          for (TopologyNode n: localTNodes) n.start();

        baseTNode.join();
      } else {
        if (params.wait) {
          waitFor();
        }
        baseTNode.run();
      }
    } catch (JGrapesException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}