#include <stdio.h>
#include <stdint.h>
#include <sys/time.h>
#include "grapes/net_helper.h"
#include "jgrapes_utils.h"


void JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg)
{
  jclass cls = (*env)->FindClass(env, name);
  /* if cls is NULL, an exception has already been thrown */
  if (cls != NULL) {
    (*env)->ThrowNew(env, cls, msg);
  }
  /* free the local ref */
  (*env)->DeleteLocalRef(env, cls);
}

struct nodeID * createStructFromNodeID(JNIEnv *env, jobject nodeID)
{
  jclass nodeIDClass;
  jfieldID ipField;
  jfieldID portField;

  jstring jip;
  jint jport;
  jsize ip_length;
  char *c_ip;

  struct nodeID *n;

  nodeIDClass = (*env)->GetObjectClass(env, nodeID);

  ipField = (*env)->GetFieldID(env, nodeIDClass, "ip", "Ljava/lang/String;");
  if (ipField == NULL) {
    (*env)->DeleteLocalRef(env, nodeIDClass);
    return NULL; /* Exception throwns */
  }

  portField = (*env)->GetFieldID(env, nodeIDClass, "port", "I");
  if (portField == NULL) {
    (*env)->DeleteLocalRef(env, nodeIDClass);
    return NULL; /* Exception throwns */
  }

  /* Convert string to char array */
  jip = (*env)->GetObjectField(env, nodeID, ipField);
  ip_length = (*env)->GetStringUTFLength(env, jip);
  c_ip = calloc(ip_length + 1, sizeof(char));
  c_ip[ip_length] = '\0';
  (*env)->GetStringUTFRegion(env, jip, 0, ip_length, c_ip);

  jport = (*env)->GetIntField(env, nodeID, portField);

  n = create_node(c_ip, (int) jport);

  return n;
}

/* Return a new NodeID from a nodeID struct. The returned class is
   NOT a peer class. You should deallocate the original nodeID after */
jobject createNodeIDFromStruct(JNIEnv *env, struct nodeID *n)
{
  jclass nodeIDClass;
  jmethodID nodeIDBuilder;
  jobject nodeID;
  jstring jAddr;
  char addr[256];

  nodeIDClass = (*env)->FindClass(env, "jgrapes/NodeID");
  if (nodeIDClass == NULL) {
    return NULL; /* exception thrown */
  }

  nodeIDBuilder = (*env)->GetStaticMethodID(env, nodeIDClass,
                                          "getUnresolvedNodeID",
                                          "(Ljava/lang/String;)Ljgrapes/NodeID;");
  if (nodeIDBuilder == NULL) {
    return NULL; /* exception thrown */
  }

  node_addr(n, addr, 256);
  jAddr = (*env)->NewStringUTF(env, addr);
  nodeID = (*env)->CallStaticObjectMethod(env, nodeIDClass, nodeIDBuilder, jAddr);
  if ((*env)->ExceptionCheck(env)) {
    return NULL;
  }

  /* Release temporary data */
  (*env)->DeleteLocalRef(env, nodeIDClass);

  return nodeID;
}


/* Return a new NodeID array from a nodeID struct array. These are
   not peer classes. You need to deallocate original nodeIDs */
jobjectArray createNodeIDArrayFromStructArray(JNIEnv *env, const struct nodeID **narr,
                                              size_t arr_size)
{
  jclass nodeIDClass;
  jmethodID nodeIDBuilder;
  jobject nodeID;
  jobjectArray nodeIDarr;
  const struct nodeID *n;
  char c_nodeAddr[256];
  jstring nodeAddr;
  int i;

  nodeIDClass = (*env)->FindClass(env, "jgrapes/NodeID");
  if (nodeIDClass == NULL) {
    return NULL; /* exception thrown */
  }

  /* Allocate NodeID array */
  nodeIDarr = (*env)->NewObjectArray(env, arr_size, nodeIDClass, NULL);
  if (nodeIDarr == NULL) {
    return NULL; /* exception thrown */
  }

  /* Obtain reference to nodeID constructor */
  nodeIDBuilder = (*env)->GetStaticMethodID(env, nodeIDClass,
                                        "getUnresolvedNodeID",
                                        "(Ljava/lang/String;)Ljgrapes/NodeID;");
  if (nodeIDBuilder == NULL) {
    return NULL; /* exception thrown */
  }

  /* populate NodeID array */
  for (i=0; i<arr_size; i++) {
    n = narr[i];
    node_addr(n, c_nodeAddr, 256);
    nodeAddr = (*env)->NewStringUTF(env, c_nodeAddr);
    if (!nodeAddr) return NULL;

    nodeID = (*env)->CallStaticObjectMethod(env, nodeIDClass, nodeIDBuilder,
                                            nodeAddr);
    (*env)->DeleteLocalRef(env, nodeAddr);

    if ((*env)->ExceptionCheck(env)){
      return NULL;
    }

    (*env)->SetObjectArrayElement(env, nodeIDarr, i, nodeID);
    if ((*env)->ExceptionCheck(env)){
      return NULL;
    }
  }

  /* Release temporary data */
  (*env)->DeleteLocalRef(env, nodeIDClass);

  return nodeIDarr;
}
