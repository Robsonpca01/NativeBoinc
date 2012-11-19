

package sk.boinc.nativeboinc.util;


public class UpdateItem {
	public String name = "";
	public String version = "";
	public String filename = "";
	public String description = "";
	public String changes = "";
	public boolean isNew = false;
	public boolean checked = false;
	
	public UpdateItem() {
	}
	
	public UpdateItem(String name, String version, String filename, String description, String changes,
			boolean isNew) {
		this.name = name;
		this.version = version;
		this.filename = filename;
		this.description = description;
		this.changes = changes;
		this.isNew = isNew;
	}
}
