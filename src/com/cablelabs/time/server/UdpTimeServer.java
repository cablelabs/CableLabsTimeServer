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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This class is a UDP Time Server
 */
public class UdpTimeServer extends TimeServer
{
	private DatagramSocket m_udpSocket = null;
	
	/**
	 * Create an instance of a UdpTimeServer
	 */
	public UdpTimeServer()
	{
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cablelabs.TimeServer#getServerRunning()
	 */
	@Override
	public boolean getServerRunning()
	{
		return (super.getServerRunning() &&
				(m_udpSocket != null));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cablelabs.TimeServer#startServer()
	 */
	@Override
	public void startServer()
	{
		super.startServer();
		
		_UdpTimeServer server = new _UdpTimeServer();
		m_listenerThread = new Thread(server, "UdpTimeServer");
		m_listenerThread.start();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cablelabs.time.server.TimeServer#stopServer()
	 */
	@Override
	public void stopServer()
	{
		if (m_udpSocket != null)
		{
			m_udpSocket.close();
		}
		super.stopServer();
	}
	
	/**
	 * This class is the UDP Time Server runner
	 */
	private class _UdpTimeServer implements Runnable
	{
		/**
		 * Create an instance of a _UdpTimeServer
		 */
		public _UdpTimeServer()
		{
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() 
		{
			try
			{
				if (m_listenAddress == null)
				{
					m_udpSocket = new DatagramSocket(m_port);
				}
				else
				{
					InetAddress inetAddr = InetAddress.getByName(m_listenAddress);
					m_udpSocket = new DatagramSocket(m_port, inetAddr);
				}
				
				String status = "Listening for UDP time requests on " +
						(m_listenAddress != null ? m_listenAddress + ", " : "") +
						"port " + m_udpSocket.getLocalPort() + ", with backlog " + m_requestBacklog;
				System.out.println(status);
				
				byte[] udpData = new byte[1024];
				while (!m_terminate)
				{
					DatagramPacket udpPacket = new DatagramPacket(udpData, udpData.length);
					
					// this blocks
		            try
		            {
						m_udpSocket.receive(udpPacket);
						
						// a request arrived
						
			 			_UdpTimeRequestHandler handler = new _UdpTimeRequestHandler(udpPacket);
			 			Thread udpTimeRequestThread = new Thread(handler, "UdpTimeRequestHandler");
			 			udpTimeRequestThread.run();
					}
		            catch(SocketException e)
					{
		            	// if we are trying to terminate, this is expected
						if (!m_terminate)
						{
							e.printStackTrace();
						}
					}
		            catch (IOException e)
		            {
						e.printStackTrace();
					}
				}
			}
			catch (UnknownHostException ex)
			{
				System.out.println("Invalid Inet Address specified: " + m_listenAddress);
			}
			catch(SocketException ex)
			{
            	ex.printStackTrace();
			}
		}
		
	}
	
	/**
	 * This class is the handler for time requests
	 */
	private class _UdpTimeRequestHandler implements Runnable
	{
		private DatagramPacket m_clientPacket = null;
		
		/**
		 * Create an instance of a TimeRequestHandler
		 * @param clientSocket The client socket on which to reply
		 */
		public _UdpTimeRequestHandler(DatagramPacket clientPacket)
		{
			m_clientPacket = clientPacket;
		}
				
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (m_clientPacket != null)
			{
				InetAddress inetAddr = m_clientPacket.getAddress();
				int port = m_clientPacket.getPort();
				
				System.out.print("Processing request from " + inetAddr.toString());
				
				long timeValueSec =
						timeServerValueFromDate(Calendar.getInstance(
								TimeZone.getTimeZone(TIME_SERVER_TIMEZONE)));
				ByteBuffer buf = ByteBuffer.allocate(4);
				buf.putInt((int)timeValueSec);
				byte[] responseData = buf.array();
				
				DatagramPacket response = new DatagramPacket(responseData, responseData.length, inetAddr, port);
            	
				try
				{
					m_udpSocket.send(response);
					
					System.out.println("; returned " + timeValueSec +
							" ( " + getDateFormat().format(
									TimeServer.timeServerValueToDate(timeValueSec).getTime()) + " )");
				}
				catch (SocketException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
                {
					e.printStackTrace();
				}
//				finally
//				{
//					if (m_udpSocket != null)
//					{
//						m_udpSocket.close();
//					}
//				}
			}
		}
	}
}
