/* Copyright (C) 2008-2013 Quartam Software / Jan Schenkel.
 
 This file is part of the [qrtjvm] project, also known as
 Quartam JavaVirtualMachine External for LiveCode.
 
 [qrtjvm] is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License v3 as published by the Free
 Software Foundation.
 
 [qrtjvm] is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.
 
 You should have received a copy of the GNU General Public License
 along with [qrtjvm].  If not see <http://www.gnu.org/licenses/>.  */


#if defined(TARGET_PLATFORM_LINUX)
	#include <jni.h>
	#include <stdlib.h>
	#include <dlfcn.h>
	#define DLOPEN(dynlib) dlopen(dynlib, RTLD_NOW)
	#define DLSYM dlsym
	#define HMODULE void *
#elif defined(TARGET_PLATFORM_WINDOWS)
	#include <jni.h>
	#include <stdlib.h>
	#include <windows.h>
	#define DLOPEN(dynlib) LoadLibrary(dynlib)
	#define DLSYM GetProcAddress
#elif defined(TARGET_PLATFORM_MACOS_X)
	#include <JavaVM/jni.h>
	#include <CoreFoundation/CoreFoundation.h>
	#include <sys/stat.h>
	#include <sys/resource.h>
#endif

#if defined(BUILD_ENV_XCODE_4)
	#include "external.h"
#else
	#include <revolution/external.h>
#endif

#include "qrtjvm.h"

static int s_jvm_status = 0;
static char * s_jvm_exception;
static char * s_ext_exception;

#if defined(TARGET_PLATFORM_WINDOWS) || defined(TARGET_PLATFORM_LINUX)
static HMODULE s_rtl_handle = 0;
#endif

static JavaVM * s_rt_jvm = NULL;

static jclass s_rt_externalstring_class = 0;
static jmethodID s_rt_externalstring_init_MID = 0;
static jmethodID s_rt_externalstring_getbytes_MID = 0;
static jmethodID s_rt_externalstring_getlength_MID = 0;
static jclass s_rt_string_class = 0;
static jclass s_rt_hashmap_class = 0;
static jmethodID s_rt_hashmap_init_MID = 0;
static jmethodID s_rt_hashmap_put_MID = 0;

static jclass s_eh_class = 0;
static jobject s_eh_object = 0;
static jmethodID s_eh_getInstance_MID = 0;
static jmethodID s_eh_getExceptionText_MID = 0;
static jmethodID s_eh_loadExternalLibrary_MID = 0;
static jmethodID s_eh_unloadExternalLibrary_MID = 0;
static jmethodID s_eh_getExternalLibraries_MID = 0;
static jmethodID s_eh_getExternalPackages_MID = 0;
static jmethodID s_eh_getExternalCommands_MID = 0;
static jmethodID s_eh_getExternalFunctions_MID = 0;
static jmethodID s_eh_callExternalCommand_MID = 0;
static jmethodID s_eh_callExternalFunction_MID = 0;


///////////////////////////////////////////////////////////////////////////////
//
// BEGIN USER DEFINITIONS

//-----------------------------------------------------------------------------
// PART 0: Helper functions
//-----------------------------------------------------------------------------

void replace_char (char *str, char from, char to) {
	for (int index = 0, count = strlen(str); index < count; index++) {
		if (str[index] == from) {
			str[index] = to;
		}
	}
}

char *jstring2cstring(JNIEnv *p_env, jstring p_jstring) {
	if (p_jstring == NULL) {
		return NULL;
	}
	unsigned int t_cstring_length = p_env->GetStringLength(p_jstring);
	char *t_cstring;
	t_cstring = (char *)malloc(t_cstring_length + 1);
	p_env->GetStringUTFRegion(p_jstring, 0, t_cstring_length, t_cstring);
	return t_cstring;
}

ExternalString jxstring2lcxstring(JNIEnv *p_env, jobject p_jxstring) {
	jint t_length = p_env->CallIntMethod(p_jxstring, s_rt_externalstring_getlength_MID);
	jbyteArray t_bytes = (jbyteArray) p_env->CallObjectMethod(p_jxstring, s_rt_externalstring_getbytes_MID);
	ExternalString t_lcxstring;
	t_lcxstring.length = (int) t_length;
	t_lcxstring.buffer = (char *)malloc(t_length);
	jbyte *t_buffer = (jbyte *)t_lcxstring.buffer;
	p_env->GetByteArrayRegion(t_bytes, 0, t_length, t_buffer);
    //	p_env->DeleteLocalRef(t_bytes);
	return t_lcxstring;
}

jobject lcxstring2jxstring(JNIEnv *p_env, ExternalString p_lcxstring) {
	jsize t_length = p_lcxstring.length;
	jbyteArray t_bytes = p_env->NewByteArray(t_length);
	jbyte *t_buffer = (jbyte *)p_lcxstring.buffer;
	if (t_bytes != NULL) {
		p_env->SetByteArrayRegion(t_bytes, 0, t_length, t_buffer);
		jobject t_jxstring = p_env->NewObject(s_rt_externalstring_class, s_rt_externalstring_init_MID, t_bytes);
        //		p_env->DeleteLocalRef(t_bytes);
		return t_jxstring;
	}
	return NULL;
}

