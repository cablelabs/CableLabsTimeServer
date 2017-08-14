/*
 * Copyright 2017 Cable Television Laboratories, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cablelabs.time.test;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cablelabs.time.server.CableLabsTimeServer;
import com.cablelabs.time.server.TimeServer;

/**
 * This class tests the CableLabsTimeServer
 */
public class TestCableLabsTimeServer
{
	private CableLabsTimeServer m_timeServer = null;

	@Before
	public void setUp() throws Exception
	{
		m_timeServer = new CableLabsTimeServer();
	}

	@After
	public void tearDown() throws Exception
	{
		m_timeServer.stopServer();
	}
	
	@Test
	public void testTimeServerValueFromDate()
	{
		/*
		 * From RFC868:
		 * the time 2,208,988,800 corresponds to 00:00  1 Jan 1970 GMT,
		 * 			2,398,291,200 corresponds to 00:00  1 Jan 1976 GMT,
		 * 			2,524,521,600 corresponds to 00:00  1 Jan 1980 GMT,
		 * 			2,629,584,000 corresponds to 00:00  1 May 1983 GMT,
		 * 		and -1,297,728,000 corresponds to 00:00 17 Nov 1858 GMT.
		 */
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TimeServer.TIME_SERVER_TIMEZONE));
		
		cal.set(1970, 0, 1, 0, 0, 0);
		long tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(2208988800L, tsValue);

		cal.set(1976, 0, 1, 0, 0, 0);
		tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(2398291200L, tsValue);
		
		cal.set(1976, 0, 1, 0, 0, 0);
		tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(2398291200L, tsValue);

		cal.set(1980, 0, 1, 0, 0, 0);
		tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(2524521600L, tsValue);
		
		cal.set(1983, 4, 1, 0, 0, 0);
		tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(2629584000L, tsValue);
		
		cal.set(1858, 10, 17, 0, 0, 0);
		tsValue = TimeServer.timeServerValueFromDate(cal);
		assertEquals(-1297728000L, tsValue);
	}
	
	@Test
	public void testTimeServerValueToDate()
	{
		/*
		 * From RFC868:
		 * the time 2,208,988,800 corresponds to 00:00  1 Jan 1970 GMT,
		 * 			2,398,291,200 corresponds to 00:00  1 Jan 1976 GMT,
		 * 			2,524,521,600 corresponds to 00:00  1 Jan 1980 GMT,
		 * 			2,629,584,000 corresponds to 00:00  1 May 1983 GMT,
		 * 		and -1,297,728,000 corresponds to 00:00 17 Nov 1858 GMT.
		 */
		
		Calendar calExpected = Calendar.getInstance(TimeZone.getTimeZone(TimeServer.TIME_SERVER_TIMEZONE));
		calExpected.set(Calendar.MILLISECOND, 0);

		calExpected.set(1970, 0, 1, 0, 0, 0);
		long testSec = 2208988800L;
		Calendar calActual = TimeServer.timeServerValueToDate(testSec);
		assertEquals(0, calExpected.compareTo(calActual));
		
		calExpected.set(1976, 0, 1, 0, 0, 0);
		testSec = 2398291200L;
		calActual = TimeServer.timeServerValueToDate(testSec);
		assertEquals(0, calExpected.compareTo(calActual));
		
		calExpected.set(1980, 0, 1, 0, 0, 0);
		testSec = 2524521600L;
		calActual = TimeServer.timeServerValueToDate(testSec);
		assertEquals(0, calExpected.compareTo(calActual));
		
		calExpected.set(1983, 4, 1, 0, 0, 0);
		testSec = 2629584000L;
		calActual = TimeServer.timeServerValueToDate(testSec);
		assertEquals(0, calExpected.compareTo(calActual));
		
		calExpected.set(1858, 10, 17, 0, 0, 0);
		testSec = -1297728000L;
		calActual = TimeServer.timeServerValueToDate(testSec);
		assertEquals(0, calExpected.compareTo(calActual));
	}
	
	@Test
	public void testTimeServerValueToBytes()
	{
		long testTimeServerValue = 3711719665L;
		byte[] expecteds = new byte[] {
				(byte)0xDD,
				(byte)0x3C,
				(byte)0x58,
				(byte)0xF1
		};
		byte[] actuals = TimeServer.timeServerValueToBytes(testTimeServerValue);
		assertArrayEquals(expecteds, actuals);
		
		testTimeServerValue = 3711722294L;
		expecteds = new byte[] {
				(byte)0xDD,
				(byte)0x3C,
				(byte)0x63,
				(byte)0x36
		};
		actuals = TimeServer.timeServerValueToBytes(testTimeServerValue);
		assertArrayEquals(expecteds, actuals);
	}
	
	@Test
	public void testTimeServerValueFromBytes()
	{
		byte[] testBytes = new byte[] {
				(byte)0xDD,
				(byte)0x3C,
				(byte)0x58,
				(byte)0xF1
		};
		long expected = 3711719665L;
		long actual = TimeServer.timeServerValueFromBytes(testBytes);
		assertEquals(expected, actual);
		
		testBytes = new byte[] {
				(byte)0xDD,
				(byte)0x3C,
				(byte)0x63,
				(byte)0x36
		};
		expected = 3711722294L;
		actual = TimeServer.timeServerValueFromBytes(testBytes);
		assertEquals(expected, actual);
	}
}
