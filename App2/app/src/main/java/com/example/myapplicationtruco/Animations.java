package com.example.myapplicationtruco;


import android.animation.ValueAnimator;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Animations {

    int mShortAnimationDuration;
    MainActivity mainActivity;

    Animations(MainActivity mainActivity_){
        mShortAnimationDuration = mainActivity_.getResources().getInteger(android.R.integer.config_shortAnimTime);
        mainActivity = mainActivity_;
    }

    public void AnimateImageRotationClockwise(ImageView imageView){
        Animation animation = AnimationUtils.loadAnimation(mainActivity, R.anim.rotate_around_center_point);
        imageView.startAnimation(animation);
    }

    public void AnimateImageRotationCounterclockwise(ImageView imageView){
        Animation animation = AnimationUtils.loadAnimation(mainActivity, R.anim.rotate_around_center_point_counter);
        imageView.startAnimation(animation);
    }

    public void AnimateLayoutSlideFromBottom(LinearLayout linearLayout){
        Animation animation = AnimationUtils.loadAnimation(mainActivity, R.anim.slide_from_bottom);
        linearLayout.startAnimation(animation);
    }

    public void AnimateLayoutSlideFromTop(LinearLayout linearLayout){

        Animation animation = AnimationUtils.loadAnimation(mainActivity, R.anim.slide_from_top);
        linearLayout.startAnimation(animation);
    }

    public void AnimateSequenceTopView() {

        Animation fadeIn1 = new AlphaAnimation(0, 1);fadeIn1.setInterpolator(new DecelerateInterpolator());
        fadeIn1.setDuration(200);fadeIn1.setStartOffset(0);
        mainActivity.findViewById(R.id.top_layout_1).setAnimation(fadeIn1);

        Animation fadeIn2 = new AlphaAnimation(0, 1);fadeIn2.setInterpolator(new DecelerateInterpolator());
        fadeIn2.setDuration(200);fadeIn2.setStartOffset(200);
        mainActivity.findViewById(R.id.top_layout_2).setAnimation(fadeIn2);

        Animation fadeIn3 = new AlphaAnimation(0, 1);fadeIn3.setInterpolator(new DecelerateInterpolator());
        fadeIn3.setDuration(200);fadeIn3.setStartOffset(400);
        mainActivity.findViewById(R.id.top_layout_3).setAnimation(fadeIn3);

        Animation fadeIn4 = new AlphaAnimation(0, 1);fadeIn4.setInterpolator(new DecelerateInterpolator());
        fadeIn4.setDuration(200);fadeIn4.setStartOffset(600);
        mainActivity.findViewById(R.id.top_layout_4).setAnimation(fadeIn4);
    }

    public void AnimateFrameLayoutFadeIn(FrameLayout frameLayout){
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(200);
        frameLayout.setAnimation(fadeIn);
    }

    public void AnimateFrameLayoutFadeOut(FrameLayout frameLayout){
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new DecelerateInterpolator()); //add this
        fadeOut.setDuration(200);
        frameLayout.setAnimation(fadeOut);
    }

    public void AnimateButtonFadeIn(FloatingActionButton button){
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(200);
        button.setAnimation(fadeIn);
    }

    public void AnimateButtonFadeOut(FloatingActionButton button){
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new DecelerateInterpolator()); //add this
        fadeOut.setDuration(200);
        button.setAnimation(fadeOut);
    }

    public void AnimateHorizontalProgressBar(final HorizontalProgressBar horizontalProgressBar, int value){

        ValueAnimator anim = ValueAnimator.ofInt(0, value);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                horizontalProgressBar.progressbar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,val));
            }
        });
        anim.setDuration(1000);
        anim.start();
    }



}

class HorizontalProgressBar {
    LinearLayout mainContainer;
    View progressbar;

    void setValue(float percentage){
        progressbar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, percentage));
    }

}