Bool check4exception(JNIEnv *p_env) {
	// an exception may have occured - if so throw it as a C++ exception
	jthrowable t_env_exception = p_env->ExceptionOccurred();
	if (t_env_exception != NULL) {
		p_env->ExceptionDescribe();
		p_env->ExceptionClear();
		jstring t_env_error = (jstring) p_env->CallStaticObjectMethod(s_eh_class, s_eh_getExceptionText_MID, t_env_exception);
		jthrowable t_eh_exception = p_env->ExceptionOccurred();
		if (t_eh_exception != NULL) {
			p_env->ExceptionClear();
			s_ext_exception = "qrtjvmerr: an internal exception occurred (no error message available)";
			return False;
		}
		s_ext_exception = "qrtjvmerr: an exception occurred (use qrtJVM_ExceptionText to examine the stacktrace)";
		s_jvm_exception = jstring2cstring(p_env, t_env_error);
		return False;
	} else {
	    s_jvm_exception = "";
		s_ext_exception = "";
		return True;
	}
}

//-----------------------------------------------------------------------------
// PART 1: Callback function implementations as glue between JVM and LiveCode
//-----------------------------------------------------------------------------

jint JNICALL externalSendCardMessage_impl(JNIEnv *p_env, jobject p_self, jstring p_message) {
	char *t_message = jstring2cstring(p_env, p_message);
	int t_success;
	SendCardMessage(t_message, &t_success);
	// cleanup and return to JVM
	free(t_message);
	return t_success;
}

jstring JNICALL externalEvalExpr_impl(JNIEnv *p_env, jobject p_self, jstring p_expression) {
	jstring t_result = NULL;
	char *t_expression = jstring2cstring(p_env, p_expression);
	int t_success;
	char *t_value = EvalExpr(t_expression, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_expression);
	free(t_value);
	return t_result;
}

jstring JNICALL externalGetGlobal_impl(JNIEnv *p_env, jobject p_self, jstring p_name) {
	jstring t_result = NULL;
	char *t_name = jstring2cstring(p_env, p_name);
	int t_success;
	char *t_value = GetGlobal(t_name, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_name);
	free(t_value);
	return t_result;
}
jint JNICALL externalSetGlobal_impl(JNIEnv *p_env, jobject p_self, jstring p_name, jstring p_value) {
	char *t_name = jstring2cstring(p_env, p_name);
	char *t_value = jstring2cstring(p_env, p_value);
	int t_success;
	SetGlobal(t_name, t_value, &t_success);
	// cleanup and return to JVM
	free(t_name);
	free(t_value);
	return t_success;
}

jstring JNICALL externalGetVariable_impl(JNIEnv *p_env, jobject p_self, jstring p_name) {
	jstring t_result = NULL;
	char *t_name = jstring2cstring(p_env, p_name);
	int t_success;
	char *t_value = GetVariable(t_name, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_name);
	free(t_value);
	return t_result;
}
jint JNICALL externalSetVariable_impl(JNIEnv *p_env, jobject p_self, jstring p_name, jstring p_value) {
	char *t_name = jstring2cstring(p_env, p_name);
	char *t_value = jstring2cstring(p_env, p_value);
	int t_success;
	SetVariable(t_name, t_value, &t_success);
	// cleanup and return to JVM
	free(t_name);
	free(t_value);
	return t_success;
}

jobject JNICALL externalGetVariableEx_impl(JNIEnv *p_env, jobject p_self, jstring p_name, jstring p_key) {
	jobject t_result = NULL;
	char *t_name = jstring2cstring(p_env, p_name);
	char *t_key = jstring2cstring(p_env, p_key);
	ExternalString t_value;
	int t_success;
	GetVariableEx(t_name, t_key, &t_value, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = lcxstring2jxstring(p_env, t_value);
	}
	// cleanup and return to JVM
	free(t_name);
	free(t_key);
	return t_result;
}
jint JNICALL externalSetVariableEx_impl(JNIEnv *p_env, jobject p_self, jstring p_name, jstring p_key, jobject p_value) {
	char *t_name = jstring2cstring(p_env, p_name);
	char *t_key = jstring2cstring(p_env, p_key);
	ExternalString t_value;
	t_value = jxstring2lcxstring(p_env,p_value);
	int t_success;
	SetVariableEx(t_name, t_key, &t_value, &t_success);
	// cleanup and return to JVM
	free(t_name);
	free(t_key);
    //	delete t_value;
	return t_success;
}

