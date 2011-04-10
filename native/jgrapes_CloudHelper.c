#include <stdint.h>

#include "grapes/cloud_helper.h"
#include "jgrapes_CloudHelper.h"
#include "jgrapes_utils.h"


/* Return the memory address of the native cloud helper instance from
   a CloudHelper object */
jlong get_CloudHelper_nativeID(JNIEnv *env, jobject obj)
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
 * CloudHelper native methods implementations
 ***********************************************************************/

JNIEXPORT jlong JNICALL Java_jgrapes_CloudHelper_init
(JNIEnv *env, jclass cls, jlong nativeReferenceId, jstring conf)
{
  struct nodeID * nh_id = NULL;
  struct cloud_helper_context *cloud_ctx;
  const char *c_conf = NULL;
  nh_id = (struct nodeID *) nativeReferenceId;
  c_conf = (*env)->GetStringUTFChars(env, conf, NULL);
  if (c_conf == NULL) {
    return 0; /* OutOfMemoryError already thrown */
  }

  cloud_ctx = cloud_helper_init(nh_id, c_conf);
  if (!cloud_ctx) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Initialization of cloud helper failed");
    (*env)->ReleaseStringUTFChars(env, conf, c_conf);
    return 0;
  }

  /* TODO: is this the right thing?
     Do not release c_conf as it could be used by the cloud helper */
  return (jlong) cloud_ctx;
}

JNIEXPORT void  JNICALL Java_jgrapes_CloudHelper_get
(JNIEnv *env, jobject obj, jstring key, jbyteArray header)
{
  struct cloud_helper_context *ctx;
  const char *c_key;
  int8_t *c_header;
  int header_len;
  int err;

  ctx = (struct cloud_helper_context *) get_CloudHelper_nativeID(env, obj);
  if (!ctx) return;

  c_key = (*env)->GetStringUTFChars(env, key, NULL);
  if (!key) return;

  header_len = (*env)->GetArrayLength(env, header);
  c_header = calloc(header_len, sizeof(int8_t));
  (*env)->GetByteArrayRegion(env, header, 0, header_len, c_header);
  if ((*env)->ExceptionCheck(env)) return;

  err = get_from_cloud(ctx, c_key, (uint8_t *)c_header, header_len, 1);

  (*env)->ReleaseStringUTFChars(env, key, c_key);
  if (err) {
    free(c_header);
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error performing get request");
  }
}

JNIEXPORT void JNICALL Java_jgrapes_CloudHelper_put
(JNIEnv *env, jobject obj, jstring key, jbyteArray data)
{
  struct cloud_helper_context *ctx;
  const char *c_key;
  int8_t *c_data;
  int data_len;
  int err;

  ctx = (struct cloud_helper_context *) get_CloudHelper_nativeID(env, obj);
  if (!ctx) return;

  c_key = (*env)->GetStringUTFChars(env, key, NULL);
  if (!key) return;

  data_len = (*env)->GetArrayLength(env, data);
  c_data = calloc(data_len, sizeof(int8_t));
  (*env)->GetByteArrayRegion(env, data, 0, data_len, c_data);
  if ((*env)->ExceptionCheck(env)) return;

  err = put_on_cloud(ctx,c_key, (uint8_t *)c_data, data_len, 1);

  (*env)->ReleaseStringUTFChars(env, key, c_key);
  if (err) {
    free(c_data);
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error performing put request");
  }
}

JNIEXPORT jlong JNICALL Java_jgrapes_CloudHelper_getLastTimestamp
(JNIEnv *env, jobject obj)
{
  struct cloud_helper_context *ctx;
  time_t tstamp;
  ctx = (struct cloud_helper_context *) get_CloudHelper_nativeID(env, obj);
  if (!ctx) return 0;

  tstamp = timestamp_cloud(ctx);

  if (tstamp <= 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "No timestamp available");
  }

  return tstamp;
}

JNIEXPORT jboolean JNICALL Java_jgrapes_CloudHelper_waitForCloud
(JNIEnv *env, jobject obj, jlong seconds)
{
  struct cloud_helper_context *ctx;
  struct timeval tout;
  int data_ready;

  ctx = (struct cloud_helper_context *) get_CloudHelper_nativeID(env, obj);
  if (!ctx) return 0;

  tout.tv_sec = seconds;
  tout.tv_usec = 0;

  data_ready = wait4cloud(ctx, &tout);
  if (data_ready < 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Get operation failed (unknown key?)");
    return 0;
  }

  if (data_ready) return JNI_TRUE;
  else return JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_jgrapes_CloudHelper_recvFromCloud
(JNIEnv *env, jobject obj, jint maxBytes)
{
  struct cloud_helper_context *ctx;
  int8_t *buffer;
  jbyteArray receivedBytes;
  int read_bytes;

  ctx = (struct cloud_helper_context *) get_CloudHelper_nativeID(env, obj);
  if (!ctx) return 0;

  buffer = calloc(maxBytes, sizeof(int8_t));
  if (!buffer) {
    JNU_ThrowByName(env, "java/lang/OutOfMemoryException",
                    "Error allocating memory for receive buffer");
    return NULL;
  }

  read_bytes = recv_from_cloud(ctx, (uint8_t *)buffer, maxBytes);
  if (read_bytes < 0) {
    JNU_ThrowByName(env, "jgrapes/JGrapesException",
                    "Error receiving data from cloud");
    return NULL;
  }

  /* Copy data to a byte[] object */
  receivedBytes = (*env)->NewByteArray(env, read_bytes);
  if (!receivedBytes) {
    free(buffer);
    return NULL;
  }
  (*env)->SetByteArrayRegion(env, receivedBytes, 0, read_bytes, buffer);
  free(buffer);
  if ((*env)->ExceptionCheck(env)) return NULL;

  return receivedBytes;
}
