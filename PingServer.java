// PingServer.java
// usage: java PingServer port passwd [-delay delay] [-loss loss]
import java.io.*;
import java.net.*;
import java.util.*;

/*
 * Server to process ping requests over UDP.
 */

public class PingServer {
  private static double LOSS_RATE = 0.3;
  private static int AVERAGE_DELAY = 100; // milliseconds
  private static int PORT;
  private static String PASSWD;

  public static void main(String[] args) throws Exception {
    if (!processArgs(args)) {
      printUsage();
      return;
    }

    // Create random number generator for use in simulating
    // packet loss and network delay.
    Random random = new Random();

    // Create a datagram socket for receiving and sending
    // UDP packets through the port specified on the
    // command line.
    DatagramSocket socket = new DatagramSocket(PORT);

    // Processing loop.
    while (true) {

      // Create a datagram packet to hold incomming UDP packet.
      DatagramPacket
        request = new DatagramPacket(new byte[1024], 1024);

      // Block until receives a UDP packet.
      socket.receive(request);

      // Print the received data, for debugging
      printData(request);

      // Decide whether to reply, or simulate packet loss.
      if (random.nextDouble() < LOSS_RATE) {
        System.out.println(" Reply not sent.");
        continue;
      }

      // Simulate prorogation delay.
      Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

      // Send reply.
      if (sendReply(socket, request)) {
        System.out.println("\tReply sent.");
      } else {
        System.out.println("\tInvalid message received.");
      }
    } // end of while
  } // end of main

  private static boolean sendReply(DatagramSocket socket,
                                   DatagramPacket request) throws Exception {
    InetAddress clientHost = request.getAddress();
    int clientPort = request.getPort();

    // Read line from received data and split into tokens.
    byte[] buf = request.getData();
    String line = getLineFromByteArray(buf);
    String[] tokens = line.split("\\s+");

    // Make sure is PING message:
    // PING sequence_number client_send_time passwd CRLF
    if (!tokens[0].equals("PING") || tokens.length != 4) {
      return false;
    }

    String seqNum = tokens[1];
    String sendTime = tokens[2];
    String passwd = tokens[3];

    // Make sure passwd is correct.
    if (!passwd.equals(PASSWD)) {
      return false;
    }

    // Build the response:
    // PINGECHO sequence_number client_send_time passwd CRLF
    String replyLine = "PINGECHO " + seqNum + " " + sendTime + " " + passwd + " \r\n";

    // Send the reply.
    byte[] sendData = replyLine.getBytes("UTF-8");
    DatagramPacket reply = new DatagramPacket(sendData, sendData.length,
                                              clientHost, clientPort);
    socket.send(reply);

    return true;
  }

  private static String getLineFromByteArray(byte[] buf) throws Exception {
    // Wrap the bytes in a byte array input stream,
    // so that you can read the data as a stream of bytes.
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);

    // Wrap the byte array output stream in an input
    // stream reader, so you can read the data as a
    // stream of **characters**: reader/writer handles
    // characters
    InputStreamReader isr = new InputStreamReader(bais, "UTF8");

    // Wrap the input stream reader in a bufferred reader,
    // so you can read the character data a line at a time.
    // (A line is a sequence of chars terminated by any
    // combination of \r and \n.)
    BufferedReader br = new BufferedReader(isr);

    // The message data is contained in a single line,
    // so read this line.
    String line = br.readLine();

    return line;
  }

  /*
  * Print ping data to the standard output stream.
  */
  private static void printData(DatagramPacket request) throws Exception {
    // Obtain references to the packet's array of bytes.
    byte[] buf = request.getData();

    String line = getLineFromByteArray(buf);

    // Print host address and data received from it.
    System.out.println("Received from " +
      request.getAddress().getHostAddress() + ": " +
      new String(line));
  } // end of printData

  private static void printUsage() {
    System.out.println(
      "usage: java PingServer port passwd [-delay delay] [-loss loss]");
  }

  private static boolean processArgs(String[] args) {
    // Get command line argument.
    if (args.length < 2) {
      return false;
    }

    // Check for -delay and -loss flags.
    boolean isDelay = false;
    boolean isLoss = false;
    for (int i = 2; i < args.length; i ++) {
      if (isDelay) {
        AVERAGE_DELAY = Integer.parseInt(args[i]);
        isDelay = false;
        continue;
      }
      if (isLoss) {
        LOSS_RATE = Integer.parseInt(args[i]);
        isLoss = false;
        continue;
      }
      if (args[i].equals("-delay")) {
        isDelay = true;
      } else if (args[i].equals("-loss")) {
        isLoss = true;
      } else {
        System.out.println("invalid flag " + args[i]);
        return false;
      }
    }
    if (isDelay || isLoss) {
      return false;
    }

    PORT = Integer.parseInt(args[0]);
    PASSWD = args[1];

    return true;
  }
} // end of class