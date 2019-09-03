// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Client CLI.
 */

package com.dialectek.coinspermia.client;

import java.net.URI;
import java.util.Scanner;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import org.glassfish.tyrus.client.ClientManager;

import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.Parameters;

public class ClientCLI
{
   public static void main(String[] args) throws Exception
   {
      System.out.println("Welcome to Coinspermia!");

      // Connect to node.
      Scanner scanner = new Scanner(System.in);
      String  input   = null;
      System.out.print("Enter node address to connect to (IP:port): ");
      input = scanner.nextLine();
      ClientManager client  = ClientManager.createClient();
      Session       session = client.connectToServer(ClientEndpoint.class,
                                                     new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + input + "/ws" + Parameters.URI));

      // Send messages to the node (until quit).
      Message message = new Message(Message.CENSUS_REQUEST);
      while (true)
      {
         System.out.print("Enter request (c=census, q=quit): ");
         input = scanner.nextLine();
         if (input.startsWith("q")) { break; }
         if (input.startsWith("c"))
         {
            message.type = Message.CENSUS_REQUEST;
            session.getBasicRemote().sendObject(message);
         }
      }
      session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session close"));
      scanner.close();
   }
}
