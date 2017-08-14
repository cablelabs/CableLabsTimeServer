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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This class is a TCP Time Server
 */
public class TcpTimeServer extends TimeServer
{
	private ServerSocket m_tcpSocket = null;
	
	/**
	 * Create an instance of a TcpTimeServer
	 */
	public TcpTimeServer()
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
				(m_tcpSocket != null) && (m_tcpSocket.isBound()) && (!m_tcpSocket.isClosed()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cablelabs.TimeServer#startServer()
	 */
	@Override
	public void startServer()
	{
		super.startServer();
		
		_TcpTimeServer server = new _TcpTimeServer();
		m_listenerThread = new Thread(server, "TcpTimeServer");
		m_listenerThread.start();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cablelabs.time.server.TimeServer#stopServer()
	 */
	@Override
	public void stopServer()
	{
		if (m_tcpSocket != null)
		{
			try
			{
				m_tcpSocket.close();
			}
			catch (IOException e)
			{
				// do nothing
			}
		}
		super.stopServer();
	}
	
	/**
	 * This class is the TCP Time Server runner
	 */
	private class _TcpTimeServer implements Runnable
	{
		/**
		 * Create an instance of a _TcpTimeServer
		 */
		public _TcpTimeServer()
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
					m_tcpSocket = new ServerSocket(m_port, m_requestBacklog);
				}
				else
				{
					InetAddress inetAddr = InetAddress.getByName(m_listenAddress);
					m_tcpSocket = new ServerSocket(m_port, m_requestBacklog, inetAddr);
				}
				
				String status = "Listening for TCP time requests on " + m_tcpSocket.getInetAddress().toString() +
						", port " + m_tcpSocket.getLocalPort() + ", with backlog " + m_requestBacklog;
				System.out.println(status);
				
				while (!m_terminate)
				{
					try
					{
						// this blocks
						Socket clientSocket = m_tcpSocket.accept();
						
						// a request arrived
						
						_TcpTimeRequestHandler handler = new _TcpTimeRequestHandler(clientSocket);
						Thread timeRequestThread = new Thread(handler, "TimeRequestHandler");
						timeRequestThread.run();
					}
					catch(SocketException e)
					{
						// if we are trying to terminate, this is expected
						if (!m_terminate)
						{
							e.printStackTrace();
						}
					}
				}
			}
			catch (UnknownHostException ex)
			{
				System.out.println("Invalid Inet Address specified: " + m_listenAddress);
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * This class is the handler for time requests
	 */
	private class _TcpTimeRequestHandler implements Runnable
	{
		private Socket m_clientSocket = null;
		
		/**
		 * Create an instance of a TimeRequestHandler
		 * @param clientSocket The client socket on which to reply
		 */
		public _TcpTimeRequestHandler(Socket clientSocket)
		{
			m_clientSocket = clientSocket;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (m_clientSocket != null)
			{
				System.out.print("Processing request from " + m_clientSocket.getRemoteSocketAddress().toString());
				
				try
				{
					long timeValueSec =
							TimeServer.timeServerValueFromDate(Calendar.getInstance(
									TimeZone.getTimeZone(TIME_SERVER_TIMEZONE)));
					
					ByteBuffer buf = ByteBuffer.allocate(4);
					buf.putInt((int)timeValueSec);
					byte[] responseData = buf.array();
					
					m_clientSocket.getOutputStream().write(responseData);
					
					// According to RFC868, it looks like the client is supposed to close the socket,
					// but let's make sure that it's closed
					m_clientSocket.close();
					
					System.out.println("; returned " + timeValueSec +
							" ( " + getDateFormat().format(
									TimeServer.timeServerValueToDate(timeValueSec).getTime()) + " )");
				}
				catch (IOException e)
				{
					// we probably didn't end the line
					System.out.println("");
					
					e.printStackTrace();
				}
			}
		}
	}
}
