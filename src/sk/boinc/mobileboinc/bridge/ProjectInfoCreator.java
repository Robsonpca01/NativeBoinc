/* 
 * AndroBOINC - BOINC Manager for Android
 * Copyright (C) 2010, Pavol Michalec
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package sk.boinc.mobileboinc.bridge;

import android.content.res.Resources;
import sk.boinc.mobileboinc.clientconnection.ProjectInfo;
import sk.boinc.nativeboinc.R;
import edu.berkeley.boinc.lite.Project;


public class ProjectInfoCreator {
	public static ProjectInfo create(final Project prj, float totalResources,
			boolean haveAti, boolean haveCuda, final Formatter formatter) {
		Resources resources = formatter.getResources();
		ProjectInfo pi = new ProjectInfo();
		pi.masterUrl = prj.master_url;
		pi.project = prj.getName();
		pi.account = prj.user_name;
		pi.team = prj.team_name;
		pi.user_credit = prj.user_total_credit;
		pi.user_rac = prj.user_expavg_credit;
		pi.host_credit = prj.host_total_credit;
		pi.host_rac = prj.host_expavg_credit;
		float pctShare = prj.resource_share/totalResources*100;
		pi.resShare = (int)(pctShare*10.0);
		pi.share = String.format("%.0f (%.2f%%)", prj.resource_share, pctShare);
		//pi.disk_usage = formatter.formatBinSize((long)prj.disk_usage);
		pi.hostid = prj.hostid;
		pi.venue = formatter.formatDefault(prj.venue);
		pi.non_cpu_intensive = prj.non_cpu_intensive;
		pi.cpu_short_term_debt = prj.cpu_short_term_debt;
		pi.cpu_long_term_debt = prj.cpu_long_term_debt;
		pi.disk_usage = formatter.formatBinSize((long)prj.disk_usage);
		pi.duration_correction_factor = prj.duration_correction_factor;
		
		pi.non_cpu_intensive = prj.non_cpu_intensive;
		pi.have_ati = haveAti;
		pi.have_cuda = haveCuda;
		
		if (haveAti) {
			pi.ati_short_term_debt = prj.ati_short_term_debt;
			pi.ati_debt = prj.ati_debt;
		}
		if (haveCuda) {
			pi.cuda_short_term_debt = prj.cuda_short_term_debt;
			pi.cuda_debt = prj.cuda_debt;
		}
		
		StringBuilder sb = formatter.getStringBuilder();
		pi.statusId = 0; // 0 = active
		if (prj.suspended_via_gui) {
			sb.append(resources.getString(R.string.projectStatusSuspended));
			pi.statusId |= ProjectInfo.SUSPENDED;
		}
		if (prj.dont_request_more_work) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(resources.getString(R.string.projectStatusNNW));
			pi.statusId |= ProjectInfo.NNW;
		}
		if (pi.statusId == 0) {
			// not suspended & new tasks allowed
			sb.append(resources.getString(R.string.projectStatusActive));
		}
		pi.status = sb.toString();
		return pi;
	}
}
