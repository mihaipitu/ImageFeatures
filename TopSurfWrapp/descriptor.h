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

#ifndef _DESCRIPTORH
#define _DESCRIPTORH

#pragma once

#include <stdlib.h>

// structure describing the location of a single visual word element
struct TOPSURF_LOCATION
{
	// location, orientation and scale of the original interest point found in the image
	float x;
	float y;
	float orientation;
	float scale;
};

// structure describing one or multiple detected visual words
struct TOPSURF_VISUALWORD
{
	TOPSURF_VISUALWORD()
	{
		identifier = -1;
		tf = 0.0f;
		idf = 0.0f;
		count = 0;
		location = NULL;
	}
	~TOPSURF_VISUALWORD()
	{
		if (location)
			delete[] location;
	}
	// visual word identifier
	int identifier;
	// term frequency
	float tf;
	// inverse document frequency
	float idf;
	// number of visual words detected
	int count;
	// the locations of the detected visual words
	TOPSURF_LOCATION *location;

	// compare two elements by their identifiers
	// Note: calling this during sorting will result in the elements
	//       being sorted from low to high
	static bool VW_COMPARE(TOPSURF_VISUALWORD &vw1, TOPSURF_VISUALWORD &vw2)
	{
		if (vw1.identifier < vw2.identifier)
			return true;
		return false;
	}
	// compare two elements by their tf scores
	// Note: calling this during sorting will result in the elements
	//       being sorted from high to low
	static bool TF_COMPARE(TOPSURF_VISUALWORD &vw1, TOPSURF_VISUALWORD &vw2)
	{
		if (vw1.tf > vw2.tf)
			return true;
		return false;
	}
	// compare two elements by their tf-idf scores
	// Note: calling this during sorting will result in the elements
	//       being sorted from high to low
	static bool TFIDF_COMPARE(TOPSURF_VISUALWORD &vw1, TOPSURF_VISUALWORD &vw2)
	{
		if (vw1.tf * vw1.idf > vw2.tf * vw2.idf)
			return true;
		return false;
	}
};

// structure describing the topsurf descriptor
struct TOPSURF_DESCRIPTOR
{
	TOPSURF_DESCRIPTOR()
	{
		count = 0;
		visualword = NULL;
		length = 0.0f;
	}
	~TOPSURF_DESCRIPTOR()
	{
		if (visualword)
			delete[] visualword;
	}
	// total number of visual words detected in the image
	int count;
	// the visual words
	TOPSURF_VISUALWORD *visualword;
	// vector length of the descriptor, useful for fast cosine distance calculation
	float length;
};

#endif