jobject JNICALL externalGetArray_impl(JNIEnv *p_env, jobject p_self, jstring p_name) {
	char *t_name = jstring2cstring(p_env, p_name);
	int t_success = 0;
	jobject t_map_object = p_env->NewObject(s_rt_hashmap_class, s_rt_hashmap_init_MID);
	if (t_map_object != NULL) {
		int t_element_count = 0;
		GetArray(t_name, &t_element_count, NULL, NULL, &t_success);
		if (t_success == EXTERNAL_SUCCESS) {
			ExternalString *t_lc_values;
			t_lc_values = new ExternalString[t_element_count];
			char **t_lc_keys;
			t_lc_keys = new char *[t_element_count];
			GetArray(t_name, &t_element_count, t_lc_values, t_lc_keys, &t_success);
			if (t_success == EXTERNAL_SUCCESS) {
				for (int t_index = 0; t_index < t_element_count; t_index++) {
					ExternalString t_lc_value = t_lc_values[t_index];
					char *t_lc_key = t_lc_keys[t_index];
					jobject t_jni_value = lcxstring2jxstring(p_env, t_lc_value);
					jstring t_jni_key = p_env->NewStringUTF(t_lc_key);
					jobject t_ignored = p_env->CallObjectMethod(t_map_object, s_rt_hashmap_put_MID, t_jni_key, t_jni_value);
					// cleanup jni references
					p_env->DeleteLocalRef(t_jni_value);
					p_env->DeleteLocalRef(t_jni_key);
					p_env->DeleteLocalRef(t_ignored);
				}
			}
			// cleanup lc array copies
			delete[] t_lc_values;
			delete[] t_lc_keys;
		}
	}
	// cleanup and return to JVM
    //	p_env->DeleteLocalRef(t_map_object);
	free(t_name);
	return t_map_object;
}
jint JNICALL externalSetArray_impl(JNIEnv *p_env, jobject p_self, jstring p_name, jint p_count, jobjectArray p_values, jobjectArray p_keys) {
	char *t_name = jstring2cstring(p_env, p_name);
	int t_element_count = (int) p_count;
	ExternalString *t_lc_values;
	t_lc_values = new ExternalString[t_element_count];
	char **t_lc_keys;
	t_lc_keys = new char *[t_element_count];
	for (int t_index = 0; t_index < t_element_count; t_index++) {
		jobject t_jni_value = p_env->GetObjectArrayElement(p_values, t_index);
		ExternalString t_lc_value;
		t_lc_value = jxstring2lcxstring(p_env, t_jni_value);
		t_lc_values[t_index] = t_lc_value;
		jstring t_jni_key = (jstring)p_env->GetObjectArrayElement(p_keys, t_index);
		t_lc_keys[t_index] = jstring2cstring(p_env, t_jni_key);
		// cleanup jni references
		p_env->DeleteLocalRef(t_jni_value);
		p_env->DeleteLocalRef(t_jni_key);
	}
	int t_success;
	SetArray(t_name, t_element_count, t_lc_values, t_lc_keys, &t_success);
	// cleanup and return to JVM
	free(t_name);
	delete[] t_lc_values;
	delete[] t_lc_keys;
	return t_success;
}

jstring JNICALL externalGetFieldByName_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jstring p_name) {
	jstring t_result = NULL;
	char *t_group = jstring2cstring(p_env, p_group);
	char *t_name = jstring2cstring(p_env, p_name);
	int t_success;
	char *t_value = GetFieldByName(t_group, t_name, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_group);
	free(t_name);
	return t_result;
}
jstring JNICALL externalGetFieldByNum_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jint p_index) {
	jstring t_result = NULL;
	char *t_group = jstring2cstring(p_env, p_group);
	int t_index = p_index;
	int t_success;
	char *t_value = GetFieldByNum(t_group, t_index, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_group);
	return t_result;
}
jstring JNICALL externalGetFieldById_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jlong p_id) {
	jstring t_result = NULL;
	char *t_group = jstring2cstring(p_env, p_group);
	unsigned long t_id = (unsigned long) p_id;
	int t_success;
	char *t_value = GetFieldById(t_group, t_id, &t_success);
	if (t_success == EXTERNAL_SUCCESS) {
		t_result = p_env->NewStringUTF(t_value);
	}
	// cleanup and return to JVM
	free(t_group);
	return t_result;
}

jint JNICALL externalSetFieldByName_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jstring p_name, jstring p_value) {
	char *t_group = jstring2cstring(p_env, p_group);
	char *t_name = jstring2cstring(p_env, p_name);
	char *t_value = jstring2cstring(p_env, p_value);
	int t_success;
	SetFieldByName(t_group, t_name, t_value, &t_success);
	// cleanup and return to JVM
	free(t_group);
	free(t_name);
	free(t_value);
	return t_success;
}
jint JNICALL externalSetFieldByNum_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jint p_index, jstring p_value) {
	char *t_group = jstring2cstring(p_env, p_group);
	int t_index = p_index;
	char *t_value = jstring2cstring(p_env, p_value);
	int t_success;
	SetFieldByNum(t_group, t_index, t_value, &t_success);
	// cleanup and return to JVM
	free(t_group);
	free(t_value);
	return t_success;
}
jint JNICALL externalSetFieldById_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jlong p_id, jstring p_value) {
	char *t_group = jstring2cstring(p_env, p_group);
	unsigned long t_id = (unsigned long) p_id;
	char *t_value = jstring2cstring(p_env, p_value);
	int t_success;
	SetFieldById(t_group, t_id, t_value, &t_success);
	// cleanup and return to JVM
	free(t_group);
	free(t_value);
	return t_success;
}

