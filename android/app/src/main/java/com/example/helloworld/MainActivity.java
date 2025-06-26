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
import android.graphics.Rect;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.ImageView;
import android.util.LruCache;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;
import java.util.Set;
import com.google.android.material.snackbar.Snackbar;
import android.view.Gravity;
import android.widget.FrameLayout;

// 新建 SegmentData 类
class SegmentData {
    String text;
    int selectedFont = 1;
    int selectedBg = 0;
    int fontSize = 60;
    float textOffsetX = 0f, textOffsetY = 0f, textRotation = 0f;
    int offsetXProgress = 0;
    Layout.Alignment textAlign = Layout.Alignment.ALIGN_CENTER;
    SegmentData(String text, int index) {
        this.text = text;
        this.fontSize = 60;
        if (index == 0) {
            this.textAlign = Layout.Alignment.ALIGN_CENTER;
            this.offsetXProgress = 0;
        } else {
            this.textAlign = Layout.Alignment.ALIGN_NORMAL;
            this.offsetXProgress = 0;
        }
    }
}

public class MainActivity extends AppCompatActivity {
    private MaterialButton btnSave;
    private EditText etInput;
    private androidx.viewpager2.widget.ViewPager2 previewPager;
    private PreviewPagerAdapter previewPagerAdapter;
    // private Spinner spinnerFont, spinnerBg, spinnerAlign; // 已废弃
    private String[] fontNames = {"平方韶华体", "平方洒脱体", "平方上上谦体"};
    private String[] fontFiles = {"fonts/平方韶华体.ttf", "fonts/平方洒脱体.ttf", "fonts/平方上上谦体.ttf"};
    private String[] bgNames = {"月亮1","叶子", "月亮2"};
    private int[] bgResIds = { R.drawable.leaf,R.drawable.moon1, R.drawable.moon2};
    private int selectedFont = 0, selectedBg = 0;
    private Slider seekBarSize;
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
    private Slider seekBarOffsetX;
    private TextView tvOffsetXLabel;
    private int offsetXProgress = 0; // SeekBar的进度
    private int maxOffsetX = 800; // 最大偏移像素

    private ImageButton btnToggleExpand;
    private boolean isInputExpanded = false; // 初始为折叠状态
    private String lastInputText = ""; // 记录上次收起时的内容

    private static final int REQUEST_WRITE_PERMISSION = 1001;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable generateRunnable;

    private List<SegmentData> segmentList = new ArrayList<>();
    private int currentSegmentIndex = 0;

    // 用 inputTextWatcher 替换原 etInput.addTextChangedListener
    private final android.text.TextWatcher inputTextWatcher = new android.text.TextWatcher() {
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
    };

    private LruCache<Integer, Bitmap> segmentBitmapCache = new LruCache<>(Math.max(20, segmentList.size()));
    private ExecutorService bitmapExecutor = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Set<Integer> generatingPositions = new HashSet<>();

    private MaterialButton btnGenerate;
    private MaterialButton btnSaveAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // 强制浅色模式
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        btnSave = findViewById(R.id.btnSave);
        etInput = findViewById(R.id.etInput);
        previewPager = findViewById(R.id.previewPager);
        seekBarSize = findViewById(R.id.seekBarSize);
        tvSizeLabel = findViewById(R.id.tvSizeLabel);
        groupFont = findViewById(R.id.groupFont);
        groupBg = findViewById(R.id.groupBg);
        groupAlign = findViewById(R.id.groupAlign);
        seekBarOffsetX = findViewById(R.id.seekBarOffsetX);
        tvOffsetXLabel = findViewById(R.id.tvOffsetXLabel);
        seekBarOffsetX.setValueFrom(-maxOffsetX);
        seekBarOffsetX.setValueTo(maxOffsetX);
        seekBarOffsetX.setValue(0);
        offsetXProgress = 0;
        tvOffsetXLabel.setText("水平偏移：" + offsetXProgress);

