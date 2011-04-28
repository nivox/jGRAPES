MAKE = make
CC = cc

LIBGRAPES = libgrapes.a
NET_HELPER = net_helper${NH_INCARNATION}.o
GRAPES_SRC_DIR = ${GRAPES_DIR}/src
GRAPES_INC_DIR = ${GRAPES_DIR}/include

GRAPES_TARGET = all

LIBGRAPES_PATH = ${GRAPES_SRC_DIR}/${LIBGRAPES}
NETHELPER_PATH = ${GRAPES_SRC_DIR}/${NET_HELPER}

INCLUDE_DIR = ./include
JNI_INCLUDE_DIR = ./jni-inc

OBJS = jgrapes_utils.o jgrapes_NetworkHelper.o jgrapes_PeerSampler.o jgrapes_CloudHelper.o
DEPS = ${NET_HELPER}

ifndef PLATFORM
PLATFORM = unix
endif

ifeq (${PLATFORM}, unix)
LIBJGRAPES = libjgrapes.so
endif

ifeq (${PLATFORM}, darwin)
LIBJGRAPES = libjgrapes.dylib
endif

CFLAGS += -Wall -g -fPIC -I${INCLUDE_DIR} -I${JNI_INCLUDE_DIR}
LDFLAGS += -L. -lgrapes
SHARED_FLAGS = -shared

all: libjgrapes

libjgrapes: ${OBJS}
	${CC} ${CFLAGS} ${SHARED_FLAGS} ${OBJS} ${DEPS} -o ${LIBJGRAPES} ${LDFLAGS}

update-grapes:
	@echo "Clean current version of GRAPES"
	@rm -f ${NET_HELPER}
	@rm -rf ${INCLUDE_DIR}/grapes
	@echo "Building grapes from (specify with GRAPES_DIR=...): ${GRAPES_DIR}"
	@cd ${GRAPES_SRC_DIR}; ${MAKE} ${GRAPES_TARGET}
	@cd ${GRAPES_SRC_DIR}; CFLAGS=-fPIC ${MAKE} ${NET_HELPER}
	@echo "Copying libgrapes and includes..."
	@cp -f ${LIBGRAPES_PATH} .
	@cp -f ${NETHELPER_PATH} .
	@cp -rf ${GRAPES_INC_DIR} ${INCLUDE_DIR}/grapes

clean:
	rm -f ${OBJS} ${LIBJGRAPES}