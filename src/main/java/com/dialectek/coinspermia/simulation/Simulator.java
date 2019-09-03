// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import com.dialectek.coinspermia.shared.Balance;
import com.dialectek.coinspermia.shared.Transaction;
import com.dialectek.coinspermia.shared.Utils;

/**
 * Coinspermia cryptocurrency simulator.
 */

public class Simulator
{
   // Log file name.
   public static String DEFAULT_LOGFILE = "coinspermia_simulation.log";

   /** Usage. */
   public static final String Usage =
      "Usage: java com.dialectek.coinspermia.simulation.Simulator"
      + "\n\t-steps <steps>"
      + "\n\t[-numNodes <number of nodes>]"
      + "\n\t[-connectionDensity <number of peer connections per node> (fraction of numNodes)]"
      + "\n\t[-numClients <number of clients>]"
      + "\n\t[-transactionOriginationRate <mean time between transaction originations>]"
      + "\n\t[-clientTransactionReEnqueuePosition <client transaction service re-enqueue position>]"
      + "\n\t[-transactionServiceTime <time>]"
      + "\n\t[-transactionTransitTime <time>]"
      + "\n\t[-mintedCoins <number of coins distributed to clients>]"
      + "\n\t[-logfile <log file name> (defaults to " + DEFAULT_LOGFILE + ")]"
      + "\n\t[-randomSeed <random seed>]";

   /** Nodes. */
   ArrayList<Node> nodes;

   /** Clients. */
   ArrayList<Client> clients;

   // Stats.
   int stepNum;
   int txStarted;
   int txComplete;
   int txLatencyAccum;

   // Random numbers.
   Random randomizer;

   private Logger logger = Logger.getLogger(this.getClass().getName());

   /** Construct the application*/
   public Simulator(int randomSeed)
   {
      // Random numbers.
      randomizer = new Random(randomSeed);

      // Construct nodes.
      nodes = new ArrayList<Node>();
      for (int i = 0; i < Parameters.NUM_NODES; i++)
      {
         nodes.add(new Node(i, this));
      }
      int n = (int)((float)Parameters.NUM_NODES * Parameters.CONNECTION_DENSITY);
      if (n >= Parameters.NUM_NODES)
      {
         System.err.println("Cannot create network: excessive connection density");
         System.exit(1);
      }
      int t     = 0;
      int tries = 10;
      for ( ; t < tries; t++)
      {
         for (Node node : nodes)
         {
            node.peers.clear();
         }
         for (Node node : nodes)
         {
            ArrayList<Node> candidates = new ArrayList<Node>();
            for (int i = 0; i < Parameters.NUM_NODES; i++)
            {
               if (nodes.get(i) != node)
               {
                  candidates.add(nodes.get(i));
               }
            }
            for (int i = 0; i < n; i++)
            {
               int j = randomizer.nextInt(candidates.size());
               node.peers.add(candidates.get(j));
               candidates.remove(j);
            }
         }
         if (nodesConnected()) { break; }
      }
      if (t == tries)
      {
         System.err.println("Network not fully connected");
         System.exit(1);
      }

      // Construct clients.
      clients = new ArrayList<Client>();
      for (int i = 0; i < Parameters.NUM_CLIENTS; i++)
      {
         clients.add(new Client(i, this));
      }

      // Mint coins.
      if (Parameters.NUM_CLIENTS > 0)
      {
         for (int i = 0; i < Parameters.MINTED_COINS; i++)
         {
            Client client = clients.get(randomizer.nextInt(Parameters.NUM_CLIENTS));
            try
            {
               KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
               kpg.initialize(512);
               KeyPair pair    = kpg.generateKeyPair();
               Balance balance = new Balance(pair.getPublic(), pair.getPrivate(), 1.0f);
               client.wallet.add(balance);
               for (Node node : nodes)
               {
                  Transaction transaction = new Transaction();
                  transaction.type = Transaction.MINT;
                  transaction.addOutput(balance.publicKey, balance.coins);
                  Transaction.Output output = transaction.outputs.get(0);
                  node.ledger.utxos.put(balance.publicKeyHash, node.ledger.newUTXO(output));
               }
            }
            catch (NoSuchAlgorithmException e)
            {
               logger.severe("Cannot mint coins: " + e.getMessage());
            }
         }
      }

      // Clear stats.
      stepNum        = -1;
      txStarted      = 0;
      txComplete     = 0;
      txLatencyAccum = 0;
   }