jint JNICALL externalShowImageByName_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jstring p_name) {
	char *t_group = jstring2cstring(p_env, p_group);
	char *t_name = jstring2cstring(p_env, p_name);
	int t_success;
	ShowImageByName(t_group, t_name, &t_success);
	// cleanup and return to JVM
	free(t_group);
	free(t_name);
	return t_success;
}
jint JNICALL externalShowImageByNum_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jint p_index) {
	char *t_group = jstring2cstring(p_env, p_group);
	int t_index = p_index;
	int t_success;
	ShowImageByNum(t_group, t_index, &t_success);
	// cleanup and return to JVM
	free(t_group);
	return t_success;
}
jint JNICALL externalShowImageById_impl(JNIEnv *p_env, jobject p_self, jstring p_group, jlong p_id) {
	char *t_group = jstring2cstring(p_env, p_group);
	unsigned long t_id = (unsigned long) p_id;
	int t_success;
	ShowImageById(t_group, t_id, &t_success);
	// cleanup and return to JVM
	free(t_group);
	return t_success;
}

//-----------------------------------------------------------------------------
// PART 2: Extracted runtime miscellanea and external host - setup and teardown
//-----------------------------------------------------------------------------

Bool loadruntimemisc(JNIEnv *p_env) {
	s_rt_externalstring_class = p_env->FindClass("Lcom/quartam/external/ExternalString;");
	if (s_rt_externalstring_class == NULL) {
		s_ext_exception = "qrtjvmerr: could not find class 'com.quartam.external.ExternalString'";
		return False;
	}
	s_rt_externalstring_init_MID = p_env->GetMethodID(s_rt_externalstring_class, "<init>", "([B)V");
	if (s_rt_externalstring_init_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalString class (missing byte[] constructor)";
		return False;
	}
	s_rt_externalstring_getbytes_MID = p_env->GetMethodID(s_rt_externalstring_class, "getBytes", "()[B");
	if (s_rt_externalstring_getbytes_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalString class (missing getBytes method)";
		return False;
	}
	s_rt_externalstring_getlength_MID = p_env->GetMethodID(s_rt_externalstring_class, "getLength", "()I");
	if (s_rt_externalstring_getlength_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalString class (missing getLength method)";
		return False;
	}
	s_rt_string_class = p_env->FindClass("Ljava/lang/String;");
	s_rt_hashmap_class = p_env->FindClass("Ljava/util/HashMap;");
	s_rt_hashmap_init_MID = p_env->GetMethodID(s_rt_hashmap_class, "<init>", "()V");
	s_rt_hashmap_put_MID = p_env->GetMethodID(s_rt_hashmap_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	return True;
}

void unloadruntimemisc(JNIEnv *p_env) {
	p_env->DeleteGlobalRef(s_rt_externalstring_class);
	p_env->DeleteGlobalRef(s_rt_string_class);
	p_env->DeleteGlobalRef(s_rt_hashmap_class);
}

Bool loadexternalhost(JNIEnv *p_env) {
	char *classname = "com/quartam/internal/ExternalHost";
	s_eh_class = p_env->FindClass(classname);
	if (s_eh_class == NULL) {
		s_ext_exception = "qrtjvmerr: could not find class 'com.quartam.internal.ExternalHost'";
		return False;
	}
	// Next load our ExternalHost Method IDs
	unsigned int t_signature_length = strlen(classname) + 3 + 1 + 1;
	char *t_signature;
	t_signature = (char *)malloc(t_signature_length);
	sprintf(t_signature, "%s%s%s", "()L", classname, ";");
	s_eh_getInstance_MID = p_env->GetStaticMethodID(s_eh_class, "getInstance", t_signature);
	if (s_eh_getInstance_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing static getInstance method)";
		return False;
	}
	s_eh_getExceptionText_MID = p_env->GetStaticMethodID(s_eh_class, "getExceptionText", "(Ljava/lang/Throwable;)Ljava/lang/String;");
	if (s_eh_getExceptionText_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing static getExceptionText method)";
		return False;
	}
	s_eh_loadExternalLibrary_MID = p_env->GetMethodID(s_eh_class, "loadExternalLibrary", "(Ljava/lang/String;)V");
	if (s_eh_loadExternalLibrary_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing loadExternalLibrary method)";
		return False;
	}
	s_eh_unloadExternalLibrary_MID = p_env->GetMethodID(s_eh_class, "unloadExternalLibrary", "(Ljava/lang/String;)V");
	if (s_eh_unloadExternalLibrary_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing unloadExternalLibrary method)";
		return False;
	}
	s_eh_getExternalLibraries_MID = p_env->GetMethodID(s_eh_class, "getExternalLibraries", "()Ljava/lang/String;");
	if (s_eh_getExternalLibraries_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing getExternalLibraries method)";
		return False;
	}
	s_eh_getExternalPackages_MID = p_env->GetMethodID(s_eh_class, "getExternalPackages", "()Ljava/lang/String;");
	if (s_eh_getExternalPackages_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing getExternalPackages method)";
		return False;
	}
	s_eh_getExternalCommands_MID = p_env->GetMethodID(s_eh_class, "getExternalCommands", "()Ljava/lang/String;");
	if (s_eh_getExternalCommands_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing getExternalCommands method)";
		return False;
	}
	s_eh_getExternalFunctions_MID = p_env->GetMethodID(s_eh_class, "getExternalFunctions", "()Ljava/lang/String;");
	if (s_eh_getExternalFunctions_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing getExternalFunctions method)";
		return False;
	}
	s_eh_callExternalCommand_MID = p_env->GetMethodID(s_eh_class, "callExternalCommand", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;");
	if (s_eh_callExternalCommand_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing callExternalCommand method)";
		return False;
	}
	s_eh_callExternalFunction_MID = p_env->GetMethodID(s_eh_class, "callExternalFunction", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;");
	if (s_eh_callExternalCommand_MID == NULL) {
		s_ext_exception = "qrtjvmerr: invalid ExternalHost class (missing callExternalFunction method)";
		return False;
	}
	
	// Next register our native glue functions
	JNINativeMethod externalMethods[] = {
		{ "externalSendCardMessage", "(Ljava/lang/String;)I", (void *)&externalSendCardMessage_impl },
		{ "externalEvalExpr", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&externalEvalExpr_impl },
		{ "externalGetGlobal", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&externalGetGlobal_impl },
		{ "externalSetGlobal", "(Ljava/lang/String;Ljava/lang/String;)I", (void *)&externalSetGlobal_impl },
		{ "externalGetVariable", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&externalGetVariable_impl },
		{ "externalSetVariable", "(Ljava/lang/String;Ljava/lang/String;)I", (void *)&externalSetVariable_impl },
		{ "externalGetVariableEx", "(Ljava/lang/String;Ljava/lang/String;)Lcom/quartam/external/ExternalString;", (void *)&externalGetVariableEx_impl },
		{ "externalSetVariableEx", "(Ljava/lang/String;Ljava/lang/String;Lcom/quartam/external/ExternalString;)I", (void *)&externalSetVariableEx_impl },
		{ "externalGetArray", "(Ljava/lang/String;)Ljava/util/Map;", (void *)&externalGetArray_impl },
		{ "externalSetArray", "(Ljava/lang/String;I[Lcom/quartam/external/ExternalString;[Ljava/lang/String;)I", (void *)&externalSetArray_impl },
		{ "externalGetFieldByName", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void *)&externalGetFieldByName_impl },
		{ "externalGetFieldByNum", "(Ljava/lang/String;I)Ljava/lang/String;", (void *)&externalGetFieldByNum_impl },
		{ "externalGetFieldById", "(Ljava/lang/String;J)Ljava/lang/String;", (void *)&externalGetFieldById_impl },
		{ "externalSetFieldByName", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", (void *)&externalSetFieldByName_impl },
		{ "externalSetFieldByNum", "(Ljava/lang/String;ILjava/lang/String;)I", (void *)&externalSetFieldByNum_impl },
		{ "externalSetFieldById", "(Ljava/lang/String;JLjava/lang/String;)I", (void *)&externalSetFieldById_impl },
		{ "externalShowImageByName", "(Ljava/lang/String;Ljava/lang/String;)I", (void *)&externalShowImageByName_impl },
		{ "externalShowImageByNum", "(Ljava/lang/String;I)I", (void *)&externalShowImageByNum_impl },
		{ "externalShowImageById", "(Ljava/lang/String;J)I", (void *)&externalShowImageById_impl },
	};
	p_env->RegisterNatives(s_eh_class, externalMethods, 19);
    
	// Finally load our ExternalHost Instance
	s_eh_object = p_env->CallStaticObjectMethod(s_eh_class, s_eh_getInstance_MID);
	if (s_eh_object == NULL) {
		s_ext_exception = "qrtjvmerr: could not create ExternalHost instance object";
		return False;
	}
	s_eh_object = p_env->NewGlobalRef(s_eh_object);
	if (s_eh_object == NULL) {
		s_ext_exception = "qrtjvmerr: could not create ExternalHost global object reference";
		return False;
	}

	// If we got here it means there were no errors
	return True;
}

