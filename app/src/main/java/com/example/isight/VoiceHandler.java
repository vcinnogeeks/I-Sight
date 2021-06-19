package com.example.isight;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class VoiceHandler extends UtteranceProgressListener implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private Context context;
    private boolean shouldSpeak, isReady, hasWaited;
    private ArrayList<String> toSpeak;
    private ArrayList<Integer> objectDistances;
    private Handler cooldownHandler;
    private Activity activity;

    private static final double FOCAL_LENGTH = 2.0;
    private static final int REAL_HEIGHT = 2000;
    private static final int SENSOR_HEIGHT = 5;
    private static final int IMG_HEIGHT = 300;
    static final int RECORD_PERM_CODE = 443;
    static final int RECOGNIZER_CODE = 322;

    private static final double MULTIPLIER = (FOCAL_LENGTH * REAL_HEIGHT * IMG_HEIGHT * 2)/(4 * 1000);
    private boolean isPermGranted = false;

    VoiceHandler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        textToSpeech = new TextToSpeech(context, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        toSpeak = new ArrayList<>();
        objectDistances = new ArrayList<Integer>();
        cooldownHandler = new Handler(context.getMainLooper());
        shouldSpeak = false;
        isReady = false;
        hasWaited = true;
        System.out.println("MULT" + MULTIPLIER);
    }

    void initRecognizer() {
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak");
        checkPermissions();
    }

    void checkPermissions() {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isPermGranted = true;
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO},RECORD_PERM_CODE);
            }
        }
    }

    void doSpeak() {
        if (toSpeak.size() == 0)
            return;

        Log.d("DoSpeak " , String.valueOf(shouldSpeak) + isReady);
        if (shouldSpeak() && isReady() && hasWaited) {
            String textToSpeak = getToSpeak(toSpeak);
            if (textToSpeak.length() > 0) {
                System.out.println("Speaking : " + textToSpeak);
                textToSpeech.speak(getToSpeak(toSpeak), TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString());
                shouldSpeak = false;
                hasWaited = false;

                cooldownHandler.postDelayed(() -> hasWaited = true, 6000);
            }
        }
    }

    public void queueText(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString());
    }

    public void forceText(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
    }

    void startRecognition() {
        if (isPermGranted) {
            forceText("Listening");
            activity.startActivityForResult(speechRecognizerIntent, RECOGNIZER_CODE);
        }
        else
            checkPermissions();
    }

    String getToSpeak(ArrayList<String> objectList) {

        String textToSpeak = "";

        for (int x = 0; x < objectList.size(); x++) {

            if (x == 0)
                textToSpeak += "I see a ";
            else
                textToSpeak += " and a ";
            textToSpeak += objectList.get(x);
            textToSpeak += " at " + objectDistances.get(x) + " steps";
        }

        Log.d("RETURNING", "textToSpeak : " + textToSpeak);

        return textToSpeak;
    }

     boolean shouldSpeak() {
        return shouldSpeak;
    }

    boolean isReady() {
        return isReady;
    }

    boolean hasWaited() {
        return hasWaited;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true;
            shouldSpeak = true;
            textToSpeech.setLanguage(Locale.CANADA);
            textToSpeech.setOnUtteranceProgressListener(this);
        }
    }

    void addToList(String text, RectF location) {
        toSpeak.add(text);
        calculateDistance(location);
    }

    void calculateDistance(RectF location) {
        double objectHeight = location.bottom - location.top;
        int distance = (int) Math.ceil((1/objectHeight * MULTIPLIER));
        System.out.println("DIST: " + distance);
        objectDistances.add(distance);
    }

    @Override
    public void onStart(String utteranceId) {

    }

    @Override
    public void onDone(String utteranceId) {
        shouldSpeak = true;
        toSpeak.clear();
        objectDistances.clear();
    }

    @Override
    public void onError(String utteranceId) {
        shouldSpeak = true;
        toSpeak.clear();
        objectDistances.clear();
    }
}