        btnToggleExpand = findViewById(R.id.btnToggleExpand);
        btnToggleExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInputExpanded = !isInputExpanded;
                if (isInputExpanded) {
                    etInput.setMaxLines(Integer.MAX_VALUE);
                    etInput.setMinLines(10);
                    btnToggleExpand.setImageResource(R.drawable.collapse_content_24dp); // 展开时显示收起图标
                    btnToggleExpand.animate().rotation(180f).setDuration(200).start();
                    lastInputText = etInput.getText().toString();
                    etInput.removeTextChangedListener(inputTextWatcher);
                } else {
                    etInput.setMaxLines(1);
                    etInput.setMinLines(1);
                    btnToggleExpand.setImageResource(R.drawable.expand_content_24dp); // 折叠时显示展开图标
                    btnToggleExpand.animate().rotation(0f).setDuration(200).start();
                    if (!lastInputText.equals(etInput.getText().toString())) {
                        inputTextWatcher.onTextChanged(etInput.getText(), 0, 0, etInput.getText().length());
                    }
                    etInput.addTextChangedListener(inputTextWatcher);
                }
            }
        });

        // 保存按钮
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (segmentBitmapCache.size() == 0) {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "请先生成图片", Snackbar.LENGTH_SHORT);
                    View snackbarView = snackbar.getView();
                    snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                    TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                    textView.setCompoundDrawablePadding(24);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    params.topMargin = 80;
                    snackbarView.setLayoutParams(params);
                    snackbar.show();
                    snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                    return;
                }
                showSavingDialogAndSave();
            }
        });

        // 水平偏移
        seekBarOffsetX.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                if (!segmentList.isEmpty()) {
                    SegmentData seg = segmentList.get(currentSegmentIndex);
                    seg.offsetXProgress = (int) value;
                    tvOffsetXLabel.setText("水平偏移：" + seg.offsetXProgress);
                    if (generateRunnable != null) handler.removeCallbacks(generateRunnable);
                    generateRunnable = new Runnable() {
                        @Override
                        public void run() {
                            autoGenerateAllSegmentImages();
                        }
                    };
                    handler.postDelayed(generateRunnable, 100);
                }
            }
        });

        // 默认字体选中平方洒脱体
        groupFont.check(R.id.btnFont2);
        groupBg.check(R.id.btnBg1);
        groupAlign.check(R.id.btnAlignCenter);
        selectedFont = 1;
        selectedBg = 0;
        textAlign = Layout.Alignment.ALIGN_CENTER;

        // SeekBar设置范围 30~300
        seekBarSize.setValueFrom(30);
        seekBarSize.setValueTo(100);
        seekBarSize.setValue(60);
        fontSize = 60;
        tvSizeLabel.setText("字号：" + fontSize);

        seekBarSize.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                if (!segmentList.isEmpty()) {
                    SegmentData seg = segmentList.get(currentSegmentIndex);
                    seg.fontSize = (int) value;
                    tvSizeLabel.setText("字号：" + seg.fontSize);
                    if (generateRunnable != null) handler.removeCallbacks(generateRunnable);
                    generateRunnable = new Runnable() {
                        @Override
                        public void run() {
                            autoGenerateAllSegmentImages();
                        }
                    };
                    handler.postDelayed(generateRunnable, 100);
                }
            }
        });

        groupFont.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked || segmentList.isEmpty()) return;
                SegmentData seg = segmentList.get(currentSegmentIndex);
                if (checkedId == findViewById(R.id.btnFont1).getId()) seg.selectedFont = 0;
                else if (checkedId == findViewById(R.id.btnFont2).getId()) seg.selectedFont = 1;
                else if (checkedId == findViewById(R.id.btnFont3).getId()) seg.selectedFont = 2;
                autoGenerateAllSegmentImages();
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
                autoGenerateAllSegmentImages();
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
                autoGenerateAllSegmentImages();
            }
        });
        // 初始化时添加监听
        etInput.addTextChangedListener(inputTextWatcher);

        // 适配器类
        previewPagerAdapter = new PreviewPagerAdapter();
        previewPager.setAdapter(previewPagerAdapter);

        // 首次自动生成
        autoGenerateAllSegmentImages();

        // 设置手势检测
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // 双指缩放时旋转文字
                float rotation = detector.getScaleFactor() > 1 ? 5f : -5f;
                textRotation += rotation;
                autoGenerateAllSegmentImages();
                return true;
            }
        });

        // ViewPager2 滑动监听同步 currentSegmentIndex 和参数区
        previewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < segmentList.size()) {
                    currentSegmentIndex = position;
                    updateSegmentUI();
                }
            }
        });

        btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoGenerateAllSegmentImages();
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "已生成图片", Snackbar.LENGTH_SHORT);
                View snackbarView = snackbar.getView();
                snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                textView.setCompoundDrawablePadding(24);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.TOP;
                params.topMargin = 80;
                snackbarView.setLayoutParams(params);
                snackbar.show();
                snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
            }
        });

        btnSaveAll = findViewById(R.id.btnSaveAll);
        btnSaveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (segmentList.isEmpty() || segmentBitmapCache.size() == 0) {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "没有可保存的图片", Snackbar.LENGTH_SHORT);
                    View snackbarView = snackbar.getView();
                    snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                    TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                    textView.setCompoundDrawablePadding(24);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    params.topMargin = 80;
                    snackbarView.setLayoutParams(params);
                    snackbar.show();
                    snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                    return;
                }
                showSavingAllDialogAndSaveAll();
            }
        });

        // 设置切换动画
        previewPager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
                float scale = 0.9f + (1 - Math.abs(position)) * 0.1f;
                page.setScaleX(scale);
                page.setScaleY(scale);
            }
        });
        // 设置预加载页面数
        previewPager.setOffscreenPageLimit(2);
    }

    // 渲染当前段落图片
    private void autoGenerateImageForCurrentSegment() {
        if (segmentList.isEmpty()) {
            generatedBitmap = null;
            return;
        }
        SegmentData seg = segmentList.get(currentSegmentIndex);
        if (TextUtils.isEmpty(seg.text)) {
            generatedBitmap = null;
            return;
        }
        generatedBitmap = generateImage(seg.text, seg.selectedFont, seg.selectedBg, seg.fontSize, seg.textOffsetX, seg.textOffsetY, seg.textRotation, seg.offsetXProgress, seg.textAlign);
    }

    // 修改 generateImage 方法参数
    private Bitmap generateImage(String text, int selectedFont, int selectedBg, int fontSize, float textOffsetX, float textOffsetY, float textRotation, int offsetXProgress, Layout.Alignment textAlign) {
        // 1. 加载背景图片
        Bitmap bg = BitmapFactory.decodeResource(getResources(), bgResIds[selectedBg]);

        // 设置固定的1:1画布大小
        int canvasSize = 1000; 
        Bitmap result = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // 裁剪并居中绘制背景图到1:1画布上
        Rect srcRect = new Rect(0, 0, bg.getWidth(), bg.getHeight());
        Rect dstRect = new Rect(0, 0, canvasSize, canvasSize);

        float scaleX = (float) dstRect.width() / srcRect.width();
        float scaleY = (float) dstRect.height() / srcRect.height();
        float scale = Math.max(scaleX, scaleY); // 缩放以填充目标

        float scaledWidth = srcRect.width() * scale;
        float scaledHeight = srcRect.height() * scale;

        float left = (dstRect.width() - scaledWidth) / 2;
        float top = (dstRect.height() - scaledHeight) / 2;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(left, top);
        canvas.drawBitmap(bg, matrix, new Paint()); // 使用 Paint 对象

        // 2. 加载字体
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.createFromAsset(getAssets(), fontFiles[selectedFont]));
        textPaint.setTextSize(fontSize);
        textPaint.setColor(0xFFFFFFFF); // 白色
        textPaint.setShadowLayer(8, 4, 4, 0x80000000);
        textPaint.setTextSkewX(-0.2f); // 微微倾斜

        // 3. 多行自动换行绘制（支持旋转、偏移和自定义行间距）
        String processedText = text;
        int minPadding = 64; // 控制换行的最小边距
        int visualPadding = (textAlign == Layout.Alignment.ALIGN_CENTER) ? minPadding : 120; // 视觉上左右对齐的边距更大
        int textBlockWidth = canvasSize - 2 * minPadding; // 换行宽度始终较大
        StaticLayout staticLayout = StaticLayout.Builder.obtain(processedText, 0, processedText.length(), textPaint, textBlockWidth)
                .setAlignment(textAlign)
                .setLineSpacing(0f, 2.5f)
                .setIncludePad(true)
                .build();

        canvas.save();
        float layoutX;
        if (textAlign == Layout.Alignment.ALIGN_CENTER) {
            layoutX = (canvasSize - textBlockWidth) / 2f + textOffsetX + offsetXProgress;
        } else if (textAlign == Layout.Alignment.ALIGN_NORMAL) { // 左对齐
            layoutX = visualPadding + textOffsetX + offsetXProgress;
        } else if (textAlign == Layout.Alignment.ALIGN_OPPOSITE) { // 右对齐
            layoutX = canvasSize - visualPadding - textBlockWidth + textOffsetX + offsetXProgress;
        } else {
            layoutX = (canvasSize - textBlockWidth) / 2f + textOffsetX + offsetXProgress;
        }
        float layoutY = (canvasSize - staticLayout.getHeight()) / 2f + textOffsetY; // 使用 canvasSize
        float rotationPivotX = layoutX + textBlockWidth / 2f;
        float rotationPivotY = layoutY + staticLayout.getHeight() / 2f;
        canvas.rotate(textRotation, rotationPivotX, rotationPivotY);
        canvas.translate(layoutX, layoutY);
        staticLayout.draw(canvas);
        canvas.restore();
        return result;
    }

    // 保存图片到相册（指定文件名，始终保存到 DCIM/mindPic）
    private boolean saveBitmapToGalleryWithName(Bitmap bitmap, String fileName) {
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
                    saveBitmapToGalleryWithName(generatedBitmap, "mindpic_single.png");
                }
            } else {
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "未获得存储权限，无法保存图片", Snackbar.LENGTH_SHORT);
                View snackbarView = snackbar.getView();
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.colorPrimary));
                TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                textView.setCompoundDrawablePadding(24);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.TOP;
                params.topMargin = 80;
                snackbarView.setLayoutParams(params);
                snackbar.show();
                snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
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
                final boolean success = saveBitmapToGalleryWithName(generatedBitmap, "mindpic_single.png");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (success) {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "图片已保存到相册", Snackbar.LENGTH_SHORT);
                            View snackbarView = snackbar.getView();
                            snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                            textView.setCompoundDrawablePadding(24);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                            params.gravity = Gravity.TOP;
                            params.topMargin = 80;
                            snackbarView.setLayoutParams(params);
                            snackbar.show();
                            snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                        } else {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "保存失败", Snackbar.LENGTH_SHORT);
                            View snackbarView = snackbar.getView();
                            snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                            textView.setCompoundDrawablePadding(24);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                            params.gravity = Gravity.TOP;
                            params.topMargin = 80;
                            snackbarView.setLayoutParams(params);
                            snackbar.show();
                            snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                        }
                    }
                });
            }
        }).start();
    }

    private void updateSegmentUI() {
        autoGenerateImageForCurrentSegment();
        if (currentSegmentIndex < segmentBitmapCache.size()) {
            previewPager.setCurrentItem(currentSegmentIndex, false);
        }
    }

    // 生成所有段落图片
    private void autoGenerateAllSegmentImages() {
        segmentBitmapCache.evictAll();
        if (segmentList.isEmpty()) {
            segmentBitmapCache.put(0, BitmapFactory.decodeResource(getResources(), R.drawable.leaf));
            previewPagerAdapter.notifyDataSetChanged();
        } else {
            for (int i = 0; i < segmentList.size(); i++) {
                final int idx = i;
                bitmapExecutor.execute(() -> {
                    SegmentData seg = segmentList.get(idx);
                    Bitmap bmp;
                    if (TextUtils.isEmpty(seg.text)) {
                        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.leaf);
                    } else {
                        bmp = generateImage(seg.text, seg.selectedFont, seg.selectedBg, seg.fontSize, seg.textOffsetX, seg.textOffsetY, seg.textRotation, seg.offsetXProgress, seg.textAlign);
                    }
                    segmentBitmapCache.put(idx, bmp);
                    mainHandler.post(() -> previewPagerAdapter.notifyItemChanged(idx));
                });
            }
            previewPagerAdapter.notifyDataSetChanged();
        }
    }

    // 适配器类
    private class PreviewPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<PreviewPagerAdapter.PreviewViewHolder> {
        @Override
        public PreviewViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new PreviewViewHolder(imageView);
        }
        @Override
        public void onBindViewHolder(PreviewViewHolder holder, int position) {
            Bitmap bmp = segmentBitmapCache.get(position);
            if (bmp != null) {
                holder.imageView.setImageBitmap(bmp);
            } else {
                holder.imageView.setImageResource(R.drawable.leaf);
            }
        }
        @Override
        public int getItemCount() {
            return Math.max(segmentList.size(), 1); // 至少返回1，保证有占位图
        }
        class PreviewViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView imageView;
            PreviewViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }

    /**
     * 保存全部图片，带进度对话框
     */
    private void showSavingAllDialogAndSaveAll() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("正在保存全部图片");
        dialog.setMax(segmentList.size());
        dialog.setCancelable(false);
        dialog.setProgress(0);
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] successCount = {0};
                for (int i = 0; i < segmentList.size(); i++) {
                    Bitmap bmp = segmentBitmapCache.get(i);
                    if (bmp != null) {
                        if (saveBitmapToGalleryWithName(bmp, "mindpic_segment_" + (i+1) + ".png")) {
                            successCount[0]++;
                        }
                    }
                    final int progress = i + 1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("正在保存第" + progress + "/" + segmentList.size() + "张...");
                            dialog.setProgress(progress);
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (successCount[0] > 0) {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "已保存全部图片到相册 (DCIM/mindPic)", Snackbar.LENGTH_SHORT);
                            View snackbarView = snackbar.getView();
                            snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                            textView.setCompoundDrawablePadding(24);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                            params.gravity = Gravity.TOP;
                            params.topMargin = 80;
                            snackbarView.setLayoutParams(params);
                            snackbar.show();
                            snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                        } else {
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "保存失败或无图片", Snackbar.LENGTH_SHORT);
                            View snackbarView = snackbar.getView();
                            snackbar.setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download_done_24dp, 0, 0, 0);
                            textView.setCompoundDrawablePadding(24);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                            params.gravity = Gravity.TOP;
                            params.topMargin = 80;
                            snackbarView.setLayoutParams(params);
                            snackbar.show();
                            snackbarView.postDelayed(() -> snackbar.dismiss(), 1000);
                        }
                    }
                });
            }
        }).start();
    }
} 