void unloadexternalhost(JNIEnv *p_env) {
	p_env->DeleteGlobalRef(s_eh_object);
	p_env->UnregisterNatives(s_eh_class);
	p_env->DeleteGlobalRef(s_eh_class);
}

//-----------------------------------------------------------------------------
// PART 3: External function implementations as glue between LiveCode and JVM
//-----------------------------------------------------------------------------

// Function:
//   qrtJVM_LoadJvm(pClasspath[, pRuntimeLibPath])
// Parameters:
//   pClasspath - string containing the java classpath
//   pRuntimeLibPath - string containing the path to the runtime library
// Result:
//   n/a
//
void qrtJVM_LoadJvm(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// Check to see if we have the right number of parameters.
	//
	if (p_argument_count < 1 || p_argument_count > 2)
	{
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: illegal number of parameters (expected <class path>[,<runtime path>])");
		return;
	}
    
	// Make sure the JVM is only loaded once.
	//
	if (s_jvm_status != 0)
	{
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: library is not in status 'initial'");
		return;
	}
    
	// Assemble the java classpath
	//
	unsigned int t_classpath_length = strlen(p_arguments[0]) + 18 + 1;
	char *jvm_classpath;
	jvm_classpath = (char *)malloc(t_classpath_length);
	sprintf(jvm_classpath, "%s%s", "-Djava.class.path=", p_arguments[0]);
    
    
	// Create the VM arguments structure
	//
	JavaVMInitArgs vm_args;
	vm_args.nOptions = 2;
	JavaVMOption options[2];
	options[0].optionString = jvm_classpath;
	options[1].optionString = "-Djava.awt.headless=true";
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_FALSE;
	vm_args.version = JNI_VERSION_1_4;

	// Load the JVM as prescribed by the target platform
	//
	JNIEnv * t_env;

	#if defined(TARGET_PLATFORM_MACOS_X)

		CFStringRef targetJVM = CFSTR("1.5");
		CFBundleRef JavaVMBundle;
		CFURLRef    JavaVMBundleURL;
		CFURLRef    JavaVMBundlerVersionsDirURL;
		CFURLRef    TargetJavaVM;
		UInt8 pathToTargetJVM [PATH_MAX] = "\0";
		struct stat sbuf;
		
		
		// Look for the JavaVM bundle using its identifier
		JavaVMBundle = CFBundleGetBundleWithIdentifier(CFSTR("com.apple.JavaVM") );
		
		if(JavaVMBundle != NULL) {
			// Get a path for the JavaVM bundle
			JavaVMBundleURL = CFBundleCopyBundleURL(JavaVMBundle);
			CFRelease(JavaVMBundle);
			
			if(JavaVMBundleURL != NULL) {
				// Append to the path the Versions Component
				JavaVMBundlerVersionsDirURL = CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,JavaVMBundleURL,CFSTR("Versions"),true);
				CFRelease(JavaVMBundleURL);
				
				if(JavaVMBundlerVersionsDirURL != NULL) {
					// Append to the path the target JVM's Version
					TargetJavaVM = CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,JavaVMBundlerVersionsDirURL,targetJVM,true);
					CFRelease(JavaVMBundlerVersionsDirURL);
					
					if(TargetJavaVM != NULL) {
						if(CFURLGetFileSystemRepresentation (TargetJavaVM,true,pathToTargetJVM,PATH_MAX )) {
							// Check to see if the directory, or a sym link for the target JVM directory exists, and if so set the
							// environment variable JAVA_JVM_VERSION to the target JVM.
							if(stat((char*)pathToTargetJVM,&sbuf) == 0) {
								// Ok, the directory exists, so now we need to set the environment var JAVA_JVM_VERSION to the CFSTR targetJVM
								// We can reuse the pathToTargetJVM buffer to set the environement var.
								if(CFStringGetCString(targetJVM,(char*)pathToTargetJVM,PATH_MAX,kCFStringEncodingUTF8))
									setenv("JAVA_JVM_VERSION", (char*)pathToTargetJVM,1);
							}
						}
						CFRelease(TargetJavaVM);
					}
				}
			}
		}
    
		// Now create the Java VM with our arguments
		//
		jint jvm_result = -1;
		jvm_result = JNI_CreateJavaVM(&s_rt_jvm, (void**)&t_env, &vm_args);
		if (jvm_result < 0) {
			*r_pass = False;
			*r_error = True;
			*r_result = strdup("qrtjvmerr: could not create Java VM");
			return;
		}

	#else

		// Read the runtime library path and attempt to load
		//
		char *jvm_rtlibpath = p_arguments[1];
		s_rtl_handle = DLOPEN(jvm_rtlibpath);
		if (s_rtl_handle == NULL) {
			*r_pass = False;
			*r_error = True;
			*r_result = strdup("qrtjvmerr: unable to load JVM library from <runtime path>");
			return;
		}

		// Fetch the function pointer for creating the VM
		typedef jint (JNICALL P_JNI_CreateJavaVM_t)(JavaVM **pvm, JNIEnv **penv, void *args);
		P_JNI_CreateJavaVM_t* pfnCreateJavaVM = NULL;
		pfnCreateJavaVM = (P_JNI_CreateJavaVM_t*) DLSYM(s_rtl_handle, "JNI_CreateJavaVM");
		if (pfnCreateJavaVM == NULL) {
			*r_pass = False;
			*r_error = True;
			*r_result = strdup("qrtjvmerr: invalid JVM library from <runtime path> (missing JNI_CreateJavaVM ProcAddress)");
			return;
		}

		// Now create the Java VM with our arguments
		//
		jint jvm_result = -1;
		jvm_result = pfnCreateJavaVM(&s_rt_jvm, &t_env, &vm_args);
		if (jvm_result < 0) {
			*r_pass = False;
			*r_error = True;
			*r_result = strdup("qrtjvmerr: could not create Java VM");
			return;
		}

	#endif

	s_jvm_status = 1;  // loaded but not initialized

	// Next load our runtime miscellanea and ExternalHost class
	//
	Bool t_success;
	t_success = loadruntimemisc(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
	t_success = loadexternalhost(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
	
	// Update our internal status
	//
	s_jvm_status = 2;  // fully loaded
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	*r_result = NULL;
}


// Function:
//   qrtJVM_UnloadJvm()
// Parameters:
//   none
// Result:
//   n/a
//
void qrtJVM_UnloadJvm(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	*r_result = NULL;
	switch (s_jvm_status) {
		case 0:
			*r_result = strdup("qrtjvmerr: JVM is not loaded");
			break;
		case 1:
			*r_result = strdup("qrtjvmerr: JVM is not initialized");
			break;
		case 3:
			*r_result = strdup("qrtjvmerr: JVM is not completely unloaded");
			break;
		case 4:
			*r_result = strdup("qrtjvmerr: JVM is already unloaded");
			break;
	}
	if (*r_result != NULL) {
		*r_pass = False;
		*r_error = True;
		return;
	}
    
	// Next check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: illegal number of parameters (expected none)");
		return;
	}
    
	s_jvm_status = 3;  // unloading

    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
	
	// Start by cleaning up global references
	//
	unloadexternalhost(t_env);
	unloadruntimemisc(t_env);
    
	// Now go ahead and destroy the JavaVM
	jint jvm_result = -1;
	jvm_result = s_rt_jvm->DestroyJavaVM();
	if (jvm_result < 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: could not destroy Java VM");
		return;
	}
    
	s_jvm_status = 4;  // fully unloaded
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	*r_result = NULL;
}


