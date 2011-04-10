MAKE = make
CC = cc

LIBGRAPES = libgrapes.a
NET_HELPER = net_helper${NH_INCARNATION}.o
GRAPES_SRC_DIR = ${GRAPES_DIR}/src
GRAPES_INC_DIR = ${GRAPES_DIR}/include
GRAPES_CLOUD_DIR = ${GRAPES_SRC_DIR}/CloudSupport

GRAPES_TARGET = all
DELEGATE_HELPERS_TARGET = delegate_helpers

LIBGRAPES_PATH = ${GRAPES_SRC_DIR}/${LIBGRAPES}
NETHELPER_PATH = ${GRAPES_SRC_DIR}/${NET_HELPER}
DELEGATE_HELPERS_PATH = ${GRAPES_CLOUD_DIR}/*.so

INCLUDE_DIR = ./include
JNI_INCLUDE_DIR = ./jni-inc

OBJS = jgrapes_utils.o jgrapes_NetworkHelper.o jgrapes_PeerSampler.o jgrapes_CloudHelper.o
DEPS = ${NET_HELPER}
LIBJGRAPES = libjgrapes.dylib

CFLAGS += -Wall -g -I${INCLUDE_DIR} -I${JNI_INCLUDE_DIR}
LDFLAGS += -lgrapes
SHARED_FLAGS = -shared -fPIC

all: libjgrapes

libjgrapes: ${OBJS}
	${CC} ${CFLAGS} ${SHARED_FLAGS} ${OBJS} ${DEPS} -o ${LIBJGRAPES} ${LDFLAGS}

update-grapes:
	@echo "Building grapes from (specify with GRAPES_DIR=...): ${GRAPES_DIR}"
	@cd ${GRAPES_SRC_DIR}; ${MAKE} ${GRAPES_TARGET}
	@cd ${GRAPES_SRC_DIR}; ${MAKE} ${NET_HELPER}
	@cd ${GRAPES_CLOUD_DIR}; ${MAKE} ${DELEGATE_HELPERS_TARGET}
	@echo "Copying libgrapes, cloud delegate helpers and includes..."
	@cp -f ${LIBGRAPES_PATH} .
	@cp -f ${NETHELPER_PATH} .
	@cp -f ${DELEGATE_HELPERS_PATH} .
	@cp -rf ${GRAPES_INC_DIR} ${INCLUDE_DIR}/grapes

clean:
	rm -f ${OBJS} ${LIBJGRAPES}