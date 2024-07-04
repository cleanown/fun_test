package com.cleanown.fun_test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;

public class OAIDTool {
    public static String TAG = "cle--";
    public static String getAIString(Context context) throws Exception {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.android.vending", 0);
        } catch (Exception e) {
            Log.e(TAG, "NameNotFound:" + e.getMessage());
            throw e;
        }

        String aid = null;
        try {
            AC connection = new AC();
            Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
            intent.setPackage("com.google.android.gms");
            if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                try {
                    AI adInterface = new AI(connection.getBinder());
                    aid = adInterface.getId();
                    return aid;
                } catch (Exception exception) {
                    throw exception;
                } finally {
                    context.unbindService(connection);
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return aid;
    }

    private static final class AC implements ServiceConnection {
        boolean retrieved = false;
        private final LinkedBlockingQueue<IBinder> queue = new LinkedBlockingQueue<IBinder>(1);

        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                this.queue.put(service);
            } catch (InterruptedException localInterruptedException) {
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }

        public IBinder getBinder() throws InterruptedException {
            if (this.retrieved) throw new IllegalStateException();
            this.retrieved = true;
            return (IBinder) this.queue.take();
        }
    }

    private static final class AI implements IInterface {
        private IBinder binder;

        public AI(IBinder pBinder) {
            binder = pBinder;
        }

        public IBinder asBinder() {
            return binder;
        }

        public String getId() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            String id;
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                binder.transact(1, data, reply, 0);
                reply.readException();
                id = reply.readString();
            } finally {
                reply.recycle();
                data.recycle();
            }
            return id;
        }
    }

}
