// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.util.logging.Logger;

/**
 * Coinspermia simulation parameters.
 */

public class Parameters
{
   /**
    * Number of nodes.
    */
   public static int NUM_NODES = 5;

   /**
    * Network connection density: fraction of NUM_NODES.
    */
   public static float CONNECTION_DENSITY = 0.5f;

   /**
    * Number of clients.
    */
   public static int NUM_CLIENTS = 10;

   /**
    * Transaction origination rate:
    * Mean time between transaction originations.
    */
   public static int TRANSACTION_ORIGINATION_RATE = 5;

   /**
    * Client transaction service re-enqueue position.
    */
   public static int CLIENT_TRANSACTION_RE_ENQUEUE_POSITION = 10;

   /**
    * Transaction service time.
    */
   public static int TRANSACTION_SERVICE_TIME = 1;

   /**
    * Transaction transit time.
    */
   public static int TRANSACTION_TRANSIT_TIME = 1;

   /**
    * Minted coins.
    */
   public static int MINTED_COINS = 50;

   /**
    * Random seed.
    */
   public static int RANDOM_SEED = 4517;

   private static Logger logger = Logger.getLogger(Parameters.class .getName());

   // Print.
   public static void print()
   {
      logger.info("NUM_NODES = " + Parameters.NUM_NODES);
      logger.info("CONNECTION_DENSITY = " + Parameters.CONNECTION_DENSITY);
      logger.info("NUM_CLIENTS = " + Parameters.NUM_CLIENTS);
      logger.info("TRANSACTION_ORIGINATION_RATE = " + Parameters.TRANSACTION_ORIGINATION_RATE);
      logger.info("CLIENT_TRANSACTION_RE_ENQUEUE_POSITION = " + Parameters.CLIENT_TRANSACTION_RE_ENQUEUE_POSITION);
      logger.info("TRANSACTION_SERVICE_TIME = " + Parameters.TRANSACTION_SERVICE_TIME);
      logger.info("TRANSACTION_TRANSIT_TIME = " + Parameters.TRANSACTION_TRANSIT_TIME);
      logger.info("MINTED_COINS = " + (Parameters.NUM_CLIENTS * 2));
      logger.info("RANDOM_SEED = " + Parameters.RANDOM_SEED);
   }
};
