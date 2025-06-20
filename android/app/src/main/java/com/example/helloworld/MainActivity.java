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
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import android.widget.ArrayAdapter;
import java.io.OutputStream;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    private EditText etInput;
    private ImageView ivPreview;
    private Button btnGenerate, btnSave;
    private Bitmap generatedBitmap;
    private Spinner spinnerFont, spinnerBg;
    private String[] fontNames = {"平方韶华体", "平方洒脱体", "平方上上谦体"};
    private String[] fontFiles = {"fonts/平方韶华体.ttf", "fonts/平方洒脱体.ttf", "fonts/平方上上谦体.ttf"};
    private String[] bgNames = {"月亮1", "月亮2", "叶子"};
    private int[] bgResIds = {R.drawable.moon1, R.drawable.moon2, R.drawable.leaf};
    private int selectedFont = 0, selectedBg = 0;
    private SeekBar seekBarSize;
    private TextView tvSizeLabel;
    private int fontSize = 80;
    private ImageButton btnClear;

    private static final int REQUEST_WRITE_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.etInput);
        ivPreview = findViewById(R.id.ivPreview);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnSave = findViewById(R.id.btnSave);
        spinnerFont = findViewById(R.id.spinnerFont);
        spinnerBg = findViewById(R.id.spinnerBg);
        seekBarSize = findViewById(R.id.seekBarSize);
        tvSizeLabel = findViewById(R.id.tvSizeLabel);
        btnClear = findViewById(R.id.btnClear);

        spinnerFont.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fontNames));
        spinnerBg.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bgNames));

        spinnerFont.setSelection(selectedFont);
        spinnerBg.setSelection(selectedBg);

        // SeekBar设置范围 30~150
        seekBarSize.setMax(600); // 实际范围=30+0~120=30~150
        seekBarSize.setProgress(200); // 初始80
        fontSize = 80;
        tvSizeLabel.setText("字号：" + fontSize);

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fontSize = progress + 30;
                tvSizeLabel.setText("字号：" + fontSize);
                autoGenerateImage();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        spinnerFont.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedFont = position;
                autoGenerateImage();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        spinnerBg.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedBg = position;
                autoGenerateImage();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoGenerateImage();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // 首次自动生成
        autoGenerateImage();

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

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etInput.setText("");
            }
        });
    }

    // 自动生成图片并预览
    private void autoGenerateImage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            ivPreview.setImageResource(R.drawable.moon1);
            generatedBitmap = null;
            return;
        }
        generatedBitmap = generateImage(text);
        Glide.with(MainActivity.this).load(generatedBitmap).into(ivPreview);
    }

    // 合成图片：背景+文字+字体
    private Bitmap generateImage(String text) {
        // 1. 加载背景图片
        Bitmap bg = BitmapFactory.decodeResource(getResources(), bgResIds[selectedBg]);
        Bitmap result = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bg, 0, 0, null);

        // 2. 加载字体
        Typeface typeface = Typeface.createFromAsset(getAssets(), fontFiles[selectedFont]);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(fontSize);
        paint.setColor(0xFFFFFFFF); // 白色
        paint.setShadowLayer(8, 4, 4, 0x80000000);
        paint.setTextSkewX(-0.2f); // 微微倾斜

        // 3. 多行自动换行绘制
        String[] lines = text.split("\\n");
        float totalHeight = lines.length * (paint.getFontSpacing());
        float y = (bg.getHeight() - totalHeight) / 2 + Math.abs(paint.ascent());
        for (String line : lines) {
            float textWidth = paint.measureText(line);
            float x = (bg.getWidth() - textWidth) / 2;
            canvas.drawText(line, x, y, paint);
            y += paint.getFontSpacing();
        }

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