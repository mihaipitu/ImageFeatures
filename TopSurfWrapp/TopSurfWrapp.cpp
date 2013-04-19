// TopSurfWrapp.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include "Surf_TopSurfWrapp.h"
#include <jni.h>
#include <iostream>

using namespace std;
#define _WIN64
// import the topsurf library
#include "api.h"
#if defined WIN32 || defined _WIN32 || defined WIN64 || defined _WIN64
	#ifdef _WIN64
		#ifdef _DEBUG
			#pragma comment(lib, "topsurf_x64d.lib")
		#else
			#pragma comment(lib, "topsurf_x64.lib")
		#endif
	#else
		#ifdef _DEBUG
			#pragma comment(lib, "topsurf_x32d.lib")
		#else
			#pragma comment(lib, "topsurf_x32.lib")
		#endif
	#endif
#endif



JNIEXPORT void JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_load(JNIEnv *env, jclass c, jstring dictionary, jint imagedim, jint top)
{
	if (!TopSurf_Initialize(imagedim, top))
	{
		printf("Error initializing TopSurf\n");
		//throw some error
	}
	const char *p = env->GetStringUTFChars(dictionary, false);
	if (!TopSurf_LoadDictionary(p))
	{
		printf("Error loading TopSurf dictionary\n");
		TopSurf_Terminate();
		//throw some error
	}
	
	env->ReleaseStringUTFChars(dictionary, p);
}

JNIEXPORT void JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_unload(JNIEnv *env, jclass c)
{
	TopSurf_Terminate();
}

unsigned char* getPixels(JNIEnv *env, jbyteArray pixels)
{
	jbyte* p = env->GetByteArrayElements(pixels, false);
	int l = env->GetArrayLength(pixels);
	unsigned char* charPixels = new unsigned char[l];
	for (int i = 0; i < l; i++)
		charPixels[i] = p[i];
	
	return charPixels;
}

JNIEXPORT jbyteArray JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_computeSurf(JNIEnv *env, jclass c, jbyteArray pixels, jint width, jint height)
{
	
	unsigned char *data;
	int length;
	unsigned char* charPixels = getPixels(env, pixels);

	if (!TopSurf_ExtractDescriptor(charPixels, width, height, data, length))
	{
		TopSurf_Terminate();
		return NULL;
	}

	jbyte* buf = new jbyte[length];
	for (int i = 0; i < length; i++)
	{
		buf[i] = data[i];
	}

	jbyteArray ret = env->NewByteArray(length);
	env->SetByteArrayRegion(ret, 0, length, buf);
	
	//delete[] data;
	return ret;
}

JNIEXPORT jobject JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_extractSurfDescriptor
  (JNIEnv *env, jclass c, jbyteArray pixels, jint width, jint height, jobject initVw, jobject initDs)
{
	unsigned char* charPixels = getPixels(env, pixels);
	TOPSURF_DESCRIPTOR td;
	
	if (!TopSurf_ExtractDescriptor(charPixels, width, height, td))
	{
		TopSurf_Terminate();
		printf("Unable to compute descriptor\n");
		return NULL;
	}

	jclass classVisualword = env->GetObjectClass(initVw);
	jclass classDescriptor = env->GetObjectClass(initDs);
	jmethodID descriptorConstructor = env->GetMethodID(classDescriptor, "<init>", "([LspeededUpRobustFeatures/TopSurfVisualword;F)V");
	jmethodID visualWordConstructor = env->GetMethodID(classVisualword, "<init>", "(IFFIFFFF)V");

	jobjectArray visualwords = (jobjectArray) env->NewObjectArray(td.count, classVisualword, NULL);
	//public TopSurfVisualword(int id, float tf, float idf, int count, float x, float y, float orientation, float scale) {
	
	for (int i = 0; i < td.count; i++)
	{
		jobject element = env->NewObject(classVisualword, visualWordConstructor, td.visualword[i].identifier, td.visualword[i].tf, td.visualword[i].idf, td.visualword[i].count, td.visualword[i].location->x, td.visualword[i].location->y, td.visualword[i].location->orientation, td.visualword[i].location->scale);
		env->SetObjectArrayElement(visualwords, i, element);
	}

	jobject descriptor = env->NewObject(classDescriptor, descriptorConstructor, visualwords, td.length);

	TopSurf_ReleaseDescriptor(td);
	env->ReleaseByteArrayElements(pixels, (jbyte*) charPixels, 0);
	
	return descriptor;
}


