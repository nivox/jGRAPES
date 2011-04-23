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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import jgrapes.NodeID;
import jgrapes.JGrapesException;

import jgrapes.test.TopologyNode;

public class TopologyTest {

  private NodeID addr = null;
  private NodeID bootstrap = null;
  private int localNodeNr = 0;
  private String nhConf = null;
  private String psConf = null;
  private String chConf = null;
  private PrintStream out = null;
  private int wait = 0;

  TopologyNode localTNodes[] = null;
  TopologyNode baseTNode = null;

  /**
   * Print help message
   */
  public static void printHelp(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("TopologyTest", opts, true);
  }

  /**
   * Parse the command line
   */
  public void parseCommandLine(String args[]) {
    CommandLineParser cmdParser = new GnuParser();
    Options opts = new Options();

    // Setting defaults
    out = System.out;

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
    opts.addOption("w", true, "wait for the specified time before starting nodes");

    // Parse the command line
    try {
      CommandLine line = cmdParser.parse(opts, args);

      if (line.hasOption("help")) printHelp(opts);

      if (line.hasOption('h')) {
        try {
          addr = new NodeID(line.getOptionValue('h'));
        } catch (UnknownHostException e) {
          throw new ParseException("Local node address is not valid");
        }
      } else throw new ParseException("Missing mandatory option -h");

      if (line.hasOption('b')) {
        try {
          bootstrap = new NodeID(line.getOptionValue('h'));
        } catch (UnknownHostException e) {
          throw new ParseException("Bootstrap node address is not valid");
        }
      }

      if (line.hasOption('n')) {
        try {
          localNodeNr = Integer.parseInt(line.getOptionValue('n'));
        } catch (NumberFormatException e) {
          throw new ParseException("Illegal argument for option -n");
        }
      }

      if (line.hasOption('q')) out = null;
      if (line.hasOption('o')) out = new PrintStream(line.getOptionValue('o'));
      if (line.hasOption('w')) wait = Integer.parseInt(line.getOptionValue('w'));

      if(line.hasOption("nh-conf")) nhConf = line.getOptionValue("nh-conf");
      if(line.hasOption("ps-conf")) psConf = line.getOptionValue("ps-conf");
      if(line.hasOption("ch-conf")) chConf = line.getOptionValue("ch-conf");

      if (bootstrap == null && localNodeNr <= 0 && chConf == null) {
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
  }

  private void waitFor() {
    while (wait > 0) {
      System.out.format("Starting in %d seconds...\n", wait--);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
    }
  }

  private void setupBaseNode() throws JGrapesException {
    System.out.println("Setup peer");
    baseTNode = new TopologyNode(addr, nhConf, psConf, chConf, 1, System.out);

  }

  private void setupOtherNodes() throws JGrapesException, UnknownHostException {
    if (localNodeNr > 0) {
      System.out.format("Setup %d local peers\n", localNodeNr);
      localTNodes = new TopologyNode[localNodeNr];

      for (int i=0; i<localTNodes.length; i++) {
        NodeID localTNode;

        localTNode = new NodeID(String.format("%s:%d", addr.getIpAddress(), (addr.getPort()+i+1)));

        localTNodes[i] = new TopologyNode(localTNode, nhConf, psConf, chConf, 0, null);
        localTNodes[i].addNode(addr);
      }
    }
  }

  private void runThreads() {
    baseTNode.start();

    for (TopologyNode localTNode: localTNodes)
      localTNode.start();

    try {
      baseTNode.join();
      for (TopologyNode localTNode: localTNodes)
        localTNode.join();
    } catch (InterruptedException e) {}
  }

  private void run() {
    waitFor();
    runThreads();
  }

  /**
   * Topology test thread entry point
   */
  public static void main(String args[]) throws JGrapesException, UnknownHostException {
    TopologyTest topologyTest = new TopologyTest();

    topologyTest.parseCommandLine(args);

    topologyTest.setupBaseNode();
    topologyTest.setupOtherNodes();

    topologyTest.run();
  }
}