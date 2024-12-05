package assignment3;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TFTPServer 
{
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final String READDIR = "read/"; //custom address at your PC
	public static final String WRITEDIR = "write/"; //custom address at your PC
	// OP codes
	public static final int OP_RRQ = 1; // Read a foreign file system request.
	public static final int OP_WRQ = 2; // Write onto a foreign file system request.
	public static final int OP_DAT = 3; // DATA, 
	public static final int OP_ACK = 4; // Poisitive return, acknowledgement 
	public static final int OP_ERR = 5; // Negative return, error
	
	public static void main(String[] args) {
		if (args.length > 0) 
		{
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		//Starting the server
		try 
		{
			TFTPServer server= new TFTPServer();
			server.start();
		}
		catch (SocketException e) 
		{e.printStackTrace();}
	}
	
	private void start() throws SocketException 
	{
		byte[] buf= new byte[BUFSIZE];
		
		// Create socket
		DatagramSocket socket= new DatagramSocket(null);
		
		// Create local bind point 
		SocketAddress localBindPoint= new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);
		
		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);
		
		// Loop to handle client requests 
		while (true) 
		{        
			
			final InetSocketAddress clientAddress = receiveFrom(socket, buf);
			
			// If clientAddress is null, an error occurred in receiveFrom()
			if (clientAddress == null) {
				System.out.println("Error retrieveing the client");
				continue;
			}
			
			final StringBuffer requestedFile = new StringBuffer();
			final int reqtype = ParseRQ(buf, requestedFile);
			
			new Thread() 
			{
				public void run() 
				{
					try 
					{
						DatagramSocket sendSocket= new DatagramSocket(0);
						
						// Connect to client
						sendSocket.connect(clientAddress);						
						
						System.out.printf("%s request for %s from %s using port %d \n",
						(reqtype == OP_RRQ)?"Read":"Write", TFTPPORT,
						clientAddress.getHostName(), clientAddress.getPort());  
						
						// Read request
						if (reqtype == OP_RRQ) 
						{      
							requestedFile.insert(0, READDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
						}
						// Write request
						else 
						{                       
							requestedFile.insert(0, WRITEDIR);
							HandleRQ(sendSocket,requestedFile.toString(),OP_WRQ);  
						}
						sendSocket.close();
					} 
					catch (SocketException e) 
					{e.printStackTrace();}
				}
			}.start();
		}
	}
	
	/**
	* Reads the first block of data, i.e., the request for an action (read or write).
	* @param socket (socket to read from)
	* @param buf (where to store the read data)
	* @return socketAddress (the socket address of the client)
	*/
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) 
	{
		try{
			// Create datagram packet
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			// Receive packet
			socket.receive(packet);
			System.out.println("[packet]" + packet);
			// Get client address and port from the packet
			InetSocketAddress socketAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
			return socketAddress;
		} catch (Exception x) {
			System.out.println("oh no");
			x.printStackTrace();
			return null;
		}
		
	}
	
	/**
	* Parses the request in buf to retrieve the type of request and requestedFile
	* 
	* @param buf (received request)
	* @param requestedFile (name of file to read/write)
	* @return opcode (request type: RRQ or WRQ)
	*/
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) 
	{
		// See "TFTP Formats" in TFTP specification for the RRQ/WRQ request contents
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
		int opcode = byteBuffer.getShort();
		int index = 2;
		
		while(buf[index] != 0) {
			requestedFile.append((char) buf[index]);
			index ++; 
		}
		return opcode;
	}
	
	/**
	* Handles RRQ and WRQ requests 
	* 
	* @param sendSocket (socket used to send/receive packets)
	* @param requestedFile (name of file to read/write)
	* @param opcode (RRQ or WRQ)
	*/
	private void HandleRQ(DatagramSocket sendSocket, String requestedFile, int opcode) 
	{	
		File file = new File(requestedFile); // we change string to file to feed files.
		byte[] pack = new byte[BUFSIZE - 4]; // we add the 512 size limit.
		short errorGeneral = OP_ERR;

		if(opcode == OP_RRQ)
		{
			FileInputStream in = null;
			try {
				in = new FileInputStream(file); // we input the file.
				System.out.println("Found file! Processing.....");
			} catch (FileNotFoundException x) {
				System.out.println("File not found, try again.");
				x.printStackTrace();
			}
			short block = 1; 

			while (true) {
				int length;		
				// Try to retrieve the length of the buffer if it was correctly initialized.		
				try {
					length = in.read(pack);
				} catch (Exception x) {
					x.printStackTrace();
					return;
				}

				if (length == -1) {
					length = 0;
				}

				// we create the packet we need to send.
				DatagramPacket packet = sendPacket(block, pack, length);

				if (send_DATA_receive_ACK(sendSocket, packet, block++)) {
					System.out.println("Success, now block " + block);
				} else {
				System.out.println("error!");
				}

			}

			// See "TFTP Formats" in TFTP specification for the DATA and ACK packet contents
		}
		else if (opcode == OP_WRQ) 
		{
			if (file.exists()) {
				System.out.println("this file already exists, error");
				return;
			} else{
				FileOutputStream out = null;
				try {
					out = new FileOutputStream(file); // the file to be produced.
				} catch (FileNotFoundException err) {
					err.printStackTrace();
					return;
				}
				short blockk = 0;
        boolean keepAlive = true;
				while (keepAlive) {
					// we create a packet.
					DatagramPacket packet = DataSendPacket(sendSocket, acknowledgePacket(blockk++), blockk);

					if (packet == null) {
						System.out.println("Cant find connection...");
						try {
							System.out.println("Closing file...");
							out.close();
						} catch (IOException x) {
							System.out.println("Cant close file, deleting incomplete file...");
						}
						file.delete(); // delete the file if the packet is null.
						break;
					} else {
						byte[] buf = packet.getData();
						try {
							System.out.println("Sending packet " + blockk + ".....");
							out.write(buf, 4, packet.getLength() - 4);
						} catch (IOException x) {
							System.out.println("Error wrtiting data.");
						}
						if (packet.getLength() - 4 < 512) {
							try {
                System.out.println("Sending ack...");
								sendSocket.send(acknowledgePacket(blockk));
                System.out.println("Ack succesfully sent!");
                keepAlive = false;
							} catch (IOException err) {
								err.printStackTrace();
							}
						} 
					}
				}
			}
		}
		else 
		{
			System.err.println("Invalid request. Sending an error packet.");
			// See "TFTP Formats" in TFTP specification for the ERROR packet contents
			send_ERR(sendSocket, errorGeneral, ""); // we send an error if the packet cant be sent			return;
		}		
	}
	
	/**
	 * the method to send data packets and receive and acknowledgement.
	*/
	private boolean send_DATA_receive_ACK (DatagramSocket socket, DatagramPacket packet, short block) {
		byte[] buf = new byte[BUFSIZE];
		DatagramPacket receivedPack = new DatagramPacket(buf, buf.length);
		Boolean loop = true;
		while (loop) {
			try {
				socket.send(packet);
				System.out.println("Sent packet.");
				socket.receive(receivedPack);
				short ack = getAcknowledgement(receivedPack);
				if (ack == block) {
					return true;
				} else if (ack == -1) {
					return false;
				}
			} catch (IOException err) {
				err.printStackTrace();
			}
		}
		return false;
	}
	
	private void send_ERR(DatagramSocket socket, short error, String msg) {
		short errorCode = OP_ERR;
		ByteBuffer wrapper = ByteBuffer.allocate(BUFSIZE);
		wrapper.putShort(errorCode); // we put the error opcode.
		wrapper.putShort(error); //we put the error message.
		wrapper.put(msg.getBytes());
		wrapper.put((byte) 0);

		// and finally we create the datagrampacket that sends the error.
		DatagramPacket receive = new DatagramPacket(wrapper.array(), wrapper.array().length);
		try {
			socket.send(receive);
		} catch (IOException err) {
			System.out.println("package could not be send, error error!");
			err.printStackTrace();
		}
	}


	/**
	 * Constructs us a datapacket that we will send.
	 * @param block blocknumber
	 * @param buf the byte array with data
	 * @param length length of the datagrampacket.
	 * @return
	 */
	private DatagramPacket sendPacket(short block, byte[] buf, int length) {
		ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
		Short op = OP_DAT;
		buffer.putShort(op); // we put the opcode and blocknumber with the length to send a packet.
		buffer.putShort(block);
		buffer.put(buf, 0, length);
		return new DatagramPacket(buffer.array(), 4+length);

	}


	/**
	 * A method to manage the transfer of packets over a datagramsocket connection.
	 * @param socket
	 * @param sendAck
	 * @param block
	 * @return
	 */

	private DatagramPacket DataSendPacket(DatagramSocket socket, DatagramPacket sendAck, short block) {
		byte[] reciever = new byte[BUFSIZE];

		// we create a packet that is to be received.
		DatagramPacket packetReciever = new DatagramPacket(reciever, reciever.length); 

		while (true) {
			try {
				System.out.println("Sending acknowledgement for block: " + block);
				socket.send(sendAck); // we send the ack packet and then we receive the data.
				socket.receive(packetReciever); 

				short blocknumber = getAcknowledgement(packetReciever);
				System.out.println(blocknumber + " " + block);
				if (blocknumber == block) {
					return packetReciever;
				} else if (blocknumber == -1) {
					return null;
				} else {
					System.out.println("Duplicate block....");
				}
			} catch (IOException d) {
				d.printStackTrace();
			}
		}
	}


	private short getAcknowledgement(DatagramPacket ack) {
		ByteBuffer buffer = ByteBuffer.wrap(ack.getData()); // we get the ack data.
		short op = buffer.getShort(); // we get the opcode.

		// if error then kill client, else return the ack packet.
		if (op == OP_ERR) { 
			System.out.println("Client is dead. Disconnecting....");
			return -1;
		} 
		return buffer.getShort();
	}

	
	/**
	 * Builds a datagram packet with both the blocknumber and acknowledge within the buffer.
	 * Then we send a DatagramPacket with the buffer loaded into it as an array.
	 * This method will be used to send over acknowledge packets.
	 * @param block
	 * @return
	 */

	private DatagramPacket acknowledgePacket(short block) {
		ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
		short ack = OP_ACK;
		buf.putShort(ack); //we put the ack opcode.
		buf.putShort(block); //we put the block Number.
		// we get the acknowledge packet.
		return new DatagramPacket(buf.array(), 4);
	}

}