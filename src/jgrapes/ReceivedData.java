package jgrapes;

public class ReceivedData {
  protected String remoteNodeAddr;
  protected byte[] data;

  protected ReceivedData(String remoteAddr, byte[] data) {
    this.remoteNodeAddr = remoteAddr;
    this.data = data;
  }

  public NodeID getRemotePeerNodeID() {
    return NodeID.getUnresolvedNodeID(remoteNodeAddr);
  }

  public byte[] getData() {
    return data;
  }
}