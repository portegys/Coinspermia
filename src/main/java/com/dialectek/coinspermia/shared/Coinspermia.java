// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Main.
 */

package com.dialectek.coinspermia.shared;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.dialectek.coinspermia.node.Node;

public class Coinspermia
{
   private static final Map < String, Class < ? >> ENTRY_POINTS = new HashMap < String, Class < ? >> ();

   static
   {
      ENTRY_POINTS.put("client", com.dialectek.coinspermia.client.Client.class );
      ENTRY_POINTS.put("clientCLI", com.dialectek.coinspermia.client.ClientCLI.class );
      ENTRY_POINTS.put("node", com.dialectek.coinspermia.node.Node.class );
   }

   public static final String Options = "Options:\n\t[-node <node options> | -client | -clientCLI (defaults to -node)]\nNode ";

   public static void main(final String[] args)
   {
      String entry     = "node";
      int    argsIndex = 0;

      if (args.length > 0)
      {
         if (args[0].equals("-client"))
         {
            entry     = "client";
            argsIndex = 1;
         }
         else if (args[0].equals("-clientCLI"))
         {
            entry     = "clientCLI";
            argsIndex = 1;
         }
         else if (args[0].equals("-node"))
         {
            entry     = "node";
            argsIndex = 1;
         }
         else if (args[0].equals("-help"))
         {
            System.err.println(Options);
            System.err.println(Node.Options);
            return;
         }
      }
      final Class<?> entryPoint = ENTRY_POINTS.get(entry);
      if (entryPoint == null)
      {
         System.err.println("Invalid entry point");
         return;
      }
      final String[] argsCopy =
         args.length > argsIndex
         ? Arrays.copyOfRange(args, argsIndex, args.length)
         : new String[0];
      try
      {
         entryPoint.getMethod("main", String[].class ).invoke(null, (Object)argsCopy);
      }
      catch (Exception e)
      {
         System.err.println("Cannot invoke " + entry);
      }
   }
}
