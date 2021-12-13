/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cz.it4i.ulman.transfers.graphexport.ui.util;

import org.scijava.prefs.PrefService;
import java.util.HashMap;
import java.util.Map;

public class PerProjectPrefsService
{
	// --------------------- external ---------------------
	/** Creates an unique key from a given dialog param and project ID.
	 * This utility class uses solely this method to create keys to store
	 * the params values. One may want to override this method if, for example,
	 * values should be shared within a certain family of params.
	 */
	public static String createKey(final String paramName, final String projectID)
	{
		return paramName+"_"+projectID;
	}


	/**
	 * Fetch value for given dialog's parameter for the given project. This value is retrieved
	 * from the preferences store if it is found there, or a given default value is used .
	 *
	 * @param ps the preferences store
	 * @param cls the dialog identified with its encompassing class
	 * @param projectID the identifier of the project
	 * @param paramName the examined dialog's param item
	 * @param defaultValue default value for the examined item
	 * @return the stored or the default value as a String
	 */
	public static
	String loadStringParam(final PrefService ps,
	                       final Class<?> cls, final String projectID,
	                       final String paramName, final String defaultValue)
	{
		final String val = loadParam(ps,cls,projectID,paramName);
		if (val == null) return defaultValue;
		return val;
	}

	/** see PerProjectPrefsService.loadStringParam() */
	public static
	int loadIntParam(final PrefService ps,
	                 final Class<?> cls, final String projectID,
	                 final String paramName, final int defaultValue)
	{
		final String val = loadParam(ps,cls,projectID,paramName);
		if (val == null) return defaultValue;
		return Integer.parseInt(val);
	}

	/** see PerProjectPrefsService.loadStringParam() */
	public static
	double loadDoubleParam(final PrefService ps,
	                       final Class<?> cls, final String projectID,
	                       final String paramName, final double defaultValue)
	{
		final String val = loadParam(ps,cls,projectID,paramName);
		if (val == null) return defaultValue;
		return Double.parseDouble(val);
	}


	public static
	void storeStringParam(final PrefService ps,
	                      final Class<?> cls, final String projectID,
	                      final String paramName, final String value)
	{
		storeParam(ps,cls,projectID,paramName,value);
	}

	public static
	void storeIntParam(final PrefService ps,
	                   final Class<?> cls, final String projectID,
	                   final String paramName, final int value)
	{
		storeParam(ps,cls,projectID,paramName,value);
	}

	public static
	void storeDoubleParam(final PrefService ps,
	                      final Class<?> cls, final String projectID,
	                      final String paramName, final double value)
	{
		storeParam(ps,cls,projectID,paramName,value);
	}


	// --------------------- internal ---------------------
	/** the designated space to store param values within the dialog's space in the preferences store */
	static String PrefServiceItemName = "project-to-data-names";

	/**
	 * Fetch value for given dialog's parameter for the given project. This value is retrieved
	 * from the preferences store if it is found there, or null is returned.
	 *
	 * @param ps the preferences store
	 * @param cls the dialog identified with its encompassing class
	 * @param projectID the identifier of the project
	 * @param paramName the examined dialog's param item
	 * @return the stored value or null
	 */
	static
	String loadParam(final PrefService ps,
	                 final Class<?> cls, final String projectID,
	                 final String paramName)
	{
		final Map<String,String> m = ps.getMap(cls,PrefServiceItemName);
		return m != null? m.getOrDefault(createKey(paramName,projectID),null) : null;
	}

	static <T>
	void storeParam(final PrefService ps,
	                final Class<?> cls, final String projectID,
	                final String paramName, final T value)
	{
		Map<String,String> m = ps.getMap(cls,PrefServiceItemName);
		if (m == null)
			m = new HashMap<>(1);
		m.put(createKey(paramName,projectID), String.valueOf(value));
		ps.put(cls, PrefServiceItemName, m);
	}
}
