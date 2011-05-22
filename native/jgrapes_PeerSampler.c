#include <stdlib.h>
#include <stdint.h>

#include <jni.h>

#include "grapes/peersampler.h"
#include "jgrapes_PeerSampler.h"

#include "jgrapes_utils.h"


/* Return the memory address of the native peersampler instance from
   an PeerSampler object */
jlong get_PeerSampler_nativeID(JNIEnv *env, jobject obj)
{
  jclass psClass;
  jfieldID psNativeID;


  psClass = (*env)->GetObjectClass(env, obj);
  psNativeID = (*env)->GetFieldID(env, psClass, "nativeInstanceID", "J");
  (*env)->DeleteLocalRef(env, psClass);
  if (psNativeID == NULL) {
    return 0; /* Exception throwns */
  }

  return (*env)->GetLongField(env, obj, psNativeID);
}

/***********************************************************************
 * PeerSampler native methods implementations
 ***********************************************************************/
JNIEXPORT jlong JNICALL Java_jgrapes_PeerSampler_init
(JNIEnv *env, jclass cls, jlong nativeReferenceId, jbyteArray metadata,
 jstring conf)
{
  struct nodeID * nh_id = NULL;
  struct psample_context *ps;
  const char *c_conf = NULL;
  int8_t *c_metadata;
  int metadata_size;

  nh_id = (struct nodeID *) nativeReferenceId;
  c_conf = (*env)->GetStringUTFChars(env, conf, NULL);
  if (c_conf == NULL) {
    return 0; /* OutOfMemoryError already thrown */
  }


  /*
   * TODO: c_metadata should be freed or not!?
   * if c_metadata is to be released after psample_init then this way
   * of obtaining metadata value is possibly more efficient.
   *
   * c_metadata = (*env)->GetByteArrayElements(env, metadata, NULL);
   *
   * But if c_metadata is not being freed then this may cause a memory
   * leak since the JVM will not GC metadata.
   */
  if (metadata != NULL) {
    metadata_size = (*env)->GetArrayLength(env, metadata);
    c_metadata = calloc(metadata_size, sizeof(int8_t));
    if (!c_metadata) {
      JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                      "Error allocating memory for metadata array");
      (*env)->ReleaseStringUTFChars(env, conf, c_conf);
      return 0;
    }
    (*env)->GetByteArrayRegion(env, metadata, 0, metadata_size, c_metadata);
    ps = psample_init(nh_id, c_metadata, metadata_size, c_conf);
  } else {
    ps = psample_init(nh_id, NULL, 0, c_conf);
  }

  if (!ps) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Initialization of peer sampler failed");
    (*env)->ReleaseStringUTFChars(env, conf, c_conf);
    return 0;
  }

  /* TODO: is this the right thing?
     Do not release c_conf as it could be used by the peer sampler  */
  return (jlong) ps;
}

JNIEXPORT jobjectArray JNICALL Java_jgrapes_PeerSampler_getCache
(JNIEnv *env, jobject obj)
{
  struct psample_context *ps;
  const struct nodeID **cache;
  int cache_size;
  jobjectArray jcache;


  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return NULL;

  cache = psample_get_cache(ps, &cache_size);

  jcache = createNodeIDArrayFromStructArray(env, cache, cache_size);

  return jcache;
}

JNIEXPORT jint JNICALL Java_jgrapes_PeerSampler_growCache
(JNIEnv *env, jobject obj, jint n)
{
  struct psample_context *ps;
  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return 0;

  return psample_grow_cache(ps, (int) n);
}

JNIEXPORT jint JNICALL Java_jgrapes_PeerSampler_shrinkCache
(JNIEnv *env, jobject obj, jint n)
{
  struct psample_context *ps;
  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return 0;

  return psample_shrink_cache(ps, (int) n);
}

JNIEXPORT void JNICALL Java_jgrapes_PeerSampler_removePeer
(JNIEnv *env, jobject obj, jobject nodeID)
{
  struct psample_context *ps;
  struct nodeID *n;

  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return;

  n = createStructFromNodeID(env, nodeID);
  if (!n) return;

  if (psample_remove_peer(ps, n) < 0) {
  JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error removing peer");
  }
  free(n);
}

