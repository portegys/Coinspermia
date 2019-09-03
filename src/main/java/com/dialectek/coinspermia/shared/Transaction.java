// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Transaction.
 */

package com.dialectek.coinspermia.shared;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.dialectek.coinspermia.shared.Utils;

public class Transaction
{
   // Type.
   public static final int MINT    = 0;
   public static final int PAYMENT = 1;
   public static final int BALANCE = 2;

   public int type;

   // Inputs.
   public class Input
   {
      public int    publicKeyHash;
      public byte[] signature;
      public Input(int publicKeyHash, byte[] signature)
      {
         this.publicKeyHash = publicKeyHash;
         this.signature     = signature;
      }
   }
   public ArrayList<Input> inputs;

   // Outputs.
   public class Output
   {
      public PublicKey publicKey;
      public float     coins;
      public UUID      id;
      public Output(PublicKey publicKey, float coins)
      {
         this.publicKey = publicKey;
         this.coins     = coins;
         id             = UUID.randomUUID();
      }
   }
   public ArrayList<Output> outputs;

   // ID.
   public UUID id;

   // Logging.
   private static Logger logger = Logger.getLogger(Transaction.class .getName());

   // Constructors.
   public Transaction(int type, ArrayList<Input> inputs, ArrayList<Output> outputs)
   {
      this.type    = type;
      this.inputs  = inputs;
      this.outputs = outputs;
      id           = UUID.randomUUID();
   }


   public Transaction()
   {
      type    = PAYMENT;
      inputs  = new ArrayList<Input>();
      outputs = new ArrayList<Output>();
      id      = UUID.randomUUID();
   }


   // Add input.
   public void addInput(int outputHash, byte[] signature)
   {
      Input input = new Input(outputHash, signature);

      inputs.add(input);
   }


   // Add output.
   public void addOutput(PublicKey publicKey, float coins)
   {
      Output output = new Output(publicKey, coins);

      outputs.add(output);
   }


   // New input.
   public Input newInput(int outputHash, byte[] signature)
   {
      Input input = new Input(outputHash, signature);

      return(input);
   }


   // New output.
   public Output newOutput(PublicKey publicKey, float coins)
   {
      Output output = new Output(publicKey, coins);

      return(output);
   }


   // Valid transaction format?
   public boolean validFormat()
   {
      if ((type != BALANCE) && (type != MINT) && (type != PAYMENT))
      {
         return(false);
      }
      if (outputs == null)
      {
         return(false);
      }
      for (Output output : outputs)
      {
         if ((output.publicKey == null) || (output.coins < 0.0f) ||
             (id == null) || id.toString().equals(""))
         {
            return(false);
         }
      }
      if (type == PAYMENT)
      {
         if (inputs == null)
         {
            return(false);
         }
         for (Input input : inputs)
         {
            if (input.signature == null)
            {
               return(false);
            }
         }
      }
      return(true);
   }


   // Format as Json.
   public JsonValue toJson()
   {
      JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

      for (Input input : inputs)
      {
         arrayBuilder.add(Json.createObjectBuilder()
                             .add("publicKeyHash", input.publicKeyHash)
                             .add("signature", Base64.getEncoder().encodeToString(input.signature))
                             .build());
      }
      JsonArray inputArray = arrayBuilder.build();
      arrayBuilder = Json.createArrayBuilder();
      for (Output output : outputs)
      {
         String publicKeyString = "";
         try
         {
            publicKeyString = Utils.stringFromPublicKey(output.publicKey);
         }
         catch (Exception e)
         {
            logger.severe("Cannot convert publicKey to string");
         }
         arrayBuilder.add(Json.createObjectBuilder()
                             .add("publicKey", publicKeyString)
                             .add("coins", output.coins + "")
                             .add("id", output.id.toString())
                             .build());
      }
      JsonArray outputArray = arrayBuilder.build();
      return(Json.createObjectBuilder()
                .add("type", type)
                .add("inputs", inputArray)
                .add("outputs", outputArray)
                .add("id", id.toString())
                .build());
   }


   // Transaction from Json.
   public static Transaction fromJson(JsonObject txObject)
   {
      Transaction tx = new Transaction();

      tx.type = txObject.getInt("type");
      JsonArray inputArray = txObject.getJsonArray("inputs");
      for (int i = 0; i < inputArray.size(); i++)
      {
         JsonObject inputObject     = inputArray.getJsonObject(i);
         int        publicKeyHash   = inputObject.getInt("publicKeyHash");
         String     signatureString = inputObject.getString("signature");
         tx.addInput(publicKeyHash, Base64.getDecoder().decode(signatureString));
      }
      JsonArray outputArray = txObject.getJsonArray("outputs");
      for (int i = 0; i < outputArray.size(); i++)
      {
         JsonObject outputObject = outputArray.getJsonObject(i);
         PublicKey  publicKey    = null;
         try
         {
            publicKey = Utils.stringToPublicKey(outputObject.getString("publicKey"));
         }
         catch (Exception e)
         {
            logger.severe("Cannot convert string to publicKey");
         }
         float coins = Float.parseFloat(outputObject.getString("coins"));
         UUID  id    = UUID.fromString(outputObject.getString("id"));
         tx.addOutput(publicKey, coins);
         tx.outputs.get(tx.outputs.size() - 1).id = id;
      }
      tx.id = UUID.fromString(txObject.getString("id"));
      return(tx);
   }
}
