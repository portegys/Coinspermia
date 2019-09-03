// For conditions of distribution and use, see copyright notice in Readme.

package com.dialectek.coinspermia.shared;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Message>
{
   @Override
   public void init(final EndpointConfig config)
   {
   }


   @Override
   public void destroy()
   {
   }


   @Override
   public String encode(final Message message) throws EncodeException
   {
      return(toJson(message));
   }


   public static String toJson(final Message message) throws EncodeException
   {
      JsonObjectBuilder builder = Json.createObjectBuilder().add("type", message.type);

      if (message.sender != null)
      {
         builder = builder.add("sender", message.sender);
      }
      if (message.id != null)
      {
         builder = builder.add("id", message.id.toString());
      }
      if (message.transaction != null)
      {
         builder = builder.add("transaction", message.transaction.toJson());
      }
      if (message.peers != null)
      {
         JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
         for (String peer : message.peers)
         {
            arrayBuilder.add(peer);
         }
         JsonArray peerArray = arrayBuilder.build();
         builder = builder.add("peers", peerArray);
      }
      builder = builder.add("result", message.result);
      if (message.password != null)
      {
         builder = builder.add("password", message.password);
      }
      String json = builder.build().toString();
      return(json);
   }
}
