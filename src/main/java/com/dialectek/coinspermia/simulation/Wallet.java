// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.util.ArrayList;
import com.dialectek.coinspermia.shared.Balance;

/**
 * Wallet.
 */

class Wallet
{
   // Balances.
   ArrayList<Balance> balances;

   // Constructor.
   Wallet()
   {
      balances = new ArrayList<Balance>();
   }


   // Add a balance.
   void add(Balance balance)
   {
      balances.add(balance);
   }


   // Remove a balance.
   void remove(Balance balance)
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
   boolean exists(Balance balance)
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
   float sum()
   {
      float coins = 0.0f;

      for (Balance b : balances)
      {
         coins += b.coins;
      }
      return(coins);
   }
}
