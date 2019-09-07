// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Parameters.
 */

package com.dialectek.coinspermia.shared;

public interface Parameters
{
   /**
    * Version.
    */
   public static final double VERSION = 1.0;

   /**
    * Website.
    */
   public static final String WEBSITE = "coinspermia.dialectek.com";

   /**
    * Default port.
    */
   public static final int DEFAULT_PORT = 8944;

   /**
    * Websocket protocol.
    */
   public static final String WEBSOCKET_PROTOCOL = "wss";

   /**
    * Relative URI.
    */
   public static final String URI = "/cs";

   /**
    * Ledger file.
    */
   public static final String LEDGER_FILE = "ledger.json";

   /**
    * Wallet file.
    */
   public static final String WALLET_FILE = "wallet.json";

   /**
    * Default peer bootstrap addresses file.
    */
   public static final String DEFAULT_BOOTSTRAP_PEER_FILE = "peers.txt";

   /**
    * Client connection to node file.
    */
   public static final String CONNECTION_FILE = "connection.txt";

   /**
    * Probability of intersecting transactions.
    * Determines peer connection density.
    */
   public static final float TRANSACTION_INTERSECTION_PROBABILITY = 0.99f;

   /**
    * Minimum and default maximum number of connected peers.
    */
   public static final int MIN_PEER_CONNECTIONS         = 10;
   public static final int DEFAULT_MAX_PEER_CONNECTIONS = 100;

   /**
    * Password file.
    */
   public static final String PASSWORD_FILE = "password.txt";

   /**
    * Transaction time-out (ms).
    */
   static final int TRANSACTION_TIME_OUT = 10000;

   /**
    * Transaction retry minimum and maximum wait times (ms).
    * Minimal time should be sufficient to propagate a commit.
    */
   static final int TRANSACTION_RETRY_MIN_WAIT = 1000;
   static final int TRANSACTION_RETRY_MAX_WAIT = 5000;

   /**
    * Peer refresh frequency (ms).
    */
   public static final int PEER_REFRESH_FREQ = 10000;

   /**
    * Default random number seed.
    */
   public static final int DEFAULT_RANDOM_SEED = 4517;

   /**
    * Default log file name.
    */
   public static final String DEFAULT_LOG_FILE = "coinspermia.log";

   /**
    * Result types.
    */
   public static final int SUCCESS   = 0;
   public static final int FAIL      = 1;
   public static final int INVALID   = 2;
   public static final int LOCK_FAIL = 3;
   public static final int DUPLICATE = 4;
   public static final int TIME_OUT  = 5;
};
