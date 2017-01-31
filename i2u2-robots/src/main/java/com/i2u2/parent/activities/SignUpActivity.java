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
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.sample.core.utils.ErrorUtils;
import com.quickblox.sample.core.utils.Toaster;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;

/**
 * Created by Archana on 9/5/16.
 */
public class SignUpActivity extends Activity {
    private static final String TAG = "SignInActivity";
    private EditText loginEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordText;

    private static QBChatService chatService;
    private ProgressBar progressBar;
    private static ArrayList<QBUser> registeredUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_activity);
        initView();
        chatService = QBChatService.getInstance();
        createSession();
    }

    /**
     * Method to create session in QuickBlox
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
     * Method to initialize all the views
     */
    private void initView() {
        loginEditText = (EditText) findViewById(R.id.login_up_edittext);
        passwordEditText = (EditText) findViewById(R.id.password_up_edittext);
        confirmPasswordText = (EditText) findViewById(R.id.password_confirm_edittext);
        progressBar = (ProgressBar) findViewById(R.id.sign_up_progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void submit(View view) {
        if(!isValidData(loginEditText.getText().toString(),passwordEditText.getText().toString(),confirmPasswordText.getText().toString())){
            return;
        }
        signUp();
    }

    /**
     * Method to show progress bar
     * @param show can be true or false
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Method to signUp with login and password
     */
    public void signUp() {
        final String login = loginEditText.getText().toString();
        final String password = passwordEditText.getText().toString();

        showProgress(true);

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
                        registeredUsers.clear();
                        registeredUsers.addAll(DataHolder.createUsersList(qbUsers));
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
     * Method to create session with login and password
     * @param login user name
     * @param password password
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
                    Intent intent = new Intent(SignUpActivity.this, CallActivity.class);
                    intent.putExtra("login", login);
                    startActivity(intent);
                    SignInActivity.signInActivity.finish();
                    finish();
                } else {
                    //To login to chat service
                    chatService.login(user, new QBEntityCallback<Void>() {

                        @Override
                        public void onSuccess(Void result, Bundle bundle) {
                            Log.d(TAG, "onSuccess login to chat");

                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showProgress(false);
                                }
                            });
                            Intent intent = new Intent(SignUpActivity.this, CallActivity.class);
                            intent.putExtra("login", login);
                            startActivity(intent);
                            SignInActivity.signInActivity.finish();
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
                Toaster.shortToast("Error when login, check test users login and password");
            }
        });
    }

    /**
     * method to validate the user signUp details
     * @param login user name
     * @param password password
     * @param confirm confirmation password
     * @return true if the data is valid else returns false
     */
    private boolean isValidData(String login, String password,String confirm) {

        if (TextUtils.isEmpty(login) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(login)) {
                loginEditText.setError(getResources().getString(R.string.error_field_is_empty));
            }
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError(getResources().getString(R.string.error_field_is_empty));
            }
            if (TextUtils.isEmpty(confirm)) {
                confirmPasswordText.setError(getResources().getString(R.string.error_field_is_empty));
            }
            return false;
        }
        if (login.length() < 3) {
            loginEditText.setError(getResources().getString(R.string.login_lenth_error));
            return false;
        }
        if (!TextUtils.equals(password, confirm)) {
            confirmPasswordText.setError(getResources().getString(R.string.confirm_error));
            return false;
        }

        if (password.length() < 8) {
            passwordEditText.setError(getResources().getString(R.string.length_error));
            return false;
        }
        return true;
    }

}
