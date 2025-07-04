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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.material.snackbar.Snackbar;

import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;
import android.util.LruCache;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import android.content.ClipboardManager;
import android.content.ClipData;

// 新建 SegmentData 类
class SegmentData {
    String text;
    int selectedFont = 1; // 默认洒脱体
    int selectedBg = 0;
    int fontSize = 60;
    float textOffsetX = 0f, textOffsetY = 0f, textRotation = 0f;
    int offsetXProgress = 0;
    Layout.Alignment textAlign = Layout.Alignment.ALIGN_CENTER;
    SegmentData(String text, int index) {
        this.text = text;
        this.fontSize = 60;
        this.selectedFont = 1; // 始终为洒脱体
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
    private String[] fontFiles = {"fonts/平方韶华体.ttf", "fonts/平方洒脱体.ttf", "fonts/平方上上谦体.ttf"};
    private String[] bgNames = {"月亮1","叶子", "月亮2"};
    private int[] bgResIds = { R.drawable.leaf,R.drawable.moon1, R.drawable.moon2};
    private int selectedBg = 0;
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
    private MaterialButtonToggleGroup groupBg, groupAlign;
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
        private boolean hasPasted = false;
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 检查是否有"----"分割符
            String fullText = s.toString();
            String lowerText = fullText;
            int splitIndex = lowerText.indexOf("----");
            String renderText = fullText;
            if (splitIndex != -1) {
                // 存在"----"，只渲染下方文本
                renderText = fullText.substring(splitIndex + 4);
            }
            // 以两个等号"=="作为分段符，分割后去除每个段落前后的换行符和空白
            String[] segments = renderText.split("==");
            segmentList.clear();
            for (int i = 0; i < segments.length; i++) {
                String seg = segments[i].replaceAll("^\\s+|\\s+$", ""); // 去除前后空白和换行
                segmentList.add(new SegmentData(seg, i));
            }
            currentSegmentIndex = 0;
            updateSegmentUI();
            // 检测第一次粘贴（文本由空变为非空）
            if (!hasPasted && before == 0 && s.length() > 0 && count > 1) {
                hasPasted = true;
                Log.d("MindPic", "首次粘贴文本，自动触发图片渲染");
                autoGenerateAllSegmentImages();
            }
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
    };

    // 提高缓存容量，避免滑动时频繁回收
    private LruCache<Integer, Bitmap> segmentBitmapCache = new LruCache<>(Math.max(50, segmentList.size()));
    // 优化线程池配置
    private final ExecutorService bitmapExecutor = new ThreadPoolExecutor(
        2, // 核心线程数
        4, // 最大线程数
        60L, // 空闲线程存活时间
        TimeUnit.SECONDS, // 时间单位
        new LinkedBlockingQueue<>(8), // 使用有界队列
        new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时，在调用者线程中执行
    );
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Set<Integer> generatingPositions = new HashSet<>();

    private MaterialButton btnGenerate;
    private MaterialButton btnSaveAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        btnSave = findViewById(R.id.btnSave);
        etInput = findViewById(R.id.etInput);
        previewPager = findViewById(R.id.previewPager);
        seekBarSize = findViewById(R.id.seekBarSize);
        tvSizeLabel = findViewById(R.id.tvSizeLabel);
        groupBg = findViewById(R.id.groupBg);
        groupAlign = findViewById(R.id.groupAlign);
        seekBarOffsetX = findViewById(R.id.seekBarOffsetX);
        tvOffsetXLabel = findViewById(R.id.tvOffsetXLabel);

        // 初始化默认值
        seekBarOffsetX.setValueFrom(-maxOffsetX);
        seekBarOffsetX.setValueTo(maxOffsetX);
        seekBarOffsetX.setValue(0);
        offsetXProgress = 0;
        tvOffsetXLabel.setText("水平偏移：" + offsetXProgress);

        // 初始化图片生成相关
        segmentList = new ArrayList<>();
        segmentBitmapCache = new LruCache<>(10);
        currentSegmentIndex = 0;

        // 添加默认的空段落
        SegmentData defaultSegment = new SegmentData("", 1);
        defaultSegment.selectedBg = 0;
        defaultSegment.fontSize = 60;
        defaultSegment.textAlign = Layout.Alignment.ALIGN_CENTER;
        segmentList.add(defaultSegment);

        // 初始化适配器
        previewPagerAdapter = new PreviewPagerAdapter();
        previewPager.setAdapter(previewPagerAdapter);

        // 初始化切换按钮
        ImageButton btnPrevImage = findViewById(R.id.btnPrevImage);
        ImageButton btnNextImage = findViewById(R.id.btnNextImage);
        
