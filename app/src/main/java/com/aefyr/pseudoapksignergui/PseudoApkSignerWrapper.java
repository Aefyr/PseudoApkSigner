package com.aefyr.pseudoapksignergui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aefyr.pseudoapksigner.PseudoApkSigner;
import com.aefyr.pseudoapksignergui.utils.IOUtils;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PseudoApkSignerWrapper {
    private static final String TAG = "PASWrapper";
    private static final String FILE_NAME_PAST = "testkey.past";
    private static final String FILE_NAME_PRIVATE_KEY = "testkey.pk8";

    @SuppressLint("StaticFieldLeak")// a p p l i c a t i o n   c o n t e x t
    private static PseudoApkSignerWrapper sInstance;

    private Context mContext;
    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private PseudoApkSigner mPseudoApkSigner;

    public static PseudoApkSignerWrapper getInstance(Context c){
        return sInstance != null? sInstance:new PseudoApkSignerWrapper(c);
    }

    private PseudoApkSignerWrapper(Context c){
        mContext = c.getApplicationContext();
        sInstance = this;
    }

    public interface SignerCallback{
        void onSigningSucceeded(File signedApkFile);
        void onSigningFailed(Exception error);
    }

    public void sign(File inputApkFile, File outputSignedApkFile, SignerCallback callback){
        mExecutor.execute(()->{
            try {
                checkAndPrepareSigningEnvironment();

                if(mPseudoApkSigner == null)
                    mPseudoApkSigner = new PseudoApkSigner(new File(getSigningEnvironmentDir(), FILE_NAME_PAST), new File(getSigningEnvironmentDir(), FILE_NAME_PRIVATE_KEY));

                mPseudoApkSigner.sign(inputApkFile, outputSignedApkFile);
                mHandler.post(()->callback.onSigningSucceeded(outputSignedApkFile));
            }catch (Exception e){
                Log.w(TAG, e);
                mHandler.post(()->callback.onSigningFailed(e));
            }

        });
    }

    private void checkAndPrepareSigningEnvironment() throws Exception {
        File signingEnvironment = getSigningEnvironmentDir();
        File pastFile = new File(signingEnvironment, FILE_NAME_PAST);
        File privateKeyFile = new File(signingEnvironment, FILE_NAME_PRIVATE_KEY);

        if (pastFile.exists() && privateKeyFile.exists())
            return;

        Log.d(TAG, "Preparing signing environment...");
        signingEnvironment.mkdir();

        IOUtils.copyFileFromAssets(mContext, FILE_NAME_PAST, pastFile);
        IOUtils.copyFileFromAssets(mContext, FILE_NAME_PRIVATE_KEY, privateKeyFile);
    }

    private File getSigningEnvironmentDir() {
        return new File(mContext.getFilesDir(), "signing");
    }
}
