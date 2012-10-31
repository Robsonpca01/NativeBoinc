

#include <sys/stat.h>
#include <jni.h>

JNIEXPORT jint JNICALL Java_sk_boinc_nativeboinc_util_Chmod_getmod(JNIEnv* env,
                jclass thiz, jstring pathStr)
{
	const char* path = (*env)->GetStringUTFChars(env, pathStr, 0);
	struct stat stbuf;
	int status = stat(path, &stbuf);
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return (status>=0) ? stbuf.st_mode : -1;
}

JNIEXPORT jboolean JNICALL Java_sk_boinc_nativeboinc_util_Chmod_chmod(JNIEnv* env,
                jclass thiz, jstring pathStr, jint mode)
{
  const char* path = (*env)->GetStringUTFChars(env, pathStr, 0);
  int status = 0;
  status = chmod(path, mode);
  (*env)->ReleaseStringUTFChars(env, pathStr, path);
  return (status>=0) ? 1 : 0;
}

