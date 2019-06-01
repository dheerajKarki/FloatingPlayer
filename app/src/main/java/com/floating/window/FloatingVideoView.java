package com.floating.window;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.GestureDetector;
import android.animation.PropertyValuesHolder;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;

/*  视频悬浮窗口View的实现 */

public class FloatingVideoView implements OnTouchListener,OnClickListener,
        ValueAnimator.AnimatorUpdateListener {

    //保存上下文对象
    private Context mContext;

    //状态栏高度
    private int mStatusBarHeight;

    //悬浮窗口最小的宽度和高度
    private int minWidth,minHeight;

    //悬浮窗口最大的宽度和高度
    private int maxWidth,maxHeight;

    //屏幕的宽度和高度
    private int screenWidth,screenHeight;

    //定义悬浮窗口默认的宽度和高度
    private int defaultWidth, defaultHeight;

    // 当前悬浮窗口View的宽度与高度
    private int mViewWidth, mViewHeight;

    //悬浮窗口中要添加的View
    private View mFloatView;

    // 全局 Handler对象
    private Handler mHandler;

    // 定义悬浮窗口上的按钮
    private ImageView fullscreen, close;

    // 悬浮窗口管理器
    private WindowManager mWindowManager;

    // 视频播放控制器
    private VideoPlayerController mController;

    // 保存数据
    private SharedPreferences mSharedPreferences;

    // 显示视频播放的窗口布局
    private FrameLayout mFrameLayout;
    // 显示视频播放的窗口布局参数
    private ViewGroup.LayoutParams lparams;

    // 悬浮窗口布局参数
    private WindowManager.LayoutParams mWindowParams;

    // 手势检测
    private GestureDetector mGestureDetector;

    // 显示进度条
    private ProgressBar mProgressbar;

    // 构造方法传入一个View作为悬浮窗口显示的View
    public FloatingVideoView(Context context, View view){
        mContext = context;
        mFloatView = view;
        mFloatView.setOnTouchListener(this);
        initParams(mFloatView);

    }


    // 构造方法传入一个xml布局文件，作为悬浮窗口显示的View
    public FloatingVideoView(Context context, int layoutId){
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences("default", mContext.MODE_PRIVATE);
        //初始化mFloatView，mFloatView就是悬浮窗口中显示的View，实则就是播放视频Activity的布局
        mFloatView = LayoutInflater.from(context).inflate(layoutId, null);
        mFloatView.setBackgroundColor(0x66ee00ee);

        mProgressbar = mFloatView.findViewById(R.id.video_progressbar);

        // 初始化视频控制器
        TextureView textureView = mFloatView.findViewById(R.id.textureView);
        mController = new VideoPlayerController(textureView, mSharedPreferences.getString("pathname",""));
        mController.setProgressbar(mProgressbar);

        // 初始化悬浮窗口视频播放，显示的一些按钮，比如关闭，全屏，暂停...
        fullscreen =  mFloatView.findViewById(R.id.video_fullscreen);
        close = mFloatView.findViewById(R.id.video_close);

        //按钮添加点击监听
        fullscreen.setOnClickListener(this);
        close.setOnClickListener(this);
        //悬浮窗口添加触摸监听
        mFloatView.setOnTouchListener(this);

        mGestureDetector = new GestureDetector(mContext, new FloatingWindowGesture());

        mHandler = ((MyApplication)mContext.getApplicationContext()).getHandler();
        initParams(mFloatView);

    }


    //初始化一些必要的参数
    private void initParams(View mFloatView){
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParams = new WindowManager.LayoutParams();
        // 获取屏幕的宽高
        screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        screenHeight = mWindowManager.getDefaultDisplay().getHeight();

        mStatusBarHeight = getStatusBarHeight();
        // 悬浮视频播放窗口最小的宽度和高度，宽高比是16:9
        // 512 / 1080 = 0.473 和Android O picture in picture大小相当
        minWidth = (int)(screenWidth * 0.473f);
        minHeight = (int)(screenWidth * 0.473f * 9 / 16);

        // 整个悬浮窗口的默认宽度与高度
        defaultWidth = screenWidth ;
        defaultHeight = screenHeight - mStatusBarHeight;

        //悬浮视频播放窗口最大的宽度和高度
        maxWidth = screenWidth;
        maxHeight = screenWidth * 9 / 16;

//		int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1, View.MeasureSpec.AT_MOST);
//		int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1, View.MeasureSpec.AT_MOST);
//		mFloatView.measure(widthMeasureSpec, heightMeasureSpec);
//
//		int mFloatViewWidth = mFloatView.getMeasuredWidth();
//		int mFloatViewHeight = mFloatView.getMeasuredHeight();


        // Set LayoutParams type for android 8.0
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowParams.format = PixelFormat.RGBA_8888;
        mWindowParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_HARDWARE_ACCELERATED
                | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        //悬浮窗口的宽度与高度
        mWindowParams.width = defaultWidth;
        mWindowParams.height = defaultHeight;

        //悬浮窗口开始的X,Y坐标
        mWindowParams.x = 0;
        mWindowParams.y = 0;

        //初始化mFrameLayout，mFrameLayout就是播放视频的View
        mFrameLayout = mFloatView.findViewById(R.id.floating_video_viewFrameLayout);
        lparams = mFrameLayout.getLayoutParams();
        lparams.width = defaultWidth;
        lparams.height = (int) ((float) lparams.width / 16 * 9);
        mViewWidth = lparams.width;
        mViewHeight = lparams.height;
        mFrameLayout.setLayoutParams(lparams);

        //mFrameLayout.setElevation(100);

    }


    // 显示悬浮窗口
    public void show(){

        mController.startPlay(mSharedPreferences.getInt("position", 0));
        // 计算悬浮窗口缩放的比例，从默认的一个宽度和高度，缩放到最小的宽度和高度
        float xscale = 1.0f * minWidth / defaultWidth ;
        float yscale = 1.0f * minHeight / defaultHeight;
        // 计算X Y移动的距离，屏幕的宽度-悬浮窗口最小的宽度-距离屏幕右侧的间距(mStatusBarHeight / 2)
        int x = screenWidth - minWidth - mStatusBarHeight / 2;
        int y = screenHeight - minHeight - mStatusBarHeight * 3 / 2;

        mWindowManager.addView(mFloatView, mWindowParams);
        // 开始进入小窗口动画
        startAnimation(x, y, xscale, yscale, 1000, new BounceInterpolator());
    }

    // 关闭悬浮窗口
    public void dismiss(){
        mWindowManager.removeView(mFloatView);
    }


    // 获取状态栏高度
    public int getStatusBarHeight(){

//		int mBarHeight = 0;
//		try{
//			Class<?> cls = Class.forName("android.internal.R$dimen");
//			Object obj = cls.newInstance();
//			Field field = cls.getField("status_bar_height");
//			int x = Integer.parseInt(field.get(obj).toString());
//			mBarHeight = mContext.getResources().getDimensionPixelSize(x);
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}

        int mBarHeight = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            mBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);

        return mBarHeight;
    }

    // 判断悬浮窗口超出屏幕的距离，为可以超出悬浮窗口一半的距离
    public void checkEdge(){
        if(mWindowParams.x <= -mWindowParams.width / 2)
            mWindowParams.x = -mWindowParams.width / 2;
        else if(mWindowParams.x >= screenWidth - mWindowParams.width / 2)
            mWindowParams.x = screenWidth - mWindowParams.width / 2;

        if(mWindowParams.y <= -mWindowParams.height / 2)
            mWindowParams.y = -mWindowParams.height / 2;
        else if(mWindowParams.y >= screenHeight - mWindowParams.height / 2 - mStatusBarHeight)
            mWindowParams.y = screenHeight - mWindowParams.height / 2 - mStatusBarHeight;
    }


    // X,Y相对于屏幕坐标
    float mInScreenX, mInScreenY;
    // X,Y相对于悬浮窗口View坐标
    float mInViewX, mInViewY;

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        // TODO: Implement this method
        switch(e.getAction()){
            case MotionEvent.ACTION_DOWN:
                // 获取相对View的坐标，即以此View左上角为原点
                mInViewX = e.getX();
                mInViewY = e.getY() - mStatusBarHeight;
                // 获取相对屏幕的坐标，即以屏幕左上角为原点
                mInScreenX = e.getRawX();
                mInScreenY = e.getRawY();

                // 在按下时获取悬浮窗口View的宽度与高度，用于
                mViewWidth = lparams.width;
                mViewHeight = lparams.height;
                // 判断按下的坐标是否在右下角位置
                if(e.getRawX() >= mWindowParams.x + mWindowParams.width - 50
                        && e.getRawX() <= mWindowParams.x + mWindowParams.width + 200
                        && e.getRawY() >= mWindowParams.y + mWindowParams.height - 50
                        && e.getRawY() <= mWindowParams.y + mWindowParams.height + 200) {
                    // 可以拖拽窗口进行缩放
                    isDragWindow = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 判断是否可以进行拖拽缩放窗口
                if(isDragWindow) {
                    // 拖拽窗口右下角，进行悬浮窗口的缩放
                    windowDragScale(e);
                } else {
                    // 如果按下的不是的窗口的右下角，进行窗口正常的移动操作
                    mWindowParams.x = (int) (e.getRawX() - mInViewX);
                    mWindowParams.y = (int) (e.getRawY() - mInViewY - mStatusBarHeight);
                    // 判断边界
                    checkEdge();
                }

                mWindowManager.updateViewLayout(mFloatView, mWindowParams);
                if(isMagnifiedState) // 如果悬浮窗口当前处于放大状态，那么在移动的过程中，移除所有的缩放操作
                    mHandler.removeCallbacksAndMessages(null);
                break;
            case MotionEvent.ACTION_UP:
                // 手势抬起，设置标志为fasle
                isDragWindow = false;
                // 重新设置mViewWidth，mViewHeight， 因为按下和抬起时两个值有可能是不相等的，中间有可能进行拖拽缩放操作
                mViewWidth = lparams.width;
                mViewHeight = lparams.height;
                if(isMagnifiedState) // 如果悬浮窗口当前处于放大状态，当抬起手势时，延迟5秒，然后恢复到放大之前的尺寸
                    windowRestore();
                break;
        }

        mGestureDetector.onTouchEvent(e);
        return true;
    }


    // 拖拽窗口进行缩放操作
    public void windowDragScale(MotionEvent e){
        // 拖拽过程中，窗口的宽度 = 缩放之前的窗口的宽度 + X方向上移动的距离
        // mViewWidth为缩放之前窗口的宽度，在缩放过程中mViewWidth是不变的(只要手指不抬起)
        // 窗口当前的宽度等于，以mViewWidth为基准 + X方向移动的距离
        // e.getRawX() - mInScreenX => 当前X相对于屏幕的坐标 - 手指按下时X相对于屏幕的坐标 = X方向上拖动的距离
        lparams.width = (int) (mViewWidth + e.getRawX() - mInScreenX);
        // 判断边界
        if(lparams.width >= maxWidth)
            lparams.width = maxWidth;
        else if(lparams.width <= minWidth)
            lparams.width = minWidth;

        // 拖拽过程中，窗口的高度 = 缩放之前的窗口的高度 + Y方向上移动的距离
        lparams.height = (int) (mViewHeight + e.getRawY() - mInScreenY);
        // 判断边界
        if(lparams.height >= maxHeight)
            lparams.height = maxHeight;
        else if(lparams.height <= minHeight)
            lparams.height = minHeight;

        mWindowParams.width = lparams.width;
        mWindowParams.height = lparams.height;
        mFrameLayout.setLayoutParams(lparams);
    }