JNIEXPORT void JNICALL Java_jgrapes_PeerSampler_addPeer
(JNIEnv *env, jobject obj, jobject nodeID, jbyteArray metadata)
{
  struct psample_context *ps;
  struct nodeID *n;
  jsize metadata_size;
  int8_t *c_metadata;
  int res;

  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return;

  n = createStructFromNodeID(env, nodeID);
  if (!n) return;

  /* TODO: free c_metadata or not?! */
  if (metadata != NULL) {
    metadata_size = (*env)->GetArrayLength(env, metadata);
    c_metadata = calloc(metadata_size, sizeof(int8_t));
    if (!c_metadata) {
      JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                      "Error allocating memory for metadata array");
      return;
    }
    (*env)->GetByteArrayRegion(env, metadata, 0, metadata_size, c_metadata);

    res = psample_add_peer(ps, n, c_metadata, metadata_size);
  } else {
    res = psample_add_peer(ps, n, NULL, 0);
  }

  if (res < 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error adding peer to cache");
    return;
  }
}

JNIEXPORT void JNICALL Java_jgrapes_PeerSampler_changeMetadata
(JNIEnv *env, jobject obj, jbyteArray metadata)
{
  struct psample_context *ps;
  jsize metadata_size;
  int8_t *c_metadata;

  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return;

  /* TODO: free c_metadata or not?! */
  metadata_size = (*env)->GetArrayLength(env, metadata);
  c_metadata = calloc(metadata_size, sizeof(uint8_t));
  if (!c_metadata) {
    JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                    "Error allocating memory for metadata array");
    return;
  }
  (*env)->GetByteArrayRegion(env, metadata, 0, metadata_size, c_metadata);

  if (psample_change_metadata(ps,c_metadata, metadata_size) < 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error changing the metadata");
  }
  return;
}

JNIEXPORT jobjectArray JNICALL Java_jgrapes_PeerSampler_getMetadata
(JNIEnv *env, jobject obj)
{
  struct psample_context *ps;
  const struct nodeID **cache;
  int metadata_size;
  int node_nr;
  const int8_t *metadata;

  jclass byteArrayClass;
  jobjectArray jMetadataArray;

  int i;

  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return NULL;

  cache = psample_get_cache(ps, &node_nr);
  metadata = (const int8_t *) psample_get_metadata(ps, &metadata_size);

  byteArrayClass = (*env)->FindClass(env, "[B");
  if (byteArrayClass == NULL) {
    return NULL; /* exception thrown */
  }

  jMetadataArray = (*env)->NewObjectArray(env, node_nr, byteArrayClass, NULL);
  (*env)->DeleteLocalRef(env, byteArrayClass);
  if (jMetadataArray == NULL) {
    return NULL; /* exception thrown */
  }

  /* Copy the data to the java byte array*/
  for (i=0; i<node_nr; i++) {
    jbyteArray jMetadata;

    jMetadata = (*env)->NewByteArray(env, metadata_size);
    if (jMetadata == NULL) {
      (*env)->DeleteLocalRef(env, jMetadataArray);
      return NULL;
    }

    (*env)->SetByteArrayRegion(env, jMetadata, 0, metadata_size,
                               metadata + (i*metadata_size));

    if ((*env)->ExceptionCheck(env)) {
      (*env)->DeleteLocalRef(env, jMetadataArray);
      return NULL;
    }
    (*env)->SetObjectArrayElement(env, jMetadataArray, i, jMetadata);
  }

  return jMetadataArray;
}

JNIEXPORT void JNICALL Java_jgrapes_PeerSampler_parseData
(JNIEnv *env, jobject obj, jbyteArray data)
{
  struct psample_context *ps;
  jsize data_size;
  int8_t *c_data;
  int res;

  ps = (struct psample_context *) get_PeerSampler_nativeID(env, obj);
  if (!ps) return;

  if (data) {
    data_size = (*env)->GetArrayLength(env, data);
    c_data = calloc(data_size, sizeof(int8_t));
    if (!c_data) {
      JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                      "Error allocating memory for data array");
      return;
    }
    (*env)->GetByteArrayRegion(env, data, 0, data_size, c_data);

    res = psample_parse_data(ps, (uint8_t *) c_data, data_size);
    free(c_data);
  } else {
    res = psample_parse_data(ps, NULL, 0);
  }
  if (res < 0) JNU_ThrowByName(env, "jgrapes/JGrapesException",
                               "Error pasring data");
  return;
}
