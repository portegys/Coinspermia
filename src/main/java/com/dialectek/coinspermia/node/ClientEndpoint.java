// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Node client endpoint.
 */

package com.dialectek.coinspermia.node;

import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.MessageDecoder;
import com.dialectek.coinspermia.shared.MessageEncoder;
import com.dialectek.coinspermia.shared.Parameters;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import java.util.logging.Logger;

@javax.websocket.ClientEndpoint(encoders = MessageEncoder.class, decoders = MessageDecoder.class )
public class ClientEndpoint
{
   private Logger logger = Logger.getLogger(this.getClass().getName());

   @OnOpen
   public void onOpen(Session session)
   {
   }


   @OnMessage
   public void onMessage(Message message, Session session)
   {
      switch (message.type)
      {
      case Message.LOCK_RESPONSE:
         if (Node.node.validPassword(message.password))
         {
            TransactionQuorum tq = null;
            synchronized (Node.node.transactionQuorums)
            {
               tq = Node.node.transactionQuorums.get(message.id);
            }
            if (tq != null) { tq.lockResponse(message); }
            try
            {
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
            }
            catch (Exception e)
            {
               logger.severe("Cannot close session, message type=" + message.type + ", session id=" + session.getId() + ": " + e.getMessage());
            }
         }
         else
         {
            String sender = message.sender;
            if (sender == null) { message.sender = "null"; }
            logger.severe("Unauthorized message response, message type=" + message.type + ", sender=" + sender + ", session id=" + session.getId());
         }
         break;

      case Message.CONNECTION_RESPONSE:
         if (Node.node.validPassword(message.password))
         {
            if (message.result == Parameters.SUCCESS)
            {
               synchronized (Node.node.connectedPeers)
               {
                  for (String peer : message.peers)
                  {
                     Node.node.addPeer(peer);
                  }
                  Node.node.calculatePeerConnectionDensity();
               }
            }
            try
            {
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
            }
            catch (Exception e)
            {
               logger.severe("Cannot close session, message type=" + message.type + ", session id=" + session.getId() + ": " + e.getMessage());
            }
         }
         else
         {
            String sender = message.sender;
            if (sender == null) { message.sender = "null"; }
            logger.severe("Unauthorized message response, message type=" + message.type + ", sender=" + sender + ", session id=" + session.getId());
         }
         break;

      default:
         logger.warning("Unknown message type=" + message.type + ", session id=" + session.getId());
      }
   }


   @OnClose
   public void onClose(Session session, CloseReason closeReason)
   {
   }
}
