#include <stdlib.h>
#include <stdint.h>

#include <jni.h>

#include "grapes/net_helper.h"
#include "jgrapes_NetworkHelper.h"

#include "jgrapes_utils.h"


/* Return the memory address of the native network helper instance from
   a NetworkHelper object */
jlong get_NetworkHelper_nativeID(JNIEnv *env, jobject obj)
{
  jclass nhClass;
  jfieldID nhNativeID;

  nhClass = (*env)->GetObjectClass(env, obj);
  nhNativeID = (*env)->GetFieldID(env, nhClass, "nativeInstanceID", "J");
  (*env)->DeleteLocalRef(env, nhClass);
  if (nhNativeID == NULL) {
    return 0; /* Exception thrown */
  }

  return (*env)->GetLongField(env, obj, nhNativeID);
}

/***********************************************************************
 * NetworkHelper native methods implementations
 ***********************************************************************/
JNIEXPORT jlong JNICALL Java_jgrapes_NetworkHelper_init
(JNIEnv *env, jclass cls, jstring ipAddr, jint port, jstring conf)
{
  struct nodeID * nh_id = NULL;
  const char *c_ip_addr = NULL;
  int c_port;
  const char *c_conf = NULL;

  c_ip_addr = (*env)->GetStringUTFChars(env, ipAddr, NULL);
  if (c_ip_addr == NULL) {
    return 0; /* OutOfMemoryError already thrown */
  }

  c_conf = (*env)->GetStringUTFChars(env, conf, NULL);
  if (c_ip_addr == NULL) {
    (*env)->ReleaseStringUTFChars(env, ipAddr, c_ip_addr);
    return 0; /* OutOfMemoryError already thrown */
  }

  c_port = (int) port;

  nh_id = net_helper_init(c_ip_addr, c_port, c_conf);
  if (!nh_id) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException", "Initialization of network helper failed");
    (*env)->ReleaseStringUTFChars(env, ipAddr, c_ip_addr);
    (*env)->ReleaseStringUTFChars(env, conf, c_conf);
    return 0;
  }

  /* TODO: is this the right thing?
     Do not release c_ip_addr and c_conf as they could be used by the
     network helper */
  return (jlong) nh_id;
}


JNIEXPORT jboolean JNICALL Java_jgrapes_NetworkHelper_waitForData
(JNIEnv *env, jobject obj, jlong secondsTimeOut)
{
  struct nodeID *nodeID;
  struct timeval tout;
  int data_ready;

  tout.tv_sec = secondsTimeOut;
  tout.tv_usec = 0;

  nodeID = (struct nodeID *) get_NetworkHelper_nativeID(env, obj);
  if (nodeID == NULL) {
    return JNI_FALSE; /* Exception thrown */
  }

  data_ready = wait4data(nodeID, &tout, NULL);

  if (data_ready) return JNI_TRUE;
  else return JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_jgrapes_NetworkHelper_recvFromPeer
(JNIEnv *env, jobject obj, jint maxBytes)
{
  struct nodeID *nodeID;
  int read_bytes;
  struct nodeID *remote;
  int8_t *buffer;
  jclass receivedDataClass;
  jmethodID receivedDataConstructor;
  jbyteArray receivedBytes;
  jstring remoteAddr;
  jobject receivedData;


  nodeID = (struct nodeID *) get_NetworkHelper_nativeID(env, obj);
  if (nodeID == NULL) {
    return JNI_FALSE; /* Exception thrown */
  }

  buffer = calloc(maxBytes, sizeof(int8_t));
  if (!buffer) {
    JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                    "Error allocating memory for receive buffer");
    return NULL;
  }
  read_bytes = recv_from_peer(nodeID, &remote, (uint8_t *) buffer, maxBytes);

  if (read_bytes < 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error receiving data from peer");
    return NULL;
  }

  /* Copy data to a byte[] object */
  receivedBytes = (*env)->NewByteArray(env, read_bytes);
  if (!receivedBytes) {
    free(buffer);
    free(remote);
    return NULL;
  }
  (*env)->SetByteArrayRegion(env, receivedBytes, 0, read_bytes, buffer);
  free(buffer);
  if ((*env)->ExceptionCheck(env)) {
    free(remote);
    return NULL;
  }

  /* Create a String object containing the address of the remote peer */
  remoteAddr = (*env)->NewStringUTF(env, node_addr(remote));
  if (!remoteAddr) {
    free(remote);
    return NULL;
  }
  free(remote);

  /* Allocate new ReceivedData object to hold received information */
  receivedDataClass = (*env)->FindClass(env, "jgrapes/ReceivedData");
  if (receivedDataClass == NULL) {
    return NULL; /* exception thrown */
  }

  receivedDataConstructor = (*env)->GetMethodID(env, receivedDataClass,
                                                "<init>",
                                                "(Ljava/lang/String;[B)V");
  if (receivedDataConstructor == NULL) {
    (*env)->DeleteLocalRef(env, receivedDataClass);
    return NULL; /* exception thrown */
  }

  receivedData = (*env)->NewObject(env, receivedDataClass,
                                   receivedDataConstructor, remoteAddr,
                                   receivedBytes);
  (*env)->DeleteLocalRef(env, receivedDataClass);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  return receivedData;
}
