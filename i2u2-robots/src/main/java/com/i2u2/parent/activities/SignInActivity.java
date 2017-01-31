
package com.i2u2.parent.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.i2u2.parent.R;
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

/**
 * Created by Archana on 11/5/16.
 */
public class SignInActivity extends Activity {

    private static final String TAG = "SignInActivity";
    private EditText loginEditText;
    private EditText passwordEditText;
    private static QBChatService chatService;
    private ProgressBar progressBar;
    public static Activity signInActivity;
    private static ArrayList<QBUser> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in_activity);
        initView();
        signInActivity = this;
        QBSettings.getInstance().init(getApplicationContext(), Consts.APP_ID, Consts.AUTH_KEY, Consts.AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(Consts.ACCOUNT_KEY);
        QBChatService.setDebugEnabled(true);
        QBChatService.setDefaultAutoSendPresenceInterval(60); // seconds
        chatService = QBChatService.getInstance();
        createSession();
    }

    /**
     * Method to create session
     */
    private void createSession() {
        showProgress(true);
        QBAuth.createSession(new QBEntityCallback<QBSession>() {

            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                showProgress(false);

            }

            @Override
            public void onError(QBResponseException exc) {
                ErrorUtils.showErrorToast(exc);
                showProgress(false);
            }
        });
    }

    /**
     * Method will initializes all views
     */
    private void initView() {
        loginEditText = (EditText) findViewById(R.id.login_in_edittext);
        passwordEditText = (EditText) findViewById(R.id.password_in_edittext);
        progressBar = (ProgressBar) findViewById(R.id.sign_in_progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void signIn(View view) {
        if (!isValidData(loginEditText.getText().toString(), passwordEditText.getText().toString())) {
            return;
        }
        signIn();
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

    /**
     * Method used to login with login name and password
     */
    public void signIn() {
        final String login = loginEditText.getText().toString();
        final String password = passwordEditText.getText().toString();

        showProgress(true);

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
                QBUsers.getUsers(requestBuilder, new QBEntityCallback<ArrayList<QBUser>>() { // used to get all
                                                                                             // registered users

                    @Override
                    public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                        showProgress(false);

                        users.clear();
                        users.addAll(DataHolder.createUsersList(qbUsers));
                    }

                    @Override
                    public void onError(QBResponseException exc) {
                        showProgress(false);

                        Toaster.shortToast("Error while loading users");
                        Log.d(TAG, "onError()");
                    }
                });
                createSession(login, password);
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
     * @param login
     *            user name
     * @param password
     *            password
     */
    private void createSession(final String login, final String password) {
        showProgress(true);

        final QBUser user = new QBUser(login, password);
        QBAuth.createSession(login, password, new QBEntityCallback<QBSession>() {

            @Override
            public void onSuccess(QBSession session, Bundle bundle) {
                Log.d(TAG, "onSuccess create session with params");
                user.setId(session.getUserId());

                DataHolder.setLoggedUser(user);
                if (chatService.isLoggedIn()) {
                    Intent intent = new Intent(SignInActivity.this, CallActivity.class);
                    intent.putExtra("login", login);
                    startActivity(intent);
                    finish();
                } else {
                    chatService.login(user, new QBEntityCallback<Void>() {

                        @Override
                        public void onSuccess(Void result, Bundle bundle) {
                            Log.d(TAG, "onSuccess login to chat");

                            SignInActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    showProgress(false);
                                }
                            });
                            Intent intent = new Intent(SignInActivity.this, CallActivity.class);
                            intent.putExtra("login", login);
                            startActivity(intent);
                            finish();
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
     * Method to check user input validation
     * 
     * @param login
     *            user name
     * @param password
     *            password
     * @return true if the data is valid else returns false
     */
    private boolean isValidData(String login, String password) {

        if (TextUtils.isEmpty(login) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(login)) {
                loginEditText.setError(getResources().getString(R.string.error_field_is_empty));
            }
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError(getResources().getString(R.string.error_field_is_empty));
            }
            return false;
        }
        return true;
    }

    public void signUp(View view) {
        Intent signUpIntent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(signUpIntent);
    }
}
