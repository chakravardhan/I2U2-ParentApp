package com.i2u2.parent.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.i2u2.parent.R;
import com.i2u2.parent.activities.CallActivity;
import com.i2u2.parent.adapters.OpponentsAdapter;
import com.quickblox.sample.core.utils.ConnectivityUtils;
import com.quickblox.sample.core.utils.Toaster;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCConfig;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QuickBlox team
 */
public class OpponentsFragment extends Fragment implements View.OnClickListener, Serializable {

    private static final String TAG = OpponentsFragment.class.getSimpleName();
    private OpponentsAdapter opponentsAdapter;
    public static String login;
    private Button btnAudioCall;
    private Button btnVideoCall;
    private View view = null;
    private ProgressDialog progresDialog;
    private ListView opponentsList;
    private Context context;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;

    private EditText friendEmailET;
    private Button friendEmailBT;

    public static OpponentsFragment getInstance() {
        return new OpponentsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreate() from OpponentsFragment");
        super.onCreate(savedInstanceState);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.context = getActivity();

        ((CallActivity) getActivity()).initActionBar();

        view = inflater.inflate(R.layout.fragment_opponents, container, false);

        initUI(view);

        // Show dialog till opponents loading
        progresDialog = new ProgressDialog(getActivity()) {
            @Override
            public void onBackPressed() {
                Toaster.shortToast("Wait until loading finish");
            }
        };
        progresDialog.setMessage("Load opponents ...");
        progresDialog.setCanceledOnTouchOutside(false);
        progresDialog.show();

        initOpponentListAdapter();

        return view;
    }

    private void initOpponentListAdapter() {
        final ListView opponentsList = (ListView) view.findViewById(R.id.opponentsList);

        List<QBUser> userList = new ArrayList<>(((CallActivity) getActivity()).getOpponentsList());
        prepareUserList(opponentsList, userList);
        progresDialog.dismiss();


    }

    private void prepareUserList(ListView opponentsList, List<QBUser> users) {
        int i = searchIndexLogginedUser(users);
        if (i >= 0)
            users.remove(i);

        // Prepare users list for simple adapter.
        //
        opponentsAdapter = new OpponentsAdapter(getActivity(), users);
        opponentsList.setAdapter(opponentsAdapter);
    }

    private void initUI(View view) {

        login = getActivity().getIntent().getStringExtra("login");

        btnAudioCall = (Button) view.findViewById(R.id.btnAudioCall);
        btnVideoCall = (Button) view.findViewById(R.id.btnVideoCall);

        btnAudioCall.setOnClickListener(this);
        btnVideoCall.setOnClickListener(this);

        opponentsList = (ListView) view.findViewById(R.id.opponentsList);

        friendEmailET = (EditText)view.findViewById(R.id.addFriendET);
        friendEmailBT = (Button)view.findViewById(R.id.addFriendBT);
        friendEmailBT.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.addFriendBT){

            String email = friendEmailET.getText().toString();

            if(!email.equals("")){

                addFriend(mFirebaseUser.getEmail().replace(".", ""), email);

            }
        }else {

            if (opponentsAdapter.getSelected().isEmpty()) {
                Toaster.longToast("Choose one opponent");
                return;
            }

            if (opponentsAdapter.getSelected().size() > QBRTCConfig.getMaxOpponentsCount()) {
                Toaster.longToast("Max number of opponents is 6");
                return;
            }
            QBRTCTypes.QBConferenceType qbConferenceType = null;

            //Init conference type
            switch (v.getId()) {
                case R.id.btnAudioCall:
                    qbConferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO;
                    break;

                case R.id.btnVideoCall:
                    // get call type
                    qbConferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO;

                    break;
            }

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("any_custom_data", "some data");
            userInfo.put("my_avatar_url", "avatar_reference");

            ((CallActivity) getActivity())
                    .addConversationFragmentStartCall(opponentsAdapter.getSelected(),
                            qbConferenceType, userInfo);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progresDialog.isShowing()) {
            progresDialog.dismiss();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_opponents, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                ((CallActivity) getActivity()).logout();
                return true;
            case R.id.settings:
                ((CallActivity) getActivity()).showSettings();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static int searchIndexLogginedUser(List<QBUser> usersList) {
        int indexLogginedUser = -1;
        for (QBUser usr : usersList) {
            if (usr.getLogin().equals(login)) {
                indexLogginedUser = usersList.indexOf(usr);
                break;
            }
        }
        return indexLogginedUser;
    }

    /**
     * Add friend to my account
     */
    private void addFriend(final String myEmail, final String friendEmail){

        mFirebaseDatabase.getReference("users").child(friendEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null){

                    mFirebaseDatabase.getReference("users").child(myEmail)
                            .child("friends").child(friendEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            // If null, friend is not in my friend list, add friend to my friend list
                            if(dataSnapshot.getValue() == null){

                                mFirebaseDatabase.getReference("users").child(myEmail).child("friends").child(friendEmail).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) {
                                            Log.d(TAG, "My friend list is updated");

                                            mFirebaseDatabase.getReference("users").child(friendEmail).child("friends").child(myEmail).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        Log.d(TAG, "Friend list of my friend is updated");
                                                    }else{
                                                        Log.d(TAG, "Error in updating Friend list of my friend");
                                                    }
                                                }
                                            });
                                        }else{
                                            Log.d(TAG, "Error in updating my friend list");
                                        }
                                    }
                                });
                            }else{
                                Log.d(TAG, "Already in friend list");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG, "onCancelled:"+databaseError.getMessage());
                        }
                    });
                }else{
                    Log.d(TAG, "No user registered with this email");
                    Toast.makeText(context,
                            "No user registered with this email", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled:"+databaseError.getMessage());
            }
        });
    }
}
