// For conditions of distribution and use, see copyright notice in Readme.

/**
 * Client.
 */

package com.dialectek.coinspermia.client;

import java.awt.AWTEvent;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import org.glassfish.tyrus.client.ClientManager;

import com.dialectek.coinspermia.shared.Balance;
import com.dialectek.coinspermia.shared.Message;
import com.dialectek.coinspermia.shared.Parameters;
import com.dialectek.coinspermia.shared.Transaction;
import com.dialectek.coinspermia.shared.Utils;

/**
 * Client.
 */

public class Client extends JFrame
{
   private static final long serialVersionUID = 1L;

   // Window size.
   public static final Dimension WINDOW_SIZE = new Dimension(485, 500);

   // GUI tabs.
   public static final int WALLET     = 0;
   public static final int PAY        = 1;
   public static final int CONNECTION = 2;
   public static final int READ_ME    = 3;

   JPanel      contentPane;
   JTabbedPane tabbedPane = new JTabbedPane();

   // Wallet.
   JPanel           walletTab        = new JPanel();
   JScrollPane      walletScroll     = new JScrollPane();
   WalletTableModel walletTableModel = new WalletTableModel();
   JTable           walletTable      = new JTable(walletTableModel);
   JButton          walletNew        = new JButton();
   JButton          walletDelete     = new JButton();
   JButton          walletRefresh    = new JButton();
   JLabel           walletSumLabel   = new JLabel();
   JTextField       walletSumText    = new JTextField();
   String           walletFile       = Parameters.WALLET_FILE;
   public Wallet    wallet;

   // Pay.
   JPanel             payTab        = new JPanel();
   JScrollPane        payScroll     = new JScrollPane();
   PayTableModel      payTableModel = new PayTableModel();
   JTable             payTable      = new JTable(payTableModel);
   JButton            payFrom       = new JButton();
   JButton            payTo         = new JButton();
   JButton            payClear      = new JButton();
   JButton            payMint       = new JButton();
   JButton            payPay        = new JButton();
   String             password      = null;
   Transaction        transaction;
   ArrayList<Balance> inputBalances;

   // Connection.
   JPanel               connectionTab = new JPanel();
   JLabel               nodeLabel     = new JLabel();
   JTextArea            nodeText      = new JTextArea();
   JButton              nodeConnect   = new JButton();
   JLabel               censusLabel   = new JLabel();
   JScrollPane          censusScroll  = new JScrollPane();
   JTextArea            censusText    = new JTextArea();
   JButton              censusRefresh = new JButton();
   public ClientManager clientManager;
   public Session       session;

   // Read me.
   JScrollPane readMeScroll = new JScrollPane();
   JEditorPane readMe       = new JEditorPane();

   // Current balance.
   JPanel     currentBalancePanel   = new JPanel();
   JLabel     currentBalanceTitle   = new JLabel();
   JLabel     currentPublicKeyLabel = new JLabel();
   TextArea   currentPublicKeyText  = new TextArea("", 1, 150, TextArea.SCROLLBARS_HORIZONTAL_ONLY);
   JLabel     currentCoinsLabel     = new JLabel();
   JTextField currentCoinsText      = new JTextField();

   // Status.
   JPanel     statusPanel = new JPanel();
   JLabel     statusLabel = new JLabel();
   JTextField statusText  = new JTextField();

   // Singleton.
   public static Client client = null;

   // Logging.
   public static Logger logger = Logger.getLogger(Client.class .getName());

