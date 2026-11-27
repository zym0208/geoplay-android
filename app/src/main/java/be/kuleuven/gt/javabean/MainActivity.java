package be.kuleuven.gt.javabean;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgAvatar,btnOpenDrawer;
    private TextView txtWelcome;
    private ImageButton btnLogout, btnAddFriend;
    private ImageButton btnGps;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private final String[] LANG_CODES = {"en", "zh", "nl"};
    private final String[] LANG_NAMES = {"English", "简体中文", "Nederlands"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用上次语言设置
        SharedPreferences prefSettings = getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefSettings.getString("lang", "en");
        LocaleHelper.setLocale(this, lang);

        setContentView(R.layout.activity_main);

        // 获取侧边栏中的头像和邮箱 TextView
        navigationView = findViewById(R.id.navigationView);
        View headerView = navigationView.getHeaderView(0);
        imgAvatar = headerView.findViewById(R.id.imgAvatar);
        TextView headerEmail = headerView.findViewById(R.id.navHeaderEmail);

        // 加载头像（如果之前保存过）
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = userPrefs.getString("email", null);


// 从服务器动态获取头像
        if (email != null) {
            ImageUploader.getImageFromServer(this, email, bitmap -> {
                if (bitmap != null) {
                    Glide.with(this).load(bitmap).circleCrop().into(btnOpenDrawer);
                    Glide.with(this).load(bitmap).circleCrop().into(imgAvatar);
                } else {
                    Toast.makeText(this, "无法加载头像", Toast.LENGTH_SHORT).show();
                }
            });
        }


        // 设置头像点击事件
        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // 其他视图
        txtWelcome = findViewById(R.id.txtWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnGps = findViewById(R.id.btnGps);
        Glide.with(this)
                .load(R.drawable.ic_gps)  // or a URL/URI
                .circleCrop()
                .into(btnGps);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        drawerLayout = findViewById(R.id.drawerLayout);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        // 显示欢迎信息和邮箱

        txtWelcome.setText(getString(R.string.welcome_prefix) + email);
        headerEmail.setText(email);
        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());
        // 注销逻辑
        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = userPrefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        ImageButton imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ARActivity.class);
                startActivity(intent);
            }
        });


        // GPS跳转
        btnGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 打开侧边栏
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 侧边栏菜单逻辑
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_language) {
                showLanguageDialog();
            } else if (id == R.id.nav_logout) {
                btnLogout.performClick();
            } else if (id == R.id.nav_my_friends) {
                startActivity(new Intent(MainActivity.this, FriendsListActivity.class));
            } else if (id == R.id.nav_pending_requests) {
                startActivity(new Intent(MainActivity.this, PendingFriendRequestsActivity.class));
            }
            else if (id == R.id.nav_favorite_games) {
                startActivity(new Intent(MainActivity.this, FavoriteGamesActivity.class));
            }
            else if (id == R.id.nav_my_creations) {
                startActivity(new Intent(MainActivity.this, MyCreationsActivity.class));
            }



            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }


    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language)
                .setItems(LANG_NAMES, (dialog, which) -> {
                    String code = LANG_CODES[which];
                    SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                    prefs.edit().putString("lang", code).apply();

                    // 重启Activity
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                    UITranslationHelper.translateUI(findViewById(android.R.id.content), code);

                    Toast.makeText(MainActivity.this, "Language changed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    private void showAddFriendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_friend_email);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.send_request, (dialog, which) -> {
            String toUsername = input.getText().toString().trim();
            if (!toUsername.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String fromEmail = prefs.getString("email", null);
                if (fromEmail != null) {
                    FriendManager.sendFriendRequest(this, fromEmail, toUsername, Volley.newRequestQueue(this));
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = userPrefs.getString("email", null);

        if (email == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("lang", "en");
        Context ctx = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(ctx);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            Glide.with(this).load(selectedImageUri).circleCrop().into(imgAvatar);  // 立即显示
            Glide.with(this).load(selectedImageUri).circleCrop().into(btnOpenDrawer);
            SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String email = userPrefs.getString("email", null);

            if (email != null) {
                ImageUploader.uploadImageToServer(this, selectedImageUri, email, email, success -> {
                    if (success) {
                        Glide.with(this).load(selectedImageUri).circleCrop().into(imgAvatar);

                        SharedPreferences avatarPrefs = getSharedPreferences("avatar", MODE_PRIVATE);
                        avatarPrefs.edit().putString("url_" + email, selectedImageUri.toString()).apply();

                    } else {
                        Toast.makeText(this, "头像上传失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


}
