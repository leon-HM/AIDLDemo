package com.leon.server;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import aidl.Book;
import aidl.IBookManager;
import aidl.IOnNewBookArrivedListener;

public class BookManagerService extends Service {
    private static final String TAG = "Server";

    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);

    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<Book>();

    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<IOnNewBookArrivedListener>();

    private Binder mBinder = new IBookManager.Stub() {

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {

            mListenerList.register(listener);
            final int N = mListenerList.beginBroadcast();
            mListenerList.finishBroadcast();
            Log.d(TAG, "registerListener, current size:" + N);
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);

            final int N = mListenerList.beginBroadcast();
            mListenerList.finishBroadcast();
            Log.d(TAG, "unregisterListener, current size:" + N);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            //验证自定义权限 com.leon.server.BookManager
            int check = checkCallingPermission("com.leon.server.BookManager");
            if(check == PackageManager.PERMISSION_DENIED){
                Log.d(TAG,"permission denied");
                return false;
            }

            //验证包名
            String packageName = null;
            String[] packages = getPackageManager().getPackagesForUid(
                    getCallingUid());
            if (packages != null && packages.length > 0) {
                packageName = packages[0];
            }
            Log.d(TAG, "onTransact: " + packageName);
            if (!packageName.startsWith("com.leon")) {
                return false;
            }

            return super.onTransact(code, data, reply, flags);
        }
    };

    public BookManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(1, "Android"));
        mBookList.add(new Book(2, "IOS"));
        Log.d(TAG, "onCreate");

        new Thread(new ServiceWorkder()).start();
    }

    @Override
    public void onDestroy() {
        mIsServiceDestoryed.set(true);
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void onNewBookArrived(Book newBook) throws RemoteException {
        mBookList.add(newBook);

        final int N = mListenerList.beginBroadcast();
        for(int i=0;i<N;i++){
            IOnNewBookArrivedListener l = mListenerList.getBroadcastItem(i);
            if(l!=null){
                try{
                    l.onNewBookArrived(newBook);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        mListenerList.finishBroadcast();
    }

    private class ServiceWorkder implements Runnable {

        @Override
        public void run() {
            while (!mIsServiceDestoryed.get()) {
                SystemClock.sleep(5000);
                int bookId = mBookList.size() + 1;
                Book newBook = new Book(bookId, "new book #" + bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}


