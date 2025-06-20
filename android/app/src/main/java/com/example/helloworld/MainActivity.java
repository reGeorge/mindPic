package com.example.helloworld;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private EditText etInput;
    private ImageView ivPreview;
    private Button btnGenerate, btnSave;
    private Bitmap generatedBitmap;

    private static final int REQUEST_WRITE_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.etInput);
        ivPreview = findViewById(R.id.ivPreview);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnSave = findViewById(R.id.btnSave);

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etInput.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    Toast.makeText(MainActivity.this, "请输入描述", Toast.LENGTH_SHORT).show();
                    return;
                }
                generatedBitmap = generateImage(text);
                Glide.with(MainActivity.this).load(generatedBitmap).into(ivPreview);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generatedBitmap == null) {
                    Toast.makeText(MainActivity.this, "请先生成图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
                        return;
                    }
                }
                saveImageToGallery(generatedBitmap);
            }
        });
    }

    // 合成图片：背景+文字+字体
    private Bitmap generateImage(String text) {
        // 1. 加载背景图片（以 moon1.png 为例）
        Bitmap bg = BitmapFactory.decodeResource(getResources(), R.drawable.moon1);
        Bitmap result = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bg, 0, 0, null);

        // 2. 加载字体
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/平方韶华体.ttf");
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(80);
        paint.setColor(0xFFFFFFFF); // 白色
        paint.setShadowLayer(8, 4, 4, 0x80000000);

        // 3. 文字居中绘制
        float textWidth = paint.measureText(text);
        float x = (bg.getWidth() - textWidth) / 2;
        float y = bg.getHeight() / 2;
        canvas.drawText(text, x, y, paint);

        return result;
    }

    // 保存图片到相册
    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "mindpic_" + System.currentTimeMillis() + ".png";
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/mindPic");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, fileName, "mindPic生成");
                fos = null; // 已保存
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }
            Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (generatedBitmap != null) {
                    saveImageToGallery(generatedBitmap);
                }
            } else {
                Toast.makeText(this, "未获得存储权限，无法保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 