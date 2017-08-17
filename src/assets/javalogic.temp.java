package com.sports.unity.game.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.sports.unity.R;
import com.sports.unity.common.model.TinyDB;
import com.sports.unity.game.controller.GameResultActivity;
import com.sports.unity.util.CommonUtil;
import com.sports.unity.util.Constants;
import com.sports.unity.util.network.FirebaseUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static android.R.attr.width;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Mad on 16-Dec-16.
 */

public class GamePlayLogic {

    public static void loadGameContentFromNetwork(String subLevel, String level, String matchKey, String requestTag, GamePlayHandler.GameContentListener listener, Context context) {
        GamePlayHandler gamePlayHandler = GamePlayHandler.getInstance();
        gamePlayHandler.loadGameContent(subLevel, level, matchKey, requestTag, listener, context);
    }

    private int questionNo = 0;
    private int inningsCount = 1;
    private int ballCount = 0;

    private boolean isInningsChanged = false;
    private boolean isBatting = false;

    private String myJid = "";
    private String opponentJid = "";
    private String batsManJid = "";
    private String botjid = "";

    private int myAggression = 0;
    private int opponentAggression = 0;
    private int requiredAggression = 0;


    private ArrayList<Integer> timeRules = new ArrayList<>();
    private ArrayList<Integer> pointRules = new ArrayList<>();
    private ArrayList<Integer> myPointsList = new ArrayList<>();

    private ArrayList<QuestionModel> questionModels = new ArrayList<>();
    private HashMap<String, AnswerModel> answerMap = new HashMap<>();

    private ArrayList<Integer> myBattingScore = new ArrayList<>();
    private ArrayList<Integer> myBowlingScore = new ArrayList<>();

    private ArrayList<Integer> opponentBattingScore = new ArrayList<>();
    private ArrayList<Integer> opponentBowlingScore = new ArrayList<>();

    private ArrayList<AnswerModel> botAnswerModel = new ArrayList<>();

    private boolean isGotOut = false;

    private boolean isWicketTaken = false;
    private boolean isOpponentBot = false;

    private GameListener gameListener;

    private int lastBall = 0;

