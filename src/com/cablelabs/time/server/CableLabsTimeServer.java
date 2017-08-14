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
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class is the CableLabs Time Protocol Server
 */
public class CableLabsTimeServer
{
	private boolean m_useTcp = true;
	private TcpTimeServer m_tcpServer = null;
	
	private boolean m_useUdp = true;
	private UdpTimeServer m_udpServer = null;
	
	private String m_listenAddress = null;
	
	/**
	 * Create an instance of a CableLabsTimeServer
	 */
	public CableLabsTimeServer()
	{
	}
	
	/**
	 * Get the flag indicating if this server will use TCP
	 * @return True if TCP is used
	 */
	public boolean getUseTcp()
	{
		return m_useTcp;
	}
	
	/**
	 * Get the flag indicating if this server will use UDP
	 * @return True if UDP is used
	 */
	public boolean getUseUdp()
	{
		return m_useUdp;
	}
	
	/**
	 * Get the Inet Address on which to listen
	 * @return String Inet Address
	 */
	public String getListenAddress()
	{
		return m_listenAddress;
	}
	
	/**
	 * Set the Inet Address on which to listen
	 * @param address String Inet Address
	 */
	public void setListenAddress(String address)
	{
		m_listenAddress = address;
	}
		
	/**
	 * Start the Time Server(s)
	 * @throws IOException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 */
	public void startServer() throws IOException, SecurityException, IllegalAccessException
	{
		if (m_useTcp)
		{
			m_tcpServer = new TcpTimeServer();
			m_tcpServer.setListenAddress(m_listenAddress);
			m_tcpServer.startServer();
		}
		
		if (m_useUdp)
		{
			m_udpServer = new UdpTimeServer();
			m_udpServer.setListenAddress(m_listenAddress);
			m_udpServer.startServer();
		}
	}
	
	/**
	 * Stop the Time Server
	 */
	public void stopServer()
	{
		if (m_tcpServer != null)
		{
			m_tcpServer.stopServer();
			m_tcpServer = null;
		}
		
		if (m_udpServer != null)
		{
			m_udpServer.stopServer();
			m_udpServer = null;
		}
	}
	
	private static Options s_cmdline_options = null;
	
	/**
	 * Print the command usage
	 */
	public static void printUsage()
	{
		String usage = CableLabsTimeServer.class.getSimpleName() + " Usage:" + System.lineSeparator();
		
		if (s_cmdline_options != null)
		{
			Iterator<Option> iter = s_cmdline_options.getOptions().iterator();
			while (iter.hasNext())
			{
				Option opt = iter.next();
				usage += System.lineSeparator() +
						"-" + opt.getOpt() +
						(opt.hasLongOpt() ? " (--" + opt.getLongOpt() + ")" : "") +
						(opt.hasArg() ? " <" + (opt.hasArgName() ? opt.getArgName() : "value") + ">" : "") +
						" : " + opt.getDescription();
			}
		}
		
		System.out.println(usage + System.lineSeparator());
	}
	
	/**
	 * Entry point of the application
	 * @param args Command line parameters
	 */
	public static void main(String[] args)
	{
		try
		{
			final CableLabsTimeServer timeServer = new CableLabsTimeServer();
			
			if (args.length > 0)
			{
				s_cmdline_options = new Options();
				s_cmdline_options.addOption("h", "help", false, "Print the command usage");
				Option interfaceOpt = new Option("i", "interface", true, "Inet Address on which to listen");
				interfaceOpt.setArgName("inet_addr");
				s_cmdline_options.addOption(interfaceOpt);
				s_cmdline_options.addOption("u", "no_tcp", false, "UDP only (disable TCP listener)");
				s_cmdline_options.addOption("t", "no_udp", false, "TCP only (disable UDP listener)");
				
				String inetAddr = "";
				
				CommandLineParser parser = new DefaultParser();
				try
				{
					CommandLine cmd = parser.parse(s_cmdline_options, args);
					
					if (cmd.hasOption("h"))
					{
						printUsage();
						System.exit(0);
					}
					
					if (cmd.hasOption("i"))
					{
						inetAddr = cmd.getOptionValue("i");
						timeServer.setListenAddress(inetAddr);
					}
					if (cmd.hasOption("u"))
					{
						timeServer.m_useTcp = false;
					}
					if (cmd.hasOption("t"))
					{
						timeServer.m_useUdp = false;
					}
				}
				catch (ParseException e)
				{
					System.err.println("Error parsing command line arguments:" +
							System.lineSeparator() + e.getLocalizedMessage());
					//e.printStackTrace();
					
					printUsage();
					
					System.exit(1);
				}
			}
			
			if ((!timeServer.m_useTcp) && (!timeServer.m_useUdp))
			{
				System.out.println("Error - TCP or UDP (or both) must be enabled!");
				System.exit(100);
			}
			else
			{
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run()
					{
				    	timeServer.stopServer();
						System.out.println("Closed socket(s), shutting down.");
				    }
				});
				
				timeServer.startServer();
			}
		}
		catch (SecurityException | IllegalAccessException | IOException e)
		{
			e.printStackTrace();
		}
	}

}
