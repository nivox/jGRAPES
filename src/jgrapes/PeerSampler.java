package jgrapes;

/**
 *
 */
public class PeerSampler {
  /***********************************************************************
   * Instance variables
   ***********************************************************************/
  private long nativeInstanceID;

  /***********************************************************************
   * Constructors
   ***********************************************************************/
  protected static native long init(long myIdReference, byte[] myMetadata, String conf) throws JGrapesException;
  protected PeerSampler(long nativeInstanceID) {
    this.nativeInstanceID = nativeInstanceID;
  }

  /***********************************************************************
   * Grapes PeerSampler interface
   ***********************************************************************/
  public native NodeID[] getCache();
  public native int growCache(int n);
  public native int shrinkCache(int n);
  public native void removePeer(NodeID neighbor);
  public native void addPeer(NodeID neighbor, byte[] metadata);
  public native void parseData(byte[] data) throws JGrapesException;

  public native void changeMetadata(byte[] metadata);
  public native byte[][] getMetadata();

}