
package sk.boinc.nativeboinc.util;


public interface PendingOpSelector<Operation> {
	public abstract boolean select(Operation op);
}
