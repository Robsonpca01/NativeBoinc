
package edu.berkeley.boinc.lite;

/**
 * @author mat
 *
 */
public class GlobalPreferences {
	public boolean run_on_batteries;
    // poorly named; what it really means is:
    // if false, suspend while on batteries
	public boolean run_if_user_active;
	public boolean run_gpu_if_user_active;
	public double idle_time_to_run;
	//public double suspend_if_no_recent_input;
	public double suspend_cpu_usage;
	public boolean leave_apps_in_memory;
	//public boolean confirm_before_connecting;
	//public boolean hangup_if_dialed;
	public boolean dont_verify_images;
	public double work_buf_min_days;
	public double work_buf_additional_days;
	public double max_ncpus_pct;
	//public int max_ncpus;
	public double cpu_scheduling_period_minutes;
	public double disk_interval;
	public double disk_max_used_gb;
	public double disk_max_used_pct;
	public double disk_min_free_gb;
	//public double vm_max_used_frac;
	public double ram_max_used_busy_frac;
	public double ram_max_used_idle_frac;
	public double max_bytes_sec_up;
	public double max_bytes_sec_down;
	public double cpu_usage_limit;
	public double daily_xfer_limit_mb;
	public int daily_xfer_period_days;
	public double run_if_battery_nl_than;
	public double run_if_temp_lt_than;
	public boolean run_always_when_plugged;
	
	public TimePreferences cpu_times = new TimePreferences();
	public TimePreferences net_times = new TimePreferences();
}
