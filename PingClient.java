// PingClient.java
// usage: java PingClient host port passwd
import java.io.*;
import java.net.*;
import java.nio.*;

public class PingClient {
  private static short SEQNUM = 0;
  private static String PASSWD;

  private static long RTT_MIN = Long.MAX_VALUE;
  private static long RTT_MAX = Long.MIN_VALUE;
  private static int RTT_N = 0;
  private static long RTT_SUM = 0;
  private static int LOSS_COUNT = 0;
  private static final int PING_COUNT = 10;

  public static void main(String[] args) throws Exception {
    // Get command line argument.
    if (args.length != 3) {
      System.out.println("usage: java PingClient host port passwd");
      return;
    }

    String serverName = args[0];
    int serverPort = Integer.parseInt(args[1]);
    PASSWD = args[2];

    InetAddress serverIPAddress = InetAddress.getByName(serverName);

    // Create socket.
    DatagramSocket clientSocket = new DatagramSocket();

    // Ping the server.
    for (int i = 0; i < PING_COUNT; i ++) {
      pingServer(clientSocket, serverIPAddress, serverPort);
      Thread.sleep(1000);
      SEQNUM ++;
    }

    // Output the statistics.
    System.out.format("RTT min/avg/max: %d/%d/%d\n",
      RTT_MIN, RTT_SUM/RTT_N, RTT_MAX);
    System.out.format("Loss rate: %d%%\n", LOSS_COUNT * 100 / PING_COUNT);

    // close client socket
    clientSocket.close();

  } // end of main

  private static void pingServer(DatagramSocket clientSocket,
                                 InetAddress serverIPAddress,
                                 int serverPort) throws Exception {
    long sendTime = System.currentTimeMillis();

    // Build message:
    // PING sequence_number client_send_time passwd CRLF
    ByteBuffer sendBuffer = ByteBuffer.allocate(20 + PASSWD.length());
    sendBuffer.put("PING ".getBytes("US-ASCII"));
    sendBuffer.putShort(SEQNUM);
    sendBuffer.put((byte)' ');
    sendBuffer.putLong(sendTime);
    sendBuffer.put((byte)' ');
    sendBuffer.put(new String(PASSWD + " \r\n").getBytes("US-ASCII"));

    // Construct and send datagram.
    byte[] sendData = sendBuffer.array();
    DatagramPacket sendPacket = new DatagramPacket(sendData,
                                                   sendData.length,
                                                   serverIPAddress,
                                                   serverPort);
    clientSocket.send(sendPacket);

    // Receive datagram.
    byte[] receiveData = new byte[1024];
    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                                                      receiveData.length);
    clientSocket.setSoTimeout(1000);
    try {
      clientSocket.receive(receivePacket);
    } catch (SocketTimeoutException e) {
      System.out.println("Server response timeout: " + SEQNUM + ".");
      LOSS_COUNT ++;
      return;
    }

    // RTT logging.
    long rtt = System.currentTimeMillis() - sendTime;
    if (rtt < RTT_MIN) {
      RTT_MIN = rtt;
    }
    if (rtt > RTT_MAX) {
      RTT_MAX = rtt;
    }
    RTT_SUM += rtt;
    RTT_N ++;

    // Print output.
    String sentenceFromServer = new String(receivePacket.getData(), "UTF-8");
    System.out.print("From Server: " + sentenceFromServer);
  }

  private static byte[] shortToBytes(short x) {
    ByteBuffer buffer = ByteBuffer.allocate(Short.SIZE/Byte.SIZE);
    buffer.putShort(x);
    return buffer.array();
  }

  private static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);
    buffer.putLong(x);
    return buffer.array();
  }

} // end of UDPClient