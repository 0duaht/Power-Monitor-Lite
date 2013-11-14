import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.TrayIcon;

import Resources.Kernel32;

import com.sun.jna.Native;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.ImageIcon;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.ExecutorService;
import java.awt.Font;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JRadioButton;
import javax.swing.JSeparator;

public class MainWindow extends WindowAdapter implements ItemListener, ActionListener, Runnable, WindowListener{
	private JFrame frame;
	private JMenuBar menuBar;
	private JMenu mnAbout;
	private JMenuItem mntmHowToUse, mntmFurtherHelp, mntmBatteryStatus;
	private JLabel timerLabel, statusLabel;
	private ExecutorService executor = Executors.newCachedThreadPool(), executor2 = Executors.newCachedThreadPool();
	private JButton startButton, stopButton;
	private JRadioButton acRadio, batteryRadio;
	private JComboBox actionCombo, timeCombo;
	private String[] options = {"Hibernate", "Shutdown"};
	private String[] timeOptions = {"Seconds", "Minutes", "Hours"};
	private JTextField timeTextField;
	private ImageIcon trayImage = new ImageIcon(getClass().getResource("/Resources/trayicon.png"));
	private int totalTime, initialTime, percent;
	private String reqText;
	private boolean powerAction = false;
	private Lock powerLock = new ReentrantLock();
	private Condition acUnplugged = powerLock.newCondition();
	private Condition acPlugged = powerLock.newCondition();
	private Timer timerAction = new Timer(1000, this);
	private TrayIcon trayIcon = new TrayIcon(trayImage.getImage(), "Power Manager Lite");
	private static SystemTray tray;
	private JTextField batteryTextField;
	Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);
	Kernel32.SYSTEM_POWER_STATUS batteryStatus = new Kernel32.SYSTEM_POWER_STATUS();
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void windowIconified(WindowEvent ev)
	{
		frame.setVisible(false);
	}
	public void trayImage()
	{
		if (SystemTray.isSupported())
			tray = SystemTray.getSystemTray();
		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEv)
			{
				frame.setVisible(true);
				frame.setExtendedState(JFrame.NORMAL);
			}
		});
		try {
	        tray.add(trayIcon);
	      } catch (AWTException e) {
	        System.err.println("TrayIcon could not be added.");
	      }
	}
	public void run()
	{
		while(true)
		{
			powerLock.lock();
			if (acRadio.isSelected())
			{
				acRadio.setEnabled(false);
				batteryRadio.setEnabled(false);
				INSTANCE.GetSystemPowerStatus(batteryStatus);
				if (batteryStatus.getACLineStatus() == 0)
				{
					SwingUtilities.invokeLater(new Runnable(){
						public void run()
						{
							reqText = "AC Adapter Off!";
							totalTime = initialTime;
							timerAction.start();
							statusLabel.setText("Action Requirement Met: " + reqText);
							trayIcon.displayMessage(null,  "Action Requirement Met: " + reqText,  TrayIcon.MessageType.INFO);
						}
					});
					try {
						acUnplugged.signal();
						acPlugged.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				else
				{
					if (powerAction)
						break;
					continue;
				}
			}
			else if (batteryRadio.isSelected())
			{
				acRadio.setEnabled(false);
				batteryRadio.setEnabled(false);
				INSTANCE.GetSystemPowerStatus(batteryStatus);
				if (powerAction)
					break;
				if (batteryStatus.getACLineStatus() == 1)
					continue;
				if (batteryStatus.getBatteryLifeInt() <= percent)
				{
					reqText = "Battery % reached!";
					SwingUtilities.invokeLater(new Runnable(){
						public void run()
						{
							totalTime = initialTime;
							timerAction.start();
							statusLabel.setText("Action Requirement Met: " + reqText);
							trayIcon.displayMessage(null,  "Action Requirement Met: " + reqText,  TrayIcon.MessageType.INFO);
						}
					});
					try {
						acUnplugged.signal();
						acPlugged.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				else
				{
					if (powerAction)
						break;
					continue;
				}
			}
			else
				break;
		}
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		nimbusSet();
		trayImage();
		initialize();	
		INSTANCE.GetSystemPowerStatus(batteryStatus);
		if (batteryStatus.getBatteryLifeInt() != -1)
			batteryRadio.setEnabled(true);
		if (batteryStatus.getACLineStatus() == 0 | batteryStatus.getACLineStatus() == 1)
		{
			acRadio.setSelected(true);
			acRadio.setEnabled(true);
		}
		else
			acRadio.setSelected(false);
		if (!System.getProperty("os.name").substring(0, 3).equalsIgnoreCase("win"))
		{
			statusLabel.setText("Your PC/OS is not yet supported!");
			trayIcon.displayMessage(null, "Your PC/OS is not yet supported!", TrayIcon.MessageType.INFO);
			startButton.setEnabled(false);
		}
	}

	/**
	 * Set Look and Feel to Nimbus Look and Feel
	 */
	public void nimbusSet()
	{
		try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
        }	
	}
	public void itemStateChanged(ItemEvent itemEv) // ItemListeners for the radio buttons
	{
		if (itemEv.getSource() == batteryRadio)
		{
			batteryTextField.setEnabled(true);
		}
		if (itemEv.getSource() == acRadio)
		{
			batteryTextField.setEnabled(false);
		}
	}
	public void actionPerformed(ActionEvent actEv) // ActionListeners for the buttons and menu items
	{
		if (actEv.getSource() == startButton)
		{
			// confirm that time interval entered is valid
			int timeEntered = 0;
			while (timeEntered == 0)
			{
				try{
					timeEntered = Integer.parseInt(timeTextField.getText());
				}
				catch(NumberFormatException e)
				{	
					JOptionPane.showMessageDialog(frame, "Enter valid time interval", "Invalid Input", JOptionPane.ERROR_MESSAGE);
					timeTextField.setText("");
					return;
				}
			}
			
			// confirm that percentage level is valid
			if (batteryRadio.isSelected())
			{
				percent = 16;
				while (percent < 20 | percent > 100)
				{
					try{
						percent = Integer.parseInt(batteryTextField.getText());
						if (percent > 100 | percent < 20)
							throw new NumberFormatException("");
					}
					catch(NumberFormatException ex)
					{
						JOptionPane.showMessageDialog(frame, "Enter valid percentage level", "Invalid Input", JOptionPane.ERROR_MESSAGE);
						batteryTextField.setText("");
						return;
					}
				}
			}
			
			// get time interval in seconds
			switch(timeCombo.getSelectedIndex())
			{	
			case 0:
				totalTime = timeEntered;
				initialTime = totalTime;
				if (totalTime > 360000000 | totalTime < 0)
				{
					JOptionPane.showMessageDialog(frame, "Enter valid time interval", "Above Limit", JOptionPane.ERROR_MESSAGE);
					timeTextField.setText("");
					return;
				}
				break;	
			case 1:	
				totalTime = timeEntered * 60;
				initialTime = totalTime;
				if (totalTime > 360000000 | totalTime < 0)
				{
					JOptionPane.showMessageDialog(frame, "Enter valid time interval", "Above Limit", JOptionPane.ERROR_MESSAGE);
					timeTextField.setText("");
					return;
				}
				break;
			case 2: 
				totalTime = timeEntered * 3600;
				initialTime = totalTime;
				if (totalTime > 360000000 | totalTime < 0)
				{
					JOptionPane.showMessageDialog(frame, "Enter valid time interval", "Above Limit", JOptionPane.ERROR_MESSAGE);
					timeTextField.setText("");
					return;
				}
				break;
			}
			statusLabel.setText("Now listening for power events...");
			trayIcon.displayMessage(null, "Now listening for power events...",  TrayIcon.MessageType.INFO);
			startButton.setEnabled(false);
			stopButton.setEnabled(true);
			powerAction = false;         // set in order to keep running (looping) the monitor thread (Runnable)
			
			// thread to move mouse every 100 seconds to prevent computer from going to sleep
			executor2.execute(new Runnable(){
				public void run()
				{
					while(true)
					{
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
						Robot roboT = null;
						try {
							roboT = new Robot();
						} catch (AWTException e) {
							e.printStackTrace();
						}
						if (roboT != null)
							roboT.mouseMove(mouseLocation.x, mouseLocation.y);
						
					}
				}
			});
			
			// thread to monitor when/if ac power is plugged in
			executor.execute(new Runnable(){
				public void run()
				{
					powerLock.lock();
					try {
						acUnplugged.await(); // placed in waiting state to free the lock for the monitor thread when it starts execution
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// loop starts when a requirement has been met, and a signal received
					while(true)
					{
						if (powerAction)
							break;
						INSTANCE.GetSystemPowerStatus(batteryStatus);
						if (acRadio.isSelected() | batteryRadio.isSelected())
						{
							if (batteryStatus.getACLineStatus() == 1)
							{
								timerAction.stop();
								timerLabel.setText("");
								statusLabel.setText("AC Plugged in..Action deferred");
								trayIcon.displayMessage(null, "AC Plugged in..Action deferred",  TrayIcon.MessageType.INFO);
								acPlugged.signalAll(); // signal monitor thread to continue monitoring
								try {
									acUnplugged.await(); // placed in waiting state for when next monitor requirement is met 
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						else
							break;
					}
				}
			});
			executor.execute(this);
		}
		
		// if monitor is stopped
		if (actEv.getSource() == stopButton)
		{
			if (timerAction.isRunning())
				timerAction.stop();
			statusLabel.setText("Action Cancelled...");
			trayIcon.displayMessage(null, "Action Cancelled...",  TrayIcon.MessageType.INFO);
			timerLabel.setText("");
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			powerAction = true;  // break out of all currently running monitor threads
			acRadio.setEnabled(true);
			batteryRadio.setEnabled(true);
		}
		if (actEv.getSource() == timerAction)
		{
			// timer to count down interval
			if (totalTime == 1) // interval stop
			{
				timerAction.stop();
				statusLabel.setText("Time Elapsed. Performing Action...");
				trayIcon.displayMessage(null, "Time Elapsed. Performing Action...",  TrayIcon.MessageType.INFO);
				timerLabel.setText("Time Left: " + 0);
				try{
					switch(actionCombo.getSelectedIndex())
					{
					case 0:  // hibernate
						Runtime.getRuntime().exec("shutdown -h");
						System.exit(0);
						break;
					case 1: // shutdown
						Runtime.getRuntime().exec("shutdown -s");
						System.exit(0);
						break;
					}
				}
				catch(IOException ex)
				{
					timerLabel.setText("Error while trying to perform action");
					timerAction.stop();
					timerLabel.setText("Time Left: " + 0);
					return;
				}
			}
			else
			{
				timerLabel.setText("Time Left: " + --totalTime);
				timerLabel.setToolTipText(timerLabel.getText());
			}
		}
		
		// how-to-use menu item
		if (actEv.getSource() == mntmHowToUse)
		{
			String details = "-  Select desired monitor requirement ( AC Power or Battery %).\n"
					+ "-  The AC Power monitor acts when the power cable is unplugged.\n"
					+ "-  The Battery Percentage monitor acts when battery level is below the specified percentage.\n"
					+ "-  The percentage level specified must be between 20% and 100%, inclusive. Integers only.\n" +
		"-  Choose action to be performed when requirement is met.\n" +
					"-  Enter the amount of time (interval) application waits after requirement is met, before action is performed,\n"
		+ "-  Then click Start.\n" + "-  To cancel at any time, click the Stop button.\n"
					+ "\n-  On some rare occasions, both monitors might not be supported."
					+ "\n-  This could happen with non-Windows PCs or corrupt software/hardware status";
			JOptionPane.showMessageDialog(null, details, "How to Use", JOptionPane.INFORMATION_MESSAGE);
		}
		
		// battery status menu item
		if (actEv.getSource() == mntmBatteryStatus)
		{
			INSTANCE.GetSystemPowerStatus(batteryStatus);
			JOptionPane.showMessageDialog(frame, batteryStatus, "Battery Information", JOptionPane.INFORMATION_MESSAGE);
		}
		
		// further help menu item
		if (actEv.getSource() == mntmFurtherHelp)
		{
			JOptionPane.showMessageDialog(frame, "Send me a mail: oduaht@gmail.com", "_where", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	private void initialize() {
		Random generate = new Random();
		int count = generate.nextInt(2);
		frame = new JFrame("Power Monitor Lite");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setIconImage(trayImage.getImage());
		frame.addWindowListener(this);
		
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		mnAbout = new JMenu("About");
		mnAbout.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		menuBar.add(mnAbout);
		
		mntmHowToUse = new JMenuItem("How to Use");
		mntmHowToUse.setBorder(null);
		mntmHowToUse.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		mnAbout.add(mntmHowToUse);
		mntmHowToUse.addActionListener(this);
		
		mntmBatteryStatus = new JMenuItem("Battery Status");
		mntmBatteryStatus.setBorder(null);
		mntmBatteryStatus.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		mnAbout.add(mntmBatteryStatus);
		mntmBatteryStatus.addActionListener(this);
		
		mntmFurtherHelp = new JMenuItem("Further Help");
		mntmFurtherHelp.setBorder(null);
		mntmFurtherHelp.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		mnAbout.add(mntmFurtherHelp);
		mntmFurtherHelp.addActionListener(this);
		frame.setContentPane(new JLabel(new ImageIcon(getClass().getResource("/Resources/img" + count + ".jpg"))));
		
		JLabel lblAction = new JLabel("Action:");
		lblAction.setBounds(97, 73, 72, 24);
		lblAction.setForeground(Color.PINK);
		lblAction.setFont(new Font("Segoe Print", Font.PLAIN, 16));
		
		actionCombo = new JComboBox(options);
		actionCombo.setBounds(187, 70, 174, 32);
		actionCombo.setForeground(Color.BLACK);
		actionCombo.setOpaque(false);
		actionCombo.setFont(new Font("Segoe Print", Font.PLAIN, 13));
		actionCombo.setToolTipText("Choose action");
		actionCombo.addActionListener(this);
		
		timeTextField = new JTextField();
		timeTextField.setFont(new Font("Segoe Print", Font.PLAIN, 13));
		timeTextField.setBounds(83, 120, 86, 28);
		timeTextField.setColumns(10);
		timeTextField.setToolTipText("Time Interval");
		
		timeCombo = new JComboBox(timeOptions);
		timeCombo.setBounds(197, 117, 121, 32);
		timeCombo.setFont(new Font("Segoe Print", Font.PLAIN, 13));
		timeCombo.setToolTipText("");
		timeCombo.addActionListener(this);
		
		startButton = new JButton("Start");
		startButton.setBounds(94, 171, 93, 33);
		startButton.setOpaque(false);
		startButton.setForeground(Color.BLACK);
		startButton.setBorder(null);
		startButton.setFont(new Font("Segoe Print", Font.PLAIN, 14));
		startButton.addActionListener(this);
		
		stopButton = new JButton("Stop");
		stopButton.setBounds(219, 171, 100, 32);
		stopButton.setOpaque(false);
		stopButton.setFont(new Font("Segoe Print", Font.PLAIN, 14));
		stopButton.addActionListener(this);
		stopButton.setEnabled(false);
		
		acRadio = new JRadioButton("Monitor AC Line");
		acRadio.setBounds(31, 17, 143, 35);
		acRadio.setOpaque(false);
		acRadio.setForeground(Color.PINK);
		acRadio.setFont(new Font("Segoe Print", Font.PLAIN, 14));
		acRadio.addItemListener(this);
		acRadio.setEnabled(false);
		
		batteryRadio = new JRadioButton("Monitor Battery %");
		batteryRadio.setEnabled(false);
		batteryRadio.setBounds(192, 18, 149, 33);
		batteryRadio.setOpaque(false);
		batteryRadio.setForeground(Color.PINK);
		batteryRadio.setFont(new Font("Segoe Print", Font.PLAIN, 13));
		batteryRadio.addItemListener(this);
		ButtonGroup actionSource = new ButtonGroup();
		actionSource.add(acRadio);
		actionSource.add(batteryRadio);
		
		batteryTextField = new JTextField();
		batteryTextField.setBounds(348, 20, 63, 27);
		batteryTextField.setEnabled(false);
		batteryTextField.setToolTipText("Set Battery % at which to carry out Action");
		batteryTextField.setFont(new Font("Segoe Print", Font.PLAIN, 13));
		batteryTextField.setColumns(10);
		
		timerLabel = new JLabel("");
		timerLabel.setBounds(313, 208, 121, 30);
		timerLabel.setForeground(Color.PINK);
		timerLabel.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		
		frame.getContentPane().setLayout(null);
		frame.getContentPane().add(lblAction);
		frame.getContentPane().add(startButton);
		frame.getContentPane().add(timeTextField);
		frame.getContentPane().add(stopButton);
		frame.getContentPane().add(actionCombo);
		frame.getContentPane().add(timeCombo);
		frame.getContentPane().add(acRadio);
		frame.getContentPane().add(batteryRadio);
		frame.getContentPane().add(batteryTextField);
		frame.getContentPane().add(timerLabel);
		
		statusLabel = new JLabel("");
		statusLabel.setFont(new Font("Segoe Print", Font.PLAIN, 12));
		statusLabel.setForeground(Color.PINK);
		statusLabel.setBounds(31, 208, 278, 30);
		frame.getContentPane().add(statusLabel);
	}
}
