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

package com.cablelabs.time.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public abstract class TimeServer
{
	// On unix-based systems, can't open a socket on port < 1024 without sudo 
	public static final int TIME_SERVER_PORT = 37;
	// TESTING!
	//public static final int TIME_SERVER_PORT = 10001;
	public static final String TIME_SERVER_TIMEZONE = "GMT";
	
	protected static SimpleDateFormat s_dateFormat = null;
	
	protected static long s_referenceValueSec = 0;
	
	protected int m_port = TIME_SERVER_PORT;
	protected int m_requestBacklog = 100; // Is this a good value?
	protected String m_listenAddress = null;
	protected boolean m_autoListenInterface = false;
	
	protected Thread m_listenerThread = null;
	
	protected boolean m_terminate = false;
	
	static {
		/*
		 * From RFC868 (https://tools.ietf.org/html/rfc868):
		 * The time is the number of seconds since 00:00 (midnight) 1 January 1900
		 * GMT, such that the time 1 is 12:00:01 am on 1 January 1900 GMT; this
		 * base will serve until the year 2036.
		 */
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TIME_SERVER_TIMEZONE));
		cal.set(1900, 0, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		s_referenceValueSec = cal.getTimeInMillis() / 1000;
		
		s_dateFormat = new SimpleDateFormat("HH:mm:ss dd MMM yyyy z");
		s_dateFormat.setTimeZone(TimeZone.getTimeZone(TIME_SERVER_TIMEZONE));
	}
	
	/**
	 * Get a flag indicating if the server is running
	 * @return True if the server is running, false otherwise
	 */
	public boolean getServerRunning()
	{
		return ((m_listenerThread != null) && (m_listenerThread.isAlive()));
	}
	
	/**
	 * Starts this server
	 */
	public void startServer()
	{
		m_terminate = false;
	}
	
	/**
	 * Stops this server
	 */
	public void stopServer()
	{
		m_terminate = true;
		if (m_listenerThread != null)
		{
			m_listenerThread.interrupt();
			
			try
			{
				m_listenerThread.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Get the date formatter
	 * @return
	 */
	public static SimpleDateFormat getDateFormat()
	{
		return s_dateFormat;
	}
	
	/**
	 * Convert the specified Time Server value to a Calendar object
	 * @param timeServerValue Time Server value (seconds since 00:00 1 January 1900 GMT)
	 * @return Calendar object
	 */
	public static Calendar timeServerValueToDate(long timeServerValue)
	{
		long timeValueMs = (timeServerValue + s_referenceValueSec) * 1000;
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TimeServer.TIME_SERVER_TIMEZONE));
		cal.setTimeInMillis(timeValueMs);
		
		return cal;
	}
	
	/**
	 * Convert the specified date (Calendar object) to a Time Server value
	 * @param calendar Calendar object holding the date
	 * @return Time Server value (seconds since 00:00 1 January 1900 GMT)
	 */
	public static long timeServerValueFromDate(Calendar calendar)
	{
		calendar.set(Calendar.MILLISECOND, 0);
		long timeValueMs = calendar.getTimeInMillis();
		long timeValueSec = (timeValueMs / 1000) - s_referenceValueSec;
		
		return timeValueSec;
	}
	
	/**
	 * Convert a Time Server value into a byte array
	 * @param timeServerValue Time Server value
	 * @return Byte array
	 */
	public static byte[] timeServerValueToBytes(long timeServerValue)
	{
		ByteBuffer buf = ByteBuffer.allocate(4);
	    buf.putInt((int)timeServerValue);
	    
	    byte[] bytes = buf.array();
	    
	    return bytes;
	}
	
	/**
	 * Convert a byte array into a Time Server value
	 * @param bytes Byte array
	 * @return Time Server value
	 */
	public static long timeServerValueFromBytes(byte[] bytes)
	{
		ByteBuffer buf = ByteBuffer.allocate(4+bytes.length);
		buf.putInt(0);
		buf.put(bytes);
		buf.flip();
		long value = buf.getLong();
		
		return value;
	}
	
	/**
	 * Restart this server only if it is currently running
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void restartServerIfNecessary() throws SecurityException, IllegalAccessException, IOException
	{
		boolean running = getServerRunning();
		if (running)
		{
			stopServer();
			startServer();
		}
	}
	
	/**
	 * Get the port on which the server will listen
	 * @return Port number
	 */
	public int getPort()
	{
		return m_port;
	}
	
	/**
	 * Set the port on which the server will listen; restarts the server if it is currently running
	 * @param port Port number
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void setPort(int port) throws SecurityException, IllegalAccessException, IOException
	{
		m_port = port;
		restartServerIfNecessary();
	}
	
	/**
	 * Get the String InetAddress on which the server will listen
	 * @return String InetAddress
	 */
	public String getListenAddress()
	{
		return m_listenAddress;
	}
	
	/**
	 * Set the String InetAddress on which the server will listen; restarts the server if it is currently running
	 * @param address String InetAddress
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void setListenAddress(String address) throws SecurityException, IllegalAccessException, IOException
	{
		m_listenAddress = address;
		restartServerIfNecessary();
	}
	
	/**
	 * Get the request backlog (requested maximum length of the queue of incoming connections)
	 * @return Queue length
	 */
	public int getRequestBacklog()
	{
		return m_requestBacklog;
	}
	
	/**
	 * Set the request backlog (requested maximum length of the queue of incoming connections);
	 *  restarts the server if it is currently running
	 * @param backlog Queue length
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void setRequestBacklog(int backlog) throws SecurityException, IllegalAccessException, IOException
	{
		m_requestBacklog = backlog;
		restartServerIfNecessary();
	}
}
