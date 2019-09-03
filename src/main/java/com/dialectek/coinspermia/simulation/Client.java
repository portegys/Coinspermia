// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

/**
 * Client.
 */

class Client
{
   // ID.
   int id;

   // Wallet.
   Wallet wallet;

   // Transaction active?
   boolean transactionActive;

   // Next transaction timer.
   int nextTransactionTimer;

   // Simulator.
   Simulator simulator;

   /**Construct the Client*/
   Client(int id, Simulator simulator)
   {
      this.id              = id;
      this.simulator       = simulator;
      wallet               = new Wallet();
      transactionActive    = false;
      nextTransactionTimer = (int)(Math.random() * (double)Parameters.TRANSACTION_ORIGINATION_RATE * 2.0);
   }


   // Step client.
   void step()
   {
      if (!transactionActive)
      {
         if (nextTransactionTimer == 0)
         {
            simulator.createClientTransaction(this);
            nextTransactionTimer = (int)(Math.random() * (double)Parameters.TRANSACTION_ORIGINATION_RATE * 2.0);
         }
         else
         {
            nextTransactionTimer--;
         }
      }
   }
}
