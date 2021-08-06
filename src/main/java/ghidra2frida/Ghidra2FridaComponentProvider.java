package ghidra2frida;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import docking.ActionContext;
import docking.WindowPosition;
import docking.action.DockingAction;
import docking.action.ToolBarData;
import ghidra.framework.options.SaveState;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
import ghidra.util.Msg;
import resources.ResourceManager;

public class Ghidra2FridaComponentProvider extends ComponentProviderAdapter {
	
	private final static ImageIcon ACTIVE_ICON = ResourceManager.loadImage("images/green-circle.png");
	private final static ImageIcon INACTIVE_ICON = ResourceManager.loadImage("images/red-circle.png");

	private Ghidra2FridaServiceProvider provider;
	
	private JTabbedPane tabbedPanel;
	
	private DockingAction serverAction;
	private DockingAction applicationAction;

	private JTextField pythonPathVenv;
	private JButton pythonPathVenvButton;
	private JTextField pyroHost;
	private JTextField pyroPort;
	private JTextField fridaPath;
	private JTextField applicationId;
	private JCheckBox useVirtualEnvCheckBox;
	private JLabel labelPythonPathVenv;
	private JRadioButton remoteRadioButton;
	private JRadioButton usbRadioButton;
	private JRadioButton localRadioButton;

	private PluginTool pluginTool;
	
	
	public Ghidra2FridaComponentProvider(PluginTool tool, String name, Ghidra2FridaServiceProvider provider) {
		
		super(tool, name, name);		
		create();
		
		Icon g2fIcon = ResourceManager.getScaledIcon(ResourceManager.loadImage("images/g2f_logo1.png"), 16, 16);
		setIcon(g2fIcon);
		
		setDefaultWindowPosition(WindowPosition.BOTTOM);
		setTitle("ghidra2frida");
		setVisible(true);
		createActions();
		
		this.provider = provider;
		
		this.pluginTool = tool;
		
	}

	@Override
	public JComponent getComponent() {
		return tabbedPanel;
	}

