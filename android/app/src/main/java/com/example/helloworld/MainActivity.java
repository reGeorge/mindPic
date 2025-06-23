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
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Intent;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;

// 新建 SegmentData 类
class SegmentData {
    String text;
    int selectedFont = 1;
    int selectedBg = 0;
    int fontSize = 200;
    float textOffsetX = 0f, textOffsetY = 0f, textRotation = 0f;
    int offsetXProgress = 0;
    Layout.Alignment textAlign = Layout.Alignment.ALIGN_CENTER;
    SegmentData(String text, int index) {
        this.text = text;
        this.fontSize = 200;
        if (index == 0) {
            this.textAlign = Layout.Alignment.ALIGN_CENTER;
            this.offsetXProgress = 0;
        } else {
            this.textAlign = Layout.Alignment.ALIGN_NORMAL;
            this.offsetXProgress = 0; // 左对齐时初始为0
        }
    }
}

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout titleBar;
    private TextView tvTitle;
    private ImageButton btnSave;
    private EditText etInput;
    private ImageView ivPreview;
    // private Spinner spinnerFont, spinnerBg, spinnerAlign; // 已废弃
    private String[] fontNames = {"平方韶华体", "平方洒脱体", "平方上上谦体"};
    private String[] fontFiles = {"fonts/平方韶华体.ttf", "fonts/平方洒脱体.ttf", "fonts/平方上上谦体.ttf"};
    private String[] bgNames = {"叶子","月亮1", "月亮2"};
    private int[] bgResIds = { R.drawable.leaf,R.drawable.moon1, R.drawable.moon2};
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
    private String[] alignNames = {"居左对齐", "居中对齐", "居右对齐"};
    private MaterialButtonToggleGroup groupFont, groupBg, groupAlign;
    private Bitmap generatedBitmap;
    private SeekBar seekBarOffsetX;
    private TextView tvOffsetXLabel;
    private int offsetXProgress = 0; // SeekBar的进度
    private int maxOffsetX = 1200; // 最大偏移像素

    private static final int REQUEST_WRITE_PERMISSION = 1001;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable generateRunnable;

    private List<SegmentData> segmentList = new ArrayList<>();
    private int currentSegmentIndex = 0;

    private Button btnPrevSegment, btnNextSegment;
    private TextView tvSegmentIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // 强制浅色模式
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        titleBar = findViewById(R.id.titleBar);
        tvTitle = findViewById(R.id.tvTitle);
        btnSave = findViewById(R.id.btnSave);
        etInput = findViewById(R.id.etInput);
        ivPreview = findViewById(R.id.ivPreview);
        seekBarSize = findViewById(R.id.seekBarSize);
        tvSizeLabel = findViewById(R.id.tvSizeLabel);
        groupFont = findViewById(R.id.groupFont);
        groupBg = findViewById(R.id.groupBg);
        groupAlign = findViewById(R.id.groupAlign);
        seekBarOffsetX = findViewById(R.id.seekBarOffsetX);
        tvOffsetXLabel = findViewById(R.id.tvOffsetXLabel);
        seekBarOffsetX.setMax(maxOffsetX * 2); // 允许负偏移
        seekBarOffsetX.setProgress(maxOffsetX); // 初始居中
        offsetXProgress = 0;
        tvOffsetXLabel.setText("水平偏移：" + offsetXProgress);

        btnPrevSegment = findViewById(R.id.btnPrevSegment);
        btnNextSegment = findViewById(R.id.btnNextSegment);
        tvSegmentIndicator = findViewById(R.id.tvSegmentIndicator);

        // 保存按钮
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generatedBitmap == null) {
                    Toast.makeText(MainActivity.this, "请先生成图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSavingDialogAndSave();
            }
        });

        // 水平偏移
        seekBarOffsetX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!segmentList.isEmpty()) {
                    SegmentData seg = segmentList.get(currentSegmentIndex);
                    seg.offsetXProgress = progress - maxOffsetX;
                    tvOffsetXLabel.setText("水平偏移：" + seg.offsetXProgress);
                    if (generateRunnable != null) handler.removeCallbacks(generateRunnable);
                    generateRunnable = new Runnable() {
                        @Override
                        public void run() {
                            autoGenerateImageForCurrentSegment();
                        }
                    };
                    handler.postDelayed(generateRunnable, 100);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 默认字体选中平方洒脱体
        groupFont.check(R.id.btnFont2);
        groupBg.check(R.id.btnBg1);
        groupAlign.check(R.id.btnAlignCenter);
        selectedFont = 1;
        selectedBg = 0;
        textAlign = Layout.Alignment.ALIGN_CENTER;

        // SeekBar设置范围 30~300
        seekBarSize.setMax(270); // 实际范围=30+0~270=30~300
        seekBarSize.setProgress(150-30); // 初始150
        fontSize = 150;
        tvSizeLabel.setText("字号：" + fontSize);

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!segmentList.isEmpty()) {
                    SegmentData seg = segmentList.get(currentSegmentIndex);
                    seg.fontSize = progress + 30;
                    tvSizeLabel.setText("字号：" + seg.fontSize);
                    if (generateRunnable != null) handler.removeCallbacks(generateRunnable);
                    generateRunnable = new Runnable() {
                        @Override
                        public void run() {
                            autoGenerateImageForCurrentSegment();
                        }
                    };
                    handler.postDelayed(generateRunnable, 100);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        groupFont.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked || segmentList.isEmpty()) return;
                SegmentData seg = segmentList.get(currentSegmentIndex);
                if (checkedId == findViewById(R.id.btnFont1).getId()) seg.selectedFont = 0;
                else if (checkedId == findViewById(R.id.btnFont2).getId()) seg.selectedFont = 1;
                else if (checkedId == findViewById(R.id.btnFont3).getId()) seg.selectedFont = 2;
                autoGenerateImageForCurrentSegment();
            }
        });
        groupBg.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked || segmentList.isEmpty()) return;
                SegmentData seg = segmentList.get(currentSegmentIndex);
                if (checkedId == findViewById(R.id.btnBg1).getId()) seg.selectedBg = 0;
                else if (checkedId == findViewById(R.id.btnBg2).getId()) seg.selectedBg = 1;
                else if (checkedId == findViewById(R.id.btnBg3).getId()) seg.selectedBg = 2;
                autoGenerateImageForCurrentSegment();
            }
        });
        groupAlign.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked || segmentList.isEmpty()) return;
                SegmentData seg = segmentList.get(currentSegmentIndex);
                if (checkedId == findViewById(R.id.btnAlignCenter).getId()) {
                    seg.textAlign = Layout.Alignment.ALIGN_CENTER;
                    seg.offsetXProgress = 0;
                } else if (checkedId == findViewById(R.id.btnAlignLeft).getId()) {
                    seg.textAlign = Layout.Alignment.ALIGN_NORMAL;
                    seg.offsetXProgress = 0;
                } else if (checkedId == findViewById(R.id.btnAlignRight).getId()) {
                    seg.textAlign = Layout.Alignment.ALIGN_OPPOSITE;
                    seg.offsetXProgress = 0;
                }
                autoGenerateImageForCurrentSegment();
            }
        });
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String[] segments = s.toString().split("\\n\\n+");
                segmentList.clear();
                for (int i = 0; i < segments.length; i++) {
                    segmentList.add(new SegmentData(segments[i].trim(), i));
                }
                currentSegmentIndex = 0;
                updateSegmentUI();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // 首次自动生成
        autoGenerateImageForCurrentSegment();

        // 设置手势检测
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // 双指缩放时旋转文字
                float rotation = detector.getScaleFactor() > 1 ? 5f : -5f;
                textRotation += rotation;
                autoGenerateImageForCurrentSegment();
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
                            autoGenerateImageForCurrentSegment();
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

        btnPrevSegment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSegmentIndex > 0) {
                    currentSegmentIndex--;
                    updateSegmentUI();
                }
            }
        });
        btnNextSegment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSegmentIndex < segmentList.size() - 1) {
                    currentSegmentIndex++;
                    updateSegmentUI();
                }
            }
        });

        updateSegmentUI(); // 首次初始化
    }

    // 渲染当前段落图片
    private void autoGenerateImageForCurrentSegment() {
        if (segmentList.isEmpty()) {
            ivPreview.setImageResource(R.drawable.leaf);
            generatedBitmap = null;
            return;
        }
        SegmentData seg = segmentList.get(currentSegmentIndex);
        if (TextUtils.isEmpty(seg.text)) {
            ivPreview.setImageResource(R.drawable.leaf);
            generatedBitmap = null;
            return;
        }
        generatedBitmap = generateImage(seg.text, seg.selectedFont, seg.selectedBg, seg.fontSize, seg.textOffsetX, seg.textOffsetY, seg.textRotation, seg.offsetXProgress, seg.textAlign);
        Glide.with(MainActivity.this).load(generatedBitmap).into(ivPreview);
    }

    // 修改 generateImage 方法参数
    private Bitmap generateImage(String text, int selectedFont, int selectedBg, int fontSize, float textOffsetX, float textOffsetY, float textRotation, int offsetXProgress, Layout.Alignment textAlign) {
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
        String processedText = text;
        int padding = 64;
        int textBlockWidth = bg.getWidth() - 2 * padding;
        StaticLayout staticLayout = StaticLayout.Builder.obtain(processedText, 0, processedText.length(), textPaint, textBlockWidth)
                .setAlignment(textAlign)
                .setLineSpacing(0f, 2.0f)
                .setIncludePad(true)
                .build();

        canvas.save();
        float layoutX;
        if (textAlign == Layout.Alignment.ALIGN_CENTER) {
            layoutX = (bg.getWidth() - textBlockWidth) / 2f + textOffsetX + offsetXProgress;
        } else if (textAlign == Layout.Alignment.ALIGN_NORMAL) { // 左对齐
            layoutX = padding + textOffsetX + offsetXProgress;
        } else if (textAlign == Layout.Alignment.ALIGN_OPPOSITE) { // 右对齐
            layoutX = bg.getWidth() - padding - textBlockWidth + textOffsetX + offsetXProgress;
        } else {
            layoutX = (bg.getWidth() - textBlockWidth) / 2f + textOffsetX + offsetXProgress;
        }
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
    private boolean saveImageToGallery(Bitmap bitmap) {
        String fileName = "mindpic_" + System.currentTimeMillis() + ".png";
        OutputStream fos = null;
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    private void showSavingDialogAndSave() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在保存图片...");
        dialog.setCancelable(false);
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean success = saveImageToGallery(generatedBitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (success) {
                            Toast.makeText(MainActivity.this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void updateSegmentUI() {
        autoGenerateImageForCurrentSegment();
        tvSegmentIndicator.setText((currentSegmentIndex + 1) + "/" + (segmentList.size() == 0 ? 1 : segmentList.size()));
        if (!segmentList.isEmpty()) {
            SegmentData seg = segmentList.get(currentSegmentIndex);
            // 字体
            int fontBtnId = R.id.btnFont2;
            if (seg.selectedFont == 0) fontBtnId = R.id.btnFont1;
            else if (seg.selectedFont == 1) fontBtnId = R.id.btnFont2;
            else if (seg.selectedFont == 2) fontBtnId = R.id.btnFont3;
            groupFont.check(fontBtnId);
            // 背景
            int bgBtnId = R.id.btnBg1;
            if (seg.selectedBg == 0) bgBtnId = R.id.btnBg1;
            else if (seg.selectedBg == 1) bgBtnId = R.id.btnBg2;
            else if (seg.selectedBg == 2) bgBtnId = R.id.btnBg3;
            groupBg.check(bgBtnId);
            // 字号
            seekBarSize.setProgress(seg.fontSize - 30);
            tvSizeLabel.setText("字号：" + seg.fontSize);
            // 水平偏移
            seekBarOffsetX.setProgress(seg.offsetXProgress + maxOffsetX);
            tvOffsetXLabel.setText("水平偏移：" + seg.offsetXProgress);
            // 对齐方式
            int alignBtnId = R.id.btnAlignCenter;
            if (seg.textAlign == Layout.Alignment.ALIGN_CENTER) alignBtnId = R.id.btnAlignCenter;
            else if (seg.textAlign == Layout.Alignment.ALIGN_NORMAL) alignBtnId = R.id.btnAlignLeft;
            else if (seg.textAlign == Layout.Alignment.ALIGN_OPPOSITE) alignBtnId = R.id.btnAlignRight;
            groupAlign.check(alignBtnId);
        }
    }
} 