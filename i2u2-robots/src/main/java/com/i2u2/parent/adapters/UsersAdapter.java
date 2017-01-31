package com.i2u2.parent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.i2u2.parent.R;
import com.i2u2.parent.activities.ListUsersActivity;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;

/**
 * QuickBlox team
 */
public class UsersAdapter extends BaseAdapter {
    private ArrayList<QBUser> user;

    private LayoutInflater inflater;

    public UsersAdapter(Context context, ArrayList<QBUser> results) {
        user = results;
        inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return user.size();
    }

    public QBUser getItem(int position) {
        return user.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_user, null);
            holder = new ViewHolder();
            holder.userNumber = (TextView) convertView.findViewById(R.id.userNumber);
            holder.loginAs = (TextView) convertView.findViewById(R.id.loginAs);
            holder.fullName = (TextView) convertView.findViewById(R.id.fullName);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.userNumber.setText(String.valueOf(position+1));
        holder.userNumber.setBackgroundResource(ListUsersActivity.resourceSelector(position));
        holder.loginAs.setText(R.string.login_as);
        holder.fullName.setText(user.get(position).getFullName());

        return convertView;
    }

    public static class ViewHolder {
        TextView userNumber;
        TextView loginAs;
        TextView fullName;
    }
}