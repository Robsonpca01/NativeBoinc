

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/file.h>
#include <errno.h>
#include <limits.h>

#include <jni.h>



#define MYBUFSIZ (FILENAME_MAX+1)

typedef struct {
	size_t rest_pos;
	size_t rest;
	char buf[MYBUFSIZ];
} mybuf_t;

static void mygetline_init(mybuf_t* buf) {
	buf->rest_pos = 0;
	buf->rest = 0;
}

static char* mygetline(mybuf_t* buf, int fd, char* line, size_t maxlength) {
	size_t line_pos = 0;
	ssize_t readed;
	char* ptr2;
	size_t next_pos2;
	size_t next_linesize;
	if (buf->rest_pos < buf->rest) {
		size_t next_pos;
		size_t linesize;
		char* ptr = memchr(buf->buf + buf->rest_pos, '\n',
				buf->rest - buf->rest_pos);

		if (ptr != NULL) // não encontrado
				{
			next_pos = (ptrdiff_t) ptr - (ptrdiff_t) (buf->buf);
			linesize = next_pos - buf->rest_pos;
			if (linesize > maxlength)
				return NULL; // linha além do tamanho máximo
			memcpy(line, buf->buf + buf->rest_pos, linesize);
			line[linesize] = 0;
			buf->rest_pos = next_pos + 1;
			return line;
		} else {
			line_pos = buf->rest - buf->rest_pos;
			if (line_pos > maxlength)
				return NULL; //linha além do tamanho máximo
			memcpy(line, buf->buf + buf->rest_pos, line_pos);
		}
	}

	
	readed = read(fd, buf->buf, MYBUFSIZ);
	if (readed < 0 || (readed == 0 && line_pos == 0)) {
		buf->rest = 0;
		buf->rest_pos = 0;
		return NULL; // erro
	}
	if (readed == 0) // lnha não vázia
			{
		buf->rest = 0;
		buf->rest_pos = 0;
		line[line_pos] = 0;
		return line;
	}

	ptr2 = memchr(buf->buf, '\n', readed);
	if (ptr2 == NULL
		) // linha além do tamanho máximo
		return NULL;

	next_pos2 = (ptrdiff_t) ptr2 - (ptrdiff_t) (buf->buf);
	next_linesize = line_pos + next_pos2;
	if (next_linesize > maxlength) //linha além do tamanho máximo
		return NULL;

	memcpy(line + line_pos, buf->buf, next_linesize - line_pos);
	line[next_linesize] = 0;

	buf->rest = readed - next_pos2 - 1;
	memmove(buf->buf, buf->buf + next_pos2 + 1, buf->rest);
	buf->rest_pos = 0;

	return line;
}

#define EXECS_NAME ".__execs__"

JNIEXPORT jint JNICALL Java_sk_boinc_nativeboinc_util_SDCardExecs_openExecsLock(JNIEnv* env,
        jclass thiz, jstring dirpathStr, jboolean iswrite) {
	char* execsname;
	const char* dirpath;
	int fd;
	size_t dirlen;
	jclass ioexcept;

	ioexcept = (*env)->FindClass(env,"java/io/IOException");

	dirpath = (*env)->GetStringUTFChars(env, dirpathStr, 0);
	if ((execsname = malloc(PATH_MAX)) == NULL)
		abort();	// não há memoria para4kB (erro fatal)
	dirlen = strlen(dirpath);

	memcpy(execsname, dirpath, dirlen);
	if (dirpath[dirlen-1] == '/')
		strcpy(execsname + dirlen, EXECS_NAME);
	else
		strcpy(execsname + dirlen, "/" EXECS_NAME);

	if (!iswrite) {
		fd = open(execsname, O_RDONLY);
		if (fd == -1) {
			free(execsname);
			(*env)->ReleaseStringUTFChars(env, dirpathStr, dirpath);
			if (errno != ENOENT)
				(*env)->ThrowNew(env, ioexcept, "openExecsLock failed (openrdonly)");
			return -1; // não há executáveis
		}

		flock(fd, LOCK_SH); 
	} else { // escrevendo
		fd = open(execsname, O_CREAT | O_RDWR,0600);
		if (fd == -1) {
			free(execsname);
			(*env)->ReleaseStringUTFChars(env, dirpathStr, dirpath);

			// throw exception
			(*env)->ThrowNew(env, ioexcept, "openExecsLock failed (creat)");
			return -1; // não há executáveis
		}

		flock(fd, LOCK_EX);
	}
	free(execsname);
	(*env)->ReleaseStringUTFChars(env, dirpathStr, dirpath);
	return fd;
}

