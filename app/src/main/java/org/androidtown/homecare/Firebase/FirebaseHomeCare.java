package org.androidtown.homecare.Firebase;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.androidtown.homecare.Activities.MainActivity;
import org.androidtown.homecare.Adapters.CandidateAdapter;
import org.androidtown.homecare.Adapters.HomeCareAdapter;
import org.androidtown.homecare.Fragments.HomeCareCreationFragment;
import org.androidtown.homecare.Fragments.MessageDialogFragment;
import org.androidtown.homecare.Models.Chat;
import org.androidtown.homecare.Models.HomeCare;
import org.androidtown.homecare.Models.User;
import org.androidtown.homecare.Utils.MyLinearLayoutManager;
import org.androidtown.homecare.Utils.ProgressDialogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hanhb on 2017-11-09.
 */

public class FirebaseHomeCare {

    /*
        FirebaseHomeCare : 홈케어 관련 컨트롤러

        HomeCares 관련 CRUD
        1. writeHomecare() C
        2. destroyHomecare() D
        3. refreshHomeCare() R
        4. updateHomecare() U

        Candidates 관련
        1. requestHomeCare() C / D
        2. initTextOfRequestButton() : 요청 상태에 따라 뷰 초기화
        3. refreshCandidates() : R
        4.
     */

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference homeCareRef = database.getReference().child("homecare");
    private final DatabaseReference userRef = database.getReference().child("user");

    private Context context;
    private RecyclerView homeCareRecyclerView, candidatesRecyclerView;
    private final static List<HomeCare> homeCareList = new ArrayList<>();
    private final static List<User> userList = new ArrayList<>();
    private final static List<User> candidateList = new ArrayList<>();
    private final static List<HomeCare> filteredHomeCareList = new ArrayList<>();
    private final static List<User> filteredUserList = new ArrayList<>();

    private static final String CANDIDATES = "candidates";
    private static final String CURRENT_HOME_CARE = "current_homecare";
    private static final String UID_OF_CARETAKER = "uidOfCareTaker";
    private static final String WAITING_FOR_DELETION = "waitingForDeletion"; //삭제 시도한 uid 저장

    public FirebaseHomeCare(Context context) {

        this.context = context;

    }

    //홈케어를 키값으로 탐색
    public HomeCare searchHomeCare(String key){
        Iterator<HomeCare> it = homeCareList.iterator();

        while (it.hasNext()){
            HomeCare homeCare = it.next();
            if(key.equals(homeCare.getKey())){
                return homeCare;
            }
        }
        return null;
    }

    public void setHomeCareRecyclerView(RecyclerView homeCareRecyclerView) {
        this.homeCareRecyclerView = homeCareRecyclerView;
    }

    public RecyclerView getHomeCareRecyclerView() {
        return homeCareRecyclerView;
    }

    public RecyclerView getCandidatesRecyclerView() {
        return candidatesRecyclerView;
    }

    public void setCandidatesRecyclerView(RecyclerView candidatesRecyclerView) {
        this.candidatesRecyclerView = candidatesRecyclerView;
    }

