package com.ford.googlenowlink.applink;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.AudioType;
import com.ford.syncV4.proxy.rpc.enums.BitsPerSample;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.LockScreenStatus;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SamplingRate;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.util.DebugTool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class AppLinkService extends Service implements IProxyListenerALM {
    // variable used to increment correlation ID for every request sent to SYNC
    public int autoIncCorrId = 0;
    // variable to contain the current state of the service
    private static AppLinkService instance = null;
    // variable to access the BluetoothAdapter
    private BluetoothAdapter mBtAdapter;
    // variable to create and call functions of the SyncProxy
    private SyncProxyALM proxy = null;

    // Service shutdown timing constants
    private static final int CONNECTION_TIMEOUT = 60000;
    private static final int STOP_SERVICE_DELAY = 5000;

    // Constant for command ID
    private static final int COMMAND_1 = 1;
    private static final int SUBMENU_1 = 100;

    public static final String TAG = "Hello Google Now"; // Global TAG used in logging
    /**
     * Runnable that stops this service if there hasn't been a connection to SYNC
     * within a reasonable amount of time since ACL_CONNECT.
     */
    private Runnable mCheckConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            Boolean stopService = true;
            // If the proxy has connected to SYNC, do NOT stop the service
            if (proxy != null && proxy.getIsConnected()) {
                stopService = false;
            }
            if (stopService) {
                mHandler.removeCallbacks(mCheckConnectionRunnable);
                mHandler.removeCallbacks(mStopServiceRunnable);
                stopSelf();
            }
        }
    };

    /**
     * Runnable that stops this service on ACL_DISCONNECT after a short time delay.
     * This is a workaround until some synchronization issues are fixed within the proxy.
     */
    private Runnable mStopServiceRunnable = new Runnable() {
        @Override
        public void run() {
            // As long as the proxy is null or not connected to SYNC, stop the service
            if (proxy == null || !proxy.getIsConnected()) {
                mHandler.removeCallbacks(mCheckConnectionRunnable);
                mHandler.removeCallbacks(mStopServiceRunnable);
                stopSelf();
            }
        }
    };

    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Remove any previous stop service runnables that could be from a recent ACL Disconnect
        mHandler.removeCallbacks(mStopServiceRunnable);

        // Start the proxy when the service starts
        if (intent != null) {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter != null) {
                if (mBtAdapter.isEnabled()) {
                    startProxy();
                }
            }
        }

        // Queue the check connection runnable to stop the service if no connection is made
        mHandler.removeCallbacks(mCheckConnectionRunnable);
        mHandler.postDelayed(mCheckConnectionRunnable, CONNECTION_TIMEOUT);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        disposeSyncProxy();
        LockScreenManager.clearLockScreen();
        instance = null;
        super.onDestroy();
    }

    public static AppLinkService getInstance() {
        return instance;
    }

    public SyncProxyALM getProxy() {
        return proxy;
    }

    /**
     * Queue's a runnable that stops the service after a small delay,
     * unless the proxy manages to reconnects to SYNC.
     */
    public void stopService() {
        mHandler.removeCallbacks(mStopServiceRunnable);
        mHandler.postDelayed(mStopServiceRunnable, STOP_SERVICE_DELAY);
    }

    public void startProxy() {
        if (proxy == null) {
            try {
                proxy = new SyncProxyALM(this, "Google Now", true, "438316430");
            } catch (SyncException e) {
                e.printStackTrace();
                // error creating proxy, returned proxy = null
                if (proxy == null) {
                    stopSelf();
                }
            }
        }
    }

    public void disposeSyncProxy() {
        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (SyncException e) {
                e.printStackTrace();
            }
            proxy = null;
            LockScreenManager.clearLockScreen();
        }
    }

    public void reset() {
        if (proxy != null) {
            try {
                proxy.resetProxy();
            } catch (SyncException e1) {
                e1.printStackTrace();
                //something goes wrong, & the proxy returns as null, stop the service.
                // do not want a running service with a null proxy
                if (proxy == null) {
                    stopSelf();
                }
            }
        } else {
            startProxy();
        }
    }

    public void subButtons() {
        /*try {
            proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
	        proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEUP, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEDOWN, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
		} catch (SyncException e) {}*/
    }

    @Override
    public void onProxyClosed(String info, Exception e, SyncDisconnectedReason reason) {
        LockScreenManager.clearLockScreen();

        if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
            if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
                Log.v(AppLinkApplication.TAG, "reset proxy in onproxy closed");
                reset();
            }
        }
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        switch (notification.getSystemContext()) {
            case SYSCTXT_MAIN:
                break;
            case SYSCTXT_VRSESSION:
                break;
            case SYSCTXT_MENU:
                break;
            default:
                return;
        }

        switch (notification.getAudioStreamingState()) {
            case AUDIBLE:
                // play audio if applicable
                break;
            case NOT_AUDIBLE:
                // pause/stop/mute audio if applicable
                break;
            default:
                return;
        }

        switch (notification.getHmiLevel()) {
            case HMI_FULL:
                Log.i(AppLinkApplication.TAG, "HMI_FULL");
                if (notification.getFirstRun()) {
                    // setup app on SYNC
                    // send welcome message if applicable
                    try {
                        proxy.show("Welcome to", "Google Now Link", TextAlignment.CENTERED, autoIncCorrId++);
                    } catch (SyncException e) {
                        DebugTool.logError("Failed to send Show", e);
                    }
                    // send addcommands
                    // subscribe to buttons
                    subButtons();
                    addCommands();
                } else {
                    try {
                        proxy.show("SyncProxy is", "Alive", TextAlignment.CENTERED, autoIncCorrId++);
                    } catch (SyncException e) {
                        DebugTool.logError("Failed to send Show", e);
                    }
                }
                break;
            case HMI_LIMITED:
                Log.i(AppLinkApplication.TAG, "HMI_LIMITED");
                break;
            case HMI_BACKGROUND:
                Log.i(AppLinkApplication.TAG, "HMI_BACKGROUND");
                break;
            case HMI_NONE:
                Log.i(AppLinkApplication.TAG, "HMI_NONE");
                break;
            default:
                return;
        }
    }

    /**
     * Add commands (should be called when the first HMI_FULL is detected).
     */
    public void addCommands() {
        // Create and build an AddSubMenu RPC
        AddSubMenu submenu = new AddSubMenu();
        submenu.setMenuName("SubMenu 1");
        submenu.setPosition(0);
        submenu.setMenuID(SUBMENU_1);
        submenu.setCorrelationID(autoIncCorrId++);
        // Send
        try {
            proxy.sendRPCRequest(submenu);
        } catch (SyncException e) {
            Log.i(TAG, "sync exception" + e.getMessage() + e.getSyncExceptionCause());
            e.printStackTrace();
        }
    }

    /**
     * Listen for a positive or negative response to the AddSubMenu request.
     */
    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        boolean bSuccess = response.getSuccess();

        // Create and build an AddCommand RPC
        AddCommand msg = new AddCommand();
        msg.setCmdID(COMMAND_1);
        String menuName = "Command 1";
        MenuParams menuParams = new MenuParams();
        menuParams.setMenuName(menuName);

        // Set the parent ID to the submenu that was added only if the response was SUCCESS
        if (bSuccess) {
            menuParams.setParentID(SUBMENU_1);
        }
        msg.setMenuParams(menuParams);

        // Build voice recognition commands
        Vector<String> vrCommands = new Vector<String>();
        vrCommands.add("Okay Google");
        vrCommands.add("Ok Google");
        msg.setVrCommands(vrCommands);

        // Set the correlation ID
        int correlationId = autoIncCorrId++;
        msg.setCorrelationID(correlationId);

        // Send to proxy
        try {
            proxy.sendRPCRequest(msg);
        } catch (SyncException e) {
            Log.i(TAG, "sync exception" + e.getMessage() + e.getSyncExceptionCause());
            e.printStackTrace();
        }
    }

    /**
     * Listen for a positive or negative response to the AddCommand request
     * in onAddCommandResponse.
     */
    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        boolean bSuccess = response.getSuccess();

        if (!bSuccess) {
            // Handle error
        }

        // Correlation ID used to associate a response with a request
        int correlationID = response.getCorrelationID();
        Result resultCode = response.getResultCode();

        if (resultCode != Result.SUCCESS) {
            // Handle error
        }
    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
    }

    @Override
    public void onError(String info, Exception e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnCommand(OnCommand notification) {
        // Get identifier for the command
        int cmdID = notification.getCmdID();

        Log.d(TAG, "S onOnCommand: " + cmdID);

        // Determine which command was selected
        if (cmdID == COMMAND_1) {
            // Perform response to when a command is selected

            startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(
            CreateInteractionChoiceSetResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(
            DeleteInteractionChoiceSetResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onResetGlobalPropertiesResponse(
            ResetGlobalPropertiesResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        notification.getButtonName();
        // TODO Auto-generated method stub
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUnsubscribeVehicleDataResponse(
            UnsubscribeVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onPutFileResponse(PutFileResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSliderResponse(SliderResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnHashChange(OnHashChange arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnSystemRequest(OnSystemRequest arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnTouchEvent(OnTouchEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification) {
        LockScreenStatus displayLockScreen = notification.getShowLockScreen();
        // Show lockscreen in both REQUIRED and OPTIONAL
        //if (displayLockScreen == LockScreenStatus.REQUIRED || displayLockScreen == LockScreenStatus.OPTIONAL) {
        //Show lockscreen in only REQUIRED
        if (displayLockScreen == LockScreenStatus.REQUIRED) {
            LockScreenManager.showLockScreen();
        } else {
            LockScreenManager.clearLockScreen();
        }
    }

    //Build Request and send to proxy object:
    public void startAPT() {
        int corrId = autoIncCorrId++;
        PerformAudioPassThru msg = new PerformAudioPassThru();
        msg.setCorrelationID(corrId);
        Vector<TTSChunk> initChunks = TTSChunkFactory
                .createSimpleTTSChunks("Initial Prompt");
        msg.setInitialPrompt(initChunks);
        msg.setAudioPassThruDisplayText1("DisplayText1");
        msg.setAudioPassThruDisplayText2("DisplayText2");
        msg.setSamplingRate(SamplingRate._16KHZ);
        msg.setMaxDuration(10000); //in milliseconds
        msg.setBitsPerSample(BitsPerSample._16_BIT);
        msg.setAudioType(AudioType.PCM);

        try {
            proxy.sendRPCRequest(msg);
        } catch (SyncException e) {
            Log.i(TAG, "sync exception" + e.getMessage() + e.getSyncExceptionCause());
            e.printStackTrace();
        }
    }

    //Listen for callbacks inside of the service
    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        byte[] aptData = notification.getAPTData();

        if (aptData == null) {
            Log.w(TAG, "onAudioPassThru aptData is null");
            //addMessage("ononAudioPassThru aptData is null");
            return;
        } else {
            APTtoDevice(aptData);
            /*Context context = this;
            AudioManager mAudioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Switch to headset
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            mAudioManager.startBluetoothSco();*/

        }
    }

    public void APTtoDevice(byte[] aptData) {
        File outFile = audioPassThruOutputFile(PCM);
        iByteCount = iByteCount + aptData.length;
        try {
            if (audioPassThruOutStream == null) {
                audioPassThruOutStream = new BufferedOutputStream(
                        new FileOutputStream(outFile, false));
            }
            audioPassThruOutStream.write(aptData);
            Log.i(TAG, "audioPassThruOutStream.write(aptData)");
            audioPassThruOutStream.flush();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Output file "
                    + (outFile != null ? outFile.toString() : "'unknown'")
                    + " can't be opened for writing", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Can't write to output file", e);
            e.printStackTrace();
            addMessage("Can't write to output file");
        } catch (Exception e) {
            Log.e(TAG, "audio media player2", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onPerformAudioPassThruResponse(
            PerformAudioPassThruResponse response) {
        Log.d(TAG, "S onPerformAudioPassThruResponse" + response.getInfo()
                + response.getSuccess());
        try {
            Log.d(TAG, "S onPerformAudioPassThruResponse: "
                    + response.serializeJSON().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Result result = response.getResultCode();
        closeAudioPassThruStream();
        closeAudioPassThruMediaPlayer();

        if (Result.SUCCESS != result) {
            File outFile = audioPassThruOutputFile(PCM);
            if ((outFile != null) && outFile.exists()) {
                if (!outFile.delete()) {
                    Log.i(TAG, "Failed to delete output file", null);
                }
            }
            if ((Result.RETRY == result)) {
                Log.i(TAG, "retry result APT");
                startAPT();
            }
        } else { //success
            saveAsWav();
            try {
                audioPassThruDone = autoIncCorrId;
                proxy.speak("playing back what you just said:", autoIncCorrId++);
            } catch (SyncException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            playAPTfile();
        }
    }

}