JNIEXPORT jobject JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_extractSurfDescriptorFile
  (JNIEnv *env, jclass c, jstring imagePath, jobject initVw, jobject initDs)
{
	TOPSURF_DESCRIPTOR td;
	const char *p = env->GetStringUTFChars(imagePath, false);
	if (!TopSurf_ExtractDescriptor(p, td))
	{
		TopSurf_Terminate();
		printf("Unable to compute descriptor\n");
		return NULL;
	}

	jclass classVisualword = env->GetObjectClass(initVw);
	jclass classDescriptor = env->GetObjectClass(initDs);
	jmethodID descriptorConstructor = env->GetMethodID(classDescriptor, "<init>", "([LspeededUpRobustFeatures/TopSurfVisualword;F)V");
	jmethodID visualWordConstructor = env->GetMethodID(classVisualword, "<init>", "(IFFIFFFF)V");

	jobjectArray visualwords = (jobjectArray) env->NewObjectArray(td.count, classVisualword, NULL);
	//public TopSurfVisualword(int id, float tf, float idf, int count, float x, float y, float orientation, float scale) {
	for (int i = 0; i < td.count; i++)
	{
		jobject element = env->NewObject(classVisualword, visualWordConstructor, td.visualword[i].identifier, td.visualword[i].tf, td.visualword[i].idf, td.visualword[i].count, td.visualword[i].location->x, td.visualword[i].location->y, td.visualword[i].location->orientation, td.visualword[i].location->scale);
		env->SetObjectArrayElement(visualwords, i, element);
	}

	jobject descriptor = env->NewObject(classDescriptor, descriptorConstructor, visualwords, td.length);

	TopSurf_ReleaseDescriptor(td);
	env->ReleaseStringUTFChars(imagePath, p);
	
	return descriptor;
}

JNIEXPORT jbyteArray JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_visualizeDescriptor
  (JNIEnv *env, jclass c, jbyteArray pixels, jint dimx, jint dimy, jstring imagePath)
{
	TOPSURF_DESCRIPTOR td;
	const char *p = env->GetStringUTFChars(imagePath, false);
	if (!TopSurf_ExtractDescriptor(p, td))
	{
		TopSurf_Terminate();
		printf("Unable to compute descriptor\n");
		return NULL;
	}

	int length = env->GetArrayLength(pixels);
	unsigned char* uPixels = getPixels(env, pixels);

	if (!TopSurf_VisualizeDescriptor(uPixels, dimx, dimy, td))
	{
		printf("Unable to visualize descriptor\n");
		return NULL;
	}

	TopSurf_ReleaseDescriptor(td);
	env->ReleaseStringUTFChars(imagePath, p);

	jbyte* buf = new jbyte[length];
	for (int i = 0; i < length; i++)
	{
		buf[i] = uPixels[i];
	}

	jbyteArray ret = env->NewByteArray(length);
	env->SetByteArrayRegion(ret, 0, length, buf);

	return ret;
}

JNIEXPORT jfloat JNICALL Java_speededUpRobustFeatures_TopSurfAlgorithm_compareDescriptors
  (JNIEnv *env, jclass c, jobject td1, jobject td2, jobject initVw)
{
	//TOPSURF_DESCRIPTOR nativeTd1;
	//TOPSURF_DESCRIPTOR nativeTd2;

	//jclass tdClass = env->GetObjectClass(td1);
	//jmethodID getVisualWordsMethod = env->GetMethodID(tdClass, "getVisualwords", "()[LspeededUpRobustFeatures/TopSurfVisualword;");
	//jobjectArray visualWords1 = env->NewObjectArray(
	return 0;
}