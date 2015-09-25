/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping <michael.entrup@wwu.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package tools;

/**
 * This is a small utility class to measure times in milliseconds. Each method
 * is available as static and non-static.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class Timer {

	private long start = 0;
	private long interval = 0;
	private static long sStart = 0;
	private static long sInterval = 0;

	/**
	 * Creates a new instance of {@link Timer}.
	 */
	public Timer() {
		super();
	}

	/**
	 * Creates a new instance of {@link Timer} and starts it.
	 */
	public Timer(final boolean start) {
		super();
		if (start) {
			start();
		}
	}

	/**
	 * Sets the current time as reference (start) point.
	 */
	public void start() {
		start = System.currentTimeMillis();
		interval = start;
	}

	/**
	 * [static] Sets the current time as reference (start) point.
	 */
	public static void sStart() {
		sStart = System.currentTimeMillis();
		sInterval = sStart;
	}

	/**
	 * @return the elapsed time of the current interval
	 */
	public long interval() {
		final long temp = interval;
		interval = System.currentTimeMillis();
		return System.currentTimeMillis() - temp;
	}

	/**
	 * [static]
	 *
	 * @return the elapsed time of the current interval
	 */
	public static long sInterval() {
		final long temp = sInterval;
		sInterval = System.currentTimeMillis();
		return System.currentTimeMillis() - temp;
	}

	/**
	 * @return the time elapsed since the call of start
	 */
	public long stop() {
		return System.currentTimeMillis() - start;
	}

	/**
	 * [static]
	 *
	 * @return the time elapsed since the call of start
	 */
	public static long sStop() {
		return System.currentTimeMillis() - sStart;
	}

}
