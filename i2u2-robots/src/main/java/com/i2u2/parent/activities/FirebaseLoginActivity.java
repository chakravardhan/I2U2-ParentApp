package com.i2u2.parent.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.i2u2.parent.Friend;
import com.i2u2.parent.QuickBlox;
import com.i2u2.parent.R;
import com.i2u2.parent.User;
import com.i2u2.parent.holder.DataHolder;
import com.i2u2.parent.utils.Consts;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBSettings;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.sample.core.utils.ErrorUtils;
import com.quickblox.sample.core.utils.Toaster;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kailash on 8/6/16.
 */
public class FirebaseLoginActivity extends AppCompatActivity {
    private static final String TAG = FirebaseLoginActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressBar progressBar;
    private static QBChatService chatService;
    private static ArrayList<QBUser> users = new ArrayList<>();
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private Button googleSigninBT;
    private List<String> friendsEmail = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_login);

        initView();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        // Check if Logged in
        if (mFirebaseUser != null) {
            Log.d(TAG, "Signed in:" + mFirebaseUser.getUid());

            QBSettings.getInstance().init(getApplicationContext(), Consts.APP_ID, Consts.AUTH_KEY, Consts.AUTH_SECRET);
            QBSettings.getInstance().setAccountKey(Consts.ACCOUNT_KEY);
            QBChatService.setDebugEnabled(true);
            QBChatService.setDefaultAutoSendPresenceInterval(60); // seconds
            chatService = QBChatService.getInstance();
            createSession();

            // User is signed in and check for Quickblox sign in
            final String email = mFirebaseUser.getEmail();
            if(email != null) {
                final String email2 = email.replace(".", "");

                // Quickblox Authentication
                quickBloxAuthentication(email2);

            }else{
                Log.d(TAG, "User email is null");
            }
        } else {
            Log.d(TAG, "No user is signed in:");

            googleSigninBT = (Button) findViewById(R.id.googleSigninBT);
            googleSigninBT.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showProgress(true);

                    QBSettings.getInstance().init(getApplicationContext(), Consts.APP_ID, Consts.AUTH_KEY, Consts.AUTH_SECRET);
                    QBSettings.getInstance().setAccountKey(Consts.ACCOUNT_KEY);
                    QBChatService.setDebugEnabled(true);
                    QBChatService.setDefaultAutoSendPresenceInterval(60); // seconds
                    chatService = QBChatService.getInstance();
                    createSession();

                    // No user is signed in
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setProviders(
                                            AuthUI.GOOGLE_PROVIDER
                                    )
                                    .build(),
                            RC_SIGN_IN);

                }
            });
        }
    }

    /**
     * Sign in/Sign up with QuickBlox
     * @param email
     */
    private void quickBloxAuthentication(final String email){
        // Get Quickblox account details associated with this Firebase account
        mFirebaseDatabase.getReference("users").child(email).child("quickblox").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.toString());

                if(dataSnapshot.getValue() == null){

                    //Generate a login password for Quickblox account
                    String login = email.replace("@", "");
                    String password = email.replace("@", "");

                    QuickBlox accountDetails = new QuickBlox(login, password);

                    // Sign up using generated login and password
                    signUp(email, accountDetails);

                }else{
                    QuickBlox details = dataSnapshot.getValue(QuickBlox.class);

                    // Sign in using details fetched from Firebase.
                    signIn(email, details);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Error:"+databaseError.getMessage());
            }
        });
    }

    /**
     * Method will initializes all views
     */
    private void initView() {
        progressBar = (ProgressBar) findViewById(R.id.sign_in_progress_bar_fb);
        progressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Method to create session
     */
    private void createSession() {
        showProgress(true);
        QBAuth.createSession(new QBEntityCallback<QBSession>() {

            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                //showProgress(false);

            }

            @Override
            public void onError(QBResponseException exc) {
                ErrorUtils.showErrorToast(exc);
                //showProgress(false);
            }
        });
    }

    /**
     * Method to display progress bar
     *
     * @param show
     *            can be true or false
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // user is signed in!
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {

                    // Update account details
                    uploadAccountDetails(user);

                }else{
                    Log.d(TAG, "Not logged in");
                }
            } else {
                // user is not signed in. Maybe just wait for the user to press
                // "sign in" again, or show a message
                Toast.makeText(getApplicationContext(),
                        "User is not signed in, Please sign in",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Update Account details in Firebase database
     * @param user
     */
    private void uploadAccountDetails(final FirebaseUser user){
        Log.i(TAG, "writing to database");

        String email = user.getEmail();

        if(email != null) {
            final String email2 = email.replace(".", "");

            mFirebaseDatabase.getReference("users").child(email2).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Data:"+dataSnapshot.toString());

                    if(dataSnapshot.getValue() == null){
                        Map<String, Object> map = new HashMap<>();

                        String displayName = user.getDisplayName();
                        if (displayName != null) {
                            map.put("name", displayName);
                        }

                        String uid = user.getUid();
                        map.put("uid", uid);

                        Uri uri = user.getPhotoUrl();

                        if(uri != null) {
                            map.put("profileURL", uri.buildUpon().build().toString());
                        }

                        map.put("type_of_user", Consts.CONTROLLER);
                        map.put("friends", null);

                        mFirebaseDatabase.getReference("users").child(email2).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "(Google) Account details updated successfully");

                                    String email = user.getEmail();

                                    if(email != null) {
                                        String email2 = email.replace(".", "");

                                        //Quickblox Authentication
                                        quickBloxAuthentication(email2);

                                    }else{
                                        Log.d(TAG, "Email is null");
                                    }

                                } else {
                                    Log.d(TAG, "Error in updating details:Google account");
                                }
                            }
                        });
                    }else{
                        Log.d(TAG, "User details are not null");

                        User userDetails = dataSnapshot.getValue(User.class);
                        if(userDetails.getType_of_user().equals(Consts.CONTROLLER)){

                            String email = user.getEmail();

                            if(email != null) {
                                String email2 = email.replace(".", "");

                                //Quickblox Authentication
                                quickBloxAuthentication(email2);

                            }else{
                                Log.d(TAG, "Email is null");
                            }

                        }else{

                            Log.d(TAG, "This account is already registered as Robot, Please login fom Robot app");

                            Toast.makeText(getApplicationContext(),
                                    "This account is already registered as Robot, Please login fom Robot app", Toast.LENGTH_SHORT).show();

                            // Stop Progress Bar
                            showProgress(false);
                            logoutGoogle();

                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "Error:"+databaseError.getMessage());
                }
            });
        }else{
            Log.d(TAG, "Email is null");
        }
    }

    /**
     * Method used to login with login name and password
     */
    public void signIn(final String email, final QuickBlox accountDetails) {
        Log.d(TAG, "Sign in");
        showProgress(true);

        final String login = accountDetails.getLogin();
        final String password = accountDetails.getPassword();

        final QBUser user = new QBUser(login, password);
        QBUsers.signIn(user, new QBEntityCallback<QBUser>() {

            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                showProgress(false);
                setResult(RESULT_OK);
                DataHolder.addQbUser(qbUser);
                DataHolder.setLoggedUser(qbUser);
                user.setFullName(login);
                showProgress(true);

                QBPagedRequestBuilder requestBuilder = new QBPagedRequestBuilder();
                requestBuilder.setPerPage(getResources().getInteger(R.integer.users_count));
                //To get all registered users
                QBUsers.getUsers(requestBuilder, new QBEntityCallback<ArrayList<QBUser>>() {
                    @Override
                    public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                        showProgress(false);

                        users.clear();
                        users.addAll(qbUsers);

                        //Get list of friends
                        getFriends(email);
                    }

                    @Override
                    public void onError(QBResponseException exc) {
                        showProgress(false);

                        Toaster.shortToast("Error while loading users");
                        Log.d(TAG, "onError()");
                    }
                });

                createSession(1, email, accountDetails);
                Toaster.longToast(R.string.user_successfully_sign_in);
            }

            @Override
            public void onError(QBResponseException errors) {
                showProgress(false);
                Toaster.longToast(errors.getErrors().toString());
            }
        });
    }

    /**
     * Method to signUp with login and password
     */
    public void signUp(final String email, final QuickBlox accountDetails) {
        Log.d(TAG, "Sign up");
        showProgress(true);

        final String login = accountDetails.getLogin();
        final String password = accountDetails.getPassword();

        final QBUser user = new QBUser(login, password);
        user.setLogin(login);
        user.setFullName(login);
        user.setPassword(password);
        QBUsers.signUpSignInTask(user, new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                showProgress(false);
                setResult(RESULT_OK);
                DataHolder.addQbUser(qbUser);
                DataHolder.setLoggedUser(qbUser);
                showProgress(true);

                QBPagedRequestBuilder requestBuilder = new QBPagedRequestBuilder();
                requestBuilder.setPerPage(getResources().getInteger(R.integer.users_count));
                //To get all registered users
                QBUsers.getUsers(requestBuilder, new QBEntityCallback<ArrayList<QBUser>>() {
                    @Override
                    public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                        showProgress(false);

                        users.clear();
                        users.addAll(qbUsers);

                        //Get list of friends
                        getFriends(email);

                    }

                    @Override
                    public void onError(QBResponseException exc) {
                        showProgress(false);

                        Toaster.shortToast("Error while loading users");
                        Log.d(TAG, "onError()");
                    }
                });

                createSession(0, email, accountDetails);
                Toaster.longToast(R.string.user_successfully_sign_in);
            }

            @Override
            public void onError(QBResponseException errors) {
                showProgress(false);
                Toaster.longToast(errors.getErrors().toString());
            }
        });
    }

    /**
     * Method will create session by passing login and password.
     *
     */
    private void createSession(final int type, final String email, final QuickBlox accountDetails) {
        showProgress(true);

        final String login = accountDetails.getLogin();
        final String password = accountDetails.getPassword();

        final QBUser user = new QBUser(login, password);
        QBAuth.createSession(login, password, new QBEntityCallback<QBSession>() {

            @Override
            public void onSuccess(QBSession session, Bundle bundle) {
                Log.d(TAG, "onSuccess create session with params");
                user.setId(session.getUserId());

                DataHolder.setLoggedUser(user);
                if (chatService.isLoggedIn()) {

                    if(type == 0) {
                        updateQuickBloxAccountDetails(email, accountDetails);
                    }else if(type == 1){
                        Intent intent = new Intent(FirebaseLoginActivity.this, CallActivity.class);
                        intent.putExtra("login", accountDetails.getLogin());
                        startActivity(intent);
                        finish();
                    }

                } else {
                    chatService.login(user, new QBEntityCallback<Void>() {

                        @Override
                        public void onSuccess(Void result, Bundle bundle) {
                            Log.d(TAG, "onSuccess login to chat");

                            FirebaseLoginActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    showProgress(false);
                                }
                            });

                            if(type == 0) {
                                updateQuickBloxAccountDetails(email, accountDetails);
                            }else if(type == 1){
                                Intent intent = new Intent(FirebaseLoginActivity.this, CallActivity.class);
                                intent.putExtra("login", accountDetails.getLogin());
                                startActivity(intent);
                                finish();
                            }
                        }

                        @Override
                        public void onError(QBResponseException exc) {
                            showProgress(false);
                            Toaster.longToast(exc.getErrors().toString());
                        }
                    });
                }
            }

            @Override
            public void onError(QBResponseException exc) {
                progressBar.setVisibility(View.INVISIBLE);
                Toaster.shortToast("Error when login, check user login and password");
            }
        });
    }

    /**
     * Update QuickBlox user account details in Firebase
     * @param email
     * @param accountDetails
     */
    private void updateQuickBloxAccountDetails(String email, final QuickBlox accountDetails){
        // Store login and password in Firebase database
        mFirebaseDatabase.getReference("users").child(email).child("quickblox")
                .setValue(accountDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "Quickblox account details updated successfully");

                    Intent intent = new Intent(FirebaseLoginActivity.this, CallActivity.class);
                    intent.putExtra("login", accountDetails.getLogin());
                    startActivity(intent);
                    finish();

                }else{
                    Log.d(TAG, "Error in updating data:QuickBlox details");
                }
            }
        });
    }

    private void logoutGoogle(){
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Logged out");
                    }
                });
    }

    /**
     * Get list of friends from Firebase
     * @param email
     */
    private void getFriends(String email){

        mFirebaseDatabase.getReference("users").child(email).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null){
                    if(dataSnapshot.hasChildren()){
                        for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                            friendsEmail.clear();
                            friendsEmail.add(dataSnapshot1.getKey());
                        }
                        Log.d(TAG, "All Friends:"+users.toString());

                        List<QBUser> opponentsList2 = new ArrayList<QBUser>();
                        opponentsList2 = users;

                        for(int i=0;i<opponentsList2.size();i++){
                            QBUser user = opponentsList2.get(i);
                            if(!friendsEmail.contains(user.getLogin())){
                                users.remove(user);
                            }
                        }

                        Log.d(TAG, "Filtered Friends:"+users.toString());

                        DataHolder.createUsersList(users);

                    }else{
                        Log.d(TAG, "0 Friends");
                    }
                }else{
                    Log.d(TAG, "0 Friends");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Error:"+databaseError.getMessage());
            }
        });
    }

}
