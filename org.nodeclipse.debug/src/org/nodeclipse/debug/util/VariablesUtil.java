package org.nodeclipse.debug.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

public class VariablesUtil {
	public static String resolveValue(String expression) throws CoreException {
		String expanded= null;
		try {
			expanded= getValue(expression);
		} catch (CoreException e) { //possibly just a variable that needs to be resolved at runtime
			validateVaribles(expression);
			return null;
		}
		return expanded;
	}
	
	/**
	 * Validates the value of the given string to determine if any/all variables are valid
	 * 
	 * @param expression expression with variables
	 * @return whether the expression contained any variable values
	 * @exception CoreException if variable resolution fails
	 */
	private static String getValue(String expression) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(expression);
	}

	/**
	 * Validates the variables of the given string to determine if all variables are valid
	 * 
	 * @param expression expression with variables
	 * @exception CoreException if a variable is specified that does not exist
	 */
	private static void validateVaribles(String expression) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		manager.validateStringVariables(expression);
	}
}
