// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Node.
 */

package com.dialectek.coinspermia.node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;

import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Utils;

public class Node
{
   // Options.
   public static final String Options = "Options:\n\t[-port <port> (defaults to " + Parameters.DEFAULT_PORT + ")]\n\t[-bootstrapPeer <address of peer to connect to> (repeatable)]\n\t[-bootstrapPeerFile <file containing peer addresses> (defaults to " + Parameters.DEFAULT_BOOTSTRAP_PEER_FILE + ")]\n\t[-maxPeerConnections <maximum number of peer connections> (defaults to " + Parameters.DEFAULT_MAX_PEER_CONNECTIONS + ")]\n\t[-password <password> (use password authorization mode; password alternatively read from " + Parameters.PASSWORD_FILE + ")]\n\t[-randomSeed <random number seed> (defaults to " + Parameters.DEFAULT_RANDOM_SEED + ")]\n\t[-logfile <log file name> (defaults to " + Parameters.DEFAULT_LOG_FILE + ") | \"none\"]";

   // Network address and port.
   public String address = "localhost:" + Parameters.DEFAULT_PORT;
   public int    port    = Parameters.DEFAULT_PORT;

   // Peers.
   public Set<String> connectedPeers = Collections.synchronizedSet(new HashSet<String>());

   // Maximum peer connections.
   public int maxPeerConnections = Parameters.DEFAULT_MAX_PEER_CONNECTIONS;

   // Peer connection density.
   public int peerConnectionDensity = Parameters.MIN_PEER_CONNECTIONS;

   // Endpoint server.
   public Server endpoint;

   // Ledger.
   public Ledger ledger;

   // Transaction quorums.
   public Map<UUID, TransactionQuorum> transactionQuorums = Collections.synchronizedMap(new HashMap<UUID, TransactionQuorum>());

   // Password.
   public String password;

   // Random numbers.
   public int    randomSeed = Parameters.DEFAULT_RANDOM_SEED;
   public Random randomizer = new Random(randomSeed);

   // Singleton.
   public static Node node = null;

   // Logging.
   private Logger logger = Logger.getLogger(this.getClass().getName());

   // Constructor.
   public Node(int port, int maxPeerConnections,
               ArrayList<String> bootstrapPeers, String password, int randomSeed) throws Exception
   {
      // Check for singleton.
      if (node != null)
      {
         throw new Exception("Node must be run as a singleton");
      }
      node = this;

      this.port = port;
      this.maxPeerConnections = maxPeerConnections;
      this.password           = password;
      if (password == null)
      {
         logger.info("Password authorization disabled");
      }
      else
      {
         logger.info("Password authorization enabled");
      }
      this.randomSeed = randomSeed;

      // Get network address.
      String localhost = null;
      try
      {
         localhost = Utils.getLocalAddress();
         address   = localhost + ":" + port;
      }
      catch (Exception e)
      {
         throw new Exception("Cannot get local address: " + e.getMessage());
      }

      // Add peers.
      for (String peer : bootstrapPeers)
      {
         if (peer.startsWith("localhost:") || peer.startsWith("127.0.0.1"))
         {
            String[] peerParts = peer.split(":");
            peer = localhost + ":" + peerParts[1];
         }
         if (!peer.equals(address))
         {
            connectedPeers.add(peer);
         }
      }

      // Random number generator.
      randomizer = new Random(randomSeed);

      // Create ledger.
      ledger = new Ledger();
      ledger.load(Parameters.LEDGER_FILE);
   }


