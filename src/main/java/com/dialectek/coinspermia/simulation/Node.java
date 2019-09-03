// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import com.dialectek.coinspermia.node.Ledger;
import com.dialectek.coinspermia.shared.Transaction;
import com.dialectek.coinspermia.shared.Utils;
import com.dialectek.coinspermia.simulation.TransactionTask.Type;

/**
 * Node.
 */

class Node
{
   // ID.
   int id;

   // Ledger.
   Ledger ledger;

   // Peers.
   ArrayList<Node> peers;

   // Task transit queue.
   LinkedList<TransactionTask> transitQueue;

   // Task service queue.
   LinkedList<TransactionTask> serviceQueue;
   TransactionTask             serviceTask;

   // Client task results.
   HashMap < UUID, ArrayList < TransactionTask.ClientTask >> clientResults;

   // Simulator.
   Simulator simulator;

   /**Construct the node*/
   Node(int id, Simulator simulator)
   {
      this.id        = id;
      this.simulator = simulator;
      ledger         = new Ledger();
      peers          = new ArrayList<Node>();
      transitQueue   = new LinkedList<TransactionTask>();
      serviceQueue   = new LinkedList<TransactionTask>();
      serviceTask    = null;
      clientResults  = new HashMap < UUID, ArrayList < TransactionTask.ClientTask >> ();
   }


   // Step network.
   void stepNetwork()
   {
      // Check task arrivals.
      LinkedList<TransactionTask> queue = new LinkedList<TransactionTask>();
      for (TransactionTask task : transitQueue)
      {
         if (task.transitTimer > 0)
         {
            task.transitTimer--;
            queue.add(task);
         }
         else
         {
            switch (task.type)
            {
            case CLIENT_SERVICE:
               enqueueServiceTask(task);
               break;

            case CLIENT_RESULT:
               clientResult(task);
               break;

            case PEER_SERVICE:
               enqueueServiceTask(task);
               break;
            }
         }
      }
      transitQueue = queue;
   }


   // Step node transaction service.
   void stepService()
   {
      // Servicing a task?
      if (serviceTask != null)
      {
         // Task ready?
         if (serviceTask.serviceTimer > 0)
         {
            serviceTask.serviceTimer--;
         }
         else
         {
            // Service task.
            serviceQueue.remove(serviceTask);
            TransactionTask task = serviceTask;
            serviceTask = null;

            // Matching unpaid output exists?
            Transaction.Input input = task.transaction.inputs.get(0);
            Ledger.UTXO       utxo  = ledger.utxos.get(input.publicKeyHash);
            if (utxo != null)
            {
               // Check for executed payment to same public key.
               Transaction.Output nextOutput = task.transaction.outputs.get(0);
               if (!utxo.output.id.equals(nextOutput.id))
               {
                  // Execute transaction.
                  ledger.utxos.remove(input.publicKeyHash);
                  ledger.utxos.put(Utils.hashPublicKey(nextOutput.publicKey), ledger.newUTXO(nextOutput));
                  if (task.type == Type.CLIENT_SERVICE)
                  {
                     // Client transaction result.
                     task.type = Type.CLIENT_RESULT;
                     task.clientTask.result = true;
                     if (task.clientTask.originNode == this)
                     {
                        clientResult(task);
                     }
                     else
                     {
                        // Add next layer of peers to be updated.
                        for (Node peer : peers)
                        {
                           task.clientTask.expandedPeers.add(peer);
                        }
                        task.clientTask.originNode.enqueueTransitTask(task.clone());
                     }
                  }
                  else
                  {
                     // Distribute to peers.
                     for (Node peer : peers)
                     {
                        peer.enqueueTransitTask(task.clone());
                     }
                  }
               }
            }
            else
            {
               // Retry client transaction.
               if (task.type == Type.CLIENT_SERVICE)
               {
                  if (serviceQueue.size() >= Parameters.CLIENT_TRANSACTION_RE_ENQUEUE_POSITION)
                  {
                     serviceQueue.add(Parameters.CLIENT_TRANSACTION_RE_ENQUEUE_POSITION, task);
                  }
                  else
                  {
                     serviceQueue.add(task);
                  }
                  task.serviceTimer = Parameters.TRANSACTION_SERVICE_TIME;
               }
            }
         }
      }
      else
      {
         // Choose next task to service.
         if (serviceQueue.size() > 0)
         {
            serviceTask = serviceQueue.get(0);
            serviceTask.serviceTimer = Parameters.TRANSACTION_SERVICE_TIME;
         }
      }
   }


   // Enqueue transaction task.
   void enqueueTask(TransactionTask task)
   {
      if (task.type == Type.CLIENT_SERVICE)
      {
         enqueueServiceTask(task);
         for (Node peer : peers)
         {
            peer.enqueueTransitTask(task.clone());
         }
      }
      else
      {
         enqueueServiceTask(task);
      }
   }


   // Enqueue a transaction service task.
   void enqueueServiceTask(TransactionTask task)
   {
      if (task.type == Type.CLIENT_SERVICE)
      {
         serviceQueue.addFirst(task);
      }
      else
      {
         serviceQueue.add(task);
      }
      task.serviceTimer = Parameters.TRANSACTION_SERVICE_TIME;
   }


   // Enqueue a transaction transit task.
   void enqueueTransitTask(TransactionTask task)
   {
      transitQueue.add(task);
      task.transitTimer = Parameters.TRANSACTION_TRANSIT_TIME;
   }


   // Client result.
   void clientResult(TransactionTask task)
   {
      ArrayList<TransactionTask.ClientTask> clientTasks = clientResults.get(task.clientTask.id);
      if (clientTasks == null)
      {
         clientTasks = new ArrayList<TransactionTask.ClientTask>();
      }
      clientTasks.add(task.clientTask);
      if (clientTasks.size() == peers.size() + 1)
      {
         clientResults.remove(task.clientTask.id);
         simulator.clientTransactionComplete(task);

         // Send transaction to expanded peers.
         for (TransactionTask.ClientTask clientTask : clientTasks)
         {
            for (Node peer : clientTask.expandedPeers)
            {
               if (peer != this)
               {
                  TransactionTask peerTask = new TransactionTask(task.transaction);
                  peer.enqueueTransitTask(peerTask);
               }
            }
         }
      }
      else
      {
         clientResults.put(task.clientTask.id, clientTasks);
      }
   }
}
