// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Transaction quorum.
 */

package com.dialectek.coinspermia.node;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.swing.Timer;
import org.glassfish.tyrus.client.ClientManager;

import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Transaction;

public class TransactionQuorum implements ActionListener
{
   // Transaction.
   public Transaction transaction;

   // Client session.
   public Session clientSession;

   // Quorum members.
   public class Member
   {
      public String address;
      public int    result, prevResult;
   }
   public ArrayList<Member> members;

   // Quorum timer.
   public Date  startTime;
   public Timer timer;

   // Logging.
   private static Logger logger = Logger.getLogger(TransactionQuorum.class .getName());

   // Constructor.
   public TransactionQuorum(Transaction transaction, Session clientSession)
   {
      members = new ArrayList<Member>();
      Member member = new Member();
      members.add(member);
      member.address     = Node.node.address;
      member.result      = member.prevResult = -1;
      this.transaction   = transaction;
      this.clientSession = clientSession;
   }


   // Start quorum.
   public void start()
   {
      // Randomly select quorum members.
      ArrayList<String> peers = new ArrayList<String>();
      synchronized (Node.node.connectedPeers)
      {
         for (String peer : Node.node.connectedPeers)
         {
            peers.add(peer);
         }
      }

      for (int i = 0; i < Node.node.peerConnectionDensity && peers.size() > 0; i++)
      {
         Member member = new Member();
         members.add(member);
         member.address = peers.get(Node.node.randomizer.nextInt(peers.size()));
         peers.remove(member.address);
         member.result = member.prevResult = -1;
      }

      // Set transaction timer.
      startTime = new Date();
      timer     = new Timer(Parameters.TRANSACTION_TIME_OUT, this);
      timer.start();

      // Lock transaction prior to commit.
      lock();
   }


