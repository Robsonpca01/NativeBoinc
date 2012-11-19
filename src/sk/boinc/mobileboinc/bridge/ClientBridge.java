
package sk.boinc.mobileboinc.bridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import sk.boinc.mobileboinc.clientconnection.BoincOp;
import sk.boinc.mobileboinc.clientconnection.ClientAccountMgrReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientAllProjectsListReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientError;
import sk.boinc.mobileboinc.clientconnection.ClientManageReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientPollReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientPreferencesReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientProjectReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientRequestHandler;
import sk.boinc.mobileboinc.clientconnection.ClientUpdateMessagesReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientUpdateProjectsReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientUpdateTasksReceiver;
import sk.boinc.mobileboinc.clientconnection.ClientUpdateTransfersReceiver;
import sk.boinc.mobileboinc.clientconnection.HostInfo;
import sk.boinc.mobileboinc.clientconnection.MessageInfo;
import sk.boinc.mobileboinc.clientconnection.ModeInfo;
import sk.boinc.mobileboinc.clientconnection.PollError;
import sk.boinc.mobileboinc.clientconnection.PollOp;
import sk.boinc.mobileboinc.clientconnection.ProjectInfo;
import sk.boinc.mobileboinc.clientconnection.TaskDescriptor;
import sk.boinc.mobileboinc.clientconnection.TaskInfo;
import sk.boinc.mobileboinc.clientconnection.TransferDescriptor;
import sk.boinc.mobileboinc.clientconnection.TransferInfo;
import sk.boinc.mobileboinc.clientconnection.VersionInfo;
import sk.boinc.mobileboinc.debug.Logging;
import sk.boinc.mobileboinc.debug.NetStats;
import sk.boinc.nativeboinc.util.ClientId;
import sk.boinc.nativeboinc.util.PendingController;
import sk.boinc.nativeboinc.util.PendingErrorHandler;
import sk.boinc.nativeboinc.util.PendingOpSelector;
import android.content.Context;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;
import edu.berkeley.boinc.lite.AccountIn;
import edu.berkeley.boinc.lite.AccountMgrInfo;
import edu.berkeley.boinc.lite.GlobalPreferences;
import edu.berkeley.boinc.lite.ProjectConfig;
import edu.berkeley.boinc.lite.ProjectListEntry;


/**
 * The <code>ClientBridge</code> runs in UI thread. 
 * Right on its own creation it creates another thread, the worker thread.
 * When request is received at this class, it is posted to worker thread.
 */
public class ClientBridge implements ClientRequestHandler {
	private static final String TAG = "ClientBridge";
		
	// for joining two requests in one (create/lookup and attach)
	//private ConcurrentMap<String, String> mJoinedAddProjectsMap = new ConcurrentHashMap<String, String>();
	private String mJoinedAddProjectUrl = null;
	
	private PendingController<BoincOp> mClientPendingController =
			new PendingController<BoincOp>("Client:PendingCtrl");
		
	public class ReplyHandler extends Handler {
		private static final String TAG = "ClientBridge.ReplyHandler";

		public void disconnecting() {
			// The worker thread started disconnecting
			// This means, that all actions needed to close connection are either
			// processed already, or waiting in queue to be processed
			// We can start clearing of worker thread (will be also done as post,
			// so it will be executed afterwards)
			if (Logging.DEBUG) Log.d(TAG, "disconnecting(), stopping ClientBridgeWorkerThread");
			mWorker.stopThread(null);
			// Clean up periodic updater as well, because no more periodic updates will be needed
			mAutoRefresh.cleanup();
			mAutoRefresh = null;
		}

		public void disconnected() {
			if (Logging.DEBUG) Log.d(TAG, "disconnected()");
			// The worker thread was cleared completely 
			mWorker = null;
			if (mCallback != null) {
				// We are ready to be deleted
				// The callback can now delete reference to us, so this
				// object can be garbage collected then
				mCallback.bridgeDisconnected(ClientBridge.this);
				mCallback = null;
			}
		}

		public void notifyProgress(BoincOp boincOp, int progress) {
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers)
				observer.clientConnectionProgress(boincOp, progress);
		}
		
		// only used internally by ClientBridgeWorkerHandler 
		public void notifyOperationBegin(BoincOp boincOp) {
			Log.d(TAG, "RunInternally:"+boincOp);
			mClientPendingController.begin(boincOp);
		}
		
