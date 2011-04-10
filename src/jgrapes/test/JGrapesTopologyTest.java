package jgrapes.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.ArrayList;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jgrapes.JGrapes;
import jgrapes.JGrapesHelper;
import jgrapes.JGrapesException;
import jgrapes.NetworkHelper;
import jgrapes.CloudHelper;
import jgrapes.NodeID;
import jgrapes.PeerSampler;
import jgrapes.ReceivedData;

class ShutdownHook extends Thread{
  private Process pslist[];

  protected ShutdownHook(Process pslist[]) {
    this.pslist = pslist;
  }

  public void run() {
    System.out.println("Killing " + pslist.length + " processes");
    for (Process p: pslist)
      p.destroy();
  }
}

class ProcessOutputPrinter extends Thread {
  private Process p;

  protected ProcessOutputPrinter(Process p) {
    this.p = p;
  }

  public void run() {
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()));

    String line;
    try {
      while ((line = r.readLine()) != null) {
        System.out.println(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}


public class JGrapesTopologyTest implements Runnable {

  protected NetworkHelper nh;
  protected PeerSampler ps;
  protected CloudHelper ch;

  protected PrintStream out;

  protected JGrapesTopologyTest(NodeID node, String nhConf, String psConf, String chConf,
                                PrintStream out) throws JGrapesException
  {
    this.nh = JGrapes.newNetworkHelperInstance(node.getIpAddress(), node.getPort(), nhConf);

    if (chConf != null)
      this.ch = JGrapes.newCloudHelperInstance(nh, chConf);
    else this.ch = null;

    this.ps = JGrapes.newPeerSamplerInstance(nh, null, psConf);
    this.out = out;
  }

  private void print(String line) {
    if (out != null) out.println(line);
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


  private static void printHelp(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("JGrapesTopologyTest", opts, true);
  }

  public static void main(String args[]) {
    CommandLineParser cmdParser = new GnuParser();
    Options opts = new Options();

    opts.addOption(null, "help", false, "print this help message");
    opts.addOption("h", true, "specify the local address to use (ip:port)");
    opts.addOption("b", true, "specify a remote node to bootstrap the local node");
    opts.addOption(null, "nh-conf", true, "specify the net_helper configuration");
    opts.addOption(null, "ps-conf", true, "specify the peer_sampler configuration");
    opts.addOption(null, "ch-conf", true, "specify the cloud_helper configuration. Causes the cloud helper to be initialized.");
    opts.addOption("n", true, "number of peer to instantiate. Cause formation of local topology (bypass -b)");
    opts.addOption("q", true, "be quiet");
    opts.addOption("o", true, "log file");


    String addr = null;
    String bootstrap = null;
    int localPeers = 0;
    String nhConf = "";
    String psConf = "";
    String chConf = "";
    PrintStream out = System.out;

    try {
      CommandLine line = cmdParser.parse(opts, args);

      if (line.hasOption("help")) printHelp(opts);

      if (line.hasOption('h')) addr = line.getOptionValue('h');
      else throw new ParseException("Missing mandatory option -h");

      if (line.hasOption('b')) bootstrap = line.getOptionValue('b');
      if (line.hasOption('n')) localPeers = Integer.parseInt(line.getOptionValue('n'));
      if (line.hasOption('q')) out = null;
      if (line.hasOption('o')) out = new PrintStream(line.getOptionValue('o'));

      if(line.hasOption("nh-conf")) nhConf = line.getOptionValue("nh-conf");
      if(line.hasOption("ps-conf")) psConf = line.getOptionValue("ps-conf");
      if(line.hasOption("ch-conf")) chConf = line.getOptionValue("ch-conf");


    } catch (ParseException e) {
      System.err.println(e.getMessage());
      printHelp(opts);
      System.exit(1);
    } catch (Exception e) {
      System.err.println("Error while parsing cml line: " + e.getMessage());
      printHelp(opts);
      System.exit(1);
    }

    if (bootstrap == null && localPeers <= 0 && chConf == null) {
      System.err.println("Specify either bootstrap node, cloud configuration or number of local topology peers\n");
      printHelp(opts);
      System.exit(1);
    }

    // Setup local node
    NodeID localNode = null;
    try {
      localNode = new NodeID(addr);
    } catch (UnknownHostException e) {
      System.err.println("Local node address is not valid");
      System.exit(1);
    }

    JGrapesTopologyTest localInstance = null;
    try {
      localInstance = new JGrapesTopologyTest(localNode, nhConf, psConf, chConf, out);
    } catch (JGrapesException e) {
      e.printStackTrace();
      System.exit(1);
    }


    //Add bootstrap node ort create local topology
    if (localPeers > 0) {
      ArrayList<String> cmdLine = new ArrayList<String>();
      Thread localTopoThreads[] = new Thread[localPeers];
      Process localTopoProcess[] = new Process[localPeers];
      ProcessBuilder pb;
      String ltaddr;

      Runtime.getRuntime().addShutdownHook(new ShutdownHook(localTopoProcess));

      pb = new ProcessBuilder(cmdLine);

      try {
        for (int i=0; i<localPeers; i++) {
          ltaddr = String.format("%s:%d", localNode.getIpAddress(), localNode.getPort()+1+i);

          cmdLine.clear();
          cmdLine.add("java");
          cmdLine.add("-cp");
          cmdLine.add(System.getProperty("java.class.path"));
          cmdLine.add(("-Djava.library.path=" + System.getProperty("java.library.path")));
          cmdLine.add(localInstance.getClass().getCanonicalName());
          cmdLine.add(String.format("-h%s", ltaddr));
          if (nhConf != null) cmdLine.add(String.format("--nh-conf=%s", nhConf));
          if (psConf != null) cmdLine.add(String.format("--ps-conf=%s", psConf));

          if (chConf != null) cmdLine.add(String.format("--ch-conf=%s", chConf));
          else cmdLine.add(String.format("-b%s", localNode));

          System.out.println("Starting " + ltaddr);
          localTopoProcess[i] = pb.start();
          localTopoThreads[i] = new ProcessOutputPrinter(localTopoProcess[i]);
          localTopoThreads[i].start();
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
      localInstance.run();
    } else {
      NodeID bootstrapNode = null;
      if (bootstrap != null) {
        try {
          bootstrapNode = new NodeID(bootstrap);
        } catch (UnknownHostException e) {
          System.err.println("Bootstrap node address is not valid");
          System.exit(1);
        }

        System.out.println("Adding bootstrap peer");
        localInstance.ps.addPeer(bootstrapNode, null);
      }
      localInstance.run();
    }

    System.exit(0);
  }

}