JNIEXPORT void JNICALL Java_sk_boinc_nativeboinc_util_SDCardExecs_readExecs(JNIEnv* env,
		jclass thiz, jint fd, jobject coll) {
	char* line;
	const char* filename;
	mybuf_t buf;
	int lineCount = 0;
	jclass collcls;
	jmethodID addMid;
	jmethodID clearMid;
	jclass ioexcept;

	ioexcept = (*env)->FindClass(env,"java/io/IOException");

	collcls = (*env)->GetObjectClass(env, coll);
	addMid = (*env)->GetMethodID(env, collcls, "add", "(Ljava/lang/Object;)Z");
	clearMid = (*env)->GetMethodID(env, collcls, "clear", "()V");

	(*env)->CallVoidMethod(env, coll, clearMid);
	if (fd == -1) // interrompe execução e retorna
		return;

	line = malloc(FILENAME_MAX + 1);

	mygetline_init(&buf);
	if (lseek(fd, 0, SEEK_SET)==-1)
		goto error;

	errno = 0;
	while (mygetline(&buf, fd, line, FILENAME_MAX) != NULL) {
		jstring lineStr = (*env)->NewStringUTF(env, line);
		(*env)->CallBooleanMethod(env, coll, addMid, lineStr);
		errno = 0;
	}
	if (errno == EIO)
		goto error;

	free(line);
	return;
error:
	free(line);
	(*env)->ThrowNew(env, ioexcept, "readExecs failed");
	return;
}

JNIEXPORT jboolean JNICALL Java_sk_boinc_nativeboinc_util_SDCardExecs_checkExecMode(JNIEnv* env,
		jclass thiz, jint fd, jstring filenameStr) {
	char* line;
	const char* filename;
	mybuf_t buf;
	jclass ioexcept;

	ioexcept = (*env)->FindClass(env,"java/io/IOException");

	if (fd == -1)
		return 0;

	line = malloc(FILENAME_MAX + 1);

	filename = (*env)->GetStringUTFChars(env, filenameStr, 0);
	mygetline_init(&buf);
	if (lseek(fd, 0, SEEK_SET)==-1) 
		goto error;

	errno = 0;
	while (mygetline(&buf, fd, line, FILENAME_MAX) != NULL) {
		if (strcmp(filename, line) == 0) { 
			free(line);
			(*env)->ReleaseStringUTFChars(env, filenameStr, filename);
			return 1;
		}
	}
	if (errno == EIO)
		goto error;

	free(line);
	(*env)->ReleaseStringUTFChars(env, filenameStr, filename);
	return 0;
error:
	free(line);
	(*env)->ReleaseStringUTFChars(env, filenameStr, filename);
	(*env)->ThrowNew(env, ioexcept, "checkExecMode failed (EIO)");
	return -1;
}

JNIEXPORT void JNICALL Java_sk_boinc_nativeboinc_util_SDCardExecs_setExecMode(JNIEnv* env,
		jclass thiz, jint fd, jstring filenameStr, jboolean isexec) {
    char* line;
	const char* filename;
	char filenamelen;
	mybuf_t buf;
	off_t tmppos, execpos = -1;
	jclass ioexcept;
	ioexcept = (*env)->FindClass(env, "java/io/IOException");

	if (fd == -1) {
		if (isexec) {
			(*env)->ThrowNew(env,ioexcept,"setExecMode failed");
			return;
		}
	}

	line = malloc(FILENAME_MAX + 1);

	filename = (*env)->GetStringUTFChars(env, filenameStr, 0);
	filenamelen = strlen(filename);

	mygetline_init(&buf);

	tmppos = 0;
	if (lseek(fd, 0, SEEK_SET)==-1) 
		goto error;
	errno = 0;
	while (mygetline(&buf, fd, line, FILENAME_MAX) != NULL) {
		if (strcmp(filename, line) == 0) { 
			execpos = tmppos;
			break;
		}
		tmppos += strlen(line) + 1;
	}
	if (errno == EIO)
		goto error;

	if (isexec) { 
		if (execpos == -1) { 
			int ret;
			ret = lseek(fd, 0, SEEK_END);
			if (ret != -1)
				write(fd, filename, filenamelen);
			if (ret != -1)
				write(fd, "\n", 1);
			if (ret == -1)
				goto error;
		}
	} else { 
		if (execpos != -1) {
			int readed;
			off_t ipos = execpos;
			char mybuf[256];

			do {
				int ret;
				ret = lseek(fd, ipos + filenamelen + 1, SEEK_SET);
				if (ret != -1)
					ret = readed = read(fd, mybuf, 256);
				if (ret != -1)
					ret = lseek(fd, ipos, SEEK_SET);
				if (ret != -1)
					ret = write(fd, mybuf, readed);
				if (ret == -1)
					goto error;
				ipos += readed;
			} while (readed == 256);
			if (ftruncate(fd, ipos)==-1) 
				goto error;
		}
	}
	free(line);
	(*env)->ReleaseStringUTFChars(env, filenameStr, filename);
	return;
error:
	free(line);
	(*env)->ReleaseStringUTFChars(env, filenameStr, filename);
	(*env)->ThrowNew(env,ioexcept,"setExecMode failed");
	return;
}

JNIEXPORT void JNICALL Java_sk_boinc_nativeboinc_util_SDCardExecs_closeExecsLock(JNIEnv* env,
        jclass thiz, jint fd) {
    if (fd != -1) {
		flock(fd, LOCK_UN);
		close(fd);
	}
}
