package com.leon.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import aidl.Book;
import aidl.IBookManager;
import aidl.IOnNewBookArrivedListener;

public class BookManagerActivity extends AppCompatActivity {

    private static final String TAG = "Client";
    public static final String START_SERVICE_ACTION = "com.leon.server.BookManagerService";

    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bookManager = IBookManager.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected");
            mTVResult.setText("bind success");
            try {
                bookManager.registerListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            bookManager=null;
            //连接意外断开了，重新连接
            bindBookManagerService();
        }
    };
    private IBookManager bookManager;
    private TextView mTVResult;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG,"receive new book :"+msg.obj);
                break;
                default:
                    super.handleMessage(msg);
                break;
            }
        }
    };

    private IOnNewBookArrivedListener mOnNewBookArrivedListener=new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book newBook) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED,newBook).sendToTarget();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTVResult = (TextView) findViewById(R.id.mTVResult);
        Button mBTNBind = (Button) findViewById(R.id.mBTNBind);
        Button mBTNAdd = (Button) findViewById(R.id.mBTNAdd);
        Button mBTNGet = (Button) findViewById(R.id.mBTNGet);
        mBTNBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isServiceAlive()){
                   return;
                }
                bindBookManagerService();
            }
        });
        mBTNAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(isServiceAlive()){
                   try {
                       Book newBook = new Book(3, "windows");
                       bookManager.addBook(newBook);
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }

            }
        });

        mBTNGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isServiceAlive()){
                    try {
                        List<Book> list = bookManager.getBookList();
                        Log.d(TAG, "book list:" + list.toString());
                        mTVResult.setText("book list:" + list.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private void bindBookManagerService() {
        try {
            Intent intent = new Intent(START_SERVICE_ACTION);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if(isServiceAlive()){
            try{
                Log.d(TAG, "unregister listener :" + mOnNewBookArrivedListener);
                bookManager.unregisterListener(mOnNewBookArrivedListener);
                unbindService(mConnection);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    private boolean isServiceAlive() {
        return bookManager!=null
                && bookManager.asBinder().isBinderAlive();
    }
}
