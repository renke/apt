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

package uniol.apt.ui.impl.returns;

import uniol.apt.analysis.snet.SNetResult;
import uniol.apt.ui.ReturnValueTransformation;

/**
 * Transform s net result into a string.
 *
 * @author Daniel
 */
public class SNetResultReturnValueTransformation implements ReturnValueTransformation<SNetResult> {
	@Override
	public String transform(SNetResult result) {
		StringBuilder sb = new StringBuilder("\n");

		if (result.getSplittingLabels().isEmpty() && result.getSynchronizationLabels().isEmpty()) {
			sb.append("  No synchronization and no splitting was found.\n");
		}

		for (String label : result.getSynchronizationLabels()) {
			sb.append("  There is a synchronization before " + label + ".\n");
		}

		for (String label : result.getSplittingLabels()) {
			sb.append("  There is a splitting after " + label + ".\n");
		}

		return sb.toString();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
