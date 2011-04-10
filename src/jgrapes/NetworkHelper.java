package jgrapes;

public class NetworkHelper {
  protected long nativeInstanceID;
  protected NodeID myNodeID;

  protected static native long init(String ipAddr, int port, String conf) throws JGrapesException;
  protected NetworkHelper(long nativeInstanceID, String ip, int port) {
    this.nativeInstanceID = nativeInstanceID;
    this.myNodeID = NodeID.getUnresolvedNodeID(String.format("%s:%d", ip, port));
  }

  public native boolean waitForData(long seconds);
  public native ReceivedData recvFromPeer(int maxBytes) throws JGrapesException;

  protected long getNodeIdRefenece() {
    return nativeInstanceID;
  }

  public NodeID getLocalNodeID() {
    return myNodeID;
  }
}