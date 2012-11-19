
package sk.boinc.mobileboinc.bridge;

import sk.boinc.mobileboinc.clientconnection.HostInfo;
import sk.boinc.nativeboinc.R;
import android.content.res.Resources;


public class HostInfoCreator {
	public static HostInfo create(final edu.berkeley.boinc.lite.HostInfo hostInfo, final Formatter formatter) {
		HostInfo hi = new HostInfo();
		hi.hostCpId = hostInfo.host_cpid;
		StringBuilder sb = formatter.getStringBuilder();
		Resources resources = formatter.getResources();
		sb.append(
				resources.getString(R.string.hostInfoPart1,
						hostInfo.domain_name,
						hostInfo.ip_addr,
						hostInfo.host_cpid,
						hostInfo.os_name,
						hostInfo.os_version,
						hostInfo.p_ncpus,
						hostInfo.p_vendor,
						hostInfo.p_model,
						hostInfo.p_features)
				);
		if (hostInfo.virtualbox_version != null)
			sb.append(resources.getString(R.string.hostInfoVB, hostInfo.virtualbox_version));
		sb.append(
				resources.getString(R.string.hostInfoPart2,
						String.format("%.2f", hostInfo.p_fpops/1000000),
						String.format("%.2f", hostInfo.p_iops/1000000),
						String.format("%.2f", hostInfo.p_membw/1000000),
						formatter.formatDate(hostInfo.p_calculated),
						String.format("%.0f %s", hostInfo.m_nbytes/1024/1024, resources.getString(R.string.unitMiB)),
						String.format("%.0f %s", hostInfo.m_cache/1024, resources.getString(R.string.unitKiB)),
						String.format("%.0f %s", hostInfo.m_swap/1024/1024, resources.getString(R.string.unitMiB)),
						String.format("%.1f %s", hostInfo.d_total/1000000000, resources.getString(R.string.unitGB)),
						String.format("%.1f %s", hostInfo.d_free/1000000000, resources.getString(R.string.unitGB)))
				);
		hi.htmlText = sb.toString();
		return hi;
	}
}