    //CREATE HOME CARE
    public void writeHomeCare(final String uid, final HomeCare homeCare, final HomeCareCreationFragment fragment){
        ProgressDialogHelper.show(context);

        /*
            Process
            0. 프로그레스 다이얼로그 띄움
            1. root/user/uid/current_homcare이 null인지 아닌지 확인 (이미 올린 홈케어가 있는지 없는지 확인)
            2. null이면 생성, 이미 존재할 경우 생성하지 않음
            3. (생성하였으면 리프레쉬)
            4. 결과를 다이얼로그로 띄움
            5. 프로그레스 다이얼로그 dismiss
         */

        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.child(CURRENT_HOME_CARE).getValue()==null) {

                    DatabaseReference specificHomeCareRef = homeCareRef.push();
                    homeCare.setKey(specificHomeCareRef.getKey());
                    specificHomeCareRef.setValue(homeCare).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            ProgressDialogHelper.dismiss();
                            MessageDialogFragment.setHomeCareCreationFragment(fragment);
                            MessageDialogFragment.showDialog(MessageDialogFragment.HOMECARE_CREATION_SUCCESS,context);
//                            refreshHomeCare(null); //리프레쉬
                            ((MainActivity)context).refresh(true, null); //리프레쉬
                        }
                    });
                    userRef.child(uid).child(CURRENT_HOME_CARE).setValue(specificHomeCareRef.getKey());

                    //일 당 200만원 이상일 경우 과도한 금액 입력으로 판단
                    long homecarePeriod = (homeCare.getEndPeriod() - homeCare.getStartPeriod())/(1000*60*60*24) +1; //단위는 일

                    if(homeCare.getPay()/homecarePeriod >100 ) {
                        userRef.child(uid).child("exceededPayments").setValue(dataSnapshot.child("exceededPayments").getValue(Integer.class)+1); // 과도한 금액 입력 Increment
                    }
                } else {
                    ProgressDialogHelper.dismiss();
                    MessageDialogFragment.showDialog(MessageDialogFragment.HOME_CARE_ALREADY_EXISTS, context);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


    //DESTROY HOME CARE
    public void destroyHomeCare(final String key, final String uid, final boolean calledByMain){

        /*
            상황
            1. 상대방과 매칭이 되지 않은 경우
                -> 걍 삭제 (db의 user와 homecare에서 삭제하고 finish and refresh)
            2. 상대방과 매칭이 이미 된 경우
                -> 상대방도 삭제 신청을 한 경우 삭제
                -> 그렇지 않을 경우 삭제 신청 상태로 전환
         */

        ProgressDialogHelper.show(context);
        homeCareRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HomeCare homeCare = dataSnapshot.getValue(HomeCare.class);
                if(homeCare==null){
                    Toast.makeText(context, "존재하지 않는 홈케어입니다. ", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(homeCare.getUidOfCareTaker() == null){
                    userRef.child(uid).child(CURRENT_HOME_CARE).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            homeCareRef.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    ProgressDialogHelper.dismiss();
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show();

                                    if(!calledByMain) {
                                        ((Activity) context).setResult(MainActivity.RESULT_REFRESH);
                                        ((Activity) context).finish();
                                    } else {
                                        ((MainActivity)context).refresh(true, null);
                                    }

                                }
                            });
                        }
                    });
                } else if(homeCare.getWaitingForDeletion() != null && !homeCare.getWaitingForDeletion().equals(uid)){
                    //신청은 됐지만 current user가 아닌 경우 (상대방이 요청한 경우)

                    FirebaseMessenger.destroyChat(key); //Chat 제거

                    userRef.child(uid).child(CURRENT_HOME_CARE).removeValue();
                    userRef.child(uid).child("newMessages").setValue(0);
                    userRef.child(homeCare.getWaitingForDeletion()).child(CURRENT_HOME_CARE).removeValue();
                    userRef.child(homeCare.getWaitingForDeletion()).child("newMessages").setValue(0);

                    homeCareRef.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            ProgressDialogHelper.dismiss();
                            Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            refreshHomeCare(null);
                            ((MainActivity)context).refresh(true, null);
                        }
                    });

                } else {
                    //홈케어 도중, 상대방에게 삭제 요청을 해야하는 경우
                    if(MainActivity.getUidOfOpponentUser()!=null)
                        userRef.child(MainActivity.getUidOfOpponentUser()).child(WAITING_FOR_DELETION).setValue(uid);
                    else
                        return;

                    userRef.child(uid).child("suspensions").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Integer suspensions = dataSnapshot.getValue(Integer.class);
                            userRef.child(uid).child("suspensions").setValue(suspensions+1); //위반 횟수를 1 increment
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    homeCareRef.child(key).child(WAITING_FOR_DELETION).setValue(uid).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            ProgressDialogHelper.dismiss();
                            MessageDialogFragment.showDialog(MessageDialogFragment.DELETION_WAITING,context);
                        }
                    });



                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //READ HOME CARES
    public void refreshHomeCare(@Nullable final SwipeRefreshLayout swipeRefreshLayout){
        /*
            1. 홈케어 리스트를 갱신한다.
            2. 갱신된 홈케어의 작성자 uid 정보로부터 유저 리스트(writers)를 갱신한다.
            3. 리사이클러뷰에 적용
         */
        if(homeCareRecyclerView == null)
            return;

        homeCareList.clear();
        userList.clear();


        homeCareRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    HomeCare homeCare = ds.getValue(HomeCare.class);
                    homeCareList.add(homeCare);
                }

                Collections.reverse(homeCareList);

                //작성된 홈케어 리스트로부터 유저 리스트도 갱신
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(swipeRefreshLayout!=null)
                            swipeRefreshLayout.setRefreshing(false);
                        Iterator<HomeCare> it = homeCareList.iterator();
                        while (it.hasNext()){
                            HomeCare homeCare = it.next();
                            userList.add(dataSnapshot.child(homeCare.getUid()).getValue(User.class));
                        }

                        HomeCareAdapter homeCareAdapter = new HomeCareAdapter(homeCareList, userList, context);
                        homeCareRecyclerView.setLayoutManager(new MyLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
                        homeCareRecyclerView.setAdapter(homeCareAdapter);
                        if(MainActivity.getProgressBarLayout()!=null && MainActivity.getProgressBarLayout().getVisibility() != View.GONE)
                            MainActivity.getProgressBarLayout().setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    public void filter(){

        HomeCareAdapter homeCareAdapter = new HomeCareAdapter(filteredHomeCareList, filteredUserList, context);
        homeCareRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        homeCareRecyclerView.setAdapter(homeCareAdapter);

    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /* 여기서부터 Candidates 관련 */

    public void pickCandidate(final String key, final String uidOfCandidate){

        /*
            0. 프로그레스바 띄움
            1. 이미 케어하는 사람이 존재할 경우 리턴
            2. homecare/key/uidOfCareTaker 에 uid 갱신
            3. Chat 생성
            4. request code를 포함하여 finish (갱신되게)
         */


        ProgressDialogHelper.show(context, "등록 중입니다...");

        //상대방이 이미 홈케어를 진행 중인지 확인한다.
        userRef.child(uidOfCandidate).child(CURRENT_HOME_CARE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.getValue(String.class) != null){
                    ProgressDialogHelper.dismiss();
                    Toast.makeText(context, "상대방이 이미 홈케어 서비스를 진행 중입니다.", Toast.LENGTH_SHORT).show();
                    return;

                } else {
                    homeCareRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            ProgressDialogHelper.dismiss(); //임시

                            if(dataSnapshot.child("uidOfCareTaker").getValue(String.class) != null){
                                Toast.makeText(context, "이미 홈케어 서비스가 진행 중입니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //홈케어의 케어테이커에 케어테이커의 uid 추가
                            //케어테이커의 현재 홈케어에 key 추가
                            userRef.child(uidOfCandidate).child(CURRENT_HOME_CARE).setValue(key);
                            homeCareRef.child(key).child(UID_OF_CARETAKER).setValue(uidOfCandidate).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    //메시지 생성
                                    Chat chat = new Chat(key, MainActivity.getUidOfCurrentUser(), uidOfCandidate);
                                    FirebaseMessenger.writeChat(chat);

                                    //생성 후 성공 메시지 띄움
                                    MessageDialogFragment.setContext(context);
                                    MessageDialogFragment.showDialog(MessageDialogFragment.CANDIDATE_PICK_SUCCESS, context);

                                }
                            });
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void refreshCandidates(String key){
        if(candidatesRecyclerView==null)
            return;
        /*
            1. key에 해당하는 homecare의 candidates uid를 불러온다.
            2. 불러오면 List에 넣는다
            3. List의 uid에 해당되는 User를 userList에 추가한다.
         */

        homeCareRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                final List<String> uidOfCandidates = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.child(CANDIDATES).getChildren()){
                    uidOfCandidates.add(ds.getValue(String.class)); //후보자의 키를 넣음
                }

                //유저 데이터스냅샷을 불러온 다음, 리스트에 해당하는 객체만 넣는다.
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        candidateList.clear();

                        Iterator<String> it = uidOfCandidates.iterator(); //유저 리스트로부터

                        while (it.hasNext()){
                            String candidateUid = it.next();
                            candidateList.add(dataSnapshot.child(candidateUid).getValue(User.class));

                        }
                        CandidateAdapter candidateAdapter = new CandidateAdapter(candidateList, context);
                        candidatesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                        candidatesRecyclerView.setAdapter(candidateAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



    }

    public void requestHomeCare(final String key, final String uid, final Button requestButton){
        /*
            requestHomeCare : 홈케어 신청 기능
            1. ProgressDialog를 띄움
            2. 신청 내역이 없다면 신청자 목록에 자신의 정보(uid)를 추가한다.
            3. 신청 내역이 있다면 신청자 목록에서 자신의 정보를 제거한다.
            4. 1이나 3의 결과에 따라 request Button의 text를 수정한다, (신청하기 <-> 신청 취소)
            5. 자신에게 신청했을 경우 토스트 띄우고 리턴
         */

        ProgressDialogHelper.show(context, "홈케어 요청 중입니다.");

        homeCareRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ProgressDialogHelper.dismiss();
                if(dataSnapshot.getValue()==null){
                    MessageDialogFragment.showDialog(MessageDialogFragment.HOMECARE_NULL, context);
                    return;
                }

                if(dataSnapshot.child("uid").getValue(String.class).equals(uid)){
                    Toast.makeText(context, "자신에게 신청할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //자신이 있는지 탐색하기
                for(DataSnapshot ds : dataSnapshot.child(CANDIDATES).getChildren()) {
                    if(ds.getValue(String.class).equals(uid)) {
                        homeCareRef.child(key).child(CANDIDATES).child(ds.getKey()).removeValue();
                        requestButton.setText("신청하기");
                        Toast.makeText(context, "신청이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                //신청 기록이 없을 경우 신청
                homeCareRef.child(key).child(CANDIDATES).push().setValue(uid);
                requestButton.setText("신청취소");
                Toast.makeText(context, "신청이 완료되었습니다!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    public void initTextOfRequestButton(final String key, final String uid, final Button contactButton){
        /*
            1. 홈케어 마감 상태에 따라 버튼의 visibility와 title 설정
            2. 신청했으면 "신청하기", 신청하지 않으면 "신청취소"로 텍스트 바꾸기
         */

        homeCareRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.getValue()==null){
                    MessageDialogFragment.showDialog(MessageDialogFragment.HOMECARE_NULL, context);
                    return;
                }

                //자신이 있는지 탐색하기
                for (DataSnapshot ds : dataSnapshot.child(CANDIDATES).getChildren()) {
                    if (ds.getValue(String.class).equals(uid)) {
                        //신청 기록이 있으면 신청 취소로
                        contactButton.setText("신청취소");
                        return;
                    }
                }
                //신청 기록이 없을 경우 신청
                contactButton.setText("신청하기");
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public User searchUser(String key) {

        Iterator<User> it = userList.iterator();
        while (it.hasNext()){
            User user = it.next();
            if(key.equals(user.getCurrent_homecare())){
                return user;
            }
        }
        return null;
    }

    public static List<HomeCare> getFilteredHomeCareList() {
        return filteredHomeCareList;
    }

    public static List<User> getFilteredUserList() {
        return filteredUserList;
    }

    public static List<HomeCare> getHomeCareList() {
        return homeCareList;
    }

    public static List<User> getUserList() {
        return userList;
    }
}
