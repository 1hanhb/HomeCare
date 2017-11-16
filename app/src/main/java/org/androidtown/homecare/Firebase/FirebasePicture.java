package org.androidtown.homecare.Firebase;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.androidtown.homecare.R;

/**
 * Created by hanhb on 2017-11-13.
 */

public class FirebasePicture {
    /*
        사진 다운로드, 업로드
        uploadImage()
        downloadImage()
     */

    private Context context;
    private final static StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profileImage");


    public FirebasePicture(Context context) {
        this.context = context;
    }


    public void downloadImage(String uid, final ImageView profileImageView){

        storageRef.child(uid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if(((Activity)context).isFinishing())
                    return;
                Glide.with(context).load(uri).into(profileImageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(((Activity)context).isFinishing())
                    return;
                Glide.with(context).load(R.drawable.common_google_signin_btn_icon_light_focused).into(profileImageView);
            }
        });

    }

}