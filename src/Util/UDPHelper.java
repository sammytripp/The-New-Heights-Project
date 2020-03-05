package Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;

/**
 * 
 * @author Samantha Tripp
 *
 */
public class UDPHelper {
	

	private DatagramSocket socket;
	private DatagramPacket sendPacket, receivePacket;
	private int portNumber;
	private byte[] data;
	
	public UDPHelper(int portNumber) {
		// Construct DatagramSocket and bind it to portNumber
		this.portNumber = portNumber;
		try {
			socket = new DatagramSocket(portNumber);
		} catch (SocketException se) {
			// Error creating DatagramSocket
			System.out.println("Error creating DatagramSocket:");
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void send(byte[] message, int destinationPort) {
		
		// Construct DataPacket to send message
		try {
			sendPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), destinationPort);
		} catch (UnknownHostException e) {
			System.out.println("Error constructing sendPacket:");
			e.printStackTrace();
			System.exit(1);
		}
				
		// Print packet information
		printUDPData(sendPacket, false);
		
				
		// Send the DatagramPacket 
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Error sending DatagramPacket:");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Packet sent.\n");
	}
	
	public byte[] receive() {
		
		// Construct DatagramPacket to receive data
		data = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);
		
		// Block until a DatagramPacket response is received
		try { 
			System.out.println("UDP Handler: waiting for packet");
			socket.receive(receivePacket);
			data = receivePacket.getData();
		} catch(IOException e) {
			System.out.println("Error receiving DatagramPacket response:");
			e.printStackTrace();
			System.exit(1);
		}
		// Process the received DataPacket
		printUDPData(receivePacket, true);
		return data;
	}
	
	/**
	 * Helper method to assist printing DatagramPacket information to the console.
	 * 
	 * @param packet, The DatagramPacket containing the data to print.
	 * @param dataReceived, Indicator of an incoming (true) or outgoing (false) DatagramPacket.
	 */
	private void printUDPData(DatagramPacket packet, boolean dataReceived) {
		if (dataReceived) { // Print statements for received DatagramPacket
			System.out.println("Port " + portNumber + ": packet received");
			System.out.println("From host: " + packet.getAddress());
			System.out.println("Host port: " + packet.getPort());
		} else {
			System.out.println("Port " + portNumber + ": sending packet");
			System.out.println("To host: " + packet.getAddress());
			System.out.println("Destination host port: " + packet.getPort());
			
		}
		
		System.out.print("Data as string: ");
		System.out.println(bytesToString(packet.getData()));
		System.out.print("Data in bytes: ");
		printBytes(packet.getData());
		System.out.println();
	}
	
	/**
	 * Function to convert CallEvent objects to byte array messages for
	 * use in constructing DatagramPackets.
	 * 
	 * @param floorEvent
	 */
	public byte[] createMessage(CallEvent floorEvent) {
		
		byte[] sendData;
		
		// Prepare byte array of data to send
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			
			
			// Convert floor event to bytes
			byteStream.write(floorEvent.toString().getBytes());
			
			// Create byte array
			sendData = byteStream.toByteArray();
			byteStream.close();
			
			return sendData;
		
		} catch (IOException e) {
			System.out.println("Create Message Error: ");
			e.printStackTrace();
			System.exit(1);
		}
		
		return null;
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] createReply() throws IOException {
		byte[] reply;
		String msg = "Received";
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			
			byteStream.write(msg.getBytes());
			byteStream.close();
			reply = byteStream.toByteArray();
			return reply;
		} catch (IOException e) {
			System.out.println("createReply() error:");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * Private method to decode byte array received in DatagramPacket to
	 * a CallEvent object.
	 * 
	 * @param message
	 * @return CallEvent
	 */
	public synchronized String decodeMessage(byte[] message) {
		return bytesToString(message);
	
	}
	

	/**
	 * Parses the byte array so that it can show the first two bits (01, 02 or 03)
	 * then formats a new string based off of the other bits.
	 * 
	 * @param b
	 * @return
	 */
	public static String bytesToString(byte[] b) {
		String s = "";
		s += new String(b, 0, b.length);
		return s;
	}
	
	/**
	 * Helper method to print arrays of bytes.
	 * Caps long strings to 80chars to make 100 byte arrays easier to read.
	 * 
	 * @param bytes
	 */
	public static void printBytes(byte[] bytes) {
		String s = "";
		if (bytes.length <= 5) {
			for (int i = 0; i < bytes.length - 1; i++) {
				s += bytes[i] + ", ";
			}
			System.out.println( s);
			return;
		}
		for (int i = 0; i < bytes.length - 1; i++) {
			if (bytes[1] == 4) {
				s += bytes[0] + ", " + bytes[1] + ", " + bytes[2] + ", " + bytes[3];
				System.out.println( s);
				return;
			}
			if (bytes[i] == 0 && bytes[i + 1] == 0) {
				break;
			}
			s += bytes[i] + ", ";
		}
		System.out.println(s);
	}

	
}
