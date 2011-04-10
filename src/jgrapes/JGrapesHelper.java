package jgrapes;

import jgrapes.NetworkHelper;
import jgrapes.CloudHelper;


class WaitSync {
  private volatile boolean terminated;
  private JGrapesException e;
  private Object resource;

  public WaitSync() {
    this.e = null;
    this.terminated = false;
  }

  public synchronized void notifyException(JGrapesException e, Object resource) {
    if (this.terminated) return;

    this.e = e;
    this.resource = resource;
    this.terminated = true;
    this.notifyAll();
  }

  public synchronized void notifyDataReady(Object resource) {
    if (this.terminated) return;

    this.resource = resource;
    this.terminated = true;
    this.notifyAll();
  }

  public synchronized boolean isTerminated() {
    return this.terminated;
  }

  public synchronized JGrapesException getException() {
    return this.e;
  }

  public synchronized Object getResource() {
    return this.resource;
  }
}

class WaitForThread extends Thread {

  private NetworkHelper nh;
  private CloudHelper ch;
  private WaitSync sync;
  private long seconds;

  public WaitForThread(NetworkHelper n, long seconds, WaitSync sync)
  {
    this.nh = n;
    this.ch = null;
    this.sync = sync;
    this.seconds = seconds;
  }

  public WaitForThread(CloudHelper c, long seconds, WaitSync sync)
  {
    this.nh = null;
    this.ch = c;
    this.sync = sync;
    this.seconds = seconds;
  }

  public void run()
  {
    Object resource = null;
    boolean res;
    try {
      if (nh != null) {
        resource = nh;
        res = nh.waitForData(seconds);
        if (this.isInterrupted()) return;
        else if (res) sync.notifyDataReady(nh);
      } else {
        resource = ch;
        res = ch.waitForCloud(seconds);
        if (this.isInterrupted()) return;
        else if (res) sync.notifyDataReady(ch);
      }
    } catch (JGrapesException e) {
      sync.notifyException(e, resource);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

public class JGrapesHelper {
  public static Object waitForAny(NetworkHelper netHelpers[], CloudHelper cloudHelpers[],
                                long seconds) throws JGrapesException
  {
    Thread waitThreads[];
    WaitSync sync;
    int i = 0;
    int len = 0;

    if (netHelpers != null) len += netHelpers.length;
    if (cloudHelpers != null) len += cloudHelpers.length;

    waitThreads = new Thread[len];
    sync = new WaitSync();

    JGrapesException ex = null;
    Object resource = null;

    for (NetworkHelper nh: netHelpers)
      if (nh != null) waitThreads[i++] = new WaitForThread(nh, seconds, sync);

    for (CloudHelper ch: cloudHelpers)
      if (ch != null) waitThreads[i++] = new WaitForThread(ch, seconds, sync);

    for (Thread t: waitThreads) if (t != null) t.start();

    try {
      synchronized(sync) {
        sync.wait(seconds * 1000);
        if (sync.isTerminated()) {
          ex = sync.getException();
          resource = sync.getResource();
        }
      }
    } catch (InterruptedException e) {e.printStackTrace();}

    for (Thread t: waitThreads) if (t != null) t.interrupt();

    if (ex != null) throw ex;
    return resource;
  }
}