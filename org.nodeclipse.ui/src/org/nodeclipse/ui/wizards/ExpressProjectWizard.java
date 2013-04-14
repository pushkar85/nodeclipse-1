package org.nodeclipse.ui.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.nodeclipse.ui.Activator;
import org.nodeclipse.ui.nature.NodeNature;
import org.nodeclipse.ui.npm.InstallLaunchShortcut;
import org.nodeclipse.ui.util.Constants;
import org.nodeclipse.ui.util.LogUtil;
import org.nodeclipse.ui.util.ProcessUtils;
import org.osgi.framework.Bundle;

public class ExpressProjectWizard extends AbstractNodeProjectWizard {
	private final String WINDOW_TITLE = "New Express Project";

	private ExpressProjectWizardPage mainPage;

	public ExpressProjectWizard() {
		super();
		setWindowTitle(WINDOW_TITLE);
	}

	@Override
	public void addPages() {
		mainPage = new ExpressProjectWizardPage("ExpressNewProjectPage") { //$NON-NLS-1$
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.ui.dialogs.WizardNewProjectCreationPage#createControl
			 * (org.eclipse.swt.widgets.Composite)
			 */
			public void createControl(Composite parent) {
				super.createControl(parent);
				createWorkingSetGroup(
						(Composite) getControl(),
						getSelection(),
						new String[] { "org.eclipse.ui.resourceWorkingSetPage" }); //$NON-NLS-1$
				Dialog.applyDialogFont(getControl());
			}
		};
		mainPage.setTitle("Create a Express Project");
		mainPage.setDescription("Create a new Express project.");
		addPage(mainPage);
	}

	@Override
	protected IProject createNewProject() {
		final IProject newProjectHandle = mainPage.getProjectHandle();
		URI location = null;
		if (!mainPage.useDefaults()) {
			location = mainPage.getLocationURI();
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace
				.newProjectDescription(newProjectHandle.getName());
		description.setLocationURI(location);
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = NodeNature.NATURE_ID;
		description.setNatureIds(newNatures);

		final boolean exists = isExistsProjectFolder(description);
		final String projectName = mainPage.getProjectName();
		final String templateEngine = mainPage.getSelectedTemplateEngine();

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				createProject(description, monitor);				
				if(exists == true) {
					return;
				}
				
				String tempdir = System.getProperty("java.io.tmpdir")
						+ "/express-work/"
						+ new Long(System.currentTimeMillis()).toString();
				File workingDirectory = new File(tempdir);
				if (workingDirectory.mkdirs() == false) {
					throw new InvocationTargetException(new IOException(
							"failed creation of working directory."));
				}
				
				generateExpressApplication(monitor, projectName, newProjectHandle,
						templateEngine, workingDirectory);
				importExpressApplication(monitor, newProjectHandle,
						workingDirectory);

				monitor.beginTask("Generating Express Project...", 3);

				deleteFiles(monitor, workingDirectory);
				monitor.worked(1);

				try {
					generateTemplates("common-templates", newProjectHandle);
					rewriteFile("README.md", newProjectHandle);
					rewritePackageJson(monitor, newProjectHandle);

					// JSHint support
					runJSHint(newProjectHandle);
				} catch (CoreException e1) {
					throw new InvocationTargetException(e1);
				}
				
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							launchNpmInstall(newProjectHandle);
						} catch (Exception e) {
							// throw new InvocationTargetException(e);
						}
					}
				});
				
				try {
					newProjectHandle.refreshLocal(1, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				
				monitor.done();
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			LogUtil.error(e);
		} catch (InterruptedException e) {
		}

		if (newProjectHandle != null) {
			// add to workingsets
			IWorkingSet[] workingSets = mainPage.getSelectedWorkingSets();
			getWorkbench().getWorkingSetManager().addToWorkingSets(
					newProjectHandle, workingSets);
		}

		return newProjectHandle;
	}

	private void createProject(IProjectDescription description,
			IProgressMonitor monitor) throws InvocationTargetException {
		CreateProjectOperation op = new CreateProjectOperation(description,
				WINDOW_TITLE);
		try {
			op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
		} catch (ExecutionException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void generateExpressApplication(IProgressMonitor monitor,
			String projectName, IProject projectHandle, String templateEngine, File workingDirectory)
			throws InvocationTargetException {
		Bundle bundle = Activator.getDefault().getBundle();
		if (bundle == null) {
			throw new InvocationTargetException(
					new CoreException(
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, "bundle not found")));
		}
		
		List<String> cmdLine = new ArrayList<String>();
		cmdLine.add(ProcessUtils.getNodePath());
		cmdLine.add(ProcessUtils.getExpressPath());
		cmdLine.add(workingDirectory.getAbsolutePath() + "/" + projectName);

		if (!templateEngine.equals(Constants.BLANK_STRING)) {
			int ver = ProcessUtils.getExpressMajorVersion();
			if (ver < 3) {
				cmdLine.add("-t");
				cmdLine.add(templateEngine);
			} else {
				cmdLine.add("--" + templateEngine);
			}
		}

		ProcessUtils.exec(cmdLine, workingDirectory);
	}

	private void importExpressApplication(IProgressMonitor monitor,
			IProject projectHandle, File workingDirectory)
			throws InvocationTargetException, InterruptedException {
		String projectName = projectHandle.getName();
		File[] children = workingDirectory.listFiles();
		File root = null;
		for (File child : children) {
			if (child.getName().equals(projectName)) {
				root = child;
				break;
			}
		}
		if (root == null) {
			throw new InvocationTargetException(new IOException("Express app("
					+ projectName + ") is not found. "));
		}

		RelativityFileSystemStructureProvider structureProvider = new RelativityFileSystemStructureProvider(
				workingDirectory);

		ImportOperation operation = new ImportOperation(
				projectHandle.getFullPath(), root, structureProvider,
				new IOverwriteQuery() {
					@Override
					public String queryOverwrite(String pathString) {
						return IOverwriteQuery.ALL;
					}
				}, structureProvider.collectFiles(root));
		operation.setCreateContainerStructure(false);
		operation.setContext(getShell());
		operation.run(monitor);
	}

	private void deleteFiles(IProgressMonitor monitor, File f) {
		if (f.exists() == false) {
			return;
		}

		if (f.isFile()) {
			f.delete();
		}

		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteFiles(monitor, files[i]);
			}
			f.delete();
		}
	}
	
	private void rewritePackageJson(IProgressMonitor monitor, IProject projectHandle) throws CoreException {
		String newLine = System.getProperty("line.separator");
		IFile file = projectHandle.getFile("package.json");
		if(!file.exists()) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, "package.json not found"));
		}
		InputStreamReader ir = new InputStreamReader(file.getContents());
		BufferedReader br = new BufferedReader(ir);
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while((line = br.readLine()) != null) {
				if(line.contains("application-name")) {
					line = line.replace("application-name", projectHandle.getName());
				}
				sb.append(line);
				sb.append(newLine);
			}
			ByteArrayInputStream source = new ByteArrayInputStream(sb.toString().getBytes());
			file.setContents(source, true, true, null);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot read package.json"));
		} finally {
			try {
				ir.close();
				br.close();
			} catch (IOException e) {
			}
			ir = null;
			br = null;
		}		
	}
	
	private void launchNpmInstall(IProject projectHandle) throws InvocationTargetException, CoreException {
		InstallLaunchShortcut launcher = new InstallLaunchShortcut();
		IFile path = projectHandle.getFile(Constants.PACKAGE_JSON);
		launcher.launchFile(path, ILaunchManager.DEBUG_MODE);
	}
}
