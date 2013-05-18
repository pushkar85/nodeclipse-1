package org.nodeclipse.debug.launch;


import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.nodeclipse.debug.util.Constants;
import org.nodeclipse.debug.util.VariablesUtil;

public class NodeArgumentsTab  extends AbstractLaunchConfigurationTab {
	protected Label fPrgmArgumentsLabel;
	protected Text fPrgmArgumentsText;

	// Node arguments widgets
	protected Label fNodeArgumentsLabel;
	protected Text fNodeArgumentsText;

	protected Text locationField;
	protected Text workDirectoryField;
	//protected Button fileLocationButton;
	protected Button workspaceLocationButton;
	protected Button fileWorkingDirectoryButton;
	protected Button workspaceWorkingDirectoryButton;
	protected Button variablesWorkingDirectoryButton;

	protected boolean fInitializing= false;
	private boolean userEdited= false;

	protected WidgetListener fListener= new WidgetListener();

	/**
	 * A listener to update for text modification and widget selection.
	 */
	protected class WidgetListener extends SelectionAdapter implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			if (!fInitializing) {
				setDirty(true);
				userEdited= true;
				updateLaunchConfigurationDialog();
			}
		}
		public void widgetSelected(SelectionEvent e) {
			setDirty(true);
			Object source= e.getSource();
			if (source == workspaceLocationButton) {
				handleWorkspaceLocationButtonSelected();
//			} else if (source == fileLocationButton) {
//				handleFileLocationButtonSelected();
			} else if (source == workspaceWorkingDirectoryButton) {
				handleWorkspaceWorkingDirectoryButtonSelected();
			} else if (source == fileWorkingDirectoryButton) {
				handleFileWorkingDirectoryButtonSelected();
//			} else if (source == argumentVariablesButton) {
//				handleVariablesButtonSelected(argumentField);
//			} else if (source == variablesLocationButton) {
//				handleVariablesButtonSelected(locationField);
			} else if (source == variablesWorkingDirectoryButton) {
				handleVariablesButtonSelected(workDirectoryField);
			}
		}

	}
	
	/**
	 * Prompts the user to choose a location from the filesystem and
	 * sets the location as the full path of the selected file.
	 */
