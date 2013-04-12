package org.nodeclipse.ui.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.PerspectiveDescriptor;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.nodeclipse.ui.Activator;
import org.nodeclipse.ui.perspectives.NodePerspective;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public abstract class AbstractNodeProjectWizard extends Wizard implements INewWizard {

    private IWorkbench workbench;
    private IStructuredSelection selection;

    private IProject newProject;

    public AbstractNodeProjectWizard() {
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    public IWorkbench getWorkbench() {
        return workbench;
    }

    protected IStructuredSelection getSelection() {
        return selection;
    }

    @Override
    public boolean performFinish() {
        newProject = createNewProject();
        if (newProject == null) {
            return false;
        }
        
        updatePerspective();
        selectAndReveal();
        return true;
    }
    
    protected abstract IProject createNewProject();

    protected void generateTemplates(String path, IProject projectHandle) throws CoreException {
		Bundle bundle = Activator.getDefault().getBundle();
		if (bundle == null) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, "bundle not found"));
		}
		try {
			URL location = FileLocator.toFileURL(bundle.getEntry("/"));
			File templateRoot = new File(location.getPath(), path);
			RelativityFileSystemStructureProvider structureProvider = new RelativityFileSystemStructureProvider(
					templateRoot);
			ImportOperation operation = new ImportOperation(
					projectHandle.getFullPath(), templateRoot,
					structureProvider, new IOverwriteQuery() {
						public String queryOverwrite(String pathString) {
							return ALL;
						}
					}, structureProvider.getChildren(templateRoot));

			operation.setContext(getShell());
			operation.run(null);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, e.getLocalizedMessage()));
		}
	}

	protected void rewriteFile(String filename, IProject projectHandle)
			throws CoreException {
		String newLine = System.getProperty("line.separator");
		IFile readme = projectHandle.getFile(filename);
		if (!readme.exists()) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, filename + "not found"));
		}
		InputStreamReader ir = new InputStreamReader(readme.getContents());
		BufferedReader br = new BufferedReader(ir);
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (line.contains("${projectname}")) {
					line = line.replace("${projectname}",
							projectHandle.getName());
				}
				sb.append(line);
				sb.append(newLine);
			}
			ByteArrayInputStream source = new ByteArrayInputStream(sb
					.toString().getBytes());
			readme.setContents(source, true, true, null);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, "Cannot read " + filename));
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

	protected void runJSHint(IProject projectHandle) throws CoreException {
		String builderId = "com.eclipsesource.jshint.ui.builder";
		IProjectDescription description = projectHandle.getDescription();

		if (!containsBuildCommand(description, builderId)) {
			addBuildCommand(description, builderId);
			projectHandle.setDescription(description, null);
		}

		triggerClean(projectHandle, builderId);
	}

	protected boolean containsBuildCommand(IProjectDescription description,
			String builderId) {
		for (ICommand command : description.getBuildSpec()) {
			if (command.getBuilderName().equals(builderId)) {
				return true;
			}
		}
		return false;
	}

	protected void addBuildCommand(IProjectDescription description, String builderId) {
		ICommand[] oldCommands = description.getBuildSpec();
		ICommand[] newCommands = new ICommand[oldCommands.length + 1];
		System.arraycopy(oldCommands, 0, newCommands, 0, oldCommands.length);
		newCommands[newCommands.length - 1] = createBuildCommand(description, builderId);
		description.setBuildSpec(newCommands);
	}

	protected ICommand createBuildCommand(IProjectDescription description, String builderId) {
		ICommand command = description.newCommand();
		command.setBuilderName(builderId);
		return command;
	}

	protected void triggerClean(IProject project, String builderName) throws CoreException {
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, builderName, null,	null);
	}

	private void selectAndReveal() {
        BasicNewResourceWizard.selectAndReveal(newProject, workbench.getActiveWorkbenchWindow());
    }

    private void updatePerspective() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IPerspectiveRegistry reg = WorkbenchPlugin.getDefault().getPerspectiveRegistry();
        PerspectiveDescriptor rtPerspectiveDesc = (PerspectiveDescriptor) reg.findPerspectiveWithId(NodePerspective.ID);
        // Now set it as the active perspective.
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            page.setPerspective(rtPerspectiveDesc);
        }
    }
}

