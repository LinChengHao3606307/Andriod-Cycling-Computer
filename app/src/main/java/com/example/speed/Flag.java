package com.example.speed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class Flag {

    public int viewId;
    public View itemView;
    public boolean isSelected;
    public float timeTaken;
    public float distanceFromStart;
    public float positionAng;

    public Flag(float ang, int screenWidth, int speedTxtCenterY, Context context, ConstraintLayout parentLayout, float haveTakenTime, float currentDist, boolean isVisible) {
        float scale = context.getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (50 * scale + 0.5f);
        itemView = LayoutInflater.from(context).inflate(R.layout.put_flage_layout, null);
        viewId = View.generateViewId();
        itemView.setId(viewId);
//        if (itemView.getId()==View.NO_ID) {
//            Toast.makeText(MainActivity.this, "NO ID", Toast.LENGTH_LONG).show();
//        }
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(sizeInPx,sizeInPx);
        // [-19,18]
        int leftMarginValue = (int) (screenWidth*(0.5+Math.sin(Math.toRadians(ang))));
        int topMarginValue = (int) (speedTxtCenterY + screenWidth*Math.cos(Math.toRadians(ang)) - sizeInPx);
        layoutParams.leftMargin = leftMarginValue;
        layoutParams.topMargin = topMarginValue;
        itemView.setLayoutParams(layoutParams);
        parentLayout.addView(itemView);
        if (isVisible) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView imageView = v.findViewById(R.id.newFlag);
                    if (!isSelected) {
                        MainActivity.isClicked(itemView);
                        imageView.setAlpha(255);
                        imageView.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                        isSelected = true;
                    }
                }
            });
        }
        else {
            ImageView imageView = itemView.findViewById(R.id.newFlag);
            imageView.setVisibility(View.INVISIBLE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isSelected) {
                        MainActivity.isClicked(itemView);
                        isSelected = true;
                    }
                }
            });
        }
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentLayout);
        constraintSet.constrainWidth(itemView.getId(), sizeInPx);
        constraintSet.constrainHeight(itemView.getId(), sizeInPx);
        constraintSet.connect(itemView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, leftMarginValue);
        constraintSet.connect(itemView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMarginValue);
        constraintSet.applyTo(parentLayout);
        this.setToUnselect();
        isSelected = false;
        timeTaken = haveTakenTime;
        distanceFromStart = currentDist;
        positionAng = ang;
    }

    public void moveTo(Context context, float ang, int screenWidth, int speedTxtCenterY) {
        float angle=ang - 0.2f;
        float scale = context.getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (50 * scale + 0.5f);
        this.itemView.setX((float) (screenWidth*(0.5+Math.sin(Math.toRadians(angle)))));
        this.itemView.setY((float) (speedTxtCenterY + screenWidth*Math.cos(Math.toRadians(angle)) - sizeInPx));
        positionAng = ang;
        // [-19,18]
    }

    public void setToUnselect() {
        ImageView imageView = this.itemView.findViewById(R.id.newFlag);
        imageView.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        imageView.setAlpha(153);
        this.isSelected = false;
    }

}
