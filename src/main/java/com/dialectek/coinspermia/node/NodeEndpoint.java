// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Node endpoint.
 */

package com.dialectek.coinspermia.node;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.CloseReason.CloseCodes;

import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.MessageDecoder;
import com.dialectek.coinspermia.shared.MessageEncoder;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Transaction;

@javax.websocket.server.ServerEndpoint(value = Parameters.URI, encoders = MessageEncoder.class, decoders = MessageDecoder.class )
public class NodeEndpoint
{
   private Logger             logger   = Logger.getLogger(this.getClass().getName());
   public static Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

   @OnOpen
   public void onOpen(Session session)
   {
      sessions.add(session);
   }


   @OnMessage
   public void onMessage(Message message, Session session)
   {
      // Check authorization.
      if (!message.authorized())
      {
         String sender = message.sender;
         if (sender == null) { message.sender = "null"; }
         logger.severe("Unauthorized message request, message type=" + message.type + ", sender=" + sender + ", session id=" + session.getId());
         return;
      }

      switch (message.type)
      {
      case Message.TRANSACTION_REQUEST:
         Transaction transaction = message.transaction;
         boolean txError = false;
         if ((transaction == null) || !transaction.validFormat())
         {
            txError = true;
         }
         else if (transaction.type == Transaction.MINT)
         {
            if (!Node.node.validPassword(message.password))
            {
               txError = true;
            }
         }
         if (!txError)
         {
            if (transaction.type == Transaction.BALANCE)
            {
               for (Transaction.Output output : message.transaction.outputs)
               {
                  output.coins = 0.0f;
                  for (Ledger.UTXO utxo : Node.node.ledger.utxos.values())
                  {
                     if (output.publicKey.hashCode() == utxo.output.publicKey.hashCode())
                     {
                        output.coins += utxo.output.coins;
                     }
                  }
               }
               message.type   = Message.TRANSACTION_RESPONSE;
               message.sender = Node.node.address;
               message.result = Parameters.SUCCESS;
               try
               {
                  session.getBasicRemote().sendObject(message);
               }
               catch (Exception e)
               {
                  logger.severe("Cannot send balance response, session id=" + session.getId() + ": " + e.getMessage());
               }
            }
            else
            {
               // Start transaction quorum.
               UUID id = message.transaction.id;
               txError = true;
               synchronized (Node.node.transactionQuorums)
               {
                  if (!Node.node.transactionQuorums.containsKey(id))
                  {
                     TransactionQuorum tq = new TransactionQuorum(message.transaction, session);
                     Node.node.transactionQuorums.put(id, tq);
                     try
                     {
                        tq.start();
                        txError = false;
                     }
                     catch (Exception e)
                     {
                        Node.node.transactionQuorums.remove(id);
                        logger.severe("Cannot start transaction quorum, transaction id=" + message.transaction.id + ", session id=" + session.getId() + ": " + e.getMessage());
                     }
                  }
                  else
                  {
                     logger.severe("Cannot start transaction quorum, duplicate transaction id=" + message.transaction.id + ", session id=" + session.getId());
                  }
               }
            }
         }
         if (txError)
         {
            message.type   = Message.TRANSACTION_RESPONSE;
            message.sender = Node.node.address;
            message.result = Parameters.FAIL;
            try
            {
               session.getBasicRemote().sendObject(message);
            }
            catch (Exception e)
            {
               logger.severe("Cannot send transaction response, session id=" + session.getId() + ": " + e.getMessage());
            }
         }
         break;

      case Message.LOCK_REQUEST:
         if (Node.node.knownPeer(message.sender))
         {
            TransactionQuorum.lockRequest(message, session);
         }
         else
         {
            String sender = message.sender;
            if (sender == null) { sender = "null"; }
            logger.warning("Invalid lock request from unknown sender address " + sender + ", session id=" + session.getId());
            message.type   = Message.LOCK_RESPONSE;
            message.sender = Node.node.address;
            message.result = Parameters.INVALID;
            try
            {
               session.getBasicRemote().sendObject(message);
            }
            catch (Exception e)
            {
               logger.severe("Cannot send lock response, session id=" + session.getId() + ": " + e.getMessage());
            }
         }
         break;

      case Message.UNLOCK_REQUEST:
         if (Node.node.knownPeer(message.sender))
         {
            TransactionQuorum.unlockRequest(message);
         }
         break;

      case Message.COMMIT_REQUEST:
         if (Node.node.knownPeer(message.sender))
         {
            if (Node.node.ledger.commit(message.transaction) == Parameters.SUCCESS)
            {
               ArrayList<String> peers = new ArrayList<String>();
               synchronized (Node.node.connectedPeers)
               {
                  for (String peer : Node.node.connectedPeers)
                  {
                     peers.add(peer);
                  }
               }
               message.sender = Node.node.address;
               ClientManager client = ClientManager.createClient();
               for (int i = 0, j = peers.size(); i < j; i++)
               {
                  String peer = peers.get(i);
                  try
                  {
                     Session targetSession = client.connectToServer(ClientEndpoint.class,
                                                                    new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + peer + "/ws" + Parameters.URI));
                     targetSession.getBasicRemote().sendObject(message);
                     targetSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
                  }
                  catch (Exception e)
                  {
                     logger.severe("Cannot propagate transaction to peer " + peer + ", session id=" + session.getId() + ": " + e.getMessage());
                  }
               }
            }
         }
         else
         {
            String sender = message.sender;
            if (sender == null) { sender = "null"; }
            logger.warning("Invalid commit request from unknown sender address " + sender + ", session id=" + session.getId());
         }
         break;

      case Message.CONNECTION_REQUEST:
         synchronized (Node.node.connectedPeers)
         {
            if (Node.node.addPeer(message.sender))
            {
               Node.node.calculatePeerConnectionDensity();
            }
            Object[] peerArray = Node.node.connectedPeers.toArray();
            ArrayList<String> peers = new ArrayList<String>();
            for (Object peer : peerArray)
            {
               peers.add((String)peer);
            }
            peers.add(Node.node.address);
            message.peers = peers;
         }
         message.type   = Message.CONNECTION_RESPONSE;
         message.sender = Node.node.address;
         try
         {
            session.getBasicRemote().sendObject(message);
         }
         catch (Exception e)
         {
            logger.severe("Cannot send connection response, session id=" + session.getId() + ": " + e.getMessage());
         }
         break;

      case Message.CENSUS_REQUEST:
         synchronized (Node.node.connectedPeers)
         {
            Object[] peerArray = Node.node.connectedPeers.toArray();
            ArrayList<String> peers = new ArrayList<String>();
            for (Object peer : peerArray)
            {
               peers.add((String)peer);
            }
            peers.add(Node.node.address);
            message.peers = peers;
         }
         message.type   = Message.CENSUS_RESPONSE;
         message.sender = Node.node.address;
         try
         {
            session.getBasicRemote().sendObject(message);
         }
         catch (Exception e)
         {
            logger.severe("Cannot send census response, session id=" + session.getId() + ": " + e.getMessage());
         }
         break;

      case Message.LOAD_LEDGER_REQUEST:
         Node.node.ledger.load(Parameters.LEDGER_FILE);
         break;

      case Message.SAVE_LEDGER_REQUEST:
         Node.node.ledger.save(Parameters.LEDGER_FILE);
         break;

      case Message.CLEAR_LEDGER_REQUEST:
         Node.node.ledger.clear();
         break;

      default:
         logger.warning("Unknown message type=" + message.type + ", session id=" + session.getId());
      }
   }


   @OnClose
   public void onClose(Session session, CloseReason closeReason)
   {
      sessions.remove(session);
   }
}
