/*	TOP-SURF: a visual words toolkit
	Copyright (C) 2010 LIACS Media Lab, Leiden University,
	                   Bart Thomee (bthomee@liacs.nl),
					   Erwin M. Bakker (erwin@liacs.nl)	and
					   Michael S. Lew (mlew@liacs.nl).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    See http://www.gnu.org/licenses/gpl.html for the full license.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

	In addition, this work is covered under the Creative Commons
	Attribution license version 3.
    See http://creativecommons.org/licenses/by/3.0/ for the full license.
*/

#ifndef _APIH
#define _APIH

#pragma once

// compiling as a dynamic/shared library
// Note: *nix-based systems export all symbols by default, whereas
//       we need to explicitly specify them on windows
#if defined WIN32 || defined _WIN32 || defined WIN64 || defined _WIN64
#define DLLAPI __declspec(dllexport)
#else
#define DLLAPI
#endif

#include "descriptor.h"
#include <stdio.h>

// initialize the wrapper
// imagedim = dimension to resize an image to
//            (suggested = 256x256)
// top      = top number of highest-scoring visual words to retain
//            (suggested = 100. passing a high value, e.g. INT_MAX, will result in
//            all visual words being returned)
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
bool DLLAPI TopSurf_Initialize(int imagedim, int top);

// terminate the wrapper
void DLLAPI TopSurf_Terminate();

// load a dictionary
// dictionarydir = directory in which the dictionary is located
//                 (the one containing the dictionary.xml file and its supporting data
//                 files, i.e. idf.dat, kdtree.dat and visualwords.dat)
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_Initialized must have been called in order to use this function.
bool DLLAPI TopSurf_LoadDictionary(const char *dictionarydir);

// save a dictionary
// dictionarydir = directory where the dictionary will be saved
//                 Ensure that no other dictionary files are stored in the same
//                 directory, or they will be overwritten
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_LoadDictionary or TopSurf_CreateDictionary must have been called
//       in order to use this function.
bool DLLAPI TopSurf_SaveDictionary(const char *dictionarydir);

// create a new dictionary from the provided images
// imagedir   = directory containing the images (jpg and png are supported)
//              All images in the subdirectories will be used as well
// clusters   = total number of clusters to create, i.e. visual words in the dictionary
//              (suggested = 200000)
// knn        = number of nearest neighbors to find of each data point while clustering
//              (suggested = 100)
// iterations = number of times to recluster to ensure stability of the clusters
//              (suggested = 1000)
// points     = number of points to randomly extract from each image
//              (suggested = 25, set this to a high value to extract all points from an
//              image)
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: during the creation occasional messages are printed to stdout to show the progress.
// Note: clustering can take a very long time and consume a lot of memory. please ensure
//       sufficient RAM is available. during our own clustering of 33.5 million points
//       (the maximum that is supported), which eventually required more than 25GB RAM,
//       we noticed that on a machine with only 8GB RAM available clustering did proceed
//       but took forever due to virtual memory swapping in and out. what took only 2 days
//       on a high-end blade server would have taken 6 months on this 8GB machine.
// Note: the dictionary will not yet be saved, but will remain in memory to be used straight
//       away. to save the dictionary, call TopSurf_SaveDictionary.
// Note: TopSurf_Initialized must have been called in order to use this function.
bool DLLAPI TopSurf_CreateDictionary(const char *imagedir, int clusters, int knn, int iterations, int points);

