// For conditions of distribution and use, see copyright notice in Readme.

package com.dialectek.coinspermia.shared;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.UUID;

public class MessageDecoder implements Decoder.Text<Message>
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
   public Message decode(final String messageJson) throws DecodeException
   {
      return(fromJson(messageJson));
   }


   @Override
   public boolean willDecode(final String s)
   {
      return(true);
   }


   public static Message fromJson(final String messageJson) throws DecodeException
   {
      Message    message    = new Message();
      JsonObject jsonObject = Json.createReader(new StringReader(messageJson)).readObject();

      message.type = jsonObject.getInt("type");
      try
      {
         message.sender = jsonObject.getString("sender");
      }
      catch (Exception e)
      {
         message.sender = null;
      }
      try
      {
         message.id = UUID.fromString(jsonObject.getString("id"));
      }
      catch (Exception e)
      {
         message.id = null;
      }
      JsonObject transactionObject = jsonObject.getJsonObject("transaction");
      if (transactionObject != null)
      {
         message.transaction = Transaction.fromJson(transactionObject);
      }
      JsonArray peerArray = jsonObject.getJsonArray("peers");
      if (peerArray != null)
      {
         message.peers = new ArrayList<String>();
         for (int i = 0; i < peerArray.size(); i++)
         {
            message.peers.add(peerArray.getString(i));
         }
      }
      message.result = jsonObject.getInt("result");
      try
      {
         message.password = jsonObject.getString("password");
      }
      catch (Exception e)
      {
         message.password = null;
      }
      return(message);
   }
}
