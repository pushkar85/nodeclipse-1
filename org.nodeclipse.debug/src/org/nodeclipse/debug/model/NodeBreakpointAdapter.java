package org.nodeclipse.debug.model;

import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.chromium.debug.core.model.LineBreakpointAdapter;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.nodeclipse.debug.util.NodeResourceUtil;

public class NodeBreakpointAdapter extends
		LineBreakpointAdapter.ForVirtualProject {
	public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
			throws CoreException {
		ITextEditor textEditor = getEditor(part);
		if (textEditor != null) {
			IResource resource = (IResource) textEditor.getEditorInput()
					.getAdapter(IResource.class);
			IResource res2 = NodeResourceUtil.get(resource);
			
			ITextSelection textSelection = (ITextSelection) selection;
			int lineNumber = textSelection.getStartLine();
			IBreakpoint[] breakpoints = DebugPlugin.getDefault()
					.getBreakpointManager().getBreakpoints(getDebugModelId());
			IBreakpoint breakpoint = findBreakpoint(breakpoints, resource, lineNumber);
			if(breakpoint != null) {
				breakpoint.delete();
				breakpoint = findBreakpoint(breakpoints, res2, lineNumber);
				if(breakpoint != null) {
					breakpoint.delete();
				}
				return;
			}
			
			addBreakpoint(resource, lineNumber);
			if(res2 != null && res2.exists()) {
				addBreakpoint(res2, lineNumber);
			}
		}
	}
	
	private IBreakpoint findBreakpoint(IBreakpoint[] breakpoints, IResource resource, int lineNumber) throws CoreException {
		if(resource == null) {
			return null;
		}
				
		for (int i = 0; i < breakpoints.length; i++) {
			IBreakpoint breakpoint = breakpoints[i];
			if (resource.equals(breakpoint.getMarker().getResource())) {
				if (((ILineBreakpoint) breakpoint).getLineNumber() == lineNumber + 1) {
					return breakpoint;
				}
			}
		}
		return null;
	}
	
	private void addBreakpoint(IResource resource, int lineNumber) throws CoreException {
		if(resource == null) {
			return;
		}
		
		// Line numbers start with 0 in V8, with 1 in Eclipse.
		ChromiumLineBreakpoint lineBreakpoint = new ChromiumLineBreakpoint(
				resource, lineNumber + 1, getDebugModelId());
		DebugPlugin.getDefault().getBreakpointManager()
				.addBreakpoint(lineBreakpoint);		
	}
}
