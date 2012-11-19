

package sk.boinc.mobileboinc.installer;


public class InstalledBinary {
	public String name;
	public String version;
	public String description;
	public String changes;
	public boolean fromSDCard;
	
	public InstalledBinary(String name, String version, String description, String changes,
			boolean fromSDCard) {
		this.name = name;
		this.version = version;
		this.description = description;
		this.changes = changes;
		this.fromSDCard = fromSDCard;
	}
	
	@Override
	public String toString() {
		if (fromSDCard)
			return name + " from SDCard";
		else
			return name + " " + version;
	}
}