//	// 保存点击次数
//	int clickCount = 0;
//	// 点击事件处理
//	public void handleEvent(){
//		++clickCount;
//		final Handler handler = ((MyApplication)mContext.getApplicationContext()).getHandler();
//		handler.postDelayed(new Runnable(){
//
//				@Override
//				public void run() {
//					// TODO: Implement this method
//					if(clickCount < 2){ //单击事件
//						Toast.makeText(mContext.getApplicationContext(), "one click...", Toast.LENGTH_SHORT).show();
//						startAnimation(1.5f, 1.5f, 1500);
//					}
//					else{ //双击，三击事件...
//						Toast.makeText(mContext.getApplicationContext(), "double click...", Toast.LENGTH_SHORT).show();
//					}
//					handler.removeCallbacksAndMessages(null);
//					clickCount = 0;
//				}
//			}, 300);
//
//	}

    @Override
    public void onClick(View v) {

        // TODO: Implement this method
        mController.stopPlay();
        Intent intent = new Intent(FloatingVideoService.VIDEO_ACTION);
        intent.setPackage(mContext.getPackageName());
        mContext.stopService(intent);
        mHandler.removeCallbacksAndMessages(null);
        switch(v.getId()){
            case R.id.video_fullscreen: //全屏按钮
                intent.setClass(mContext.getApplicationContext(), TextureVideoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                mSharedPreferences.edit().putInt("position", mController.getVideoPosition()).commit();

                mContext.startActivity(intent);

                break;
            case R.id.video_close: //关闭按钮
                break;
        }
    }


    // 进入小窗口视频播放的动画，类似Android O picture in picture
    public void startAnimation(int x, int y, float xscale, float yscale, long duration, Interpolator interpolator) {

        PropertyValuesHolder translationX = PropertyValuesHolder.ofInt("translationX", mWindowParams.x, x);
        PropertyValuesHolder translationY = PropertyValuesHolder.ofInt("translationY", mWindowParams.y, y);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, xscale);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, yscale);
        ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(translationX, translationY,scaleX, scaleY);
        animator.addUpdateListener(this);
        animator.addListener(new AnimatorAdapter());
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.start();
    }


    // 当前是否处于放大状态标志
    private boolean isMagnifiedState = false;
    // 拖拽缩放窗口标志
    private boolean isDragWindow = false;
    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        // TODO: Implement this method

        // 悬浮窗口X坐标
        mWindowParams.x = (int) animator.getAnimatedValue("translationX");
        // 悬浮窗口Y坐标
        mWindowParams.y = (int) animator.getAnimatedValue("translationY");
        // 宽度和高度缩放比例
        float xscale = (float) animator.getAnimatedValue("scaleX");
        float yscale = (float) animator.getAnimatedValue("scaleY");
        // 判断边界
        lparams.width = (int) (mViewWidth * xscale);
        if(lparams.width <= minWidth)
            lparams.width = minWidth;
        else if(lparams.width >= maxWidth)
            lparams.width = maxWidth;
        // 当悬浮窗口的高度，缩放到最小高度时就不再进行缩小，此时就等于最小高度
        lparams.height = (int) (mViewHeight * yscale);
        if(lparams.height <= minHeight)
            lparams.height = minHeight;
        else if(lparams.height >= maxHeight)
            lparams.height = maxHeight;


        // 判断悬浮的宽度，如果高度等于默认的高度，说明是第一次进入悬浮窗口
        if(mWindowParams.height == defaultHeight){
            // 如果是第一次进入悬浮窗口，那么宽度和高度按比例进行缩放
            mWindowParams.width = (int)(xscale * defaultWidth);
            mWindowParams.height = (int)(yscale * defaultHeight);
        } else {
            // 如果已经是处于悬浮窗口状态，宽度和高度就等于悬浮窗口中View的宽度和高度
            mWindowParams.width = lparams.width;
            mWindowParams.height = lparams.height;

        }

        // 更新视频显示View与悬浮窗口的参数
        mFrameLayout.setLayoutParams(lparams);
        mWindowManager.updateViewLayout(mFloatView, mWindowParams);
    }



    /*  动画监听类  */
    class AnimatorAdapter extends AnimatorListenerAdapter {

        @Override
        public void onAnimationStart(Animator animation) {
            // TODO: Implement this method
            super.onAnimationStart(animation);
            if(isMagnifiedState){
                // 如果当前处于放大状态，设置按钮可见
                fullscreen.setVisibility(View.VISIBLE);
                close.setVisibility(View.VISIBLE);
            }
        }


        @Override
        public void onAnimationEnd(Animator animation) {
            // TODO: Implement this method
            super.onAnimationEnd(animation);
            // 当单击悬浮窗口放大之后，mViewWidth，mViewHeight在缩放过程中是不变的
            // 当悬浮窗口放大再缩小后，mViewWidth会大于或者等于悬浮窗口宽度(比例是浮点数)
            // 根据mViewWidth >= lparams.width和isMagnifiedState，来判断窗口是否已经缩小到原来的大小
            if(mViewWidth >= lparams.width && mViewHeight >= lparams.height && isMagnifiedState) {
                isMagnifiedState = false;
                fullscreen.setVisibility(View.GONE);
                close.setVisibility(View.GONE);
            }

            // 动画结束后，当前View的宽高就等于悬浮窗口的宽高
            mViewWidth = lparams.width;
            mViewHeight = lparams.height;
            animation.removeAllListeners();
        }
    }


    // 延迟5秒后，把悬浮窗口缩小到放大之前的尺寸
    // 悬浮窗口在移动的过程中，不进行缩小处理
    public void windowRestore(){
        final int x = (int) (mWindowParams.x + mViewWidth / 12f);
        final int y = (int) (mWindowParams.y + mViewHeight / 12f);
        mHandler.postDelayed(new Runnable(){

            @Override
            public void run() {
                // TODO: Implement this method
                // 1f - 1.5f - 1f   1.5f * 2 / 3 = 1f
                startAnimation(x, y, 2f / 3, 2f / 3, 200, new DecelerateInterpolator());
            }
        }, 5000);
    }




    // 点击悬浮窗口，然后进行缩放
    public void windowScale(){

        final int oldx = mWindowParams.x;
        final int oldy = mWindowParams.y;
        int x = (int) (mWindowParams.x - mViewWidth * 1.5f / 12f);
        int y = (int) (mWindowParams.y - mViewHeight * 1.5f / 12f);
        // 1f - 1.5f 放大悬浮窗口1.5倍
        startAnimation(x, y, 1.5f, 1.5f, 200, new DecelerateInterpolator());
        // 延迟5秒后，把悬浮窗口缩小到放大之前的尺寸
        mHandler.postDelayed(new Runnable(){

            @Override
            public void run() {
                // TODO: Implement this method
                // 1f - 1.5f - 1f   1.5f * 2 / 3 = 1f
                startAnimation(oldx, oldy, 2f / 3, 2f / 3, 200, new DecelerateInterpolator());
            }
        }, 5000);
    }
    
    
    

    /* 悬浮窗口手势检测类 */
    class FloatingWindowGesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // TODO: Implement this method
            if(!isMagnifiedState){
                // 状态改变，处于放大状态
                isMagnifiedState = true;
                // 如果悬浮窗口没有处于放大的状态，进行窗口放大
                windowScale();
            }

            return super.onSingleTapConfirmed(e);
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // TODO: Implement this method
            // 双击悬浮窗口进行视频播放的暂停与恢复
            //mController.stopPlay();
            return super.onDoubleTap(e);
        }


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // TODO: Implement this method

            int x = 0, y = 0;

            // 向下快速滑动
            if(e2.getRawX() - e1.getRawX() > 0){
                x = mWindowParams.x + mViewWidth / 2;
                // 判断X方向上边界
                if(x >= screenWidth - mViewWidth / 2)
                    x = screenWidth - mViewWidth / 2;
            }
            // 向上快速滑动
            else if(e2.getRawX() - e1.getRawX()  < 0){
                x = mWindowParams.x - mViewWidth / 2;
                if(x <= -mViewWidth / 2)
                    x = -mViewWidth / 2;
            }
            // 向右快速滑动
            if(e2.getRawY() - e1.getRawY() > 0){
                y = mWindowParams.y + mViewHeight / 2;
                if(y >= screenHeight - mViewHeight / 2 - mStatusBarHeight * 2)
                    y = screenHeight - mViewHeight / 2 - mStatusBarHeight * 2;
            }
            // 向左快速滑动
            else if(e2.getRawY() - e1.getRawY() < 0){
                y = mWindowParams.y - mViewHeight / 2;
                if(y <= -mViewHeight / 2)
                    y = -mViewHeight / 2;
            }

            startAnimation(x, y, 1f, 1f, 1000, new OvershootInterpolator());

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