   /**Construct the Client*/
   public Client() throws Exception
   {
      // Check for singleton.
      if (client != null)
      {
         throw new Exception("Client must be run as a singleton");
      }
      client = this;

      // Create components.
      wallet = new Wallet();
      if (new File(Parameters.WALLET_FILE).exists())
      {
         wallet.load(Parameters.WALLET_FILE);
      }
      transaction   = new Transaction();
      inputBalances = new ArrayList<Balance>();
      clientManager = ClientManager.createClient();

      // Initialize GUI.
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class .getResource("/pipcoin.png")));
      contentPane = (JPanel) this.getContentPane();
      contentPane.setLayout(null);
      this.setSize(new Dimension(472, 522));
      this.setTitle("Coinspermia");
      tabbedPane.setBounds(new Rectangle(10, 10, 457, 294));
      tabbedPane.addChangeListener(new TabbedPaneChangeListener());

      // Wallet.
      walletTab.setLayout(null);
      walletNew.setToolTipText("New balance");
      walletNew.setText("New");
      walletNew.setBounds(new Rectangle(27, 242, 89, 22));
      walletNew.addActionListener(new WalletNewListener());
      walletDelete.setBounds(new Rectangle(119, 242, 89, 22));
      walletDelete.setText("Delete");
      walletDelete.setToolTipText("Delete balance");
      walletDelete.addActionListener(new WalletDeleteListener());
      walletRefresh.setBounds(new Rectangle(211, 242, 89, 22));
      walletRefresh.setText("Refresh");
      walletRefresh.setToolTipText("Refresh wallet");
      walletRefresh.addActionListener(new WalletRefreshListener());
      walletSumLabel.setText("Sum:");
      walletSumLabel.setBounds(new Rectangle(305, 242, 85, 22));
      walletSumLabel.setToolTipText("Coins in wallet");
      walletSumText.setBounds(new Rectangle(341, 242, 80, 22));
      walletSumText.setEditable(false);
      walletSumText.setToolTipText("Coins in wallet");
      walletScroll.setBounds(new Rectangle(1, 0, 450, 239));
      walletTable.setToolTipText("Balances");
      walletTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      ListSelectionModel walletRowSM = walletTable.getSelectionModel();
      walletRowSM.addListSelectionListener(new WalletListener());
      walletTab.add(walletNew, null);
      walletTab.add(walletDelete, null);
      walletTab.add(walletRefresh, null);
      walletTab.add(walletSumLabel, null);
      walletTab.add(walletSumText, null);
      walletTab.add(walletScroll, null);
      walletScroll.getViewport().add(walletTable, null);

      // Pay.
      payTab.setLayout(null);
      payFrom.setToolTipText("Pay from");
      payFrom.setText("From");
      payFrom.setBounds(new Rectangle(15, 242, 79, 22));
      payFrom.addActionListener(new PayFromListener());
      payTo.setBounds(new Rectangle(102, 242, 79, 22));
      payTo.setText("To");
      payTo.setToolTipText("Pay to");
      payTo.addActionListener(new PayToListener());
      payClear.setBounds(new Rectangle(189, 242, 79, 22));
      payClear.setText("Clear");
      payClear.setToolTipText("Clear transaction");
      payClear.addActionListener(new PayClearListener());
      payMint.setBounds(new Rectangle(276, 242, 79, 22));
      payMint.setText("Mint");
      payMint.setToolTipText("Mint transaction");
      payMint.addActionListener(new PayMintListener());
      payPay.setBounds(new Rectangle(363, 242, 79, 22));
      payPay.setText("Pay");
      payPay.setToolTipText("Pay transaction");
      payPay.addActionListener(new PayPayListener());
      payScroll.setBounds(new Rectangle(1, 0, 450, 239));
      payTable.setToolTipText("Transaction");
      payTab.add(payFrom, null);
      payTab.add(payTo, null);
      payTab.add(payClear, null);
      payTab.add(payMint, null);
      payTab.add(payPay, null);
      payTab.add(payScroll, null);
      payScroll.getViewport().add(payTable, null);

      // Connection.
      connectionTab.setLayout(null);
      nodeLabel.setToolTipText("Node address to connect to, e.g. 302.4.44.17:8944 or somehost.com:port");
      nodeLabel.setText("Node address:");
      nodeLabel.setBounds(new Rectangle(5, 5, 100, 17));
      nodeText.setBounds(new Rectangle(5, 30, 200, 21));
      nodeText.setText("localhost:" + Parameters.DEFAULT_PORT);
      nodeConnect.setBounds(new Rectangle(5, 60, 100, 22));
      nodeConnect.setText("Connect");
      nodeConnect.setToolTipText("Connect to node");
      nodeConnect.addActionListener(new NodeConnectListener());
      censusLabel.setText("Node census:");
      censusLabel.setBounds(new Rectangle(5, 90, 90, 17));
      censusScroll.setBounds(new Rectangle(4, 115, 201, 121));
      censusRefresh.setBounds(new Rectangle(5, 240, 100, 22));
      censusRefresh.setText("Refresh");
      censusRefresh.setToolTipText("Take node census");
      censusRefresh.addActionListener(new NodeCensusListener());
      connectionTab.add(nodeLabel, null);
      connectionTab.add(nodeText, null);
      connectionTab.add(nodeConnect, null);
      connectionTab.add(censusLabel, null);
      connectionTab.add(censusScroll, null);
      connectionTab.add(censusRefresh, null);
      censusScroll.getViewport().add(censusText, null);
      BufferedReader in = null;
      try
      {
         in = new BufferedReader(new FileReader(Parameters.CONNECTION_FILE));
         String nodeAddress = null;
         if ((nodeAddress = in.readLine()) != null)
         {
            String[] parts = nodeAddress.split(":");
            if (parts.length == 1)
            {
               nodeAddress = nodeAddress + ":" + Parameters.DEFAULT_PORT;
            }
            nodeText.setText(nodeAddress);
         }
      }
      catch (Exception e)
      {
         if (in != null)
         {
            System.err.println("Error reading connection file " + Parameters.CONNECTION_FILE + ": " + e.getMessage());
            return;
         }
      }
      finally
      {
         try
         {
            if (in != null)
            {
               in.close();
            }
         }
         catch (Exception e) {}
      }

      // Read me.
      readMe.setEditable(false);
      readMe.setContentType("text/html");
      readMe.addHyperlinkListener(new ReadmeHyperlinkListener());
      createReadmeText();
      readMeScroll.getViewport().add(readMe, null);

      // Current balance.
      currentBalancePanel.setBorder(BorderFactory.createRaisedBevelBorder());
      currentBalancePanel.setBounds(new Rectangle(9, 307, 459, 100));
      currentBalancePanel.setLayout(null);
      currentBalanceTitle.setText("Current balance");
      currentBalanceTitle.setBounds(new Rectangle(157, 6, 124, 17));
      currentPublicKeyLabel.setText("Public key:");
      currentPublicKeyLabel.setBounds(new Rectangle(12, 25, 78, 17));
      currentPublicKeyText.setBounds(new Rectangle(91, 25, 350, 45));
      currentCoinsLabel.setText("Coins:");
      currentCoinsLabel.setBounds(new Rectangle(12, 75, 51, 17));
      currentCoinsText.setBounds(new Rectangle(91, 75, 80, 21));
      currentCoinsText.setToolTipText("Current coins");
      currentBalancePanel.add(currentBalanceTitle, null);
      currentBalancePanel.add(currentPublicKeyLabel, null);
      currentBalancePanel.add(currentPublicKeyText, null);
      currentBalancePanel.add(currentCoinsLabel, null);
      currentBalancePanel.add(currentCoinsText, null);

      // Status.
      statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
      statusPanel.setBounds(new Rectangle(8, 410, 459, 30));
      statusPanel.setLayout(null);
      statusLabel.setToolTipText("Status message");
      statusLabel.setText("Status:");
      statusLabel.setBounds(new Rectangle(6, 6, 56, 22));
      statusText.setToolTipText("Status message");
      statusText.addActionListener(new StatusTextListener());
      statusText.setBounds(new Rectangle(68, 5, 380, 21));
      statusPanel.add(statusLabel, null);
      statusPanel.add(statusText, null);

      // Assemble the GUI.
      contentPane.add(tabbedPane, null);
      tabbedPane.add(walletTab, "Wallet");
      tabbedPane.add(payTab, "Pay");
      tabbedPane.add(connectionTab, "Connection");
      tabbedPane.add(readMeScroll, "Read me");
      contentPane.add(currentBalancePanel, null);
      contentPane.add(statusPanel, null);
   }


   /**Overridden so we can exit when window is closed*/
   protected void processWindowEvent(WindowEvent e)
   {
      if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
         if (session != null)
         {
            try
            {
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Client shut down"));
            }
            catch (Exception ex) {
               logger.warning("Cannot close connection to node " + nodeText.getText().trim() + ": " + ex.getMessage());
            }
            session = null;
            nodeConnect.setText("Connect");
         }
         super.processWindowEvent(e);
         wallet.save(Parameters.WALLET_FILE);
         System.exit(0);
      }
   }


   // Create text for Read me.
   void createReadmeText()
   {
      StringWriter writer = new StringWriter();

      writer.write("<HTML>\n");
      writer.write("<HEAD>\n");
      writer.write("<TITLE>Coinspermia Read me</TITLE>\n");
      writer.write("</HEAD>\n");
      writer.write("<BODY>\n");
      writer.write("<H1>Coinspermia " + Parameters.VERSION + "</H1>\n");
      writer.write("<HR>\n");
      writer.write("<H3>Description</H3>\n");
      writer.write("Coinspermia is a cryptocurrency distributed in a network of peer nodes<br>\n");
      writer.write("that supports secure and reliable transactions. Transactions are replicated<br>\n");
      writer.write("throughout the network for later query and update. A client can be assured<br>\n");
      writer.write("of the completion of an transaction when a quorum of the nodes validate it.<br>\n");
      writer.write("<H3>Files</H3>\n");
      writer.write(walletFile + " - wallet file.<br>\n");
      writer.write(Parameters.CONNECTION_FILE + " - node connection file.<br>\n");
      writer.write("<H3>Password authorized currency minting</H3>\n");
      writer.write("If the network has password authorization enabled, enter the password in the status text field before minting currency.<br>\n");
      writer.write("</BODY>\n");
      writer.write("</HTML>\n");
      readMe.setText(writer.toString());
   }


   // Tabs listener.
   class TabbedPaneChangeListener implements ChangeListener
   {
      public void stateChanged(ChangeEvent evt)
      {
         // Clear message when changing tabs.
         statusText.setText("");
      }
   }

   // Wallet table model.
   class WalletTableModel extends AbstractTableModel
   {
      private static final long serialVersionUID = 1L;

      // Column names.
      private String[] columnNames =
      {
         "Public key hash",
         "Coins"
      };

      // Row data.
      Object[][] rowData = new Object[0][2];

      public int getColumnCount()
      {
         return(columnNames.length);
      }


      public int getRowCount()
      {
         return(rowData.length);
      }


      public String getColumnName(int col)
      {
         return(columnNames[col]);
      }


      public Object getValueAt(int row, int col)
      {
         return(rowData[row][col]);
      }
   }

   // Wallet row selection listener.
   class WalletListener implements ListSelectionListener
   {
      public void valueChanged(ListSelectionEvent e)
      {
         if (e.getValueIsAdjusting()) { return; }
         ListSelectionModel lsm = (ListSelectionModel)e.getSource();
         if (!lsm.isSelectionEmpty())
         {
            walletSelect(lsm.getMinSelectionIndex());
            lsm.clearSelection();
         }
      }
   }

   // Wallet add button listener.
   class WalletNewListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         walletNew();
      }
   }

   // Wallet delete button listener.
   class WalletDeleteListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         walletDelete();
      }
   }

   // Wallet refresh button listener.
   class WalletRefreshListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         walletRefresh();
      }
   }

   // Select wallet balance.
   void walletSelect(int index)
   {
      statusText.setText("");

      if ((index >= 0) && (index <= wallet.balances.size()))
      {
         wallet.balanceIndex = index;
         Balance balance = wallet.balances.get(index);
         try
         {
            currentPublicKeyText.setText(Utils.stringFromPublicKey(balance.publicKey));
         }
         catch (GeneralSecurityException e)
         {
            statusText.setText(e.getMessage());
         }
         currentCoinsText.setText(balance.coins + "");
      }
   }


   // Add balance to wallet.
   void walletNew()
   {
      statusText.setText("");
      currentPublicKeyText.setText("");
      currentCoinsText.setText("");

      // Generate new key pair and add to wallet.
      try
      {
         KeyPairGenerator kpg    = KeyPairGenerator.getInstance("RSA");
         SecureRandom     random = SecureRandom.getInstance("SHA1PRNG", "SUN");
         kpg.initialize(512, random);
         KeyPair pair = kpg.generateKeyPair();
         wallet.balances.add(new Balance(pair.getPublic(), pair.getPrivate(), 0.0f));
         walletRefresh();
      }
      catch (Exception e)
      {
         statusText.setText(e.getMessage());
      }
   }


   // Delete wallet balance.
   void walletDelete()
   {
      statusText.setText("");
      currentPublicKeyText.setText("");
      currentCoinsText.setText("");

      if ((wallet.balanceIndex >= 0) && (wallet.balanceIndex < wallet.balances.size()))
      {
         wallet.balances.remove(wallet.balanceIndex);
         walletRefresh();
      }
   }


   // Refresh wallet balances.
   void walletRefresh()
   {
      statusText.setText("");
      if (session != null)
      {
         Transaction tx = new Transaction();
         tx.type = Transaction.BALANCE;
         for (Balance balance : wallet.balances)
         {
            tx.addOutput(balance.publicKey, balance.coins);
         }
         try
         {
            Message message = new Message(Message.TRANSACTION_REQUEST);
            message.transaction = tx;
            session.getBasicRemote().sendObject(message);
         }
         catch (Exception e)
         {
            logger.warning("Cannot send transaction to node: " + e.getMessage());
            statusText.setText("Cannot refresh wallet: " + e.getMessage());
         }
      }
      else
      {
         statusText.setText("Cannot refresh wallet: no connection");
      }
   }


   // Display wallet balances.
   void walletDisplay()
   {
      Object[][] rowData = new Object[wallet.balances.size()][2];
      float coins = 0.0f;
      for (int i = 0; i < wallet.balances.size(); i++)
      {
         Balance balance = wallet.balances.get(i);
         coins        += balance.coins;
         rowData[i][0] = balance.publicKeyHash + "";
         rowData[i][1] = balance.coins + "";
      }

      walletTableModel.rowData = rowData;
      walletTableModel.fireTableDataChanged();
      walletSumText.setText(coins + "");
   }


   // Pay table model.
   class PayTableModel extends AbstractTableModel
   {
      private static final long serialVersionUID = 1L;

      // Column names.
      private String[] columnNames =
      {
         "Type",
         "Public key hash",
         "Coins"
      };

      // Row data.
      Object[][] rowData = new Object[0][3];

      public int getColumnCount()
      {
         return(columnNames.length);
      }


      public int getRowCount()
      {
         return(rowData.length);
      }


      public String getColumnName(int col)
      {
         return(columnNames[col]);
      }


      public Object getValueAt(int row, int col)
      {
         return(rowData[row][col]);
      }
   }

   // Pay from button listener.
   class PayFromListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         payFrom();
      }
   }

   // Pay to button listener.
   class PayToListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         payTo();
      }
   }

   // Pay clear button listener.
   class PayClearListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         transaction   = new Transaction();
         inputBalances = new ArrayList<Balance>();
         payRefresh();
      }
   }

   // Mint button listener.
   class PayMintListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         payMint();
      }
   }

   // Pay button listener.
   class PayPayListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         payPay();
      }
   }

   // Node connect button listener.
   class NodeConnectListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         statusText.setText("");
         if (session != null)
         {
            try
            {
               session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Session closed by user"));
            }
            catch (Exception ex) {
               logger.warning("Cannot close connection to node " + nodeText.getText().trim() + ": " + ex.getMessage());
            }
            session = null;
            nodeConnect.setText("Connect");
         }
         else
         {
            String address = nodeText.getText().trim();
            if ((address != null) && !address.isEmpty())
            {
               try
               {
                  session = clientManager.connectToServer(ClientEndpoint.class,
                                                          new URI(Parameters.WEBSOCKET_PROTOCOL + "://" + address + "/ws" + Parameters.URI));
               }
               catch (Exception ex) {
                  statusText.setText("Cannot connect to node " + nodeText.getText().trim() + ": " + ex.getMessage());
                  session = null;
                  return;
               }
               nodeConnect.setText("Disconnect");
            }
         }
      }
   }

   // Node census refresh button listener.
   class NodeCensusListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         statusText.setText("");
         if (session != null)
         {
            String address = nodeText.getText().trim();
            if ((address != null) && !address.isEmpty())
            {
               Message message = new Message(Message.CENSUS_REQUEST);
               try
               {
                  session.getBasicRemote().sendObject(message);
               }
               catch (Exception ex) {
                  statusText.setText("Cannot send census request to " + nodeText.getText().trim() + ": " + ex.getMessage());
               }
            }
         }
         else
         {
            statusText.setText("Cannot refresh census: no connection");
         }
      }
   }

   // Pay from balance.
   void payFrom()
   {
      String currentPublicKey;
      String currentCoins;
      float  coins = 0.0f;

      statusText.setText("");

      currentPublicKey = currentPublicKeyText.getText().trim();
      if (currentPublicKey.equals(""))
      {
         statusText.setText("Public key required");
         return;
      }
      currentCoins = currentCoinsText.getText().trim();
      if (currentCoins.equals(""))
      {
         statusText.setText("Coins required");
         return;
      }
      try
      {
         coins = Float.parseFloat(currentCoins);
      }
      catch (NumberFormatException e)
      {
         statusText.setText("Invalid coins");
         return;
      }
      try
      {
         PublicKey publicKey = Utils.stringToPublicKey(currentPublicKey);
         for (Balance balance : inputBalances)
         {
            if (balance.publicKey.hashCode() == publicKey.hashCode())
            {
               statusText.setText("Duplicate public key");
               return;
            }
         }
         for (Balance balance : wallet.balances)
         {
            if (balance.publicKey.hashCode() == publicKey.hashCode())
            {
               if (balance.coins != coins)
               {
                  statusText.setText("Invalid coins: try refreshing wallet");
                  return;
               }
               transaction.addInput(balance.publicKeyHash, Utils.signMessage(balance.privateKey,
                                                                             Utils.intToBytes(balance.publicKeyHash)));
               inputBalances.add(balance);
               payRefresh();
               return;
            }
         }
      }
      catch (GeneralSecurityException e)
      {
         statusText.setText("Invalid public key");
         return;
      }
   }


   // Pay to balance.
   void payTo()
   {
      String currentPublicKey;
      String currentCoins;
      float  coins = 0.0f;

      statusText.setText("");

      currentPublicKey = currentPublicKeyText.getText().trim();
      if (currentPublicKey.equals(""))
      {
         statusText.setText("Public key required");
         return;
      }
      currentCoins = currentCoinsText.getText().trim();
      if (currentCoins.equals(""))
      {
         statusText.setText("Coins required");
         return;
      }
      try
      {
         coins = Float.parseFloat(currentCoins);
      }
      catch (NumberFormatException e)
      {
         statusText.setText("Invalid coins");
         return;
      }
      try
      {
         transaction.addOutput(Utils.stringToPublicKey(currentPublicKey), coins);
         payRefresh();
      }
      catch (GeneralSecurityException e)
      {
         statusText.setText("Invalid public key");
      }
   }


   // Mint transaction.
   void payMint()
   {
      statusText.setText("");
      transaction.type = Transaction.MINT;
      payTx(transaction);
   }


   // Pay transaction.
   void payPay()
   {
      statusText.setText("");
      transaction.type = Transaction.PAYMENT;
      payTx(transaction);
   }


   // Payment transaction.
   void payTx(Transaction transaction)
   {
      if (session != null)
      {
         try
         {
            Message message = new Message(Message.TRANSACTION_REQUEST);
            message.transaction = transaction;
            if (transaction.type == Transaction.MINT)
            {
               message.password = password;
            }
            session.getBasicRemote().sendObject(message);
         }
         catch (Exception e)
         {
            String message = "Cannot send transaction to node: " + e.getMessage();
            logger.warning(message);
            statusText.setText(message);
         }
      }
      else
      {
         statusText.setText("Cannot send transaction: no connection");
      }
   }


   // Send ledger message.
   void ledgerMessage(int type)
   {
      if (session != null)
      {
         try
         {
            Message message = new Message(type);
            message.password = password;
            session.getBasicRemote().sendObject(message);
         }
         catch (Exception e)
         {
            String message = "Cannot send ledger message to node: " + e.getMessage();
            logger.warning(message);
            statusText.setText(message);
         }
      }
      else
      {
         statusText.setText("No connection");
      }
   }


   // Refresh pay balances.
   void payRefresh()
   {
      statusText.setText("");
      payDisplay();
   }


   // Display pay balances.
   void payDisplay()
   {
      Object[][] rowData = new Object[transaction.inputs.size() + transaction.outputs.size()][3];
      int j = 0;
      for (int i = 0; i < transaction.inputs.size(); i++, j++)
      {
         Balance balance = inputBalances.get(i);
         rowData[j][0] = "from";
         rowData[j][1] = balance.publicKeyHash + "";
         rowData[j][2] = balance.coins + "";
      }
      for (int i = 0; i < transaction.outputs.size(); i++, j++)
      {
         Transaction.Output output = transaction.outputs.get(i);
         rowData[j][0] = "to";
         rowData[j][1] = Utils.hashPublicKey(output.publicKey) + "";
         rowData[j][2] = output.coins + "";
      }
      payTableModel.rowData = rowData;
      payTableModel.fireTableDataChanged();
   }


   // Main.
   public static void main(String[] args)
   {
      Client client = null;

      try
      {
         client = new Client();
      }
      catch (Exception e)
      {
         System.err.println("Cannot create client: " + e.getMessage());
         return;
      }

      // Show client GUI.
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      client.setSize(WINDOW_SIZE);
      client.setLocation((screenSize.width - WINDOW_SIZE.width) / 2,
                         (screenSize.height - WINDOW_SIZE.height) / 2);
      client.validate();
      client.setVisible(true);
      client.walletDisplay();
   }


   // Status text listener.
   class StatusTextListener implements ActionListener
   {
      public void actionPerformed(ActionEvent e)
      {
         String s = new String(statusText.getText());

         if (s.equals("load ledger"))
         {
            ledgerMessage(Message.LOAD_LEDGER_REQUEST);
            statusText.setText("ledger loaded");
         }
         else if (s.equals("save ledger"))
         {
            ledgerMessage(Message.SAVE_LEDGER_REQUEST);
            statusText.setText("ledger saved");
         }
         else if (s.equals("clear ledger"))
         {
            ledgerMessage(Message.CLEAR_LEDGER_REQUEST);
            statusText.setText("ledger cleared");
         }
         else if (!s.equals(""))
         {
            password = s;
            statusText.setText("password=\"" + password + "\"");
         }
         else
         {
            password = null;
            statusText.setText("password cleared");
         }
      }
   }

   // Listen for Read me link clicks.
   class ReadmeHyperlinkListener implements HyperlinkListener
   {
      public void hyperlinkUpdate(HyperlinkEvent evt)
      {
         if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
         {
            try
            {
               // Show the page in a new browser window.
               if (Desktop.isDesktopSupported())
               {
                  // Windows.
                  Desktop.getDesktop().browse(new URI(evt.getDescription()));
               }
               else
               {
                  // Linux.
                  Runtime runtime = Runtime.getRuntime();
                  runtime.exec("/usr/bin/firefox -new-window " + evt.getDescription());
               }
            }
            catch (Exception e) {}
         }
      }
   }
}
