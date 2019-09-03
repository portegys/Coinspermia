// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Message.
 */

package com.dialectek.coinspermia.shared;

import java.util.ArrayList;
import java.util.UUID;

import com.dialectek.coinspermia.node.Node;

public class Message
{
   /**
    * Transaction submission.
    */
   public static final int TRANSACTION_REQUEST  = 1;
   public static final int TRANSACTION_RESPONSE = 2;

   /**
    * Lock/unlock transaction.
    */
   public static final int LOCK_REQUEST    = 3;
   public static final int LOCK_RESPONSE   = 4;
   public static final int UNLOCK_REQUEST  = 5;
   public static final int UNLOCK_RESPONSE = 6;

   /**
    * Commit transaction.
    */
   public static final int COMMIT_REQUEST  = 7;
   public static final int COMMIT_RESPONSE = 8;

   /**
    * Network connection.
    */
   public static final int CONNECTION_REQUEST  = 9;
   public static final int CONNECTION_RESPONSE = 10;

   /**
    * Peer census.
    */
   public static final int CENSUS_REQUEST  = 11;
   public static final int CENSUS_RESPONSE = 12;

   /**
    * Ledger operations.
    */
   public static final int LOAD_LEDGER_REQUEST   = 13;
   public static final int LOAD_LEDGER_RESPONSE  = 14;
   public static final int SAVE_LEDGER_REQUEST   = 15;
   public static final int SAVE_LEDGER_RESPONSE  = 16;
   public static final int CLEAR_LEDGER_REQUEST  = 17;
   public static final int CLEAR_LEDGER_RESPONSE = 18;

   public int               type;
   public String            sender;
   public UUID              id;
   public Transaction       transaction;
   public ArrayList<String> peers;
   public int               result;
   public String            password;

   // Constructors.
   public Message()
   {
      type        = -1;
      sender      = null;
      id          = null;
      transaction = null;
      peers       = null;
      result      = Parameters.SUCCESS;
      password    = null;
   }


   public Message(int type)
   {
      this.type   = type;
      sender      = null;
      id          = null;
      transaction = null;
      peers       = null;
      result      = Parameters.SUCCESS;
      password    = null;
   }


   // Password authorized?
   public boolean authorized()
   {
      if ((type == LOCK_REQUEST) ||
          (type == UNLOCK_REQUEST) ||
          (type == COMMIT_REQUEST) ||
          (type == CONNECTION_REQUEST) ||
          (type == LOAD_LEDGER_REQUEST) ||
          (type == SAVE_LEDGER_REQUEST) ||
          (type == CLEAR_LEDGER_REQUEST))
      {
         if (!Node.node.validPassword(password))
         {
            return(false);
         }
      }
      return(true);
   }
}