   // Lock transaction.
   public void lock()
   {
      for (Member member : members)
      {
         member.result = -1;
      }
      Member member = members.get(0);
      int    result = Node.node.ledger.validate(transaction);
      if (result == Parameters.SUCCESS)
      {
         result = Node.node.ledger.lock(transaction);
      }
      if ((result == Parameters.SUCCESS) || (result == Parameters.FAIL) || (result == Parameters.LOCK_FAIL))
      {
         tallyResult(member.address, result);
      }
      else
      {
         abort(result);
      }
      if (members.size() > 1)
      {
         Message message = new Message(Message.LOCK_REQUEST);
         message.sender      = Node.node.address;
         message.transaction = transaction;
         message.password    = Node.node.password;
         ClientManager client = ClientManager.createClient();
         for (int i = 1, j = members.size(); i < j; i++)
         {
            member     = members.get(i);
            message.id = transaction.id;
            try
            {
               Session session = client.connectToServer(ClientEndpoint.class,
                                                        new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + member.address + "/ws" + Parameters.URI));
               session.getBasicRemote().sendObject(message);
            }
            catch (Exception e)
            {
               logger.severe("Cannot send transaction lock request to member " + member.address + ": " + e.getMessage());
            }
         }
      }
   }


   // Unlock transaction.
   public void unlock()
   {
      for (Member member : members)
      {
         member.result = -1;
      }
      Node.node.ledger.unlock(transaction);
      if (members.size() > 1)
      {
         Message message = new Message(Message.UNLOCK_REQUEST);
         message.sender      = Node.node.address;
         message.transaction = transaction;
         message.password    = Node.node.password;
         ClientManager client = ClientManager.createClient();
         for (int i = 1, j = members.size(); i < j; i++)
         {
            Member member = members.get(i);
            message.id = transaction.id;
            try
            {
               Session session = client.connectToServer(ClientEndpoint.class,
                                                        new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + member.address + "/ws" + Parameters.URI));
               session.getBasicRemote().sendObject(message);
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
            }
            catch (Exception e)
            {
               logger.severe("Cannot send transaction unlock request to member " + member.address + ": " + e.getMessage());
            }
         }
      }
   }


   // Lock transaction request.
   public static void lockRequest(Message message, Session session)
   {
      message.type   = Message.LOCK_RESPONSE;
      message.sender = Node.node.address;
      message.result = Node.node.ledger.validate(message.transaction);
      if (message.result == Parameters.SUCCESS)
      {
         message.result = Node.node.ledger.lock(message.transaction);
      }
      try
      {
         session.getBasicRemote().sendObject(message);
      }
      catch (Exception e)
      {
         logger.severe("Cannot send lock response, message type=" + message.type + ", session id=" + session.getId() + ": " + e.getMessage());
      }
   }


   // Lock response.
   public void lockResponse(Message message)
   {
      tallyResult(message.sender, message.result);
   }


   // Tally member lock result.
   public void tallyResult(String address, int result)
   {
      boolean complete = true;

      for (int i = 0, j = members.size(); i < j; i++)
      {
         Member member = members.get(i);
         if (member.address.equals(address))
         {
            member.result = result;
         }
         if (member.result == -1)
         {
            complete = false;
         }
      }
      if (!complete) { return; }

      // Count votes.
      int votes = 0;
      result = Parameters.SUCCESS;
      for (int i = 0, j = members.size(); i < j; i++)
      {
         Member member = members.get(i);
         switch (member.result)
         {
         case Parameters.SUCCESS:
            votes++;
            break;

         case Parameters.FAIL:
            if (result != Parameters.LOCK_FAIL)
            {
               result = member.result;
            }
            break;

         case Parameters.LOCK_FAIL:
            result = member.result;
            break;

         default:
            abort(member.result);
            return;
         }
      }

      // Commit?
      if (result == Parameters.SUCCESS)
      {
         commit();
         return;
      }
      else if (result == Parameters.FAIL)
      {
         // Force commit if failures are unchanged and majority votes in favor.
         boolean changed = false;
         for (Member member : members)
         {
            if (member.result != member.prevResult)
            {
               changed = true;
               break;
            }
         }
         if (!changed)
         {
            if (votes > members.size() / 2)
            {
               commit();
            }
            else
            {
               abort(result);
            }
            return;
         }
      }

      // Wait to retry.
      timer.stop();
      if (result == Parameters.FAIL)
      {
         for (Member member : members)
         {
            member.prevResult = member.result;
         }
      }
      else
      {
         for (Member member : members)
         {
            member.prevResult = -1;
         }
         unlock();
      }
      int wait = Node.node.randomizer.nextInt(Parameters.TRANSACTION_RETRY_MAX_WAIT - Parameters.TRANSACTION_RETRY_MIN_WAIT) +
                 Parameters.TRANSACTION_RETRY_MIN_WAIT;
      Date now = new Date();
      long t   = now.getTime() - startTime.getTime();
      if ((t + wait) >= Parameters.TRANSACTION_TIME_OUT)
      {
         abort(Parameters.TIME_OUT);
      }
      else
      {
         timer = new Timer(wait, this);
         timer.start();
      }
   }


   // Unlock transaction request.
   public static void unlockRequest(Message message)
   {
      Node.node.ledger.unlock(message.transaction);
   }


   // Commit transaction.
   public void commit()
   {
      timer.stop();
      synchronized (Node.node.transactionQuorums)
      {
         Node.node.transactionQuorums.remove(transaction.id);
      }
      unlock();
      int result = Node.node.ledger.commit(transaction);
      if (result == Parameters.SUCCESS)
      {
         // Propagate commit.
         ArrayList<String> peers = new ArrayList<String>();
         synchronized (Node.node.connectedPeers)
         {
            for (String peer : Node.node.connectedPeers)
            {
               peers.add(peer);
            }
         }
         Message message = new Message(Message.COMMIT_REQUEST);
         message.sender      = Node.node.address;
         message.transaction = transaction;
         message.password    = Node.node.password;
         ClientManager client = ClientManager.createClient();
         for (int i = 0, j = peers.size(); i < j; i++)
         {
            String peer = peers.get(i);
            try
            {
               Session session = client.connectToServer(ClientEndpoint.class,
                                                        new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + peer + "/ws" + Parameters.URI));
               session.getBasicRemote().sendObject(message);
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
            }
            catch (Exception e)
            {
               logger.severe("Cannot propagate transaction commit to peer " + peer + ": " + e.getMessage());
            }
         }
      }

      // Respond to client.
      Message message = new Message(Message.TRANSACTION_RESPONSE);
      message.sender      = Node.node.address;
      message.transaction = transaction;
      message.result      = result;
      try
      {
         clientSession.getBasicRemote().sendObject(message);
      }
      catch (Exception e)
      {
         logger.severe("Cannot send transaction result to client, session id=" + clientSession.getId() + ": " + e.getMessage());
      }
   }


   // Abort transaction.
   public void abort(int result)
   {
      timer.stop();
      synchronized (Node.node.transactionQuorums)
      {
         Node.node.transactionQuorums.remove(transaction.id);
      }
      unlock();
      Message message = new Message(Message.TRANSACTION_RESPONSE);
      message.sender      = Node.node.address;
      message.transaction = transaction;
      message.result      = result;
      try
      {
         clientSession.getBasicRemote().sendObject(message);
      }
      catch (Exception e)
      {
         logger.severe("Cannot send transaction result to client, session id=" + clientSession.getId() + ": " + e.getMessage());
      }
   }


   // Timer expiration.
   public void actionPerformed(ActionEvent arg)
   {
      timer.stop();
      Date now = new Date();
      long t   = now.getTime() - startTime.getTime();
      if (t >= Parameters.TRANSACTION_TIME_OUT)
      {
         abort(Parameters.TIME_OUT);
      }
      else
      {
         timer = new Timer(Parameters.TRANSACTION_TIME_OUT - (int)t, this);
         timer.start();
         lock();
      }
   }
}
