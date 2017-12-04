
package com.voip.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

import com.skype.Friend;
import com.skype.SkypeException;
import com.voip.steganography.Manager;
import com.voip.ui.listener.StartStatus;
import com.voip.ui.listener.UIStatusListener;

public class MainFrame extends JFrame {
	private Logger logger = Logger.getLogger(MainFrame.class);
	private static final long serialVersionUID = 1L;
	private static UIStatusListener statusListener;
	private static List<Friend> friends;
	private JPanel jContentPane = null;  //  jve:decl-index=0:visual-constraint="41,4"
	public JButton sendButton;
	private Component closeButton;
	private JTextArea histText;
	private JTextArea currentText;
	private Component component;
	public StartStatus startStatus = StartStatus.Waiting;
	public JButton callButton;
	public JButton doneButton;
	public JList list;
	private JTextArea statusText;
	private List<Friend> friendList;
	private boolean recCallStatus = false;
	private String[] friendsName;
	/**
	 * This is the default constructor
	 */
	public MainFrame() {
		super();
		
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.component = this;
		this.setSize(546, 442);
		this.getContentPane().setLayout(null);
		
		sendButton = new JButton("Send");
		sendButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String str = currentText.getText();
				if (!sendButton.isEnabled()){
					return;
				}
					
				if (!str.isEmpty()){
					currentText.setText("");
					displayMyMsg(str);
				}
				Manager.getInstance().sendMsg(str);
			}
			
		});
		
		sendButton.setBounds(119, 349, 74, 23);
		getContentPane().add(sendButton);
		sendButton.setEnabled(false);
		
		closeButton = new JButton("Close");
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				logger.info("Exit: clean the environment");
				Manager.getInstance().exit();
				System.exit(DISPOSE_ON_CLOSE);
			}
		});

		closeButton.setBounds(211, 349, 74, 23);
		getContentPane().add(closeButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 317, 249);
		getContentPane().add(scrollPane);
		
		histText = new JTextArea();
		histText.setLineWrap(true);
		scrollPane.setViewportView(histText);
		histText.setEditable(false);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 271, 317, 67);
		getContentPane().add(scrollPane_1);
		
		currentText = new JTextArea();
		currentText.setLineWrap(true);
		scrollPane_1.setViewportView(currentText);
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(337, 31, 174, 112);
		getContentPane().add(scrollPane_2);
		
		list = new JList();
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				int selectIndex = list.getSelectedIndex();
				if (selectIndex == -1){
					callButton.setEnabled(false);
				}else{
					callButton.setEnabled(true);
				}
			}
		});
		scrollPane_2.setViewportView(list);
		
		JLabel lblNewLabel = new JLabel("Friends:");
		lblNewLabel.setBounds(339, 11, 60, 14);
		getContentPane().add(lblNewLabel);
		
		callButton = new JButton("Call");
		callButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int selectedIndex = list.getSelectedIndex();
				Friend f = friendList.get(selectedIndex);
				Manager.getInstance().callFriend(f);
			}
		});
		callButton.setEnabled(false);
		callButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
			}
		});
		callButton.setBounds(337, 150, 74, 23);
		getContentPane().add(callButton);
		
		doneButton = new JButton("Done");
		doneButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Manager.getInstance().finishCall();
			}
		});
		doneButton.setEnabled(false);
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		doneButton.setBounds(437, 150, 74, 23);
		getContentPane().add(doneButton);
		
		JScrollPane scrollPane_3 = new JScrollPane();
		scrollPane_3.setBounds(337, 196, 131, 58);
		getContentPane().add(scrollPane_3);
		
		statusText = new JTextArea();
		statusText.setEditable(false);
		scrollPane_3.setViewportView(statusText);
		
		JButton RefreshButton = new JButton("Refresh");
		RefreshButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				list.clearSelection();
				List<Friend> friends = Manager.getInstance().getFriends();
				displayFriendList(friends);
				setFriendList(friends);
			}
		});
		RefreshButton.setBounds(422, 7, 89, 23);
		getContentPane().add(RefreshButton);
		
		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setBounds(339, 184, 60, 14);
		getContentPane().add(lblStatus);
		this.setTitle("Steganography");
	}
	
	public void displayMyMsg(String str) {
		SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
		String   date   =   sDateFormat.format(new   java.util.Date());
		str = "Sent Msg:   " + date + ":\n  " + str;
		logger.info("displayMyMsg:" + str);
		displayAsHistoryMsg(str);
	}
	
	public void displayOtherMsg(String str) {
		logger.info("displayOtherMsg:" + str);
		SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
		String   date   =   sDateFormat.format(new   java.util.Date());
//		String name = this.getSelectedFriendName();
		String name = "Received Msg";
		str = name + ":   " + date + ":\n  " + str;
		logger.info("displayOtherMsg:" + str);
		displayAsHistoryMsg(str);
	}
	
	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
		}
		return jContentPane;
	}
	
	public void displayStatus(String statusStr){
		logger.info("UI display: " + statusStr);
		this.statusText.setText("Calling status:" + statusStr);
	}
	
	public void displayRecCallingStatus(String statusStr, String callID){
		this.statusText.setText("Received Calling status:" + statusStr + ". From your friend: " + callID);
	}
	
	public void setFriendList(List<Friend> friends){
		this.friendList = friends;
	}
	
	public static void main(String[] args){
//		System.out.println(System.getProperty("java.library.path"));
		
		MainFrame mainFrame = new MainFrame();
		mainFrame.setVisible(true);
		mainFrame.setResizable(false);
	
		statusListener = new UIStatusListener(mainFrame);
		mainFrame.displayStartInfo();
		Manager.getInstance().setUIListener(statusListener);
		friends = Manager.getInstance().getFriends();
		mainFrame.displayFriendList(friends);
		mainFrame.setFriendList(friends);
	}
	
	private void displayFriendList(List<Friend> friends){
		friendsName = new String[friends.size()];
		String[] friendsStat = new String[friends.size()];
		for (int i=0;i<friends.size();i++){
			friendsName[i] = friends.get(i).getId();
			try {
				friendsStat[i] = friends.get(i).getStatus().toString();
			} catch (SkypeException e) {
				e.printStackTrace();
			}
		}
		list.clearSelection();
		list.setListData(friends.toArray());
		
	}
	public String getSelectedFriendName(){
		int index = list.getSelectedIndex();
		logger.info("index:" + index);
//		String name = (String) list.getSelectedValue();
		String name = friendsName[index];
		logger.info(" name:" + name);
		if (name == null){
			return "";
		}else{
			return name;
		}
	}
	
	private void displayStartInfo(){
		String startMsg = "Welcome to Steganographic Message!" ;
		displayAsHistoryMsg(startMsg);
	}
	
	
	public void displayAsHistoryMsg(String msg){
		this.histText.append("\n" + msg);
	}
	
	
	public void newCallMakedDone(String statusStr, String callID) {
		this.callButton.setEnabled(true);
		this.doneButton.setEnabled(false);
		this.sendButton.setEnabled(false);
		this.displayStatus(statusStr);
	}
	
	public void newCallMakedInProgress(String statusStr, String callID) {
		this.callButton.setEnabled(false);
		this.doneButton.setEnabled(true);
		this.displayStatus(statusStr);
	}

	public void newCallMakedFinish(String statusStr, String callID) {
		newCallMakedDone(statusStr, callID);
	}

	public void newCallRec(String statusStr, String callID) {
		this.recCallStatus  = true;
		this.displayRecCallingStatus(statusStr, callID);
	}

	public void newCallRecInProgress(String statusStr, String callID) {
		this.doneButton.setEnabled(true);
		this.callButton.setEnabled(false);
		this.displayRecCallingStatus(statusStr, callID);
	}

	
	public void newCallRecFinish(String statusStr, String callID) {
		this.doneButton.setEnabled(false);
		this.callButton.setEnabled(true);
		this.sendButton.setEnabled(false);
		this.displayRecCallingStatus(statusStr, callID);
	}
}  
