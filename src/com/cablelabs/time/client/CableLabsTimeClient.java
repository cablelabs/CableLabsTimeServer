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

package com.cablelabs.time.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.cablelabs.time.server.TimeServer;

/**
 * This class is a CableLabs Time Protocol Client
 */
public class CableLabsTimeClient
{
	private String m_serverIp = null;
	
	private boolean m_useTcp = true;
	
	/**
	 * Create an instance of a CableLabsTimeClient
	 */
	public CableLabsTimeClient()
	{
		
	}
	
	/**
	 * Get the server IP address
	 * @return IP address string
	 */
	public String getServerIp()
	{
		return m_serverIp;
	}
	
	/**
	 * Set the server IP address
	 * @param address IP address string
	 */
	public void setServerIp(String address)
	{
		m_serverIp = address;
	}
	
	/**
	 * Perform a TCP Time Protocol request
	 */
	protected void doTcpRequest()
	{
		try
		{
			System.out.println("Performing TCP Time Request...");
			
			Socket sock = new Socket(m_serverIp, TimeServer.TIME_SERVER_PORT);
			
			ByteBuffer buf = ByteBuffer.allocate(4);
			int byteVal = 0;
			while (((byteVal = sock.getInputStream().read()) != -1) && (buf.hasRemaining()))
			{
				buf.put((byte)byteVal);
			}
			byte[] response = buf.array();

			String printout = "Server response: " + Arrays.toString(response);
			if (response.length > 0)
			{
				long responseSec = TimeServer.timeServerValueFromBytes(response);
				Calendar responseCal = TimeServer.timeServerValueToDate(responseSec);
				printout += " -> " + responseSec + " ( " + TimeServer.getDateFormat().format(responseCal.getTime()) + " )";
			}
			System.out.println(printout);
			
			sock.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform a UDP Time Protocol request
	 */
	protected void doUdpRequest()
	{
		try
		{
			System.out.println("Performing UDP Time Request...");
			
			DatagramSocket socket = new DatagramSocket();
		    InetAddress inetAddr = InetAddress.getByName(m_serverIp);
		    
		    byte[] sendData = new byte[0];
		    byte[] receiveData = new byte[4];
		    
		    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddr, TimeServer.TIME_SERVER_PORT);
		    socket.send(sendPacket);
		    
		    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    socket.receive(receivePacket);
		    
		    long dataVal = TimeServer.timeServerValueFromBytes(receivePacket.getData());
		    String response = String.valueOf(dataVal);
		    String printout = "Server response: " + response;
			if (response.length() > 0)
			{
				long responseSec = Long.parseLong(response);
				
				Calendar responseCal = TimeServer.timeServerValueToDate(responseSec);
				printout += " ( " + TimeServer.getDateFormat().format(responseCal.getTime()) + " )";
			}
			System.out.println(printout);
		    
		    socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform a Time Protocol request
	 */
	public void doRequest()
	{
		if (m_useTcp)
		{
			doTcpRequest();
		}
		else
		{
			doUdpRequest();
		}
	}
	
	private static Options s_cmdline_options = null;
	
	/**
	 * Print the command usage
	 */
	public static void printUsage()
	{
		String usage = CableLabsTimeClient.class.getSimpleName() + " Usage:" + System.lineSeparator();
		
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
		CableLabsTimeClient client = new CableLabsTimeClient();
		
		if (args.length > 0)
		{
			s_cmdline_options = new Options();
			s_cmdline_options.addOption("h", "help", false, "Print the command usage");
			s_cmdline_options.addOption("i", "inet_addr", true, "Inet Address to send request (server address)");
			s_cmdline_options.addOption("t", "tcp", false, "Use TCP for the time request (default)");
			s_cmdline_options.addOption("u", "udp", false, "Use UDP for the time request");
			
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
					client.setServerIp(inetAddr);
				}
				if (cmd.hasOption("t"))
				{
					client.m_useTcp = true;
				}
				else if (cmd.hasOption("u"))
				{
					client.m_useTcp = false;
				}
			}
			catch (ParseException e)
			{
				System.err.println("Error parsing command line arguments:");
				e.printStackTrace();
				
				System.exit(1);
			}
		}
		
		client.doRequest();
	}

}