        btnPrevImage.setOnClickListener(v -> {
            if (currentSegmentIndex > 0) {
                previewPager.setCurrentItem(currentSegmentIndex - 1, true);
                // 异步触发下一张图片的渲染
                if (currentSegmentIndex > 1) {
                    generateImageAsync(currentSegmentIndex - 2);
                }
            }
        });
        
        btnNextImage.setOnClickListener(v -> {
            if (currentSegmentIndex < segmentList.size() - 1) {
                previewPager.setCurrentItem(currentSegmentIndex + 1, true);
                // 异步触发下一张图片的渲染
                if (currentSegmentIndex < segmentList.size() - 2) {
                    generateImageAsync(currentSegmentIndex + 2);
                }
            }
        });

        // 禁用 ViewPager2 的滑动
        previewPager.setUserInputEnabled(false);
        
        // 设置自定义页面切换动画
        previewPager.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            
            // 缩放效果
            float scale = 0.85f + (1 - absPosition) * 0.15f;
            page.setScaleX(scale);
            page.setScaleY(scale);
            
            // 透明度效果
            page.setAlpha(1.0f - (absPosition * 0.5f));
            
            // 3D旋转效果
            float rotation = position * 30f; // 30度旋转
            page.setRotationY(rotation);
        });

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
        groupBg.check(R.id.btnBg1);
        groupAlign.check(R.id.btnAlignCenter);
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

        // 首次生成默认图片
        mainHandler.postDelayed(() -> autoGenerateAllSegmentImages(), 500);

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
        previewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < segmentList.size()) {
                    currentSegmentIndex = position;
                    updateSegmentUI();
                    // 当切换到新页面时，预加载下一张图片
                    if (position < segmentList.size() - 1) {
                        generateImageAsync(position + 1);
                    }
                    if (position > 0) {
                        generateImageAsync(position - 1);
                    }
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

        // 设置切换动画为丝滑缩放，无透明度遮罩
        previewPager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float scale = 0.92f + (1 - Math.abs(position)) * 0.08f;
                page.setScaleX(scale);
                page.setScaleY(scale);
                page.setAlpha(1f); // 保证无白色遮罩
            }
        });
        // 设置预加载页面数为2，避免过多预加载导致的性能问题
        previewPager.setOffscreenPageLimit(2);

        // 优化 ViewPager2 的滑动性能
        previewPager.post(() -> {
            View child = previewPager.getChildAt(0);
            if (child instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) child;
                recyclerView.setItemAnimator(null); // 禁用动画提升性能
                recyclerView.setHasFixedSize(true); // 固定大小提升性能
                // 设置更大的缓存来减少创建和绑定 ViewHolder 的次数
                recyclerView.setItemViewCacheSize(4);
                // 禁用预测动画提升性能
                recyclerView.getLayoutManager().setItemPrefetchEnabled(false);
            }
        });

        MaterialButton btnSaveAndShare = findViewById(R.id.btnSaveAndShare);
        btnSaveAndShare.setOnClickListener(v -> {
            // 复制上方文本到剪贴板（如有"----"分割），否则复制全部
            String fullText = etInput.getText().toString();
            int splitIndex = fullText.indexOf("----");
            String copyText;
            if (splitIndex != -1) {
                copyText = fullText.substring(0, splitIndex).replaceAll("^\\s+|\\s+$", "");
            } else {
                copyText = fullText;
            }
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("mindpic_text", copyText);
            clipboard.setPrimaryClip(clip);
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "已复制到剪贴板", Snackbar.LENGTH_SHORT);
            snackbar.show();
            // 执行原有分享逻辑
            saveAndShareToXiaoHongShu();
        });

        // 优化 ViewPager2 配置
        previewPager.setOffscreenPageLimit(2); // 预加载前后各2页
        ViewPager2.PageTransformer transformer = (page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - (absPos * 0.5f)); // 渐变效果
            page.setTranslationX(-position * page.getWidth() * 0.1f); // 轻微位移
        };
        previewPager.setPageTransformer(transformer);

        // 禁用过度滚动效果
        try {
            Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
            recyclerViewField.setAccessible(true);
            RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(previewPager);
            recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        } catch (Exception e) {
            Log.e("MindPic", "Error configuring ViewPager2", e);
        }
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
        generatedBitmap = generatePreviewImage(seg.text, seg.selectedFont, seg.selectedBg, seg.fontSize, seg.textOffsetX, seg.textOffsetY, seg.textRotation, seg.offsetXProgress, seg.textAlign);
    }

    // 生成预览用图片（优化尺寸）
    private Bitmap generatePreviewImage(String text, int selectedFont, int selectedBg, int fontSize, float textOffsetX, float textOffsetY, float textRotation, int offsetXProgress, Layout.Alignment textAlign) {
        try {
            // 预览时使用较小的尺寸以提升性能
            int previewSize = 800;
            return generateImageWithSize(text, selectedFont, selectedBg, fontSize, textOffsetX, textOffsetY, textRotation, offsetXProgress, textAlign, previewSize);
        } catch (Exception e) {
            Log.e("MindPic", "Error in generatePreviewImage: " + e.getMessage());
            e.printStackTrace();
            throw e; // 向上传递异常以便更好地处理
        }
    }

    // 生成导出用大尺寸图片
    private Bitmap generateExportImage(String text, int selectedFont, int selectedBg, int fontSize, float textOffsetX, float textOffsetY, float textRotation, int offsetXProgress, Layout.Alignment textAlign) {
        // 导出时使用更高的分辨率
        int exportSize = 1200;
        return generateImageWithSize(text, selectedFont, selectedBg, fontSize, textOffsetX, textOffsetY, textRotation, offsetXProgress, textAlign, exportSize);
    }

    // 通用尺寸图片生成（优化性能）
    private Bitmap generateImageWithSize(String text, int selectedFont, int selectedBg, int fontSize, float textOffsetX, float textOffsetY, float textRotation, int offsetXProgress, Layout.Alignment textAlign, int canvasSize) {
        if (text == null) {
            text = "";
        }

        // 创建位图
        Bitmap result = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        try {
            // 绘制背景
            Bitmap bg = BitmapFactory.decodeResource(getResources(), bgResIds[selectedBg]);
            if (bg != null) {
                Rect srcRect = new Rect(0, 0, bg.getWidth(), bg.getHeight());
                Rect dstRect = new Rect(0, 0, canvasSize, canvasSize);
                canvas.drawBitmap(bg, srcRect, dstRect, null);
            }

            // 优化文字渲染
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            try {
                Typeface typeface = Typeface.createFromAsset(getAssets(), fontFiles[selectedFont]);
                textPaint.setTypeface(typeface);
            } catch (Exception e) {
                Log.e("MindPic", "Error loading typeface: " + e.getMessage());
                textPaint.setTypeface(Typeface.DEFAULT);
            }
            
            textPaint.setTextSize(fontSize * canvasSize / 1000f);
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setShadowLayer(4, 2, 2, 0x80000000);

            // 文字布局优化
            int minPadding = (int)(64 * canvasSize / 1000f);
            int textBlockWidth = canvasSize - 2 * minPadding;

            // 使用 StaticLayout.Builder 优化文字排版
            StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, textBlockWidth)
                    .setAlignment(textAlign)
                    .setLineSpacing(0f, 2.0f) // 增加行距
                    .setIncludePad(false)
                    .build();

            canvas.save();
            
            // 计算文字位置
            float layoutX = (canvasSize - textBlockWidth) / 2f + offsetXProgress * canvasSize / 1000f;
            float layoutY = (canvasSize - staticLayout.getHeight()) / 2f;
            
            // 应用变换
            canvas.translate(layoutX, layoutY);
            
            // 绘制文字
            staticLayout.draw(canvas);
            
            canvas.restore();
            
            return result;
        } catch (Exception e) {
            Log.e("MindPic", "Error in generateImageWithSize: " + e.getMessage());
            e.printStackTrace();
            // 如果生成失败，返回空白图片
            return result;
        }
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
                // 使用当前选中段落的参数重新生成导出图片
                SegmentData seg = segmentList.get(currentSegmentIndex);
                final Bitmap exportBitmap = generateExportImage(
                    seg.text,
                    seg.selectedFont,
                    seg.selectedBg,
                    seg.fontSize,
                    seg.textOffsetX,
                    seg.textOffsetY,
                    seg.textRotation,
                    seg.offsetXProgress,
                    seg.textAlign
                );
                
                final boolean success = saveBitmapToGalleryWithName(exportBitmap, "mindpic_single.png");
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
                        // 释放导出图片的内存
                        if (exportBitmap != null && !exportBitmap.isRecycled()) {
                            exportBitmap.recycle();
                        }
                    }
                });
            }
        }).start();
    }

    private void updateSegmentUI() {
        if (currentSegmentIndex >= 0 && currentSegmentIndex < segmentList.size()) {
            SegmentData seg = segmentList.get(currentSegmentIndex);
            // 更新UI控件状态
            seekBarSize.setValue(seg.fontSize);
            tvSizeLabel.setText("字号：" + seg.fontSize);
            seekBarOffsetX.setValue(seg.offsetXProgress);
            tvOffsetXLabel.setText("水平偏移：" + seg.offsetXProgress);
            
            // 更新背景选择
            if (seg.selectedBg == 0) groupBg.check(R.id.btnBg1);
            else if (seg.selectedBg == 1) groupBg.check(R.id.btnBg2);
            else if (seg.selectedBg == 2) groupBg.check(R.id.btnBg3);
            
            // 更新对齐方式
            if (seg.textAlign == Layout.Alignment.ALIGN_NORMAL) groupAlign.check(R.id.btnAlignLeft);
            else if (seg.textAlign == Layout.Alignment.ALIGN_CENTER) groupAlign.check(R.id.btnAlignCenter);
            else if (seg.textAlign == Layout.Alignment.ALIGN_OPPOSITE) groupAlign.check(R.id.btnAlignRight);
            
            // 如果当前图片还没有生成，触发生成
            if (segmentBitmapCache.get(currentSegmentIndex) == null) {
                generateImageAsync(currentSegmentIndex);
            }
        }
    }

    // 优化图片生成策略
    private void autoGenerateAllSegmentImages() {
        if (segmentList.isEmpty()) {
            // 如果列表为空，添加一个默认段落
            SegmentData defaultSegment = new SegmentData("", 1);
            defaultSegment.selectedBg = 0;
            defaultSegment.fontSize = 60;
            defaultSegment.textAlign = Layout.Alignment.ALIGN_CENTER;
            segmentList.add(defaultSegment);
            currentSegmentIndex = 0;
        }

        // 计算需要生成的图片范围
        int startIndex = Math.max(0, currentSegmentIndex - 2);
        int endIndex = Math.min(segmentList.size() - 1, currentSegmentIndex + 2);
        int totalToGenerate = endIndex - startIndex + 1;

        // 创建原子计数器跟踪完成数量
        AtomicInteger completedCount = new AtomicInteger(0);

        // 清除旧的缓存
        segmentBitmapCache.evictAll();

        // 一次性提交所有任务
        for (int i = startIndex; i <= endIndex; i++) {
            final int position = i;
            
            // 如果已经在生成中，跳过
            if (generatingPositions.contains(position)) {
                completedCount.incrementAndGet();
                continue;
            }

            // 标记该位置正在生成中
            generatingPositions.add(position);

            bitmapExecutor.execute(() -> {
                try {
                    SegmentData seg = segmentList.get(position);
                    Bitmap bmp = null;

                    if (TextUtils.isEmpty(seg.text)) {
                        try {
                            // 在主线程中加载资源
                            CountDownLatch latch = new CountDownLatch(1);
                            final AtomicReference<Bitmap> bitmapRef = new AtomicReference<>();
                            
                            mainHandler.post(() -> {
                                try {
                                    Bitmap resourceBmp = BitmapFactory.decodeResource(getResources(), bgResIds[seg.selectedBg]);
                                    if (resourceBmp == null) {
                                        resourceBmp = BitmapFactory.decodeResource(getResources(), R.drawable.leaf);
                                    }
                                    bitmapRef.set(resourceBmp);
                                } finally {
                                    latch.countDown();
                                }
                            });
                            
                            // 等待主线程加载完成
                            latch.await(2, TimeUnit.SECONDS);
                            bmp = bitmapRef.get();
                        } catch (Exception e) {
                            Log.e("MindPic", "Error loading default image: " + e.getMessage());
                        }
                    } else {
                        try {
                            bmp = generatePreviewImage(
                                seg.text,
                                seg.selectedFont,
                                seg.selectedBg,
                                seg.fontSize,
                                seg.textOffsetX,
                                seg.textOffsetY,
                                seg.textRotation,
                                seg.offsetXProgress,
                                seg.textAlign
                            );
                        } catch (Exception e) {
                            Log.e("MindPic", "Error in generatePreviewImage: " + e.getMessage());
                            
                            // 在主线程中加载默认背景
                            CountDownLatch latch = new CountDownLatch(1);
                            final AtomicReference<Bitmap> bitmapRef = new AtomicReference<>();
                            
                            mainHandler.post(() -> {
                                try {
                                    Bitmap resourceBmp = BitmapFactory.decodeResource(getResources(), bgResIds[seg.selectedBg]);
                                    bitmapRef.set(resourceBmp);
                                } finally {
                                    latch.countDown();
                                }
                            });
                            
                            // 等待主线程加载完成
                            latch.await(2, TimeUnit.SECONDS);
                            bmp = bitmapRef.get();
                        }
                    }

                    // 确保bitmap不为null
                    if (bmp != null) {
                        final Bitmap finalBmp = bmp;
                        mainHandler.post(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                segmentBitmapCache.put(position, finalBmp);
                                // 只在必要时更新UI
                                if (position == currentSegmentIndex || 
                                    position == currentSegmentIndex - 1 || 
                                    position == currentSegmentIndex + 1) {
                                    previewPagerAdapter.notifyItemChanged(position);
                                }
                            }
                        });
                    }

                    // 增加完成计数
                    completedCount.incrementAndGet();
                } catch (Exception e) {
                    Log.e("MindPic", "Error generating image at position " + position, e);
                    completedCount.incrementAndGet();
                } finally {
                    // 移除生成中的标记
                    generatingPositions.remove(position);
                }
            });
        }
    }

    // 修改 generateImageAsync 方法，使其专门用于单张图片的生成
    private void generateImageAsync(int position) {
        if (position < 0 || position >= segmentList.size() || 
            generatingPositions.contains(position) || 
            segmentBitmapCache.get(position) != null) {
            return;
        }

        // 标记该位置正在生成中
        generatingPositions.add(position);

        bitmapExecutor.execute(() -> {
            try {
                SegmentData seg = segmentList.get(position);
                Bitmap bmp = null;

                if (TextUtils.isEmpty(seg.text)) {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.leaf);
                } else {
                    try {
                        bmp = generatePreviewImage(
                            seg.text,
                            seg.selectedFont,
                            seg.selectedBg,
                            seg.fontSize,
                            seg.textOffsetX,
                            seg.textOffsetY,
                            seg.textRotation,
                            seg.offsetXProgress,
                            seg.textAlign
                        );
                    } catch (Exception e) {
                        Log.e("MindPic", "Error in generatePreviewImage: " + e.getMessage());
                        e.printStackTrace();
                        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.leaf);
                    }
                }

                if (bmp != null) {
                    segmentBitmapCache.put(position, bmp);
                    
                    if (position == currentSegmentIndex) {
                        mainHandler.post(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                previewPagerAdapter.notifyItemChanged(position);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("MindPic", "Error generating image at position " + position, e);
                if (position == currentSegmentIndex) {
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(MainActivity.this, 
                                "图片生成出错，请重试", 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } finally {
                generatingPositions.remove(position);
            }
        });
    }

    // 适配器类
    private class PreviewPagerAdapter extends RecyclerView.Adapter<PreviewPagerAdapter.PreviewViewHolder> {
        class PreviewViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            PreviewViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }

        @NonNull
        @Override
        public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new PreviewViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
            Bitmap bitmap = segmentBitmapCache.get(position);
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
            } else {
                // 如果没有缓存的图片，使用默认背景
                holder.imageView.setImageResource(R.drawable.leaf);
                // 触发异步生成
                generateImageAsync(position);
            }
        }

        @Override
        public int getItemCount() {
            return segmentList.size();
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

    // 保存图片并返回Uri（导出用大图）
    private Uri saveBitmapAndGetUri(Bitmap bitmap, String fileName) {
        OutputStream fos = null;
        Uri uri = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/mindPic");
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, fileName, "mindPic生成");
                uri = Uri.parse(path);
                fos = null;
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }
            return uri;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存并分享到小红书（导出用大图）
    private void saveAndShareToXiaoHongShu() {
        if (segmentList.isEmpty() || segmentBitmapCache.size() == 0) {
            Snackbar.make(findViewById(android.R.id.content), "请先生成图片", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            ArrayList<Uri> uriList = new ArrayList<>();
            for (int i = 0; i < segmentList.size(); i++) {
                SegmentData seg = segmentList.get(i);
                Bitmap bmp;
                if (TextUtils.isEmpty(seg.text)) {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.leaf);
                } else {
                    bmp = generateExportImage(seg.text, seg.selectedFont, seg.selectedBg, seg.fontSize, seg.textOffsetX, seg.textOffsetY, seg.textRotation, seg.offsetXProgress, seg.textAlign);
                }
                Uri uri = saveBitmapAndGetUri(bmp, "mindpic_share_" + (i+1) + ".png");
                if (uri != null) uriList.add(uri);
            }
            runOnUiThread(() -> {
                if (!uriList.isEmpty()) {
                    shareImagesToXiaoHongShu(uriList);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "保存失败，无法分享", Snackbar.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // 多图分享至小红书
    private void shareImagesToXiaoHongShu(ArrayList<Uri> imageUris) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("image/*");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        shareIntent.setPackage("com.xingin.xhs");
        try {
            startActivity(shareIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "未检测到小红书App", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }
} 