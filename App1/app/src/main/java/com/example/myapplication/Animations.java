package com.example.myapplication;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.support.design.widget.FloatingActionButton;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.transitionseverywhere.Rotate;

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


