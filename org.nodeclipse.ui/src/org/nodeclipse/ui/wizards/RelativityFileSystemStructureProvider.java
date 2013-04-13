package org.nodeclipse.ui.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.nodeclipse.ui.util.LogUtil;

@SuppressWarnings({ "restriction" })
public class RelativityFileSystemStructureProvider implements
		IImportStructureProvider {
	private File root;

	public RelativityFileSystemStructureProvider(File root) {
		this.root = root;
	}

	public RelativityFileSystemStructureProvider(String basepath, String name) {
		this(new File(basepath, name));
	}

	public File getRoot() {
		LogUtil.info("RelativityFileSystemStructureProvider.getRoot() called");
		return root;
	}

	public List<File> getChildren(Object element) {
		File folder = (File) element;
		String[] children = folder.list();
		int childrenLength = children == null ? 0 : children.length;
		List<File> result = new ArrayList<File>(childrenLength);

		for (int i = 0; i < childrenLength; i++) {
			result.add(new File(folder, children[i]));
		}

		return result;
	}

	public List<File> collectFiles(Object element) {
		List<File> result = new ArrayList<File>();

		File root = (File) element;
		if (root.isDirectory()) {
			collectFiles(root, result);
		} else {
			result.add(root);
		}
		LogUtil.info("RelativityFileSystemStructureProvider.cllectFiles() called. file count="
				+ result.size());
		return result;
	}

	private void collectFiles(File parent, List<File> result) {
		File[] children = parent.listFiles();
		for (File child : children) {
			if (child.isDirectory()) {
				collectFiles(child, result);
			} else {
				result.add(child);
			}
		}
	}

	public InputStream getContents(Object element) {
		try {
			LogUtil.info("RelativityFileSystemStructureProvider.getContents() called. elemnt="
					+ element.toString());
			return new FileInputStream((File) element);
		} catch (FileNotFoundException e) {
			LogUtil.error(e.getLocalizedMessage(), e);
			IDEWorkbenchPlugin.log(e.getLocalizedMessage(), e);
		}
		return null;
	}

	private String stripPath(String path) {
		int index = path.indexOf(root.getName());
		path = path.substring(index + root.getName().length());
		return path;
	}

	public String getFullPath(Object element) {
		return stripPath(((File) element).getPath());
	}

	public String getLabel(Object element) {
		File file = (File) element;
		String name = file.getName();
		if (name.length() == 0) {
			return file.getPath();
		}
		return name;
	}

	public boolean isFolder(Object element) {
		return ((File) element).isDirectory();
	}
}
