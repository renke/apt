/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2012-2013  Members of the project group APT
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.apt.ui.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import uniol.apt.module.exception.ModuleException;
import uniol.apt.module.exception.NoSuchTransformationException;
import uniol.apt.ui.AptParameterTransformation;
import uniol.apt.ui.DescribedParameterTransformation;
import uniol.apt.ui.ParameterTransformation;
import uniol.apt.ui.ParametersTransformer;
import uniol.apt.ui.StreamParameterTransformation;
import uniol.apt.ui.StreamWithOptionsParameterTransformation;

/**
 * This class manages a bunch of parameter transformations and uses them to
 * transform a given parameter string to an object of a specific type.
 *
 * @author Renke Grunwald
 */
public abstract class ParametersTransformerImpl implements ParametersTransformer {

	/**
	 * Symbol that signals that a file should be read from the standard
	 * input.
	 */
	public static final String STANDARD_INPUT_SYMBOL = "-";

	private Map<Class<?>, ParameterTransformation<?>> transformations = new HashMap<>();

	/**
	 * Adds a transformation.
	 *
	 * @param klass the class of the resulting object
	 * @param transformation the actual transformation
	 * @param <T> the type of the resulting object
	 * @return the resulting, transformed object
	 */
	@SuppressWarnings("unchecked")
	protected <T> ParameterTransformation<T> addTransformation(Class<T> klass, ParameterTransformation<T> transformation) {
		return (ParameterTransformation<T>) transformations.put(klass, transformation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParameterTransformation<T> getTransformation(Class<T> klass) {
		return (ParameterTransformation<T>) transformations.get(klass);
	}

	@Override
	public String getTransformationDescription(Class<?> klass) throws NoSuchTransformationException {
		ParameterTransformation<?> transformation = transformations.get(klass);
		if (transformation == null)
			throw new NoSuchTransformationException(klass);
		if (!(transformation instanceof DescribedParameterTransformation))
			return "";
		return ((DescribedParameterTransformation<?>) transformation).getFormatDescription();
	}

	@Override
	public Object transform(String arg, Class<?> klass) throws ModuleException {
		ParameterTransformation<?> transformation = transformations.get(klass);
		if (transformation == null)
			throw new NoSuchTransformationException(klass);
		AptParameterTransformation annotation = transformation.getClass()
				.getAnnotation(AptParameterTransformation.class);
		if (annotation.fileSource()) {
			String[] parts = new String[] { "", arg };
			if (transformation instanceof StreamWithOptionsParameterTransformation) {
				String[] tmpParts = arg.split(":", 2);
				if (tmpParts.length == 2 && !ignoreParts(tmpParts[0], tmpParts[1]))
					parts = tmpParts;
			}
			if (STANDARD_INPUT_SYMBOL.equals(parts[1])) {
				return transformStream(System.in, klass, parts[0]);
			} else {
				return transformFile(parts[1], klass, parts[0]);
			}
		} else {
			return transformString(arg, klass);
		}
	}

	static private boolean ignoreParts(String first, String second) {
		// Only one character before the ':'?
		if (first.length() != 1)
			return false;

		// A backslash right after the ':'?
		if (second.isEmpty() || second.charAt(0) != '\\')
			return false;

		// This is an absolute path like d:\whatever, not an option
		return true;
	}

	@Override
	public Object transformString(String arg, Class<?> klass) throws ModuleException {
		ParameterTransformation<?> transformation = transformations.get(klass);
		if (transformation == null)
			throw new NoSuchTransformationException(klass);
		Object obj = transformation.transform(arg);
		if (obj == null)
			throw new NullPointerException("Parameter transformation for class " + klass + " returned "
					+ "null when given the argument '" + arg + "'");
		return obj;
	}

	@Override
	public Object transformStream(InputStream istr, Class<?> klass) throws ModuleException {
		return transformStream(istr, klass, "");
	}

	private Object transformStream(InputStream istr, Class<?> klass, String options) throws ModuleException {
		ParameterTransformation<?> transformation = transformations.get(klass);
		if (transformation == null)
			throw new NoSuchTransformationException(klass);
		try {
			Object obj;
			if (transformation instanceof StreamWithOptionsParameterTransformation) {
				StreamWithOptionsParameterTransformation<?> streamTrans = (StreamWithOptionsParameterTransformation<?>) transformation;
				obj = streamTrans.transform(istr, options);
			} else if (transformation instanceof StreamParameterTransformation) {
				StreamParameterTransformation<?> streamTrans = (StreamParameterTransformation<?>) transformation;
				obj = streamTrans.transform(istr);
			} else {
				obj = transformation.transform(IOUtils.toString(istr, "UTF-8"));
			}
			if (obj == null)
				throw new NullPointerException("Parameter transformation for class "
						+ klass + " returned null");
			return obj;
		} catch (IOException e) {
			throw new ModuleException("Can't read stream: " + e.getMessage());
		}
	}

	private Object transformFile(String file, Class<?> klass, String options) throws ModuleException {
		try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			return transformStream(stream, klass, options);
		} catch (IOException e) {
			throw new ModuleException("Can't read " + file + ": " + e.getMessage());
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
