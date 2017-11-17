package org.androidtown.homecare.Fragments;


import android.content.Intent;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.androidtown.homecare.Activities.MainActivity;
import org.androidtown.homecare.Activities.MessageActivity;
import org.androidtown.homecare.Activities.RatingActivity;
import org.androidtown.homecare.Models.HomeCare;
import org.androidtown.homecare.Models.User;
import org.androidtown.homecare.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeCareFragment extends Fragment {

    private static LinearLayout hiddenLayout, noneCareLayout; //진행중인 홈케어의 존재 여부에 따라 레이아웃을 띄운다.
    private static ImageView profileImageView;
    private static TextView titleText, dateText, payText, periodText, careTypeText, locationText, commentText
            , nameText, starText;
    private static Button messageButton, estimationButton, cancelButton;

    public HomeCareFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_care, container, false);
        initView(view);

        return view;
    }

    public static void setViews() {
        HomeCare homeCare = MainActivity.getHomeCareOfCurrentUser(); //메인에서 서버로부터 받은 홈케어 리스트에서 해당 key에 맞는 홈케어를 탐색.
        User user = MainActivity.getOpponentUser(); //작성자 정보를 불러온다.

        //사진을 띄움
        MainActivity.getFirebasePicture().downloadImage(user.getUid(), profileImageView);

        //시간 관련 텍스트뷰 (Period, Date)
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat fmt2 = new SimpleDateFormat("MM/dd");
        SimpleDateFormat fmt3 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(homeCare.getStartPeriod());
        periodText.setText(fmt.format(cal.getTime()));
        cal.setTimeInMillis(homeCare.getEndPeriod());
        periodText.append(" - "+ fmt2.format(cal.getTime()));
        cal.setTimeInMillis((long)homeCare.getTimestamp());
        dateText.setText(fmt3.format(cal.getTime()));

        //나머지
        payText.setText(String.valueOf(homeCare.getPay()));
        careTypeText.setText(homeCare.getCareType());
        locationText.setText(homeCare.getLocation());
        commentText.setText(homeCare.getComment());
        titleText.setText(homeCare.getTitle());

        //유저 정보를 띄움
        starText.setText("★ " + user.getStar());
        nameText.setText(user.getName());

    }


    private void initView(View view) {

        hiddenLayout = view.findViewById(R.id.hidden_view_in_message_fragment);
        noneCareLayout = view.findViewById(R.id.none_care_view_in_message_fragment);
        profileImageView = view.findViewById(R.id.profile_image_view_in_fragment_home_care);
        profileImageView.setBackground(new ShapeDrawable(new OvalShape()));
        profileImageView.setClipToOutline(true);
        profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        titleText = view.findViewById(R.id.title_text_view_in_fragment_home_care);
        dateText = view.findViewById(R.id.upload_date_text_view_in_fragment_home_care);
        payText = view.findViewById(R.id.home_care_pay_text_view_in_fragment_home_care);
        periodText = view.findViewById(R.id.home_care_period_text_view_in_fragment_home_care);
        careTypeText = view.findViewById(R.id.home_care_care_type_text_view_in_fragment_home_care);
        locationText = view.findViewById(R.id.home_care_location_text_view_in_fragment_home_care);
        commentText = view.findViewById(R.id.comment_text_view_in_fragment_home_care);
        nameText = view.findViewById(R.id.name_text_view_in_fragment_home_care);
        starText = view.findViewById(R.id.star_text_view_in_fragment_home_care);

        cancelButton = view.findViewById(R.id.cancel_button_in_fragment_home_care);
        messageButton = view.findViewById(R.id.message_button_in_fragment_home_care);
        estimationButton = view.findViewById(R.id.estimation_button_in_fragment_home_care);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageDialogFragment.setContext(HomeCareFragment.this.getActivity());
                MessageDialogFragment.setKeyAndUid(MainActivity.getHomeCareOfCurrentUser().getKey(), MainActivity.getUidOfCurrentUser());
                MessageDialogFragment.showDialog(MessageDialogFragment.HOMECARE_CANCELLATION, HomeCareFragment.this.getActivity());
            }
        });

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeCareFragment.this.getActivity(), MessageActivity.class);
                startActivity(intent);
            }
        });

        estimationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeCareFragment.this.getActivity(), RatingActivity.class);
                startActivity(intent);
            }
        });

    }


    public static LinearLayout getHiddenLayout() {
        return hiddenLayout;
    }

    public static LinearLayout getNoneCareLayout() {
        return noneCareLayout;
    }
}