//	protected void handleFileLocationButtonSelected() {
//		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
//		fileDialog.setFileName(locationField.getText());
//		String text= fileDialog.open();
//		if (text != null) {
//			locationField.setText(text);
//		}
//	}
	
	/**
	 * Prompts the user for a workspace location within the workspace and sets
	 * the location as a String containing the workspace_loc variable or
	 * <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceLocationButtonSelected() {
		ResourceSelectionDialog dialog;
		dialog = new ResourceSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Select_a_resource_22);
		dialog.open();
		Object[] results = dialog.getResult();
		if (results == null || results.length < 1) {
			return;
		}
		IResource resource = (IResource)results[0];
		locationField.setText(newVariableExpression("workspace_loc", resource.getFullPath().toString())); //$NON-NLS-1$
	}
	
	/**
	 * Prompts the user for a working directory location within the workspace
	 * and sets the working directory as a String containing the workspace_loc
	 * variable or <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceWorkingDirectoryButtonSelected() {
		ContainerSelectionDialog containerDialog;
		containerDialog = new ContainerSelectionDialog(
			getShell(), 
			ResourcesPlugin.getWorkspace().getRoot(),
			false,
			ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_23);
		containerDialog.open();
		Object[] resource = containerDialog.getResult();
		String text= null;
		if (resource != null && resource.length > 0) {
			text= newVariableExpression("workspace_loc", ((IPath)resource[0]).toString()); //$NON-NLS-1$
		}
		if (text != null) {
			workDirectoryField.setText(text);
		}
	}
	
	/**
	 * Returns a new variable expression with the given variable and the given argument.
	 * @see IStringVariableManager#generateVariableExpression(String, String)
	 */
	protected String newVariableExpression(String varName, String arg) {
		return VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(varName, arg);
	}
	
	/**
	 * Prompts the user to choose a working directory from the filesystem.
	 */
	protected void handleFileWorkingDirectoryButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_23);
		dialog.setFilterPath(workDirectoryField.getText());
		String text= dialog.open();
		if (text != null) {
			workDirectoryField.setText(text);
		}
	}
	
	/**
	 * A variable entry button has been pressed for the given text
	 * field. Prompt the user for a variable and enter the result
	 * in the given field.
	 */
	private void handleVariablesButtonSelected(Text textField) {
		String variable = getVariable();
		if (variable != null) {
			textField.insert(variable);
		}
	}

	/**
	 * Prompts the user to choose and configure a variable and returns
	 * the resulting string, suitable to be used as an attribute.
	 */
	private String getVariable() {
		StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
		dialog.open();
		return dialog.getVariableExpression();
	}
	
	@Override
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		comp.setLayout(layout);
		comp.setFont(font);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);
		setControl(comp);
		//setHelpContextId();
		
		Group group = new Group(comp, SWT.NONE);
		group.setFont(font);
		layout = new GridLayout();
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		group.setText("Program Arguments");
		
		fPrgmArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		fPrgmArgumentsText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				switch (e.detail) {
					case SWT.TRAVERSE_ESCAPE:
					case SWT.TRAVERSE_PAGE_NEXT:
					case SWT.TRAVERSE_PAGE_PREVIOUS:
						e.doit = true;
						break;
					case SWT.TRAVERSE_RETURN:
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS:
						if ((fPrgmArgumentsText.getStyle() & SWT.SINGLE) != 0) {
							e.doit = true;
						} else {
							if (!fPrgmArgumentsText.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
								e.doit = true;
							}
						}
						break;
				}
			}
		});
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 40;
		gd.widthHint = 100;
		fPrgmArgumentsText.setLayoutData(gd);
		fPrgmArgumentsText.setFont(font);
		fPrgmArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				scheduleUpdateJob();
			}
		});
		//ControlAccessibleListener.addListener(fPrgmArgumentsText, group.getText());
		
		String buttonLabel = "Variables...";  
		Button pgrmArgVariableButton = createPushButton(group, buttonLabel, null); 
		pgrmArgVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		pgrmArgVariableButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
                    fPrgmArgumentsText.insert(variable);
				}
			}
		});
		
		Group groupNode = new Group(comp, SWT.NONE);
		groupNode.setFont(font);
		groupNode.setLayout(new GridLayout());
		groupNode.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		groupNode.setText("Node Arguments");
		
		fNodeArgumentsText = new Text(groupNode, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		fNodeArgumentsText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				switch (e.detail) {
					case SWT.TRAVERSE_ESCAPE:
					case SWT.TRAVERSE_PAGE_NEXT:
					case SWT.TRAVERSE_PAGE_PREVIOUS:
						e.doit = true;
						break;
					case SWT.TRAVERSE_RETURN:
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS:
						if ((fPrgmArgumentsText.getStyle() & SWT.SINGLE) != 0) {
							e.doit = true;
						} else {
							if (!fPrgmArgumentsText.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
								e.doit = true;
							}
						}
						break;
				}
			}
		});
		GridData gd2 = new GridData(GridData.FILL_BOTH);
		gd2.heightHint = 40;
		gd2.widthHint = 100;
		fNodeArgumentsText.setLayoutData(gd2);
		fNodeArgumentsText.setFont(font);
		fNodeArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				scheduleUpdateJob();
			}
		});
		//ControlAccessibleListener.addListener(fPrgmArgumentsText, group.getText());
		
		String buttonLabel2 = "Variables...";  
		Button pgrmArgVariableButton2 = createPushButton(groupNode, buttonLabel2, null); 
		pgrmArgVariableButton2.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		pgrmArgVariableButton2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
                    fNodeArgumentsText.insert(variable);
				}
			}
		});
		
		createWorkDirectoryComponent(comp);
	}

	/**
	 * Creates the controls needed to edit the working directory
	 * attribute of an external tool
	 * 
	 * @param parent the composite to create the controls in
	 */
	protected void createWorkDirectoryComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		String groupName = getWorkingDirectoryLabel();
		group.setText(groupName);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayout(layout);
		group.setLayoutData(gridData);
		
		workDirectoryField = new Text(group, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		workDirectoryField.setLayoutData(data);
		workDirectoryField.addModifyListener(fListener);
		addControlAccessibleListener(workDirectoryField,group.getText());
		
		Composite buttonComposite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
		layout.numColumns = 3;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(parent.getFont());
		
		workspaceWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Browse_Wor_kspace____6, null);
		workspaceWorkingDirectoryButton.addSelectionListener(fListener);
		addControlAccessibleListener(workspaceWorkingDirectoryButton, group.getText() + " " + workspaceWorkingDirectoryButton.getText()); //$NON-NLS-1$
		
		fileWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Browse_F_ile_System____7, null);
		fileWorkingDirectoryButton.addSelectionListener(fListener);
		//addControlAccessibleListener(fileWorkingDirectoryButton, group.getText() + " " + fileLocationButton.getText()); //$NON-NLS-1$
		
		variablesWorkingDirectoryButton = createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_32, null);
		variablesWorkingDirectoryButton.addSelectionListener(fListener);
		addControlAccessibleListener(variablesWorkingDirectoryButton, group.getText() + " " + variablesWorkingDirectoryButton.getText()); //$NON-NLS-1$
	}
	
	/**
	 * Return the String to use as the label for the working directory field.
	 * Subclasses may wish to override.
	 */
	protected String getWorkingDirectoryLabel() {
		return ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Working__Directory__5;
	}

	/*
	 * Fix for Bug 60163 Accessibility: New Builder Dialog missing object info for textInput controls
	 */
	public void addControlAccessibleListener(Control control, String controlName) {
		//strip mnemonic (&)
		String[] strs = controlName.split("&"); //$NON-NLS-1$
		StringBuffer stripped = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			stripped.append(strs[i]);
		}
		control.getAccessible().addAccessibleListener(new ControlAccessibleListener(stripped.toString()));
	}
	
	private class ControlAccessibleListener extends AccessibleAdapter {
		private String controlName;
		ControlAccessibleListener(String name) {
			controlName = name;
		}
		public void getName(AccessibleEvent e) {
			e.result = controlName;
		}
		
	}
	
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		return validateWorkDirectory();
	}

	/**
	 * Validates the content of the working directory field.
	 */
	protected boolean validateWorkDirectory() {
		String dir = workDirectoryField.getText().trim();
		if (dir.length() <= 0) {
			return true;
		}

		String expandedDir= null;
		try {
			expandedDir= VariablesUtil.resolveValue(dir);
			if (expandedDir == null) { //a variable that needs to be resolved at runtime
				return true;
			}
		} catch (CoreException e) {
			setErrorMessage(e.getStatus().getMessage());
			return false;
		}
			
		File file = new File(expandedDir);
		if (!file.exists()) { // The directory does not exist.
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_External_tool_working_directory_does_not_exist_or_is_invalid_21);
			return false;
		}
		if (!file.isDirectory()) {
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Not_a_directory);
			return false;
		}
		return true;
	}
	
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fPrgmArgumentsText.setText((String)configuration.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, ""));
			fNodeArgumentsText.setText((String)configuration.getAttribute(Constants.ATTR_NODE_ARGUMENTS, ""));
			workDirectoryField.setText((String)configuration.getAttribute(Constants.ATTR_WORKING_DIRECTORY, ""));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, getAttributeValueFrom(fPrgmArgumentsText));
		configuration.setAttribute(Constants.ATTR_NODE_ARGUMENTS, getAttributeValueFrom(fNodeArgumentsText));
		configuration.setAttribute(Constants.ATTR_WORKING_DIRECTORY, getAttributeValueFrom(workDirectoryField));
	}

	@Override
	public String getName() {
		return "Arguments";
	}

	/**
	 * Returns the string in the text widget, or <code>null</code> if empty.
	 * 
	 * @return text or <code>null</code>
	 */
	protected String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}
}