// Function:
//   qrtJVM_IsJvmLoaded()
// Parameters:
//   none
// Result:
//   a string containing true or false
//
void qrtJVM_IsJvmLoaded(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: illegal number of parameters (expected none)");
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	if (s_jvm_status == 2) {
		*r_result = strdup("true");
	} else {
		*r_result = strdup("false");
	}
}


// Function:
//   qrtJVM_JvmStatus()
// Parameters:
//   none
// Result:
//   a string containing the JavaVM status
//
void qrtJVM_JvmStatus(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: illegal number of parameters (expected none)");
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	switch (s_jvm_status) {
		case 0:
			*r_result = strdup("initial");
			break;
		case 1:
			*r_result = strdup("loading");
			break;
		case 2:
			*r_result = strdup("loaded");
			break;
		case 3:
			*r_result = strdup("unloading");
			break;
		case 4:
			*r_result = strdup("unloaded");
			break;
	}
}


// Function:
//   qrtJVM_ExceptionText()
// Parameters:
//   none
// Result:
//   a string containing the stacktrace of the exception
//
void qrtJVM_ExceptionText(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: illegal number of parameters (expected none)");
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	*r_result = strdup(s_jvm_exception);
}


// Function:
//   qrtJVM_LoadXlib()
// Parameters:
//   pXlibPath - the path to the external library .jar to load
// Result:
//   a string containing true or false
//
void qrtJVM_LoadXlib(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Next check we have been passed a single argument - if not return error
	//
	if (p_argument_count != 1) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected <library path>)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and try to load the external library into the jvm
	//
	jstring t_xlibpath = t_env->NewStringUTF(p_arguments[0]);
	t_env->CallVoidMethod(s_eh_object, s_eh_loadExternalLibrary_MID, t_xlibpath);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	*r_result = NULL;
}


