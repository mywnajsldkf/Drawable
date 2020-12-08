package com.example.videochat;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoCallActivity extends AppCompatActivity
        implements Session.SessionListener,
        PublisherKit.PublisherListener

{

    private static String API_Key = "47032914";
    private static String SESSION_ID="1_MX40NzAzMjkxNH5-MTYwNzQzNjUyNzE2Nn5aTi80b2ZCaFN0OVVkRFpkMjNTRE5hNHh-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NzAzMjkxNCZzaWc9YTBlOTgxOTRlZWU1NzQzNGE5MTc0ZmE1NjZhMTVkNDU3YzgxZjZiMzpzZXNzaW9uX2lkPTFfTVg0ME56QXpNamt4Tkg1LU1UWXdOelF6TmpVeU56RTJObjVhVGk4MGIyWkNhRk4wT1ZWa1JGcGtNak5UUkU1aE5IaC1mZyZjcmVhdGVfdGltZT0xNjA3NDM2NTk2Jm5vbmNlPTAuODA1MTEwNjg2NzI1NjE5MyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNjA4MDQxMzk1JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = VideoCallActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private ImageView closeVideoChatBtn;
    private DatabaseReference usersRef;
    private String userId = "";

    private FrameLayout mPubViewController;
    private FrameLayout mSubViewController;

    private Session mSession;
    private Publisher mpublisher;
    private Subscriber mSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videocall);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef  = FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(userId).hasChild("Ringing")){

                            usersRef.child(userId).child("Ringing").removeValue();

                            if (mpublisher != null)
                            {
                                mpublisher.destroy();
                            }
                            if (mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }
                        if (dataSnapshot.child(userId).hasChild("Calling")){

                            usersRef.child(userId).child("Calling").removeValue();

                            if (mpublisher != null)
                            {
                                mpublisher.destroy();
                            }
                            if (mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }
                        else {

                            if (mpublisher != null)
                            {
                                mpublisher.destroy();
                            }
                            if (mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoCallActivity.this, RegistrationActivity.class));
                            finish();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });

        requestPermissions();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoCallActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if(EasyPermissions.hasPermissions(this,perms))
        {
            mPubViewController = findViewById(R.id.pub_container);
            mSubViewController = findViewById(R.id.sub_container);

            mSession = new Session.Builder(this, API_Key, SESSION_ID).build();

            mSession.setSessionListener(VideoCallActivity.this);

            mSession.connect(TOKEN);
        }

        else {
            EasyPermissions.requestPermissions(this, "Need Camera and Mic Permissions...", RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        mpublisher = new Publisher.Builder(this).build();
        mpublisher.setPublisherListener(VideoCallActivity.this);

        mPubViewController.addView(mpublisher.getView());

        if (mpublisher.getView() instanceof GLSurfaceView)
        {
            ((GLSurfaceView) mpublisher.getView()).setZOrderOnTop(true);
        }
        mSession.publish(mpublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Stream Disconnected");

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        if (mSubscriber == null)
        {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null)
        {
            mSubscriber = null;

            mSubViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}