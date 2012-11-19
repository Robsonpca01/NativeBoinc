
package sk.boinc.nativeboinc.util;


public interface PendingErrorHandler<Operation> {
	public abstract boolean handleError(Operation op, Object error);
}
