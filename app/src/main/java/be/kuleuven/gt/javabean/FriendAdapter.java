package be.kuleuven.gt.javabean;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FriendAdapter extends ArrayAdapter<Friend> {
    private Context context;
    private List<Friend> friendList;

    public FriendAdapter(Context context, List<Friend> friendList) {
        super(context, 0, friendList);
        this.context = context;
        this.friendList = friendList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Friend friend = friendList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        }

        TextView usernameView = convertView.findViewById(R.id.textViewUsername);
        ImageView avatarView = convertView.findViewById(R.id.imageViewAvatar);

        usernameView.setText(friend.getUsername());

        if (friend.getAvatarBitmap() != null) {
            avatarView.setImageBitmap(friend.getAvatarBitmap());
        } else {
            avatarView.setImageResource(R.drawable.account_circle_24px);

            // 异步加载头像
            ImageUploader.getImageFromServer(context, friend.getEmail(), bitmap -> {
                if (bitmap != null) {
                    friend.setAvatarBitmap(bitmap);
                    notifyDataSetChanged(); // 更新 ListView
                }
            });
        }

        return convertView;
    }
}