   // Run simulation.
   void run(int steps)
   {
      for (int i = 0; i < steps; i++)
      {
         stepNum = i;

         // Log stats.
         logStats();

         // Step clients.
         for (Client client : clients)
         {
            client.step();
         }

         // Step network.
         for (Node node : nodes)
         {
            node.stepNetwork();
         }

         // Step node services.
         for (Node node : nodes)
         {
            node.stepService();
         }
      }
   }


   // Create client transaction task.
   void createClientTransaction(Client clientFrom)
   {
      Client clientTo = clients.get(randomizer.nextInt(Parameters.NUM_CLIENTS));
      int    n        = clientFrom.wallet.balances.size();

      if ((n > 0) && (clientTo.wallet.balances.size() > 0))
      {
         int     k           = randomizer.nextInt(n);
         Balance balanceFrom = clientFrom.wallet.balances.get(k);
         for (int i = 0; i < n && balanceFrom.coins == 0.0f; i++)
         {
            k           = (k + 1) % n;
            balanceFrom = clientFrom.wallet.balances.get(k);
         }
         if (balanceFrom.coins > 0.0f)
         {
            Transaction transaction = new Transaction();
            transaction.addInput(balanceFrom.publicKeyHash,
                                 Utils.signMessage(balanceFrom.privateKey, Utils.intToBytes(balanceFrom.publicKeyHash)));
            n = clientTo.wallet.balances.size();
            Balance balanceTo = clientTo.wallet.balances.get(randomizer.nextInt(n));
            transaction.addOutput(balanceTo.publicKey, 1.0f);
            Node            node = nodes.get(randomizer.nextInt(nodes.size()));
            TransactionTask task = new TransactionTask(transaction, stepNum, clientFrom, clientTo, node);
            node.enqueueTask(task);
            clientFrom.transactionActive = true;
            txStarted++;
            Log.getLog().logInformation("Client " + clientFrom.id + "->" + clientTo.id +
                                        " transaction " + transaction.id + " started at time " + stepNum +
                                        " utxo hit ratio = " + utxoHits(node, transaction) +
                                        "/" + (node.peers.size() + 1));
         }
      }
   }


   // Get utxo transaction hits.
   int utxoHits(Node node, Transaction transaction)
   {
      int n = 0;

      Transaction.Input input = transaction.inputs.get(0);
      if (node.ledger.utxos.get(input.publicKeyHash) != null) { n++; }
      for (Node peer : node.peers)
      {
         if (peer.ledger.utxos.get(input.publicKeyHash) != null) { n++; }
      }
      return(n);
   }


   // Client transaction task completion.
   void clientTransactionComplete(TransactionTask task)
   {
      // Transfer money to recipient.
      Transaction transaction = task.transaction;

      Transaction.Input  input         = transaction.inputs.get(0);
      int                publicKeyHash = input.publicKeyHash;
      Transaction.Output output        = transaction.outputs.get(0);
      PublicKey          publicKey     = output.publicKey;
      Client             clientFrom    = task.clientTask.clientFrom;
      Client             clientTo      = task.clientTask.clientTo;
      for (Balance balance : clientFrom.wallet.balances)
      {
         if (balance.publicKeyHash == publicKeyHash)
         {
            balance.coins -= output.coins;
            break;
         }
      }
      for (Balance balance : clientTo.wallet.balances)
      {
         if (balance.publicKey.hashCode() == publicKey.hashCode())
         {
            balance.coins += output.coins;
            break;
         }
      }
      clientFrom.transactionActive = false;
      txComplete++;
      int latency = (stepNum - task.clientTask.stepNum);
      txLatencyAccum += latency;
      Log.getLog().logInformation("Client " + clientFrom.id + "->" + clientTo.id +
                                  " transaction " + transaction.id + " completed at time " + stepNum +
                                  " latency = " + latency);
   }


