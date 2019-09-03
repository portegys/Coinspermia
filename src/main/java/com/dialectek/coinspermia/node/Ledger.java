// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Ledger.
 */

package com.dialectek.coinspermia.node;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Transaction;
import com.dialectek.coinspermia.shared.Utils;

public class Ledger
{
   // Transactions.
   public ArrayList<Transaction> transactions;

   // Unspent transaction output.
   public class UTXO
   {
      public Transaction.Output output;
      public UUID               txlock;
      public Date               txtime;

      public UTXO(Transaction.Output output)
      {
         this.output = output;
         txlock      = null;
         txtime      = null;
      }
   };
   public HashMap<Integer, UTXO> utxos;

   // Logging.
   private static Logger logger = Logger.getLogger(Ledger.class .getName());

   // Constructor.
   public Ledger()
   {
      transactions = new ArrayList<Transaction>();
      utxos        = new HashMap<Integer, UTXO>();
   }


   // Validate transaction.
   // Returns: SUCCESS, INVALID, DUPLICATE, FAIL (transaction cannot be applied).
   public synchronized int validate(Transaction transaction)
   {
      float coinsIn  = 0.0f;
      float coinsOut = 0.0f;

      // Sanity checks.
      for (Transaction.Input input : transaction.inputs)
      {
         if (input == null)
         {
            return(Parameters.INVALID);
         }
      }
      for (Transaction.Output output : transaction.outputs)
      {
         if (output == null)
         {
            return(Parameters.INVALID);
         }
      }

      // Check for duplicate application.
      for (Transaction.Output output : transaction.outputs)
      {
         int  hash = Utils.hashPublicKey(output.publicKey);
         UTXO utxo = utxos.get(hash);
         if ((utxo != null) && utxo.output.id.toString().equals(output.id.toString()))
         {
            return(Parameters.DUPLICATE);
         }
      }

      // Check input-output validity.
      int result = Parameters.SUCCESS;
      if (transaction.type == Transaction.PAYMENT)
      {
         for (Transaction.Input input : transaction.inputs)
         {
            UTXO utxo = utxos.get(input.publicKeyHash);
            if (utxo == null)
            {
               // Possible double spend.
               result = Parameters.FAIL;
               continue;
            }
            if (!Utils.verifySignature(utxo.output.publicKey, Utils.intToBytes(input.publicKeyHash), input.signature))
            {
               return(Parameters.INVALID);
            }
            for (Transaction.Output nextOutput : transaction.outputs)
            {
               if (utxo.output.id.equals(nextOutput.id))
               {
                  return(Parameters.INVALID);
               }
            }
            coinsIn += utxo.output.coins;
         }
         for (Transaction.Output output : transaction.outputs)
         {
            coinsOut += output.coins;
         }
         if ((result == Parameters.SUCCESS) && (coinsIn != coinsOut))
         {
            return(Parameters.INVALID);
         }
      }
      return(result);
   }


   // Lock transaction utxos.
   public synchronized int lock(Transaction transaction)
   {
      if (transaction.type == Transaction.PAYMENT)
      {
         Date txtime = new Date();
         for (Transaction.Input input : transaction.inputs)
         {
            UTXO utxo = utxos.get(input.publicKeyHash);
            if (utxo == null)
            {
               return(Parameters.INVALID);
            }
            if ((utxo.txlock == null) || (utxo.txlock == transaction.id) ||
                ((txtime.getTime() - utxo.txtime.getTime()) >= Parameters.TRANSACTION_TIME_OUT))
            {
               utxo.txlock = transaction.id;
               utxo.txtime = txtime;
            }
            else
            {
               return(Parameters.LOCK_FAIL);
            }
         }
      }
      return(Parameters.SUCCESS);
   }


   // Unlock transaction utxos.
   public synchronized void unlock(Transaction transaction)
   {
      if (transaction.type == Transaction.PAYMENT)
      {
         for (Transaction.Input input : transaction.inputs)
         {
            UTXO utxo = utxos.get(input.publicKeyHash);
            if (utxo != null)
            {
               if (utxo.txlock == transaction.id)
               {
                  utxo.txlock = null;
                  utxo.txtime = null;
               }
            }
         }
      }
   }


