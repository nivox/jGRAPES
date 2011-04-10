package jgrapes;

public class CloudHelper {

  protected long nativeInstanceID;
  protected NodeID cloudNode;

  protected static native long init(long myIdReference, String conf) throws JGrapesException;
  protected CloudHelper(long nativeInstance) {
    this.nativeInstanceID = nativeInstance;
    this.cloudNode = null;
  }

  public native boolean isCloudNode(NodeID node);
  protected native NodeID getCloudNodeImpl();

  public NodeID getCloudNode() {
    if (cloudNode == null)
      cloudNode = getCloudNodeImpl();

    return cloudNode;
  }

  public native void get(String key, byte[] header) throws JGrapesException;
  public native void put(String key, byte[] data) throws JGrapesException;
  public native long getLastTimestamp() throws JGrapesException;
  public native boolean waitForCloud(long seconds) throws JGrapesException;
  public native byte[] recvFromCloud(int maxBytes) throws JGrapesException;
}