	private void create() {
		
		tabbedPanel = new JTabbedPane();
		
		
		JPanel confPanel = new JPanel();
		confPanel.setLayout(new BoxLayout(confPanel, BoxLayout.Y_AXIS));
		
		JScrollPane confPanelScrollPane = new JScrollPane(confPanel);
		confPanelScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		confPanelScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		confPanelScrollPane.setBorder(new LineBorder(Color.BLACK));
		confPanelScrollPane.setMaximumSize( confPanelScrollPane.getPreferredSize() );

		JPanel virtualEnvPanel = new JPanel();
		virtualEnvPanel.setLayout(new BoxLayout(virtualEnvPanel, BoxLayout.X_AXIS));
		virtualEnvPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel labelUseVirtualEnv = new JLabel("Use virtual env: ");
		useVirtualEnvCheckBox = new JCheckBox();
		useVirtualEnvCheckBox.setSelected(false);
		useVirtualEnvCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					labelPythonPathVenv.setText("Virtual env folder: ");
					pythonPathVenvButton.setText("Select folder");
				} else {
					labelPythonPathVenv.setText("Python binary path: ");
					pythonPathVenvButton.setText("Select file");
				}
			}
		});
		virtualEnvPanel.add(labelUseVirtualEnv);
		virtualEnvPanel.add(useVirtualEnvCheckBox);

		// The same field is used to take the virtual env folder, if virtual env checkbox is selected
		JPanel pythonPathVenvPanel = new JPanel();
		pythonPathVenvPanel.setLayout(new BoxLayout(pythonPathVenvPanel, BoxLayout.X_AXIS));
		pythonPathVenvPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		labelPythonPathVenv = new JLabel("Python binary path: ");

		pythonPathVenv = new JTextField(50);
		if (System.getProperty("os.name").startsWith("Windows")) {
			pythonPathVenv.setText("C:\\python27\\python");
		} else {
			pythonPathVenv.setText("/usr/bin/python");
		}

		pythonPathVenv.setMaximumSize(pythonPathVenv.getPreferredSize());
		pythonPathVenvButton = new JButton("Select file");
		pythonPathVenvButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JFrame parentFrame = new JFrame();
						JFileChooser fileChooser = new JFileChooser();
						
						if(useVirtualEnvCheckBox.isSelected()) {							
							fileChooser.setDialogTitle("Virtual ENV folder");
							fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);							
						} else {							
							fileChooser.setDialogTitle("Python Path");							
						}						
						fileChooser.setCurrentDirectory(new File(fridaPath.getText().trim()));
						

						int userSelection = fileChooser.showOpenDialog(parentFrame);

						if (userSelection == JFileChooser.APPROVE_OPTION) {

							final File pythonPathFile = fileChooser.getSelectedFile();

							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									pythonPathVenv.setText(pythonPathFile.getAbsolutePath());
								}

							});

						}
					}
				});
			}
		});
		pythonPathVenvPanel.add(labelPythonPathVenv);
		pythonPathVenvPanel.add(pythonPathVenv);
		pythonPathVenvPanel.add(pythonPathVenvButton);

		JPanel pyroHostPanel = new JPanel();
		pyroHostPanel.setLayout(new BoxLayout(pyroHostPanel, BoxLayout.X_AXIS));
		pyroHostPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel labelPyroHost = new JLabel("Pyro host: ");
		pyroHost = new JTextField(50);
		pyroHost.setText("localhost");
		pyroHost.setMaximumSize(pyroHost.getPreferredSize());
		pyroHostPanel.add(labelPyroHost);
		pyroHostPanel.add(pyroHost);

		JPanel pyroPortPanel = new JPanel();
		pyroPortPanel.setLayout(new BoxLayout(pyroPortPanel, BoxLayout.X_AXIS));
		pyroPortPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel labelPyroPort = new JLabel("Pyro port: ");
		pyroPort = new JTextField(50);
		pyroPort.setText("9999");
		pyroPort.setMaximumSize(pyroPort.getPreferredSize());
		pyroPortPanel.add(labelPyroPort);
		pyroPortPanel.add(pyroPort);

		JPanel fridaPathPanel = new JPanel();
		fridaPathPanel.setLayout(new BoxLayout(fridaPathPanel, BoxLayout.X_AXIS));
		fridaPathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel labelFridaPath = new JLabel("Frida JS file: ");
		fridaPath = new JTextField(50);
		if (System.getProperty("os.name").startsWith("Windows")) {
			fridaPath.setText("C:\\burp\\ghidra2frida\\ghidra2frida.js");
		} else {
			fridaPath.setText("/opt/burp/ghidra2frida/ghidra2frida.js");
		}
		fridaPath.setMaximumSize(fridaPath.getPreferredSize());
		JButton fridaPathButton = new JButton("Select file");
		fridaPathButton.addActionListener(e -> {			
			SwingUtilities.invokeLater(() -> {
				JFrame parentFrame = new JFrame();
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Frida JS file");
				fileChooser.setCurrentDirectory(new File(fridaPath.getText().trim()));
				int userSelection = fileChooser.showOpenDialog(parentFrame);
				if (userSelection == JFileChooser.APPROVE_OPTION) {
					final File fridaPathFile = fileChooser.getSelectedFile();
					fridaPath.setText(fridaPathFile.getAbsolutePath());
				}
			});
		});
		JButton fridaDefaultPathButton = new JButton("Create skeleton JS file");
		fridaDefaultPathButton.addActionListener(e -> {			
			SwingUtilities.invokeLater(() -> {
				JFrame parentFrame = new JFrame();
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Select location for Frida skeleton JS file");
				fileChooser.setCurrentDirectory(new File(fridaPath.getText().trim()));
				int userSelection = fileChooser.showSaveDialog(parentFrame);
				if (userSelection == JFileChooser.APPROVE_OPTION) {
					final File fridaPathFile = fileChooser.getSelectedFile();
					provider.copyJsSkeleton(fridaPathFile);
					fridaPath.setText(fridaPathFile.getAbsolutePath());					
				}				
			});
		});
		fridaPathPanel.add(labelFridaPath);
		fridaPathPanel.add(fridaPath);
		fridaPathPanel.add(fridaPathButton);
		fridaPathPanel.add(fridaDefaultPathButton);

		JPanel applicationIdPanel = new JPanel();
		applicationIdPanel.setLayout(new BoxLayout(applicationIdPanel, BoxLayout.X_AXIS));
		applicationIdPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel labelApplicationId = new JLabel("Application ID (spawn) / PID (attach): ");
		applicationId = new JTextField(50);
		applicationId.setText("org.test.application");
		applicationId.setMaximumSize(applicationId.getPreferredSize());
		applicationIdPanel.add(labelApplicationId);
		applicationIdPanel.add(applicationId);

		JPanel localRemotePanel = new JPanel();
		localRemotePanel.setLayout(new BoxLayout(localRemotePanel, BoxLayout.X_AXIS));
		localRemotePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		remoteRadioButton = new JRadioButton("Frida Remote");
		usbRadioButton = new JRadioButton("Frida USB");
		localRadioButton = new JRadioButton("Frida Local");
		remoteRadioButton.setSelected(true);
		ButtonGroup localRemoteButtonGroup = new ButtonGroup();
		localRemoteButtonGroup.add(remoteRadioButton);
		localRemoteButtonGroup.add(usbRadioButton);
		localRemoteButtonGroup.add(localRadioButton);
		localRemotePanel.add(remoteRadioButton);
		localRemotePanel.add(usbRadioButton);
		localRemotePanel.add(localRadioButton);
		
		JPanel buttonsServerControlPanel = new JPanel();
		buttonsServerControlPanel.setLayout(new BoxLayout(buttonsServerControlPanel, BoxLayout.X_AXIS));
		buttonsServerControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel buttonsServerControlLabel = new JLabel("Server: ");
		JButton startServer = new JButton("Start");
		startServer.addActionListener(e -> { 
			if(provider.launchPyroServer(pyroHost.getText(), pyroPort.getText(), useVirtualEnvCheckBox.isSelected(), pythonPathVenv.getText()))
				changeAction("server", true);			
		});
		JButton killServer = new JButton("Kill");
		killServer.addActionListener(e -> {
			if(provider.killPyroServer()) {
				changeAction("server", false);
				changeAction("application", false);
			}
		});
		JButton checkServer = new JButton("Check");
		checkServer.addActionListener(e -> { 
			boolean serverRunningStatus = provider.pingServer();
			changeAction("server", serverRunningStatus);
			if(!serverRunningStatus)
				changeAction("application", serverRunningStatus);
		});
		buttonsServerControlPanel.add(buttonsServerControlLabel);
		buttonsServerControlPanel.add(startServer);
		buttonsServerControlPanel.add(killServer);
		buttonsServerControlPanel.add(checkServer);

		JPanel buttonsApplicationControlPanel = new JPanel();
		buttonsApplicationControlPanel.setLayout(new BoxLayout(buttonsApplicationControlPanel, BoxLayout.X_AXIS));
		buttonsApplicationControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel buttonsApplicationControlLabel = new JLabel("Application: ");
		JButton spawnApplication = new JButton("Spawn");
		spawnApplication.addActionListener(e -> {
			String device = "";
			if(remoteRadioButton.isSelected())
				device = "remote";
			else if(usbRadioButton.isSelected())
				device = "usb";
			else
				device = "local";
			if(provider.spawnApplication(true, device, applicationId.getText(), fridaPath.getText()))
				changeAction("application", true);
		});		
		JButton attachApplication = new JButton("Attach");
		attachApplication.addActionListener(e -> {
			String device = "";
			if(remoteRadioButton.isSelected())
				device = "remote";
			else if(usbRadioButton.isSelected())
				device = "usb";
			else
				device = "local";
			if(provider.spawnApplication(false, device, applicationId.getText(), fridaPath.getText()))
				changeAction("application", true);
		});	
		JButton killApplication = new JButton("Kill");
		killApplication.addActionListener(e -> {
			if(provider.killOrDetachApplication(true))
				changeAction("application", false);
		});
		JButton detachApplication = new JButton("Detach");
		detachApplication.addActionListener(e -> {
			if(provider.killOrDetachApplication(false))
				changeAction("application", false);
		});
		JButton checkApplication = new JButton("Check");
		checkApplication.addActionListener(e -> changeAction("application", provider.pingApplication()));
		buttonsApplicationControlPanel.add(buttonsApplicationControlLabel);
		buttonsApplicationControlPanel.add(spawnApplication);
		buttonsApplicationControlPanel.add(attachApplication);
		buttonsApplicationControlPanel.add(killApplication);
		buttonsApplicationControlPanel.add(detachApplication);
		buttonsApplicationControlPanel.add(checkApplication);
		
		JPanel buttonsMiscControlPanel = new JPanel();
		buttonsMiscControlPanel.setLayout(new BoxLayout(buttonsMiscControlPanel, BoxLayout.X_AXIS));
		buttonsMiscControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel buttonsMiscControlLabel = new JLabel("Misc: ");
		JButton reloadScript = new JButton("Reload JS");
		reloadScript.addActionListener(e -> provider.reloadJs());
		JButton detachAllHooks = new JButton("Detach all hooks");
		detachAllHooks.addActionListener(e -> {
			int dialogButton = JOptionPane.YES_NO_OPTION;
			JFrame parentDialogResult = new JFrame();
			int dialogResult = JOptionPane.showConfirmDialog(parentDialogResult, "Are you sure to want to detach ALL Frida hooks (including "
					+ "graphical hooks, custom plugin hooks and Frida JS file hooks)? Enabled hooks will be enabled again on next application spawn.", 
					"Confirm detach all", dialogButton);
			if(dialogResult == 0) {
				provider.detachAllHooks();				
			}
		});
		buttonsMiscControlPanel.add(buttonsMiscControlLabel);
		buttonsMiscControlPanel.add(reloadScript);
		buttonsMiscControlPanel.add(detachAllHooks);		

		confPanel.add(virtualEnvPanel);
		confPanel.add(pythonPathVenvPanel);
		confPanel.add(pyroHostPanel);
		confPanel.add(pyroPortPanel);
		confPanel.add(fridaPathPanel);
		confPanel.add(applicationIdPanel);
		confPanel.add(localRemotePanel);
		confPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		confPanel.add(buttonsServerControlPanel);
		confPanel.add(buttonsApplicationControlPanel);
		confPanel.add(buttonsMiscControlPanel);
		
		JPanel exportConsolePanel = new JPanel();
		exportConsolePanel.setLayout(new BorderLayout());		
		
		JTextArea executeExportResultTextArea = new JTextArea("Output commands");
        executeExportResultTextArea.setEditable(false);
        JScrollPane scrollExecuteExportResultTextArea = new JScrollPane (executeExportResultTextArea, 
        		JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		final JTextField executeExportField = new JTextField("exported_function(\"argument1\",...)"); 
        final JPanel executeExportPanel = new JPanel(new GridBagLayout()); 
        GridBagConstraints c = new GridBagConstraints();
        c.weightx=1.0;
        c.fill=GridBagConstraints.HORIZONTAL;
        executeExportPanel.add(executeExportField,c); 
        executeExportPanel.add(new JPanel() { {
        	
        	JButton executeExportConsoleButton = new JButton("Run");
        	executeExportConsoleButton.addActionListener(e -> { 
        		String res = provider.executeDebugMethod(executeExportField.getText());        		
        		SwingUtilities.invokeLater(() -> executeExportResultTextArea.append(res));        		
        	});
        	
        	JButton clearExportConsoleButton = new JButton("Clear");
        	clearExportConsoleButton.addActionListener(e -> SwingUtilities.invokeLater(() -> executeExportResultTextArea.setText(null)));
        	
        	add(executeExportConsoleButton); 
        	add(clearExportConsoleButton);
        	
	    }}); 
        	        
        exportConsolePanel.add(executeExportPanel, BorderLayout.NORTH);
        exportConsolePanel.add(scrollExecuteExportResultTextArea, BorderLayout.CENTER);
	     		
		tabbedPanel.add("Configurations",confPanelScrollPane);
		tabbedPanel.add("Run exports",exportConsolePanel);

	}
	
	public void changeAction(String type, boolean active) {
		
		if(type.equals("server")) {
				
			SwingUtilities.invokeLater(() -> serverAction.setToolBarData(new ToolBarData((active ? ACTIVE_ICON : INACTIVE_ICON), null)));
			
		} else {
			
			SwingUtilities.invokeLater(() -> applicationAction.setToolBarData(new ToolBarData((active ? ACTIVE_ICON : INACTIVE_ICON), null)));
			
		}
				
	}
	
	private void createActions() {
		
		serverAction = new DockingAction("Server", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				Msg.showInfo(this, tool.getActiveWindow(), "ghidra2frida Pyro4 server", (provider.getServerStarted() ? "ghidra2frida Pyro4 server is running" : "ghidra2frida Pyro4 server is stopped"));
			}
		};
		serverAction.setToolBarData(new ToolBarData(INACTIVE_ICON, null));

		serverAction.setEnabled(true);
		tool.addLocalAction(this, serverAction);
		
		applicationAction = new DockingAction("Application", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				Msg.showInfo(this, tool.getActiveWindow(), "ghidra2frida application", (provider.getApplicationSpawned() ? "ghidra2frida application is attached" : "ghidra2frida application is NOT attached"));
			}
		};
		applicationAction.setToolBarData(new ToolBarData(INACTIVE_ICON, null));

		applicationAction.setEnabled(true);
		tool.addLocalAction(this, applicationAction);
				
	}
	
	public void saveConfigurationsInSaveState(SaveState saveState) {
	
		saveState.putBoolean("useVirtualEnv", useVirtualEnvCheckBox.isSelected());
		saveState.putString("pythonPathVenv", pythonPathVenv.getText());
		saveState.putString("pyroHost", pyroHost.getText());
		saveState.putString("pyroPort", pyroPort.getText());
		saveState.putString("fridaPath", fridaPath.getText());
		saveState.putString("applicationId", applicationId.getText());
				
		if(remoteRadioButton.isSelected()) { 
			saveState.putString("device", "remote");
		} else if(usbRadioButton.isSelected()) { 
			saveState.putString("device", "usb");
		} else {
			saveState.putString("device", "local");
		}
			
	}
	
	public void loadConfigurationsFromSaveState(SaveState saveState) {
		
		useVirtualEnvCheckBox.setSelected(saveState.getBoolean("useVirtualEnv", false));
		if (useVirtualEnvCheckBox.isSelected()) {
			labelPythonPathVenv.setText("Virtual env folder: ");
			pythonPathVenvButton.setText("Select folder");
		} else {
			labelPythonPathVenv.setText("Python binary path: ");
			pythonPathVenvButton.setText("Select file");
		}
		
		if (System.getProperty("os.name").startsWith("Windows")) {
			pythonPathVenv.setText(saveState.getString("pythonPathVenv","C:\\python27\\python"));
		} else {
			pythonPathVenv.setText(saveState.getString("pythonPathVenv","/usr/bin/python"));
		}
				
		pyroHost.setText(saveState.getString("pyroHost","localhost"));
		
		pyroPort.setText(saveState.getString("pyroPort", "9999"));
		
		if (System.getProperty("os.name").startsWith("Windows")) {
			fridaPath.setText(saveState.getString("fridaPath","C:\\burp\\ghidra2frida\\ghidra2frida.js"));
		} else {
			fridaPath.setText(saveState.getString("fridaPath","/opt/burp/ghidra2frida/ghidra2frida.js"));
		}
		
		applicationId.setText(saveState.getString("applicationId","org.test.application"));
				                	
    	if(saveState.getString("device","remote").equals("remote"))
    		remoteRadioButton.setSelected(true);
    	else if(saveState.getString("device","remote").equals("usb"))
    		usbRadioButton.setSelected(true);
    	else
    		localRadioButton.setSelected(true);  
		
	}

	/*
	@Override
	public void componentShown() {
	}*/
	
}