   // Commit transaction.
   public synchronized int commit(Transaction transaction)
   {
      // Check for duplicate application.
      for (Transaction.Output output : transaction.outputs)
      {
         int  hash = Utils.hashPublicKey(output.publicKey);
         UTXO utxo = utxos.get(hash);
         if ((utxo != null) && utxo.output.id.toString().equals(output.id.toString()))
         {
            return(Parameters.DUPLICATE);
         }
      }

      // Remove precedents.
      if (transaction.type == Transaction.PAYMENT)
      {
         for (Transaction.Input input : transaction.inputs)
         {
            utxos.remove(input.publicKeyHash);
         }
      }

      // Apply changes.
      for (Transaction.Output output : transaction.outputs)
      {
         int  hash = Utils.hashPublicKey(output.publicKey);
         UTXO utxo = utxos.get(hash);
         if (utxo == null)
         {
            utxos.put(hash, new UTXO(output));
         }
         else
         {
            utxo.output.coins += output.coins;
            utxo.output.id     = output.id;
         }
      }
      transactions.add(transaction);
      return(Parameters.SUCCESS);
   }


   public UTXO newUTXO(Transaction.Output output)
   {
      return(new UTXO(output));
   }


   // Load ledger.
   public void load(String ledgerFile)
   {
      if (new File(ledgerFile).exists())
      {
         try
         {
            Ledger ledger = fromJson(new String(Files.readAllBytes(Paths.get(ledgerFile))));

            utxos.clear();
            for (Map.Entry<Integer, UTXO> entry : ledger.utxos.entrySet())
            {
               Integer hash = entry.getKey();
               UTXO    utxo = entry.getValue();
               utxos.put(hash, utxo);
            }
         }
         catch (Exception e)
         {
            logger.severe("Cannot load ledger from " + ledgerFile);
         }
      }
      else
      {
         logger.info("Ledger " + ledgerFile + " does not exist");
      }
   }


   // Save ledger.
   public void save(String ledgerFile)
   {
      try (PrintWriter out = new PrintWriter(ledgerFile))
         {
            out.println(toJson());
         }
         catch (Exception e)
         {
            logger.severe("Cannot save ledger to " + ledgerFile);
         }

   }


   // Clear ledger.
   public void clear()
   {
      utxos.clear();
   }


   // Ledger to Json.
   public String toJson()
   {
      JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

      for (Map.Entry<Integer, UTXO> entry : utxos.entrySet())
      {
         Integer           hash         = entry.getKey();
         UTXO              utxo         = entry.getValue();
         JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
         entryBuilder = entryBuilder.add("hash", hash);
         String publicKeyString = "";
         try
         {
            publicKeyString = Utils.stringFromPublicKey(utxo.output.publicKey);
         }
         catch (Exception e)
         {
            logger.severe("Cannot convert publicKey to string");
         }
         JsonObjectBuilder outputBuilder = Json.createObjectBuilder()
                                              .add("publicKey", publicKeyString)
                                              .add("coins", utxo.output.coins + "");
         entryBuilder = entryBuilder.add("output", outputBuilder.build());
         arrayBuilder.add(entryBuilder.build());
      }
      JsonArray utxoArray = arrayBuilder.build();
      return(Json.createObjectBuilder()
                .add("utxos", utxoArray)
                .build().toString());
   }


   // Ledger from Json.
   public static Ledger fromJson(final String ledgerJson)
   {
      Ledger      ledger     = new Ledger();
      JsonObject  jsonObject = Json.createReader(new StringReader(ledgerJson)).readObject();
      JsonArray   utxoArray  = jsonObject.getJsonArray("utxos");
      Transaction tx         = new Transaction();

      for (int i = 0; i < utxoArray.size(); i++)
      {
         JsonObject entryObject  = utxoArray.getJsonObject(i);
         int        hash         = entryObject.getInt("hash");
         JsonObject outputObject = entryObject.getJsonObject("output");
         PublicKey  publicKey    = null;
         try
         {
            publicKey = Utils.stringToPublicKey(outputObject.getString("publicKey"));
         }
         catch (Exception e)
         {
            logger.severe("Cannot convert string to publicKey");
         }
         float              coins  = Float.parseFloat(outputObject.getString("coins"));
         Transaction.Output output = tx.newOutput(publicKey, coins);
         ledger.utxos.put(hash, ledger.newUTXO(output));
      }
      return(ledger);
   }
}
