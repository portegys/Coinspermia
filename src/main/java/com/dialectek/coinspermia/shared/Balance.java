// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Balance.
 */

package com.dialectek.coinspermia.shared;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.dialectek.coinspermia.shared.Utils;

public class Balance
{
   public PublicKey  publicKey;
   public PrivateKey privateKey;
   public float      coins;
   public int        publicKeyHash;

   // Logging.
   private static Logger logger = Logger.getLogger(Balance.class .getName());

   // Constructors.
   public Balance(PublicKey publicKey, PrivateKey privateKey, float coins)
   {
      this.publicKey  = publicKey;
      this.privateKey = privateKey;
      this.coins      = coins;
      publicKeyHash   = Utils.hashPublicKey(publicKey);
   }


   public Balance()
   {
      publicKey     = null;
      privateKey    = null;
      coins         = 0.0f;
      publicKeyHash = -1;
   }


   // Equality test.
   public boolean equals(Balance balance)
   {
      if ((balance.publicKey.hashCode() != publicKey.hashCode()) ||
          (balance.privateKey.hashCode() != privateKey.hashCode()) ||
          (balance.coins != coins))
      {
         return(false);
      }
      else
      {
         return(true);
      }
   }


   // Balance to Json.
   public JsonValue toJson()
   {
      String publicKeyString = null;

      try
      {
         publicKeyString = Utils.stringFromPublicKey(publicKey);
      }
      catch (Exception e)
      {
         logger.severe("Cannot convert publicKey to string");
      }
      String privateKeyString = null;
      try
      {
         privateKeyString = Utils.stringFromPrivateKey(privateKey);
      }
      catch (Exception e)
      {
         logger.severe("Cannot convert privateKey to string");
      }
      return(Json.createObjectBuilder()
                .add("publicKey", publicKeyString)
                .add("privateKey", privateKeyString)
                .add("coins", coins + "")
                .add("publicKeyHash", publicKeyHash)
                .build());
   }


   // Balance from Json.
   public static Balance fromJson(JsonObject balanceObject)
   {
      Balance balance = new Balance();

      try
      {
         balance.publicKey = Utils.stringToPublicKey(balanceObject.getString("publicKey"));
      }
      catch (Exception e)
      {
         logger.severe("Cannot convert string to publicKey");
      }
      try
      {
         balance.privateKey = Utils.stringToPrivateKey(balanceObject.getString("privateKey"));
      }
      catch (Exception e)
      {
         logger.severe("Cannot convert string to privateKey");
      }
      balance.coins         = Float.parseFloat(balanceObject.getString("coins"));
      balance.publicKeyHash = balanceObject.getInt("publicKeyHash");
      return(balance);
   }
}