    private String extraResultData = "";
    int cacheSize = 4 * 1024 * 1024; // 1MiB
    private LruCache<String, Bitmap> memCache = new LruCache<String, Bitmap>(cacheSize) {
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    GamePlayLogic() {

    }

    public boolean setUpGameRules(JSONObject object) {
        boolean success = false;
        questionNo = 0;
        inningsCount = 1;
        isInningsChanged = false;
        ballCount = 0;
        isBatting = false;
        myAggression = 0;
        opponentAggression = 0;
        myPointsList.clear();
        answerMap.clear();
        myBattingScore.clear();
        myBowlingScore.clear();
        opponentBattingScore.clear();
        opponentBowlingScore.clear();

        isGotOut = false;
        isWicketTaken = false;
        try {
            {
                JSONArray pointJson = object.getJSONArray("game_rules");
                HashMap<String, ArrayList<Integer>> scoreMap = GameQuizParser.parseScoringRules(pointJson);
                timeRules = scoreMap.get("t");
                Collections.sort(timeRules);
                pointRules = scoreMap.get("p");
                Collections.sort(pointRules, Collections.<Integer>reverseOrder());
            }
            {
                this.requiredAggression = object.getInt("aggression");
            }
            {
                JSONArray quizArray = object.getJSONArray("questions");
                this.questionModels = GameQuizParser.parseQuiz(quizArray);
            }
            {
                JSONArray botRules = object.getJSONArray("bot_responses");
                this.botAnswerModel = GameQuizParser.parseBotRules(botRules, this.questionModels);
            }
            {
                botjid = object.getString("bot_username");
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public void setUpPlayerData(String myJid, String opponentJid, String batsManJid) {
        this.myJid = myJid;
        this.opponentJid = opponentJid;
        this.batsManJid = batsManJid;
        isBatting = this.batsManJid.equals(myJid) ? true : false;
        isOpponentBot = false;

    }

    public void preLoadImages(Context context) {
        for (final QuestionModel questionModel : questionModels) {
            if (questionModel.getQuestionType().equalsIgnoreCase("i")) {
                if (!TextUtils.isEmpty(questionModel.getImageUrl())) {
                    Glide.with(context).load(Uri.parse(questionModel.getImageUrl())).asBitmap().diskCacheStrategy(DiskCacheStrategy.NONE).listener(new RequestListener<Uri, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, Uri model, Target<Bitmap> target, boolean isFirstResource) {
                            Log.d("max", "Load failed" + e.getMessage());
                            HashMap<String, Object> challange = new HashMap<String, Object>();
                            challange.put(FirebaseUtil.Event.REASON, FirebaseUtil.Event.DOWNLOAD_FAILED);
                            challange.put(FirebaseUtil.Event.MESSAGE, e.getMessage());
                            FirebaseUtil.cleverTapPushEventswithProperties(getApplicationContext(), FirebaseUtil.Event.QUIZ_IMAGE_NOT_FOUND, challange);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Uri model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            return false;
                        }
                    }).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            Log.d("max", "Load success");
                            memCache.put(questionModel.getImageUrl(), resource);
                        }
                    });
                } else {
                    Log.d("max", "url is null for= " + questionModel.getQuestion());
                    HashMap<String, Object> challange = new HashMap<String, Object>();
                    challange.put(FirebaseUtil.Event.REASON, FirebaseUtil.Event.URL_BLANK);
                    challange.put(FirebaseUtil.Event.QUESTION, questionModel.getQuestion());
                    FirebaseUtil.cleverTapPushEventswithProperties(getApplicationContext(), FirebaseUtil.Event.QUIZ_IMAGE_NOT_FOUND, challange);
                }
            }
        }
    }

    public Bitmap getImagesFromCache(String url) {
        Bitmap bitmap = memCache.get(url);
        return bitmap;
    }

    public void startingGamePlay(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    public boolean isGamePlayON() {
        return gameListener != null;
    }

    public void endingGamePlay() {
        this.gameListener = null;
        memCache.evictAll();

    }

    public QuestionModel getQuestion(int questionNo) {
        QuestionModel questionModel = questionModels.get(questionNo - 1);

        return questionModel;
    }

    public boolean isBatting() {
        return isBatting;
    }

    public void clearAnswerMap() {
        answerMap.clear();
    }

    public void submitAnswer(int questionNumber, String jid, String selectedOption, int delta) {
        AnswerModel answerModel = new AnswerModel(selectedOption, delta);
        if (!answerMap.containsKey(jid) && isGamePlayON()) {
            answerMap.put(jid, answerModel);
            if (answerMap.size() == 2) {
                ballCount++;
                questionNo = questionNumber;
                AnswerModel myAnswer = answerMap.get(myJid);
                int myDelta = myAnswer.getDelta();
                String myOption = myAnswer.getSelectedOption();

                AnswerModel opponentAnswer = answerMap.get(opponentJid);
                int opponentDelta = opponentAnswer.getDelta();
                String opponentOption = opponentAnswer.getSelectedOption();

                boolean isMyAnswerCorrect = decideIfAnsweredCorrectly(questionNo, myOption);
                boolean isOpponentAnswerCorrect = decideIfAnsweredCorrectly(questionNo, opponentOption);

                if (myDelta == opponentDelta && isMyAnswerCorrect && isOpponentAnswerCorrect) {
                    if (isBatting()) {
                        myDelta = myDelta - 1;
                    } else {
                        opponentDelta = opponentDelta - 1;
                    }
                }

                int deltaDiff = Math.max(myDelta, opponentDelta) - Math.min(myDelta, opponentDelta);
                if (isMyAnswerCorrect && isOpponentAnswerCorrect && deltaDiff < 1000) {
                    ISFSObject isfsObject = new SFSObject();
                    isfsObject.putUtfString(GameConstant.PARAM_NAME_OPTION, myOption);
                    isfsObject.putInt(GameConstant.PARAM_NAME_MY_DELTA, myDelta);
                    isfsObject.putBool(GameConstant.PARAM_NAME_CORRECT, isMyAnswerCorrect);
                    isfsObject.putInt(GameConstant.PARAM_NAME_OPPONENT_DELTA, opponentDelta);
                    gameListener.dispatchGameEvent(GameConstant.CMD_SCORE_SAME_TIME, isfsObject);
                } else if (myOption.equals(opponentOption)) {
                    ISFSObject isfsObject = new SFSObject();
                    isfsObject.putUtfString(GameConstant.PARAM_NAME_OPTION, myOption);
                    isfsObject.putBool(GameConstant.PARAM_NAME_CORRECT, isMyAnswerCorrect);
                    gameListener.dispatchGameEvent(GameConstant.CMD_SCORE_BOTH_SAME, isfsObject);
                } else {
                    ISFSObject isfsObject = new SFSObject();
                    isfsObject.putUtfString(GameConstant.PARAM_NAME_OPTION, opponentOption);
                    isfsObject.putBool(GameConstant.PARAM_NAME_CORRECT, isOpponentAnswerCorrect);
                    gameListener.dispatchGameEvent(GameConstant.CMD_SCORE_OPPONENT_ANSWER, isfsObject);
                }


                if (!isMyAnswerCorrect && !isOpponentAnswerCorrect) {
                    String correctOption = getCorrectOption(questionNumber);
                    ISFSObject isfsObject = new SFSObject();
                    isfsObject.putUtfString(GameConstant.PARAM_NAME_CORRECT, correctOption);
                    gameListener.dispatchGameEvent(GameConstant.CMD_SCORE_CORRECT_ANSWER, isfsObject);
                }
                calculateScore(isMyAnswerCorrect, isOpponentAnswerCorrect, myDelta, opponentDelta);
            }
        } else {
            //nothing
        }
    }

    private void calculateScore(boolean isMyAnswerCorrect, boolean isOpponentAnswerCorrect, int myDelta, int opponentDelta) {

        int myRun = calculateScore(myDelta);
        int opponentRun = calculateScore(opponentDelta);
        int currentBallScore = 0;

        if (isMyAnswerCorrect) {
            myPointsList.add(myRun);
        } else {
            myPointsList.add(0);
        }

        if (isMyAnswerCorrect && isOpponentAnswerCorrect) {

            if (myDelta < opponentDelta) {
                currentBallScore = updateMyScoreAndGetCurrentBallRun(myRun);
            } else {
                currentBallScore = updateOpponentScoreAndGetCurrentBallRun(opponentRun);
            }

        } else if (isMyAnswerCorrect) {
            currentBallScore = updateMyScoreAndGetCurrentBallRun(myRun);

        } else if (isOpponentAnswerCorrect) {
            currentBallScore = updateOpponentScoreAndGetCurrentBallRun(opponentRun);

        } else {

            if (isBatting) {
                myBattingScore.add(0);
                opponentBowlingScore.add(0);
            } else {
                myBowlingScore.add(0);
                opponentBattingScore.add(0);
            }

        }

        int totalRun = currentBatsmanTotal();
        int aggression = currentBowlerAggression();
        boolean isOut = checkIfInningsChanged(totalRun);

        dispatchScoreUpdate(ballCount, currentBallScore, totalRun, aggression, isInningsChanged, isOut, myDelta, opponentDelta, isMyAnswerCorrect, isOpponentAnswerCorrect);

        if (isInningsChanged) {
            ballCount = 0;
            isInningsChanged = false;
        }
    }

    private int updateMyScoreAndGetCurrentBallRun(int myRun) {
        int currentBallScore = 0;
        if (isBatting) {
            myBattingScore.add(myRun);
            opponentBowlingScore.add(myRun);
            opponentAggression = Math.max(0, (opponentAggression - myRun));
            currentBallScore = myRun;
        } else {
            myBowlingScore.add(0);
            opponentBattingScore.add(0);
            myAggression = myAggression + myRun;
        }
        return currentBallScore;
    }

    private int updateOpponentScoreAndGetCurrentBallRun(int opponentRun) {
        int currentBallScore = 0;
        if (isBatting) {
            myBattingScore.add(0);
            opponentBowlingScore.add(0);
            opponentAggression = opponentAggression + opponentRun;
        } else {
            currentBallScore = opponentRun;
            myBowlingScore.add(opponentRun);
            opponentBattingScore.add(opponentRun);
            myAggression = Math.max(0, (myAggression - opponentRun));
        }

        return currentBallScore;
    }

    private boolean checkIfInningsChanged(int totalRun) {
        boolean isOut = false;
        if (isBatting) {
            if (opponentAggression >= requiredAggression) {
                isGotOut = true;
                isOut = true;
                opponentAggression = 0;
                inningsCount++;
                isInningsChanged = true;
                batsManJid = isBatting ? opponentJid : myJid;
                isBatting = !isBatting;
                return isOut;
            }
        } else {
            if (myAggression >= requiredAggression) {
                isWicketTaken = true;
                myAggression = 0;
                isOut = true;
                inningsCount++;
                isInningsChanged = true;
                batsManJid = isBatting ? opponentJid : myJid;
                isBatting = !isBatting;
                return isOut;
            }
        }

        if (inningsCount == 1 && questionNo == 6) {
            inningsCount = 2;
            isInningsChanged = true;
            myAggression = 0;
            opponentAggression = 0;
            batsManJid = isBatting ? opponentJid : myJid;
            isBatting = !isBatting;
        } else if (inningsCount == 2 && (ballCount == 6 || totalRun > getLastInningsTotal())) {
            inningsCount = 3;
            isInningsChanged = true;
            myAggression = 0;
            opponentAggression = 0;
            batsManJid = isBatting ? opponentJid : myJid;
            isBatting = !isBatting;
            lastBall = 6 - ballCount;
        }
        return isOut;
    }

    private int currentBowlerAggression() {
        if (isBatting) {
            return opponentAggression;
        } else {
            return myAggression;
        }
    }

    private int currentBatsmanTotal() {
        int totalRun = 0;
        if (isBatting && myBattingScore.size() > 0) {
            for (int run : myBattingScore) {
                totalRun = totalRun + run;
            }
        } else if (opponentBattingScore.size() > 0) {
            for (int run : opponentBattingScore) {
                totalRun = totalRun + run;
            }
        }
        return totalRun;
    }

    public int getInningsCount() {
        return inningsCount;
    }

    public int getLastInningsTotal() {
        int totalRun = 0;
        if (isBatting && opponentBattingScore.size() > 0) {
            for (int run : opponentBattingScore) {
                totalRun = totalRun + run;
            }
        } else if (myBattingScore.size() > 0) {
            for (int run : myBattingScore) {
                totalRun = totalRun + run;
            }
        }
        return totalRun;
    }

    public String getWhoWon() {
        int myTotal = 0;
        int opponentTotal = 0;
        for (int run : myBattingScore) {
            myTotal = myTotal + run;
        }
        for (int run : opponentBattingScore) {
            opponentTotal = opponentTotal + run;
        }
        if (myTotal > opponentTotal) {
            return myJid;
        } else if (opponentTotal > myTotal) {
            return opponentJid;
        } else {
            return GameConstant.GAME_TIE;
        }
    }

    private void dispatchScoreUpdate(int currentBall, int currentBallScore, int totalRun, int aggression, boolean isInningsChanged, boolean isOut, int myDelta, int opponentDelta, boolean isMyAnswerCorrect, boolean isOpponentAnswerCorrect) {
        ISFSObject isfsObject = new SFSObject();
        isfsObject.putUtfString(GameConstant.PARAM_NAME_BATSMAN, batsManJid);
        isfsObject.putInt(GameConstant.PARAM_NAME_CURRENT_BALL, currentBall);
        isfsObject.putInt(GameConstant.PARAM_NAME_BALL_SCORE, currentBallScore);
        isfsObject.putInt(GameConstant.PARAM_NAME_TOTAL_SCORE, totalRun);
        isfsObject.putInt(GameConstant.PARAM_CURRENT_AGGRESSION, aggression);
        isfsObject.putBool(GameConstant.PARAM_NAME_INNINGS_CHANGED, isInningsChanged);
        isfsObject.putBool(GameConstant.PARAM_NAME_OUT, isOut);

        isfsObject.putInt(GameConstant.PARAM_NAME_MY_DELTA, myDelta);
        isfsObject.putInt(GameConstant.PARAM_NAME_OPPONENT_DELTA, opponentDelta);
        isfsObject.putBool(GameConstant.PARAM_NAME_ME_CORRECT, isMyAnswerCorrect);
        isfsObject.putBool(GameConstant.PARAM_NAME_OPPONENT_CORRECT, isOpponentAnswerCorrect);
        gameListener.dispatchGameEvent(GameConstant.CMD_SCORE_UPDATE, isfsObject);
    }

    public boolean decideIfAnsweredCorrectly(int questionNumber, String selectedOption) {
        boolean isCorrect = false;
        QuestionModel model = questionModels.get(questionNumber - 1);
        String correctAnswer = model.getCorrectOption();
        isCorrect = correctAnswer.equalsIgnoreCase(selectedOption);
        return isCorrect;
    }

    private String getCorrectOption(int questionNumber) {
        QuestionModel model = questionModels.get(questionNumber - 1);
        String correctAnswer = model.getCorrectOption();
        return correctAnswer;
    }

    private int calculateScore(int delta) {
        int score = 0;
        if (delta > 0 && delta <= timeRules.get(0)) {
            score = pointRules.get(0);
        } else if (delta > timeRules.get(0) && delta <= timeRules.get(1)) {
            score = pointRules.get(1);
        } else if (delta > timeRules.get(1) && delta <= timeRules.get(2)) {
            score = pointRules.get(2);
        } else {
            score = 1;
        }
        return score;
    }

    public Intent getScoreJson(Context context, String winnerJid, boolean isIAmLeaving, boolean fiftyFiftyPowerUsed) {
           /*data for backend submission
    runs_scored
    sixes
    fours
    out: T/F
    match_status: w/l/t (won/lose/tie)
    balls_faced
    wicket_taken: T/F
    runs_conceded
    balls_bowled
    perfect_match_score: T/F
    match_points*/
        int totalRun = 0;
        int numberOfSixes = 0;
        int numberOfFours = 0;
        int ballsFaced = 0;
        boolean isOut = false;
        boolean isWicketTaken = false;
        String matchStatus = "";
        int runConceded = 0;
        int ballsBowled = 0;
        String myWinStatus = "";
        String opponentWinStatus = "";
        boolean perfectMatchScore = false;
        int matchPoints = 0;
        for (int run : myBattingScore) {
            totalRun = totalRun + run;
            if (run == 6) {
                numberOfSixes++;
            } else if (run == 4) {
                numberOfFours++;
            }
        }
        ballsFaced = myBattingScore.size();
        isOut = this.isGotOut;
        isWicketTaken = this.isWicketTaken;

        for (int run : myBowlingScore) {
            runConceded = runConceded + run;
        }
        ballsBowled = myBowlingScore.size();
        if (totalRun == 36) {
            perfectMatchScore = true;
        }
        for (int points : myPointsList) {
            matchPoints = matchPoints + points;
        }
        String runDiff = String.valueOf(Math.max(totalRun, runConceded) - Math.min(totalRun, runConceded));
        if (winnerJid.equals(myJid)) {
            matchStatus = "w";
            if (isBatting) {
                extraResultData = "You won by " + context.getResources().getQuantityString(R.plurals.small_s, Integer.parseInt(runDiff), Integer.parseInt(runDiff) + " run");
            } else {
                if (lastBall == 0) {
                    extraResultData = "You won on the last ball";
                } else {
                    extraResultData = "You won with " + context.getResources().getQuantityString(R.plurals.small_s, lastBall, lastBall + " ball") + " to spare";
                }
            }
        } else if (winnerJid.equals(opponentJid)) {
            matchStatus = "l";
            if (!isBatting) {
                extraResultData = "You lost by " + context.getResources().getQuantityString(R.plurals.small_s, Integer.parseInt(runDiff), Integer.parseInt(runDiff) + " run");
            } else {
                if (lastBall == 0) {
                    extraResultData = "You lost on the last ball";
                } else {
                    extraResultData = "You lost with " + context.getResources().getQuantityString(R.plurals.small_s, lastBall, lastBall + " ball") + " to spare";
                }
            }
        } else if (winnerJid.equals(GameConstant.GAME_TIE)) {
            matchStatus = "t";
        } else if (winnerJid.equals(GameConstant.GAME_ABANDONED)) {
            matchStatus = "a";
        } else if (winnerJid.equals(GameConstant.CONNECTION_ERROR)) {
            matchStatus = "e";
        }
        String winStatus = matchStatus.equals(GameConstant.GAME_ABANDONED) ? "w" : matchStatus;
        JSONObject scoreSubmitJsonObject = new JSONObject();
        try {
            JSONObject scoreObject = new JSONObject();
            scoreObject.put("runs_scored", totalRun);
            scoreObject.put("sixes", numberOfSixes);
            scoreObject.put("fours", numberOfFours);
            scoreObject.put("out", isOut);
            scoreObject.put("wicket_taken", isWicketTaken);
            scoreObject.put("match_status", winStatus);
            scoreObject.put("balls_faced", ballsFaced);
            scoreObject.put("runs_conceded", runConceded);
            scoreObject.put("balls_bowled", ballsBowled);
            scoreObject.put("perfect_match_score", perfectMatchScore);
            scoreObject.put("match_points", matchPoints);

            scoreSubmitJsonObject.put(Constants.REQUEST_PARAMETER_KEY_USER_NAME, TinyDB.getInstance(context).getString(TinyDB.KEY_USER_JID));
            scoreSubmitJsonObject.put(Constants.REQUEST_PARAMETER_KEY_PASSWORD, TinyDB.getInstance(context).getString(TinyDB.KEY_PASSWORD));
            scoreSubmitJsonObject.put(Constants.REQUEST_PARAMETER_KEY_APK_VERSION, CommonUtil.getBuildConfig());
            scoreSubmitJsonObject.put(Constants.REQUEST_PARAMETER_KEY_UDID, CommonUtil.getDeviceId(context));

            scoreSubmitJsonObject.put("match_details", scoreObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        myWinStatus = context.getResources().getQuantityString(R.plurals.small_s, totalRun, totalRun + " Run") + " in " + context.getResources().getQuantityString(R.plurals.small_s, ballsFaced, ballsFaced + " Ball");
        opponentWinStatus = context.getResources().getQuantityString(R.plurals.small_s, runConceded, runConceded + " Run") + " in " + context.getResources().getQuantityString(R.plurals.small_s, ballsBowled, ballsBowled + " Ball");
        String serverData = scoreSubmitJsonObject.toString();
        Log.d("max", "Server data " + serverData);
        Intent intent = GameResultActivity.getGameResultIntent(context, matchStatus, myWinStatus, opponentWinStatus, serverData, runDiff, myBattingScore, opponentBattingScore, isIAmLeaving, extraResultData, isOpponentBot, fiftyFiftyPowerUsed);
        return intent;
    }

    public AnswerModel getBotAnswerModel(int questionNumber) {
        AnswerModel model = botAnswerModel.get(questionNumber - 1);
        return model;
    }

    public String getMyJid() {
        return myJid;
    }

    public String getOpponentJid() {
        return opponentJid;
    }

    public String getBotJid() {
        return botjid;
    }

    public void setOpponentBot(String myJid, String opponentJid) {
        this.myJid = myJid;
        this.opponentJid = opponentJid;
        this.batsManJid = myJid;
        isBatting = true;
        this.isOpponentBot = true;
    }

    public boolean isOpponentBot() {
        return isOpponentBot;
    }

    public int getAggressionToOut() {
        return requiredAggression;
    }

    public ArrayList<Integer> getTimeRules() {
        return timeRules;
    }

    public ArrayList<Integer> getPointRules() {
        return pointRules;
    }

}