// Function:
//   qrtJVM_UnloadXlib()
// Parameters:
//   pXlibPath - the path to the external library .jar to unload
// Result:
//   a string containing true or false
//
void qrtJVM_UnloadXlib(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// First check we have been passed a single argument - if not return error
	//
	if (p_argument_count != 1) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected <library path>)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and try to unload the external library from the jvm
	//
	jstring t_xlibpath = t_env->NewStringUTF(p_arguments[0]);
	t_env->CallVoidMethod(s_eh_object, s_eh_unloadExternalLibrary_MID, t_xlibpath);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
	*r_result = NULL;
}


// Function:
//   qrtJVM_LoadedXlibs()
// Parameters:
//   none
// Result:
//   a string containing the loaded external libraries
//
void qrtJVM_LoadedXlibs(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Nexst check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected none)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and call the getExternalLibraries method via JNI
	//
	jstring t_xlibs = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_getExternalLibraries_MID);
	*r_result = jstring2cstring(t_env, t_xlibs);
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_xlibs);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// Function:
//   qrtJVM_LoadedXpacks()
// Parameters:
//   none
// Result:
//   a string containing the loaded external packages
//
void qrtJVM_LoadedXpacks(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Nexst check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected none)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and call the getExternalPackages method via JNI
	//
	jstring t_xpacks = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_getExternalPackages_MID);
	*r_result = jstring2cstring(t_env, t_xpacks);
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_xpacks);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// Function:
//   qrtJVM_LoadedXcmds()
// Parameters:
//   none
// Result:
//   a string containing the loaded external commands
//
void qrtJVM_LoadedXcmds(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Nexst check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected none)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and call the getExternalCommands method via JNI
	//
	jstring t_xcmds = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_getExternalCommands_MID);
	*r_result = jstring2cstring(t_env, t_xcmds);
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_xcmds);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// Function:
//   qrtJVM_LoadedXfcns()
// Parameters:
//   none
// Result:
//   a string containing the loaded external functions
//
void qrtJVM_LoadedXfcns(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Nexst check we have been passed no arguments - if not return error
	//
	if (p_argument_count != 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected none)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);
    
	// Now go ahead and call the getExternalFunctions method via JNI
	//
	jstring t_xfcns = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_getExternalFunctions_MID);
	*r_result = jstring2cstring(t_env, t_xfcns);
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_xfcns);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// Function:
//   qrtJVM_CallXcmd()
// Parameters:
//   pCommand - the name of the external command
//   pParams... - variable number of arguments
// Result:
//   a string containing the result of the external command
//
void qrtJVM_CallXcmd(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Next check we have been passed a command name - if not return error
	//
	if (p_argument_count == 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected <command name> [,<param>]*)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);

	// Next convertt he command name to a jstring
	//
	jstring t_commandname = t_env->NewStringUTF(p_arguments[0]);
    
	// Next convert the arguments into a jstring array
	//
	jobjectArray t_arguments = t_env->NewObjectArray(p_argument_count - 1, s_rt_string_class, NULL);
	for(int t_index = 1; t_index < p_argument_count; t_index++) {
		jstring t_param = t_env->NewStringUTF(p_arguments[t_index]);
		t_env->SetObjectArrayElement(t_arguments, t_index - 1, t_param);
	}
    
	// Now invoke the callExternalCommand method of the ExternalHost
	//
	jstring t_call_result = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_callExternalCommand_MID, t_commandname, t_arguments);
	if (t_call_result != NULL) {
		*r_result = jstring2cstring(t_env, t_call_result);
	}
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_arguments);
	t_env->DeleteLocalRef(t_call_result);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// Function:
