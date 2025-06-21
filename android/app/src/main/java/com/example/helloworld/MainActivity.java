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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;

public class MainActivity extends AppCompatActivity {
    private EditText etInput;
    private ImageView ivPreview;
    private Button btnGenerate, btnSave;
    private Bitmap generatedBitmap;
    private Spinner spinnerFont, spinnerBg, spinnerAlign;
    private String[] fontNames = {"平方韶华体", "平方洒脱体", "平方上上谦体"};
    private String[] fontFiles = {"fonts/平方韶华体.ttf", "fonts/平方洒脱体.ttf", "fonts/平方上上谦体.ttf"};
    private String[] bgNames = {"月亮1", "月亮2", "叶子"};
    private int[] bgResIds = {R.drawable.moon1, R.drawable.moon2, R.drawable.leaf};
    private int selectedFont = 0, selectedBg = 0;
    private SeekBar seekBarSize;
    private TextView tvSizeLabel;
    private int fontSize = 150;
    private ImageButton btnClear;
    private float textRotation = 0f; // 文字旋转角度
    private float textOffsetX = 0f; // 文字X偏移
    private float textOffsetY = 0f; // 文字Y偏移
    private float lastTouchX = 0f, lastTouchY = 0f;
    private boolean isDragging = false;
    private ScaleGestureDetector scaleGestureDetector;
    private Layout.Alignment textAlign = Layout.Alignment.ALIGN_CENTER;
    private String[] alignNames = {"居中对齐", "居左对齐", "居右对齐"};

    private static final int REQUEST_WRITE_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.etInput);
        ivPreview = findViewById(R.id.ivPreview);
        btnSave = findViewById(R.id.btnSave);
        spinnerFont = findViewById(R.id.spinnerFont);
        spinnerBg = findViewById(R.id.spinnerBg);
        spinnerAlign = findViewById(R.id.spinnerAlign);
        seekBarSize = findViewById(R.id.seekBarSize);
        tvSizeLabel = findViewById(R.id.tvSizeLabel);
        btnClear = findViewById(R.id.btnClear);

        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontNames);
        fontAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_dark);
        spinnerFont.setAdapter(fontAdapter);

        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bgNames);
        bgAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_dark);
        spinnerBg.setAdapter(bgAdapter);

        ArrayAdapter<String> alignAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alignNames);
        alignAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_dark);
        spinnerAlign.setAdapter(alignAdapter);

        // 默认字体选中平方洒脱体
        selectedFont = 1;
        spinnerFont.setSelection(selectedFont);

        // SeekBar设置范围 30~300
        seekBarSize.setMax(270); // 实际范围=30+0~270=30~300
        seekBarSize.setProgress(150-30); // 初始150
        fontSize = 150;
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
        spinnerAlign.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        textAlign = Layout.Alignment.ALIGN_NORMAL;
                        break;
                    case 2:
                        textAlign = Layout.Alignment.ALIGN_OPPOSITE;
                        break;
                    default:
                        textAlign = Layout.Alignment.ALIGN_CENTER;
                        break;
                }
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

        // 设置手势检测
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // 双指缩放时旋转文字
                float rotation = detector.getScaleFactor() > 1 ? 5f : -5f;
                textRotation += rotation;
                autoGenerateImage();
                return true;
            }
        });

        // 设置触摸事件
        ivPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);

                // Map view coordinates to bitmap coordinates for accurate dragging
                final float[] points = new float[] { event.getX(), event.getY() };
                Matrix inverse = new Matrix();
                ivPreview.getImageMatrix().invert(inverse);
                inverse.mapPoints(points);
                final float bitmapX = points[0];
                final float bitmapY = points[1];

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (event.getPointerCount() == 1) {
                            lastTouchX = bitmapX;
                            lastTouchY = bitmapY;
                            isDragging = true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isDragging && event.getPointerCount() == 1) {
                            float deltaX = bitmapX - lastTouchX;
                            float deltaY = bitmapY - lastTouchY;
                            textOffsetX += deltaX;
                            textOffsetY += deltaY;
                            lastTouchX = bitmapX;
                            lastTouchY = bitmapY;
                            autoGenerateImage();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        break;
                }
                return true;
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
                    // Android 10 以下需要动态权限
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
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.createFromAsset(getAssets(), fontFiles[selectedFont]));
        textPaint.setTextSize(fontSize);
        textPaint.setColor(0xFFFFFFFF); // 白色
        textPaint.setShadowLayer(8, 4, 4, 0x80000000);
        textPaint.setTextSkewX(-0.2f); // 微微倾斜

        // 3. 多行自动换行绘制（支持旋转、偏移和自定义行间距）
        String processedText = text.replace(",", ",\n")
                                   .replace("，", "，\n")
                                   .replace(".", ".\n")
                                   .replace("。", "。\n");

        int padding = 64;
        int textBlockWidth = bg.getWidth() - padding;
        
        StaticLayout staticLayout = StaticLayout.Builder.obtain(processedText, 0, processedText.length(), textPaint, textBlockWidth)
                .setAlignment(textAlign)
                .setLineSpacing(0f, 2.0f)
                .setIncludePad(true)
                .build();

        canvas.save();
        float layoutX = (bg.getWidth() - textBlockWidth) / 2f + textOffsetX;
        float layoutY = (bg.getHeight() - staticLayout.getHeight()) / 2f + textOffsetY;
        
        float rotationPivotX = layoutX + textBlockWidth / 2f;
        float rotationPivotY = layoutY + staticLayout.getHeight() / 2f;
        canvas.rotate(textRotation, rotationPivotX, rotationPivotY);
        
        canvas.translate(layoutX, layoutY);
        staticLayout.draw(canvas);
        canvas.restore();

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