#ifndef JGRAPES_UTILS_H
#define JGRAPES_UTILS_H

#include <stdlib.h>
#include <stdint.h>

#include <jni.h>
#include <grapes/net_helper.h>

void JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg);
struct nodeID * createStructFromNodeID(JNIEnv *env, jobject nodeID);
jobject createNodeIDFromStruct(JNIEnv *env, struct nodeID *n);
jobjectArray createNodeIDArrayFromStructArray(JNIEnv *env,
                                              const struct nodeID **narr,
                                              size_t arr_size);
#endif /* JGRAPES_UTILS_H */