   // log stats.
   void logStats()
   {
      if (stepNum == 0)
      {
         Log.getLog().logInformation("Parameters:");
         Parameters.print();
         int n = (int)((float)Parameters.NUM_NODES * Parameters.CONNECTION_DENSITY);
         Log.getLog().logInformation("Total network connections = " + (Parameters.NUM_NODES * n));
         Log.getLog().logInformation("Transaction intersection probability = " + Utils.intersectionProbability(Parameters.NUM_NODES, n + 1, n + 1));
         Log.getLog().logInformation("step,tx_started,tx_complete,tx_latency,tx_queue_length");
      }
      float length = 0.0f;
      for (Node node : nodes)
      {
         length += (float)node.serviceQueue.size();
      }
      if (nodes.size() > 0) { length /= (float)nodes.size(); }
      float latency = 0.0f;
      if (txComplete > 0) { latency = (float)txLatencyAccum / (float)txComplete; }
      Log.getLog().logInformation(stepNum + "," + txStarted + "," + txComplete + "," + latency + "," + length);
   }


   // Network fully connected?
   boolean nodesConnected()
   {
      for (int i = 0, j = nodes.size(); i < j; i++)
      {
         boolean[] marks = new boolean[j];
         for (int k = 0; k < j; k++)
         {
            marks[k] = false;
         }
         peerSearch(i, marks);
         for (int k = 0; k < j; k++)
         {
            if (!marks[k]) { return(false); }
         }
      }
      return(true);
   }


   void peerSearch(int index, boolean[] marks)
   {
      if (marks[index]) { return; }
      marks[index] = true;
      Node node = nodes.get(index);
      for (Node peer : node.peers)
      {
         peerSearch(peer.id, marks);
      }
   }


   /** Main */
   public static void main(String[] args)
   {
      int    steps   = -1;
      String logfile = DEFAULT_LOGFILE;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-steps"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            steps = Integer.parseInt(args[i].trim());
         }
         else if (args[i].equals("-numNodes"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.NUM_NODES = Integer.parseInt(args[i].trim());
            if (Parameters.NUM_NODES < 0)
            {
               System.err.println("Invalid number of nodes");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-connectionDensity"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.CONNECTION_DENSITY = Float.parseFloat(args[i].trim());
            if (Parameters.CONNECTION_DENSITY < 0.0f)
            {
               System.err.println("Invalid connection density");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-numClients"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.NUM_CLIENTS = Integer.parseInt(args[i].trim());
            if (Parameters.NUM_CLIENTS < 0)
            {
               System.err.println("Invalid number of clients");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-transactionOriginationRate"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.TRANSACTION_ORIGINATION_RATE = Integer.parseInt(args[i].trim());
            if (Parameters.TRANSACTION_ORIGINATION_RATE < 0)
            {
               System.err.println("Invalid transaction origination rate");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-clientTransactionReEnqueuePosition"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.CLIENT_TRANSACTION_RE_ENQUEUE_POSITION = Integer.parseInt(args[i].trim());
            if (Parameters.CLIENT_TRANSACTION_RE_ENQUEUE_POSITION < 0)
            {
               System.err.println("Invalid client transaction service re-enqueue position");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-transactionServiceTime"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.TRANSACTION_SERVICE_TIME = Integer.parseInt(args[i].trim());
            if (Parameters.TRANSACTION_SERVICE_TIME < 0)
            {
               System.err.println("Invalid transaction service time");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-transactionTransitTime"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.TRANSACTION_TRANSIT_TIME = Integer.parseInt(args[i].trim());
            if (Parameters.TRANSACTION_TRANSIT_TIME < 0)
            {
               System.err.println("Invalid transaction transit time");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-mintedCoins"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.MINTED_COINS = Integer.parseInt(args[i].trim());
            if (Parameters.MINTED_COINS < 0)
            {
               System.err.println("Invalid number of minted coins");
               System.err.println(Usage);
               return;
            }
         }
         else if (args[i].equals("-logfile"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            logfile = args[i].trim();
         }
         else if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(Usage);
               return;
            }
            Parameters.RANDOM_SEED = Integer.parseInt(args[i].trim());
         }
         else
         {
            System.out.println(Usage);
            return;
         }
      }

      // Logging.
      Log.LOGGING_FLAG = new String("CoinspermiaSimulation.Log");
      Log.LOG_FILE     = new String(logfile);
      System.setProperty("CoinspermiaSimulation.Log", "true"); // "true" for logging.

      // Run simulation.
      Simulator simulator = new Simulator(Parameters.RANDOM_SEED);
      simulator.run(steps);
   }
}
