// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.util.ArrayList;
import java.util.UUID;

import com.dialectek.coinspermia.shared.Transaction;

/**
 * Transaction task.
 */
class TransactionTask
{
   // Type.
   public enum Type
   {
      CLIENT_SERVICE,
      CLIENT_RESULT,
      PEER_SERVICE
   }
   Type type;

   // Transaction.
   Transaction transaction;

   // Transaction timers.
   int transitTimer;
   int serviceTimer;

   // Client task.
   class ClientTask
   {
      UUID            id;
      int             stepNum;
      Client          clientFrom;
      Client          clientTo;
      Node            originNode;
      boolean         result;
      ArrayList<Node> expandedPeers;

      // Constructor.
      ClientTask(int stepNum, Client clientFrom, Client clientTo, Node originNode)
      {
         id              = UUID.randomUUID();
         this.stepNum    = stepNum;
         this.clientFrom = clientFrom;
         this.clientTo   = clientTo;
         this.originNode = originNode;
         this.result     = true;
         expandedPeers   = new ArrayList<Node>();
      }


      // Clone.
      public ClientTask clone()
      {
         ClientTask task = new ClientTask(stepNum, clientFrom, clientTo, originNode);

         task.id     = id;
         task.result = result;
         for (Node peer : expandedPeers)
         {
            task.expandedPeers.add(peer);
         }
         return(task);
      }
   }
   ClientTask clientTask;

   // Client service constructor.
   TransactionTask(Transaction transaction, int stepNum, Client from, Client to, Node originNode)
   {
      type             = Type.CLIENT_SERVICE;
      this.transaction = transaction;
      transitTimer     = 0;
      serviceTimer     = 0;
      clientTask       = new ClientTask(stepNum, from, to, originNode);
   }


   // Client result constructor.
   TransactionTask(TransactionTask task, boolean result)
   {
      type              = Type.CLIENT_RESULT;
      transaction       = task.transaction;
      transitTimer      = 0;
      serviceTimer      = 0;
      clientTask        = task.clientTask.clone();
      clientTask.result = result;
   }


   // Peer-to-peer constructor.
   TransactionTask(Transaction transaction)
   {
      type             = Type.PEER_SERVICE;
      this.transaction = transaction;
      transitTimer     = 0;
      serviceTimer     = 0;
      clientTask       = null;
   }


   // Clone task.
   public TransactionTask clone()
   {
      TransactionTask task = new TransactionTask(transaction);

      task.type = type;
      if (clientTask != null)
      {
         task.clientTask = clientTask.clone();
      }
      return(task);
   }
}
