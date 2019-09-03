// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Wallet.
 */

package com.dialectek.coinspermia.client;

import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import com.dialectek.coinspermia.shared.Balance;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Utils;

public class Wallet implements Parameters
{
   // Balances.
   public ArrayList<Balance> balances;
   public int                balanceIndex;

   // Logging.
   private static Logger logger = Logger.getLogger(Wallet.class .getName());

   // Constructor.
   public Wallet()
   {
      balances     = new ArrayList<Balance>();
      balanceIndex = -1;
   }


   // Add a balance.
   public void add(Balance balance)
   {
      balances.add(balance);
   }


   // Delete a balance.
   public void delete(Balance balance)
   {
      for (Balance b : balances)
      {
         if (b.equals(balance))
         {
            balances.remove(b);
            break;
         }
      }
   }


   // Check balance existence.
   public boolean exists(Balance balance)
   {
      for (Balance b : balances)
      {
         if (b.equals(balance))
         {
            return(true);
         }
      }
      return(false);
   }


   // Sum balances.
   public float sum()
   {
      float coins = 0.0f;

      for (Balance b : balances)
      {
         coins += b.coins;
      }
      return(coins);
   }


   // Load wallet.
   public void load(String walletFile) throws Exception
   {
      Wallet wallet = fromJson(new String(Files.readAllBytes(Paths.get(walletFile))));

      balances.clear();
      for (Balance balance : wallet.balances)
      {
         balances.add(balance);
      }
      balanceIndex = wallet.balanceIndex;
   }


   // Save wallet.
   public void save(String walletFile)
   {
      try (PrintWriter out = new PrintWriter(walletFile))
         {
            out.println(toJson());
         }
         catch (Exception e)
         {
            logger.severe("Cannot save wallet to " + walletFile);
         }

   }


   // Wallet to Json.
   public String toJson()
   {
      JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

      for (Balance balance : balances)
      {
         arrayBuilder.add(balance.toJson());
      }
      JsonArray balanceArray = arrayBuilder.build();
      return(Json.createObjectBuilder()
                .add("balances", balanceArray)
                .add("balanceIndex", balanceIndex)
                .build().toString());
   }


   // Wallet from Json.
   public static Wallet fromJson(final String walletJson)
   {
      Wallet     wallet       = new Wallet();
      JsonObject jsonObject   = Json.createReader(new StringReader(walletJson)).readObject();
      JsonArray  balanceArray = jsonObject.getJsonArray("balances");

      for (int i = 0; i < balanceArray.size(); i++)
      {
         Balance    balance       = new Balance();
         JsonObject balanceObject = balanceArray.getJsonObject(i);
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
            logger.severe("Cannot convert string to publicKey");
         }
         balance.coins         = Float.parseFloat(balanceObject.getString("coins"));
         balance.publicKeyHash = balanceObject.getInt("publicKeyHash");
         wallet.balances.add(balance);
      }
      wallet.balanceIndex = jsonObject.getInt("balanceIndex");
      return(wallet);
   }
}