//   qrtJVM_CallXfcn()
// Parameters:
//   pFunction - the name of the external function
//   pParams... - variable number of arguments
// Result:
//   a string containing the result of the external function
//
void qrtJVM_CallXfcn(char *p_arguments[], int p_argument_count, char **r_result, Bool *r_pass, Bool *r_error)
{
	// First check if the JVM is loaded - if not return error
	//
	if (s_jvm_status != 2) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: JVM is not loaded");
		return;
	}
    
	// Next check if we have been passed a function name - if not return error
	//
	if (p_argument_count == 0) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup("qrtjvmerr: invalid number of parameters (expected <function name> [,<param>]*)");
		return;
	}
	
    // Fetch the right JNI environment for the current thread
	//
	JNIEnv * t_env;
	s_rt_jvm->AttachCurrentThread((void **)&t_env, NULL);

	// Next convert the function name to a jstring
	jstring t_functionname = t_env->NewStringUTF(p_arguments[0]);
    
	// Next convert the arguments into a jstring array
	//
	jobjectArray t_arguments = t_env->NewObjectArray(p_argument_count - 1, s_rt_string_class, NULL);
	for(int t_index = 1; t_index < p_argument_count; t_index++) {
		jstring t_param = t_env->NewStringUTF(p_arguments[t_index]);
		t_env->SetObjectArrayElement(t_arguments, t_index - 1, t_param);
	}
    
	// Now invoke the callExternalFunction method of the ExternalHost
	//
	jstring t_call_result = (jstring) t_env->CallObjectMethod(s_eh_object, s_eh_callExternalFunction_MID, t_functionname, t_arguments);
	if (t_call_result != NULL) {
		*r_result = jstring2cstring(t_env, t_call_result);
	}
    
	// Don't forget to cleanup our resources
	//
	t_env->DeleteLocalRef(t_arguments);
	t_env->DeleteLocalRef(t_call_result);
    
	// Finally check for jvm exception and rethrow to livecode if applicable
	//
	Bool t_success;
	t_success = check4exception(t_env);
	if (!t_success) {
		*r_pass = False;
		*r_error = True;
		*r_result = strdup(s_ext_exception);
		return;
	}
    
	// If we got to here it means there were no errors
	//
	*r_pass = False;
	*r_error = False;
}


// END USER DEFINITIONS
//
///////////////////////////////////////////////////////////////////////////////


EXTERNAL_BEGIN_DECLARATIONS("qrtjvm")

// BEGIN USER DECLARATIONS

EXTERNAL_DECLARE_COMMAND("qrtjvm_loadjvm", qrtJVM_LoadJvm)
EXTERNAL_DECLARE_COMMAND("qrtjvm_unloadjvm", qrtJVM_UnloadJvm)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_jvmisloaded", qrtJVM_IsJvmLoaded)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_jvmstatus", qrtJVM_JvmStatus)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_exceptiontext", qrtJVM_ExceptionText)
EXTERNAL_DECLARE_COMMAND("qrtjvm_loadexternallibrary", qrtJVM_LoadXlib)
EXTERNAL_DECLARE_COMMAND("qrtjvm_loadxlib", qrtJVM_LoadXlib)
EXTERNAL_DECLARE_COMMAND("qrtjvm_unloadexternallibrary", qrtJVM_UnloadXlib)
EXTERNAL_DECLARE_COMMAND("qrtjvm_unloadxlib", qrtJVM_UnloadXlib)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_externallibraries", qrtJVM_LoadedXlibs)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_xlibs", qrtJVM_LoadedXlibs)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_externalpackages", qrtJVM_LoadedXpacks)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_xpacks", qrtJVM_LoadedXpacks)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_externalcommands", qrtJVM_LoadedXcmds)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_xcmds", qrtJVM_LoadedXcmds)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_externalfunctions", qrtJVM_LoadedXfcns)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_xfcns", qrtJVM_LoadedXfcns)
EXTERNAL_DECLARE_COMMAND("qrtjvm_callexternalcommand", qrtJVM_CallXcmd)
EXTERNAL_DECLARE_COMMAND("qrtjvm_callxcmd", qrtJVM_CallXcmd)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_callexternalfunction", qrtJVM_CallXfcn)
EXTERNAL_DECLARE_FUNCTION("qrtjvm_callxfcn", qrtJVM_CallXfcn)

// END USER DECLARATIONS

EXTERNAL_END_DECLARATIONS
