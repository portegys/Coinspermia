// For conditions of distribution and use, see copyright notice in Coinspermia.java

package com.dialectek.coinspermia.simulation;

import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;


/**
 *  Basic logging
 */
public class Log
{
   public static String LOGGING_FLAG = "Coinspermia.Log";
   public static String LOG_FILE     = "coinspermia.log";
   public static String ERROR_FILE   = "coinspermia.err";

   private static Log              log;
   private static RandomAccessFile logFile;

   protected Log()
   {
   }


   /**
    *  Access the log
    *
    */
   public static Log getLog()
   {
      if (null == log)
      {
         log = new Log();
      }

      return(log);
   }


   /**
    *  Logs an unexpected exception, some exceptions, such as IO problems
    *  are to be expected
    *
    */
   public void logUnexpectedException(Throwable t)
   {
      StringWriter stringWriter = new StringWriter();

      t.printStackTrace(new PrintWriter(stringWriter));

      logToDebug("Exception: \n" + stringWriter.toString());
   }


   /**
    *  Logs an exception
    *
    */
   public void log(Throwable t)
   {
      StringWriter stringWriter = new StringWriter();

      t.printStackTrace(new PrintWriter(stringWriter));

      logToDebug("Exception: \n" + stringWriter.toString());
   }


   /**
    *  Logs an error
    *
    */
   public void logError(String txt)
   {
      logToDebug("ERROR: " + txt);
   }


   /**
    *  Logs a warning
    *
    */
   public void logWarning(String txt)
   {
      logToDebug("WARNING: " + txt);
   }


   /**
    *  Logs debug information
    *
    */
   public void logDebug(String txt)
   {
      // TODO whether are not this produces output a system property
      logToDebug("DEBUG: " + txt);
   }


   /**
    *  Logs information
    *
    */
   public void logInformation(String txt)
   {
      logToDebug("INFORMATION: " + txt);
   }


   /**
    *   Writes a log to the debug log
    *
    */
   private void logToDebug(String logString)
   {
      String flag = System.getProperty(LOGGING_FLAG);

      if ((flag == null) || !flag.equals("true"))
      {
         // debug mode off
         return;
      }

      if (null == logFile)
      {
         // open file
         try
         {
            logFile = new RandomAccessFile(LOG_FILE, "rw");
            logFile.seek(logFile.length());
         }
         catch (IOException io)
         {
            io.printStackTrace();
         }
      }

      log(logFile, logString);
   }


   /**
    *  Rights the string to the log
    *
    */
   private void log(RandomAccessFile file, String logString)
   {
      Date date = new Date();

      logString = "[" +
                  DateFormat.getDateInstance(DateFormat.SHORT).format(date) +
                  " - " +
                  DateFormat.getTimeInstance(DateFormat.FULL).format(date) +
                  "] " +
                  logString +
                  "\r\n";

      try
      {
         synchronized (file)
         {
            file.write(logString.getBytes());
         }
      }
      catch (IOException io)
      {
         io.printStackTrace();
      }
   }


   // test
   public static void main(String[] args)
   {
      getLog().logDebug("Test");
   }
}