// extract the descriptor of an image
// fname  = path to image file
// pixels = RGB pixels of image, seen from top-left to bottom-right. note that the array
//          should be preallocated to hold dimx*dimy*3 elements
// dimx   = horizontal dimension of the image
// dimy   = vertical dimension of the image
// td     = top surf descriptor, which will contain the detected visual words in the image
//          sorted by their identifiers
// data   = unallocated array that will hold a converted descriptor, e.g. useful for easy
//          transmission across the internet or across process/dll boundaries
// length = length in bytes of the data array once filled with the descriptor
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_Initialized must have been called in order to use this function.
// Note: The data array will contain the following elements (see also descriptor.h):
//          count        = number of visual words in the descriptor
//          length       = vector length of the descriptor (only written if count > 0)
//          visualwords  = the visual words (only written if count > 0)
bool DLLAPI TopSurf_ExtractDescriptor(const char *fname, TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_ExtractDescriptor(const char *fname, unsigned char *&data, int &length);
bool DLLAPI TopSurf_ExtractDescriptor(const unsigned char *pixels, int dimx, int dimy, TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_ExtractDescriptor(const unsigned char *pixels, int dimx, int dimy, unsigned char *&data, int &length);

// choice of similarity measure for comparing descriptors
enum TOPSURF_SIMILARITY
{
	TOPSURF_COSINE,  // cosine normalized difference between the tf-idf scores of the visual words
	TOPSURF_ABSOLUTE // absolute difference between the td-idf scores of the visual words
};
// compare two descriptors
// td1, td2     = the descriptors to compare
// data1, data2 = the descriptors to compare in form of a byte array.
//                note that we don't need to specify their lengths as we can infer
//                it from the individual elements inside the array
// similarity   = the chosen similarity mode, see above
// returns the distance between the two descriptors.
// Note: TopSurf_Initialized does not have to be called in order to use this function.
float DLLAPI TopSurf_CompareDescriptors(const TOPSURF_DESCRIPTOR &td1, const TOPSURF_DESCRIPTOR &td2, TOPSURF_SIMILARITY similarity);
float DLLAPI TopSurf_CompareDescriptors(const unsigned char *data1, const unsigned char *data2, TOPSURF_SIMILARITY similarity);

// load a descriptor from disk
// fname  = path to descriptor file
// file   = already opened descriptor file
// td     = an empty descriptor that will be filled with the loaded descriptor
// data   = unallocated array that will hold the loaded descriptor
// length = length in bytes of the data array once filled with the descriptor
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_Initialized does not have to be called in order to use this function.
bool DLLAPI TopSurf_LoadDescriptor(const char *fname, TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_LoadDescriptor(const char *fname, unsigned char *&data, int &length);
bool DLLAPI TopSurf_LoadDescriptor(FILE *file, TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_LoadDescriptor(FILE *file, unsigned char *&data, int &length);

// save a descriptor to disk
// fname    = path to descriptor file
// file     = already opened descriptor file
// td, data = a descriptor that has previously been returned by TopSurf_ExtractDescriptor
//            or filled by TopSurf_LoadDescriptor
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_Initialized does not have to be called in order to use this function.
bool DLLAPI TopSurf_SaveDescriptor(const char *fname, const TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_SaveDescriptor(const char *fname, const unsigned char *data);
bool DLLAPI TopSurf_SaveDescriptor(FILE *file, const TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_SaveDescriptor(FILE *file, const unsigned char *data);

// visualize a descriptor
// pixels   = RGB pixels of image, seen from top-left to bottom-right
// dimx     = horizontal dimension of the image
// dimy     = vertical dimension of the image
// td, data = a descriptor that has previously been returned by TopSurf_ExtractDescriptor
//            or filled by TopSurf_LoadDescriptor
// returns true for success and false for failure. in case of failure a message is
// printed to stderr.
// Note: TopSurf_Initialized does not have to be called in order to use this function.
bool DLLAPI TopSurf_VisualizeDescriptor(unsigned char *pixels, int dimx, int dimy, TOPSURF_DESCRIPTOR &td);
bool DLLAPI TopSurf_VisualizeDescriptor(unsigned char *pixels, int dimx, int dimy, const unsigned char *data);

// release the memory used by a descriptor
// td, data = a descriptor that has previously been returned by TopSurf_ExtractDescriptor
//            or filled by TopSurf_LoadDescriptor
// Note: TopSurf_Initialized does not have to be called in order to use this function.
void DLLAPI TopSurf_ReleaseDescriptor(TOPSURF_DESCRIPTOR &td);
void DLLAPI TopSurf_ReleaseDescriptor(unsigned char *data);

#endif