// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Client endpoint.
 */

package com.dialectek.coinspermia.client;

import com.dialectek.coinspermia.shared.Balance;
import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.MessageDecoder;
import com.dialectek.coinspermia.shared.MessageEncoder;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Transaction;

import java.util.ArrayList;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@javax.websocket.ClientEndpoint(encoders = MessageEncoder.class, decoders = MessageDecoder.class )
public class ClientEndpoint
{
   @OnOpen
   public void onOpen(Session session)
   {
   }


   @OnMessage
   public void onMessage(Message message, Session session)
   {
      switch (message.type)
      {
      case Message.TRANSACTION_RESPONSE:
         if (message.result == Parameters.SUCCESS)
         {
            if (Client.client != null)
            {
               if (message.transaction.type == Transaction.BALANCE)
               {
                  for (Balance balance : Client.client.wallet.balances)
                  {
                     for (Transaction.Output output : message.transaction.outputs)
                     {
                        if (balance.publicKey.hashCode() == output.publicKey.hashCode())
                        {
                           balance.coins = output.coins;
                           break;
                        }
                     }
                  }
                  Client.client.walletDisplay();
               }
               else
               {
                  Client.client.transaction   = new Transaction();
                  Client.client.inputBalances = new ArrayList<Balance>();
                  Client.client.payRefresh();
                  Client.client.statusText.setText("Success");
               }
            }
         }
         else
         {
            if (Client.client != null)
            {
               Client.client.statusText.setText("Fail");
            }
         }
         break;

      case Message.CENSUS_RESPONSE:
         if (message.result == Parameters.SUCCESS)
         {
            if (Client.client != null)
            {
               Client.client.censusText.setText("");
               for (String peer : message.peers)
               {
                  Client.client.censusText.append(peer + "\n");
               }
            }
            else
            {
               System.out.println("\nPeers:");
               for (String peer : message.peers)
               {
                  System.out.println(peer);
               }
            }
         }
         break;
      }
   }


   @OnClose
   public void onClose(Session session, CloseReason closeReason)
   {
   }
}