   // Run node.
   public void run() throws Exception
   {
      // Start endpoint.
      endpoint = new Server("localhost", port, "/ws", NodeEndpoint.class );
      endpoint.start();

      // Create client.
      ClientManager nodeClient = ClientManager.createClient();

      // Periodically refresh peers.
      while (true)
      {
         String refreshPeer = null;
         synchronized (connectedPeers)
         {
            if (connectedPeers.size() > 0)
            {
               Object[] peers = connectedPeers.toArray();
               refreshPeer    = (String)peers[randomizer.nextInt(peers.length)];
            }
         }
         if (refreshPeer != null)
         {
            Message message = new Message(Message.CONNECTION_REQUEST);
            message.sender   = address;
            message.password = password;
            try
            {
               Session session = nodeClient.connectToServer(ClientEndpoint.class,
                                                            new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + refreshPeer + "/ws" + Parameters.URI));
               session.getBasicRemote().sendObject(message);
            }
            catch (Exception e)
            {
               logger.warning("Cannot connect to peer " + refreshPeer + ": " + e.getMessage());
               synchronized (connectedPeers)
               {
                  connectedPeers.remove(refreshPeer);
               }
            }
         }

         try
         {
            Thread.sleep(Parameters.PEER_REFRESH_FREQ);
         }
         catch (InterruptedException e) { break; }
      }
   }


   // Add peer to connections.
   public boolean addPeer(String peer)
   {
      if ((peer != null) && !peer.equals(address))
      {
         synchronized (connectedPeers)
         {
            if (!connectedPeers.contains(peer))
            {
               connectedPeers.add(peer);
               return(true);
            }
         }
      }
      return(false);
   }


   // Is this peer a known connection?
   public boolean knownPeer(String peer)
   {
      if ((peer == null) || peer.equals(address))
      {
         return(false);
      }
      synchronized (connectedPeers)
      {
         if (connectedPeers.contains(peer))
         {
            return(true);
         }
      }
      return(false);
   }


   // Valid password?
   public boolean validPassword(String password)
   {
      if (this.password == null) { return(true); }
      if (password == null) { return(false); }
      if (this.password.equals(password)) { return(true); }
      return(false);
   }


   // Calculate peer connection density.
   public void calculatePeerConnectionDensity()
   {
      int n = connectedPeers.size() + 1;

      for (peerConnectionDensity = 0; peerConnectionDensity < n; peerConnectionDensity++)
      {
         float p = Utils.intersectionProbability(n, peerConnectionDensity + 1, peerConnectionDensity + 1);
         if (p >= Parameters.TRANSACTION_INTERSECTION_PROBABILITY)
         {
            break;
         }
      }
      if (peerConnectionDensity < Parameters.MIN_PEER_CONNECTIONS)
      {
         peerConnectionDensity = Parameters.MIN_PEER_CONNECTIONS;
      }
   }


   public static void main(String[] args)
   {
      ArrayList<String> bootstrapPeers    = new ArrayList<String>();
      String            bootstrapPeerFile = Parameters.DEFAULT_BOOTSTRAP_PEER_FILE;

      // Get arguments.
      int    port = Parameters.DEFAULT_PORT;
      int    maxPeerConnections = Parameters.MIN_PEER_CONNECTIONS;
      String password           = null;
      int    randomSeed         = Parameters.DEFAULT_RANDOM_SEED;
      String logfile            = Parameters.DEFAULT_LOG_FILE;
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-port"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            port = Integer.parseInt(args[i]);
            if (port < 0)
            {
               System.err.println("Invalid port " + port);
               return;
            }
         }
         else if (args[i].equals("-bootstrapPeer"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            bootstrapPeers.add(args[i]);
         }
         else if (args[i].equals("-bootstrapPeerFile"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            bootstrapPeerFile = args[i];
            if (!new File(bootstrapPeerFile).exists())
            {
               System.err.println("bootstrap peer file " + bootstrapPeerFile + " does not exist");
               return;
            }
         }
         else if (args[i].equals("-maxPeerConnections"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            maxPeerConnections = Integer.parseInt(args[i]);
         }
         else if (args[i].equals("-password"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            password = args[i];
         }
         else if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            randomSeed = Integer.parseInt(args[i]);
         }
         else if (args[i].equals("-logfile"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Options);
               return;
            }
            logfile = args[i];
         }
         else
         {
            System.err.println(Options);
            return;
         }
      }

      // Read bootstrap peers file.
      BufferedReader in = null;
      try
      {
         in = new BufferedReader(new FileReader(bootstrapPeerFile));
         String peerAddress = null;
         while ((peerAddress = in.readLine()) != null)
         {
            String[] parts = peerAddress.split(":");
            if (parts.length == 1)
            {
               peerAddress = peerAddress + ":" + Parameters.DEFAULT_PORT;
            }
            bootstrapPeers.add(peerAddress);
         }
      }
      catch (Exception e)
      {
         if (in != null)
         {
            System.err.println("Error reading bootstrap peers from file " + bootstrapPeerFile + ": " + e.getMessage());
            return;
         }
      }
      finally
      {
         try
         {
            if (in != null)
            {
               in.close();
            }
         }
         catch (Exception e) {}
      }

      // Read password file?
      if ((password == null) && new File(Parameters.PASSWORD_FILE).exists())
      {
         in = null;
         try
         {
            in       = new BufferedReader(new FileReader(Parameters.PASSWORD_FILE));
            password = in.readLine();
            if (password == null)
            {
               System.err.println("Invalid password in file " + Parameters.PASSWORD_FILE);
               return;
            }
         }
         catch (Exception e)
         {
            if (in != null)
            {
               System.err.println("Error reading password from file " + Parameters.PASSWORD_FILE + ": " + e.getMessage());
               return;
            }
         }
         finally
         {
            try
            {
               if (in != null)
               {
                  in.close();
               }
            }
            catch (Exception e) {}
         }
      }

      // Logfile output.
      if (!logfile.equals("none"))
      {
         try
         {
            Handler fh = new FileHandler(logfile);
            Logger.getLogger("").addHandler(fh);
         }
         catch (Exception e)
         {
            System.err.println("Warning: cannot configure logging output to " + logfile);
         }
      }

      // Create and run node.
      try
      {
         Node node = new Node(port, maxPeerConnections, bootstrapPeers, password, randomSeed);
         node.run();
      }
      catch (Exception e)
      {
         System.err.println("Node failure: " + e.getMessage());
      }
   }
}