		public void notifyOperationFinish(BoincOp boincOp) {
			mClientPendingController.finish(boincOp);
		}
		
		public void notifyError(BoincOp boincOp, int errorNum, String errorMessage) {
			if (boincOp.isAddProject()) // addProject operation
				mJoinedAddProjectUrl = null;
			
			mClientPendingController.finish(boincOp);
			
			boolean called = false;
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer.clientError(boincOp, errorNum, errorMessage))
					called = true;
			}
			
			if (!called)
				mClientPendingController.finishWithError(boincOp, new ClientError(errorNum, errorMessage));
		}
		
		public void notifyPollError(BoincOp boincOp, int errorNum, int operation,
				String errorMessage, String param) {
			
			if (boincOp.isAddProject()) // addProject operation
				mJoinedAddProjectUrl = null;
			
			mClientPendingController.finish(boincOp);
			
			boolean called = false;
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientPollReceiver) {
					if (((ClientPollReceiver)observer).onPollError(errorNum, operation,
							errorMessage, param))
						called = true;
				}
			}
			
			
			if (!called) {
				String key = (param != null) ? param : "";
				mClientPendingController.finishWithError(boincOp,
						new PollError(errorNum, operation, errorMessage, key));
			}
		}
		
		public void notifyConnected(VersionInfo clientVersion) {
			mConnected = true;
			mRemoteClientVersion = clientVersion;
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers)
				observer.clientConnected(mRemoteClientVersion);
		}

		public void notifyDisconnected(boolean disconnectedByManager) {
			mConnected = false;
			mRemoteClientVersion = null;
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				observer.clientDisconnected(disconnectedByManager);
				if (Logging.DEBUG) Log.d(TAG, "Detached observer: " + observer.toString()); // see below clearing of all observers
			}
			// before clearing observers inform is client connection finish work
			onChangeIsWorking(false);
			mObservers.clear();
			mClientPendingController = null;
		}

		public void cancelledUpdate(final BoincOp boincOp) {
			// finish operation
			mClientPendingController.finish(boincOp);
		}

		public void updatedClientMode(final ModeInfo modeInfo) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateClientMode, modeInfo);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientManageReceiver) {
					ClientManageReceiver callback = (ClientManageReceiver)observer;
					
					boolean periodicAllowed = callback.updatedClientMode(modeInfo);
					if (periodicAllowed)
						mAutoRefresh.scheduleAutomaticRefresh(callback, AutoRefresh.CLIENT_MODE, -1);
				}
			}
		}

		public void updatedHostInfo(final HostInfo hostInfo) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateHostInfo, hostInfo);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientManageReceiver)
					((ClientManageReceiver)observer).updatedHostInfo(hostInfo);
			}
		}
		
		public void currentBAMInfo(final AccountMgrInfo bamInfo) {
			mClientPendingController.finishWithOutput(BoincOp.GetBAMInfo, bamInfo);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientAccountMgrReceiver)
					((ClientAccountMgrReceiver)observer).currentBAMInfo(bamInfo);
			}
		}
		
		public void currentAllProjectsList(final ArrayList<ProjectListEntry> projects) {
			mClientPendingController.finishWithOutput(BoincOp.GetAllProjectList, projects);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientAllProjectsListReceiver)
					((ClientAllProjectsListReceiver)observer).currentAllProjectsList(projects);
			}
		}
		
		public void currentAuthCode(final String projectUrl, final String authCode) {
			
			if (mJoinedAddProjectUrl == null || !mJoinedAddProjectUrl.equals(projectUrl))
				// if not add project (standalone calls)
				mClientPendingController.finish(BoincOp.AddProject);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientProjectReceiver)
					((ClientProjectReceiver)observer).currentAuthCode(projectUrl, authCode);
			}
			
			if (mJoinedAddProjectUrl.equals(projectUrl)) {
				if (Logging.DEBUG) Log.d(TAG, "Join with project attach:"+projectUrl);
				projectAttach(projectUrl, authCode, "");
			}
		}
		
		public void currentProjectConfig(String projectUrl, final ProjectConfig projectConfig) {
			mClientPendingController.finishWithOutput(BoincOp.GetProjectConfig, projectConfig);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientProjectReceiver)
					((ClientProjectReceiver)observer).currentProjectConfig(projectUrl,
							projectConfig);
			}
		}
		
		public void currentGlobalPreferences(final GlobalPreferences globalPrefs) {
			mClientPendingController.finishWithOutput(BoincOp.GlobalPrefsWorking, globalPrefs);
			
			// First, check whether callback is still present in observers
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientPreferencesReceiver)
					((ClientPreferencesReceiver)observer).currentGlobalPreferences(globalPrefs);
			}
		}
		
		public void afterAccountMgrRPC() {
			mClientPendingController.finish(BoincOp.SyncWithBAM);
			
			// First, check whether callback is still present in observers
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientAccountMgrReceiver)
					((ClientAccountMgrReceiver)observer).onAfterAccountMgrRPC();
			}
		}
		
		public void afterProjectAttach(final String projectUrl) {
			// First, check whether callback is still present in observers
			if (Logging.DEBUG) Log.d(TAG, "Remove after project attach:"+projectUrl);
			
			mClientPendingController.finish(BoincOp.AddProject);
			mJoinedAddProjectUrl = null;
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientProjectReceiver)
					((ClientProjectReceiver)observer).onAfterProjectAttach(projectUrl);
			}
		}
		
		public void onGlobalPreferencesChanged() {
			mClientPendingController.finish(BoincOp.GlobalPrefsOverride);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientPreferencesReceiver)
					((ClientPreferencesReceiver)observer).onGlobalPreferencesChanged();
			}
		}

		public void updatedProjects(final ArrayList <ProjectInfo> projects) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateProjects, projects);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientUpdateProjectsReceiver) {
					ClientUpdateProjectsReceiver callback = (ClientUpdateProjectsReceiver)observer;
					
					boolean periodicAllowed = callback.updatedProjects(projects);
					if (periodicAllowed)
						mAutoRefresh.scheduleAutomaticRefresh(callback, AutoRefresh.PROJECTS, -1);
				}
			}
		}

		public void updatedTasks(final ArrayList <TaskInfo> tasks) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateTasks, tasks);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientUpdateTasksReceiver) {
					ClientUpdateTasksReceiver callback = (ClientUpdateTasksReceiver)observer;
					
					boolean periodicAllowed = callback.updatedTasks(tasks);
					if (periodicAllowed)
						mAutoRefresh.scheduleAutomaticRefresh(callback, AutoRefresh.TASKS, -1);
				}
			}
		}

		public void updatedTransfers(final ArrayList <TransferInfo> transfers) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateTransfers, transfers);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientUpdateTransfersReceiver) {
					ClientUpdateTransfersReceiver callback = (ClientUpdateTransfersReceiver)observer;
					
					boolean periodicAllowed = callback.updatedTransfers(transfers);
					if (periodicAllowed)
						mAutoRefresh.scheduleAutomaticRefresh(callback, AutoRefresh.TRANSFERS, -1);
				}
			}
		}

		public void updatedMessages(final ArrayList <MessageInfo> messages) {
			mClientPendingController.finishWithOutput(BoincOp.UpdateMessages, messages);
			
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientUpdateMessagesReceiver) {
					ClientUpdateMessagesReceiver callback = (ClientUpdateMessagesReceiver)observer;
					
					boolean periodicAllowed = callback.updatedMessages(messages);
					if (periodicAllowed)
						mAutoRefresh.scheduleAutomaticRefresh(callback, AutoRefresh.MESSAGES, -1);
				}
			}
		}
		
		
		
		public void onChangeIsWorking(boolean isWorking) {
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers)
				observer.onClientIsWorking(isWorking);
		}
		
		/*
		 * cancel poll operations
		 */
		public void cancelPollOperations(final int opFlags) {
			Log.d(TAG, "on cancel poll operations:"+opFlags);;
			// finish bam sync and add project operations
			mClientPendingController.finishSelected(new PendingOpSelector<BoincOp>() {
				@Override
				public boolean select(BoincOp boincOp) {
					boolean toRemove = false;
					if ((opFlags & PollOp.POLL_BAM_OPERATION_MASK) != 0 && boincOp.isBAMOperation())
						toRemove = true;
					if ((opFlags & PollOp.POLL_CREATE_ACCOUNT_MASK) != 0 && boincOp.isCreateAccount())
						toRemove = true;
					if ((opFlags & PollOp.POLL_LOOKUP_ACCOUNT_MASK) != 0 && boincOp.isLookupAccount())
						toRemove = true;
					if ((opFlags & PollOp.POLL_PROJECT_ATTACH_MASK) != 0 && boincOp.isProjectAttach())
						toRemove = true;
					if ((opFlags & PollOp.POLL_PROJECT_CONFIG_MASK) != 0 && boincOp.isProjectConfig())
						toRemove = true;
					
					return toRemove;
				}
			});
			
			// send notification to observers about cancelling
			ClientReceiver[] observers = mObservers.toArray(new ClientReceiver[0]);
			for (ClientReceiver observer: observers) {
				if (observer instanceof ClientPollReceiver) {
					((ClientPollReceiver)observer).onPollCancel(opFlags);
				}
			}
			
			// clear 'add project' operations
			if ((opFlags & (PollOp.POLL_CREATE_ACCOUNT_MASK | PollOp.POLL_LOOKUP_ACCOUNT_MASK |
					PollOp.POLL_PROJECT_ATTACH_MASK)) != 0)
				mJoinedAddProjectUrl = null;
		}
	}

	protected final ReplyHandler mReplyHandler = new ReplyHandler();

	private Set<ClientReceiver> mObservers = new HashSet<ClientReceiver>();
	protected boolean mConnected = false;

	protected ClientBridgeCallback mCallback = null;
	protected ClientBridgeWorkerThread mWorker = null;

	protected ClientId mRemoteClient = null;
	private VersionInfo mRemoteClientVersion = null;

	protected AutoRefresh mAutoRefresh = null;

	public ClientBridge() {
	}
	
	/**
	 * Constructs a new <code>ClientBridge</code> and starts worker thread
	 * 
	 * @throws RuntimeException if worker thread cannot start in a timely fashion
	 */
	public ClientBridge(ClientBridgeCallback callback, NetStats netStats) throws RuntimeException {
		mCallback = callback;
		if (Logging.DEBUG) Log.d(TAG, "Starting ClientBridgeWorkerThread");
		ConditionVariable lock = new ConditionVariable(false);
		Context context = (Context)callback;
		mAutoRefresh = new AutoRefresh(context, this);
		mWorker = new ClientBridgeWorkerThread(lock, mReplyHandler, context, netStats);
		mWorker.start();
		boolean runningOk = lock.block(2000); // Locking until new thread fully runs
		if (!runningOk) {
			// Too long time waiting for worker thread to be on-line - cancel it
			if (Logging.ERROR) Log.e(TAG, "ClientBridgeWorkerThread did not start in 1 second");
			throw new RuntimeException("Worker thread cannot start");
		}
		if (Logging.DEBUG) Log.d(TAG, "ClientClientBridgeWorkerThread started successfully");
	}
	
	public int getAutoRefresh() {
		if (mAutoRefresh != null)
			return mAutoRefresh.getAutoRefresh();
		return -1;
	}

	@Override
	public void registerStatusObserver(ClientReceiver observer) {
		// Another observer wants to be notified - add him into collection of observers
		mObservers.add(observer);
		if (Logging.DEBUG) Log.d(TAG, "Attached new observer: " + observer.toString());
		if (mConnected) {
			// New observer is attached while we are already connected
			// Notify new observer that we are connected, so it can fetch data
			observer.clientConnected(mRemoteClientVersion);
		}
	}

	@Override
	public void unregisterStatusObserver(ClientReceiver observer) {
		// Observer does not want to receive notifications anymore - remove him
		mObservers.remove(observer);
		if (mConnected) {
			// The observer could have automatic refresh pending
			// Remove it now
			if (observer instanceof ClientManageReceiver)
				mAutoRefresh.unscheduleAutomaticRefresh((ClientManageReceiver)observer);
		}
		if (Logging.DEBUG) Log.d(TAG, "Detached observer: " + observer.toString());
	}

	public boolean isWorking() {
		return mWorker.isWorking();
	}
	
	@Override
	public void connect(final ClientId remoteClient, final boolean retrieveInitialData) {
		if (mRemoteClient != null) {
			// already connected
			if (Logging.ERROR) Log.e(TAG, "Request to connect to: " + remoteClient.getNickname() +
					" while already connected to: " + mRemoteClient.getNickname());
			return;
		}
		mRemoteClient = remoteClient;
		mWorker.connect(remoteClient, retrieveInitialData);
	}
	
	@Override
	public void disconnect() {
		if (mRemoteClient == null) return; // not connected
		mWorker.disconnect();
		mRemoteClient = null; // This will prevent further triggers towards worker thread
	}

	@Override
	public final ClientId getClientId() {
		return mRemoteClient;
	}
	
	@Override
	public boolean isNativeConnected() {
		if (mRemoteClient == null) return false;
		return mRemoteClient.isNativeClient();
	}

	@Override
	public boolean updateClientMode() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateClientMode)) {
			mWorker.updateClientMode();
			return true;
		}
		return false;
	}

	@Override
	public boolean updateHostInfo() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateHostInfo)) {
			mWorker.updateHostInfo();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean updateProjects() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateProjects)) {
			mWorker.updateProjects();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean updateTasks() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateTasks)) {
			mWorker.updateTasks();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean updateTransfers() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateTransfers)) {
			mWorker.updateTransfers();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean updateMessages() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.UpdateMessages)) {
			mWorker.updateMessages();
			return true;
		}
		return false;
	}
	
	public boolean updateNotices() {
		if (mRemoteClient == null || mClientPendingController == null) {
			return false; // not connected
		}
		
		if (mClientPendingController.begin(BoincOp.UpdateNotices)) {
			mWorker.updateNotices();
			return true;
		}
		return false;
	}
	
	@Override
	public void addToScheduledUpdates(ClientReceiver callback, int refreshType, int period) {
		if (mRemoteClient == null || mClientPendingController == null) return; // not connected
		mAutoRefresh.scheduleAutomaticRefresh(callback, refreshType, period);
	}

	@Override
	public void cancelScheduledUpdates(final int refreshType) {
		if (mRemoteClient == null || mClientPendingController == null) return; // not connected
		// Cancel pending updates in worker thread
		mWorker.cancelPendingUpdates(refreshType);
		// remove from clientPendingController
		mClientPendingController.finishSelected(new PendingOpSelector<BoincOp>() {
			@Override
			public boolean select(BoincOp boincOp) {
				switch(refreshType) {
				case AutoRefresh.CLIENT_MODE:
					return boincOp.isAutoRefreshOp(refreshType);
				}
				return false;
			}
		});
		
		// Remove scheduled auto-refresh (if any)
		if (mAutoRefresh!=null)
			mAutoRefresh.unscheduleAutomaticRefresh(refreshType);
	}

	@Override
	public boolean getBAMInfo() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.GetBAMInfo)) {
			mWorker.getBAMInfo();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean attachToBAM(String name, String url, String password) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.SyncWithBAM)) {
			mWorker.attachToBAM(name, url, password);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean synchronizeWithBAM() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.SyncWithBAM)) {
			mWorker.synchronizeWithBAM();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean stopUsingBAM() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.StopUsingBAM);
		mWorker.stopUsingBAM();
		return true;
	}
	
	@Override
	public boolean getAllProjectsList(boolean excludeAttachedProjects) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.GetAllProjectList)) {
			mWorker.getAllProjectsList(excludeAttachedProjects);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean lookupAccount(AccountIn accountIn) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.AddProject)) {
			mWorker.lookupAccount(accountIn);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean createAccount(AccountIn accountIn) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.AddProject)) {
			mWorker.createAccount(accountIn);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean projectAttach(String url, String authCode, String projectName) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		// if join exists we continue operation by performing project attach
		if ((mJoinedAddProjectUrl != null && mJoinedAddProjectUrl.equals(url)) ||
				// otherwise run as standalone (ok)
				mClientPendingController.begin(BoincOp.AddProject)) {
			mWorker.projectAttach(url, authCode, projectName);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean addProject(AccountIn accountIn, boolean create) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mJoinedAddProjectUrl != null)
			return false; // do nothing ignore
		
		if (!mClientPendingController.begin(BoincOp.AddProject))
			return false;
		
		mJoinedAddProjectUrl = accountIn.url;
		
		if (create)
			mWorker.createAccount(accountIn);
		else
			mWorker.lookupAccount(accountIn);
		return true;
	}
	
	@Override
	public boolean getProjectConfig(String url) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.GetProjectConfig)) {
			mWorker.getProjectConfig(url);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean handlePendingClientErrors(BoincOp boincOp, final ClientReceiver receiver) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false;
		
		return mClientPendingController.handlePendingErrors(boincOp, new PendingErrorHandler<BoincOp>() {
			@Override
			public boolean handleError(BoincOp boincOp, Object error) {
				if (error instanceof ClientError) {
					ClientError cError = (ClientError)error;
					return receiver.clientError(boincOp, cError.errorNum, cError.message);
				} else
					return false;
			}
		});
	}
	
	@Override
	public void cancelPollOperations(int opFlags) {
		if (mRemoteClient == null || mClientPendingController == null)
			return;
		mWorker.cancelPollOperations(opFlags);
	}
	
	@Override
	public boolean handlePendingPollErrors(BoincOp boincOp,
			final ClientPollReceiver receiver) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false;
		
		return mClientPendingController.handlePendingErrors(boincOp, new PendingErrorHandler<BoincOp>() {
			@Override
			public boolean handleError(BoincOp boincOp, Object error) {
				if (error instanceof PollError) {
					PollError pollError = (PollError)error;
					return receiver.onPollError(pollError.errorNum, pollError.operation,
							pollError.message, pollError.param);
				} else
					return false;
			}
		});
	}
	
	@Override
	public Object getPendingOutput(BoincOp boincOp) {
		if (mRemoteClient == null || mClientPendingController == null)
			return null;
		
		return mClientPendingController.takePendingOutput(boincOp);
	}
	
	@Override
	public boolean isOpBeingExecuted(BoincOp boincOp) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false;
		
		return mClientPendingController.isRan(boincOp);
	}
	
	@Override
	public boolean getGlobalPrefsWorking() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		if (mClientPendingController.begin(BoincOp.GlobalPrefsWorking)) {
			mWorker.getGlobalPrefsWorking();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean setGlobalPrefsOverride(String globalPrefs) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		// this call should be always enqueued
		mClientPendingController.begin(BoincOp.GlobalPrefsOverride);
		mWorker.setGlobalPrefsOverride(globalPrefs);
		return true;
	}
	
	@Override
	public boolean setGlobalPrefsOverrideStruct(GlobalPreferences globalPrefs) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false;// not connected
		
		// this call should be always enqueued
		mClientPendingController.begin(BoincOp.GlobalPrefsOverride);
		mWorker.setGlobalPrefsOverrideStruct(globalPrefs, mRemoteClient.isNativeClient());
		return true;
	}
	
	@Override
	public boolean runBenchmarks() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.RunBenchmarks);
		mWorker.runBenchmarks();
		return true;
	}

	@Override
	public boolean setRunMode(final int mode) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		// this call always enqueued
		if (mClientPendingController.begin(BoincOp.SetRunMode))
		mWorker.setRunMode(mode);
		return true;
	}

	@Override
	public boolean setNetworkMode(final int mode) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		// this call always enqueued
		mClientPendingController.begin(BoincOp.SetNetworkMode);
		mWorker.setNetworkMode(mode);
		return true;
	}

	@Override
	public void shutdownCore() {
		if (mRemoteClient == null || mClientPendingController == null) return; // not connected
		
		mClientPendingController.begin(BoincOp.ShutdownCore);
		mWorker.shutdownCore();
	}

	@Override
	public boolean doNetworkCommunication() {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.DoNetworkComm);
		mWorker.doNetworkCommunication();
		return true;
	}

	@Override
	public boolean projectOperation(final int operation, final String projectUrl) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		// this call should be always enqueued
		mClientPendingController.begin(BoincOp.ProjectOperation);
		mWorker.projectOperation(operation, projectUrl);
		return true;
	}
	
	@Override
	public boolean projectsOperation(final int operation, final String[] projectUrls) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.ProjectOperation);
		mWorker.projectsOperation(operation, projectUrls);
		return true;
	}

	@Override
	public boolean taskOperation(final int operation, final String projectUrl, final String taskName) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.TaskOperation);
		mWorker.taskOperation(operation, projectUrl, taskName);
		return true;
	}
	
	@Override
	public boolean tasksOperation(final int operation, final TaskDescriptor[] tasks) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.TaskOperation);
		mWorker.tasksOperation(operation, tasks);
		return true;
	}

	@Override
	public boolean transferOperation(final int operation, final String projectUrl, final String fileName) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.TransferOperation);
		mWorker.transferOperation(operation, projectUrl, fileName);
		return true;
	}
	
	@Override
	public boolean transfersOperation(final int operation, final TransferDescriptor[] transfers) {
		if (mRemoteClient == null || mClientPendingController == null)
			return false; // not connected
		
		mClientPendingController.begin(BoincOp.TransferOperation);
		mWorker.transfersOperation(operation, transfers);
		return true;
	}
}
