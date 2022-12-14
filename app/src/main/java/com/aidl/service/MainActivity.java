package com.aidl.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MyService.MyBinder binder = null;
    ServiceConnection mConnection;
    private ListView mListView;
    private EditText mEditText;
    private List<Msg> mMsgs = new ArrayList<>();
    private ListAdapter mAdapter;
    private IMsgManager mIMsgManager;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listview);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mAdapter = new ListAdapter(this, mMsgs);
        mListView.setAdapter(mAdapter);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = (MyService.MyBinder) iBinder;
                IMsgManager msgManager = IMsgManager.Stub.asInterface(iBinder);
                mIMsgManager = msgManager;
                try {
                    mIMsgManager.asBinder().linkToDeath(mDeathRecipient, 0);
                    mIMsgManager.registerReceiveListener(mReceiveMsgListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                //onServiceDisconnected???????????????UI????????????????????????binderDied???????????????Binder?????????????????????
            }
        };

        //??????Activity???Service???????????????????????????Intent??????
        Intent intent = new Intent(MainActivity.this, MyService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);//????????????

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEditText.getText().toString())) {
                    Toast.makeText(MainActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                    return;
                }
                binder.sendMsg(new Msg(mEditText.getText().toString().trim()));
            }
        });
        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });
    }

    private IReceiveMsgListener mReceiveMsgListener = new IReceiveMsgListener.Stub() {

        @Override
        public void onReceive(Msg msg) throws RemoteException {
            msg.setTime(System.currentTimeMillis());
            mMsgs.add(msg);
            mHandler.sendEmptyMessage(1);
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        //?????????IBinder???????????????????????????????????????
        @Override
        public void binderDied() {
            if (null == mIMsgManager) {
                return;
            }
            //??????????????????
            mIMsgManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mIMsgManager = null;
            //??????????????????
            Log.d("AIDLDEMO", "?????????????????????");
        }
    };

    @Override
    protected void onDestroy() {
        //????????????
        if (null != mIMsgManager && mIMsgManager.asBinder().isBinderAlive()) {
            try {
                mIMsgManager.unregisterReceiveListener(mReceiveMsgListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //??????????????????
        unbindService(mConnection);
        super.onDestroy();
    }
}
