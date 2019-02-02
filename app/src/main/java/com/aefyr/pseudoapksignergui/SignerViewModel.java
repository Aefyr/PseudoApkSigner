package com.aefyr.pseudoapksignergui;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.aefyr.pseudoapksignergui.utils.Event;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SignerViewModel extends AndroidViewModel implements PseudoApkSignerWrapper.SignerCallback {
    public static final String EVENT_SIGNING_SUCCEED = "signing_succeed";
    public static final String EVENT_SIGNING_FAILED = "signing_failed";

    private Context mContext;

    public enum State{
        IDLE, SIGNING
    }

    private MutableLiveData<State> mState = new MutableLiveData<>();
    private MutableLiveData<Event<String[]>> mEvents = new MutableLiveData<>();


    public SignerViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
        mState.setValue(State.IDLE);
    }

    public LiveData<State> getState(){
        return mState;
    }

    public LiveData<Event<String[]>> getEvents(){
        return mEvents;
    }

    public void sign(File apkFile){
        if(mState.getValue() == State.SIGNING)
            throw new IllegalStateException("SignerViewModel is already signing an APK");

        mState.setValue(State.SIGNING);
        PseudoApkSignerWrapper.getInstance(mContext).sign(apkFile, getSignedApkFilePath(apkFile), this);
    }

    private File getSignedApkFilePath(File originalAPK){
        File signedApkFilesDir = new File(Environment.getExternalStorageDirectory(), "PseudoApkSigner");
        signedApkFilesDir.mkdir();

        String rawFileName = originalAPK.getName();
        int indexOfLastDot = rawFileName.lastIndexOf('.');
        String fileName = rawFileName.substring(0, indexOfLastDot);
        String fileExtension = rawFileName.substring(indexOfLastDot+1);

        return new File(signedApkFilesDir, String.format("%s_signed.%s", fileName, fileExtension));
    }

    @Override
    public void onSigningSucceeded(File signedApkFile) {
        mState.setValue(State.IDLE);
        mEvents.setValue(new Event<>(new String[]{EVENT_SIGNING_SUCCEED, signedApkFile.getAbsolutePath()}));
    }

    @Override
    public void onSigningFailed(Exception error) {
        mState.setValue(State.IDLE);
        mEvents.setValue(new Event<>(new String[]{EVENT_SIGNING_FAILED, error.getMessage()}));
    }

}
