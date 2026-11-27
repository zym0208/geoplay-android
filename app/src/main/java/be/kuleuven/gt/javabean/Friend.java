package be.kuleuven.gt.javabean;

import android.graphics.Bitmap;

public class Friend {
    private String username;
    private String email;
    private Bitmap avatarBitmap; // 修改为 Bitmap 类型

    public Friend(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Bitmap getAvatarBitmap() { return avatarBitmap; }
    public void setAvatarBitmap(Bitmap bitmap) { this.avatarBitmap = bitmap; }
}
