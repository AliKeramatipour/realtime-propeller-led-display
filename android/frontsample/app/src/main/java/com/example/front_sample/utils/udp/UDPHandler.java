package com.example.front_sample.utils.udp;

import android.util.Log;

import com.example.front_sample.config.Config;
import com.example.front_sample.utils.ImageHandler;
import com.example.front_sample.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class UDPHandler {
    private static volatile UDPHandler instance = new UDPHandler();
    private UDPServerThread udpServerThread = null;
    private volatile List<int[][]> angularContext = new ArrayList<>();
    private String log = "";
    private volatile int frameDuration = 500;

    private UDPHandler() {
        this.angularContext.add(new int[Config.MAX_DEGREE][Config.NUM_OF_LEDS]);
    }

    public static UDPHandler getInstance() {
        return instance;
    }

    public void startServer() {
        if(udpServerThread == null) {
            udpServerThread = new UDPServerThread(Config.ANDROID_PORT, Config.BOARD_PORT, this);
            udpServerThread.start();
        }
    }

    public void stopServer() throws Exception {
        if(udpServerThread != null){
            udpServerThread.setRunning(false);
            udpServerThread = null;
        }else{
            throw new Exception("Server is not running!");
        }
    }

    public synchronized List<int[][]> getAngularContext() {
        return this.angularContext;
    }

    public synchronized int[][] getCurrentFrame() {
        return this.angularContext.get(0);
    }

    public synchronized boolean nextFrame() {
        return this.popFirstAngularContext(); // returns false if context size is 1 and there's no next
    }

    public synchronized boolean popFirstAngularContext() {  // returns false if context size is 1
        if (this.angularContext.size() > 1) {
            this.angularContext.add(this.angularContext.get(0));  // repeat
            this.angularContext.remove(0);
            return true;
        }else{
            return false;
        }
    }

    public synchronized void setAngularContext(List<int[][]> newAngularContext){  // Board won't show it immediately
        this.angularContext = newAngularContext;
    }

    public synchronized void setAngularContext(int[][] angularPic){  // Will send and show it immediately
        List<int[][]> newContext = new ArrayList<>();
        newContext.add(angularPic);
        this.angularContext = newContext;
        sendPictureAsync(angularPic);
    }

    public synchronized void setSquareContext(List<int[][]> newSquareContext){  // Board won't show it immediately
        List<int[][]> newAngularContext = new ArrayList<>();
        for (int[][] ctx:newSquareContext) {
            newAngularContext.add(ImageHandler.squareToAngular(ctx));
        }
        this.angularContext = newAngularContext;
    }

    public synchronized void setSquareContext(int[][] squarePic) {  // Will send and show it immediately
        List<int[][]> newAngularContext = new ArrayList<>();
        int[][] angularPic = ImageHandler.squareToAngular(squarePic);
        newAngularContext.add(angularPic);
        this.angularContext = newAngularContext;
        sendPictureAsync(angularPic);
    }

    public synchronized void setFrameDuration(int frameDuration) throws Exception {
        if(frameDuration < 10 || frameDuration > 5000) {
            throw new Exception("10 < FrameDuration < 5000");
        }
        this.frameDuration = frameDuration;
    }

    public synchronized int getFrameDuration() {
        return this.frameDuration;
    }

    private void sendPictureAsync(int[][] angularPic) {
        class AsyncThread extends Thread {
            private int[][] angularPic;
            private UDPHandler parent;
            private AsyncThread(int[][] _angularPic, UDPHandler _parent) {
                angularPic = _angularPic;
                parent = _parent;
            }
            public void run() {
                try {
                    udpServerThread.sendPictureImmediately(angularPic);
                } catch (Exception e) {
                    parent.log(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        AsyncThread asyncThread = new AsyncThread(angularPic, this);
        asyncThread.start();
    }

//    private void updateState(final String state){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                textViewState.setText(state);
//            }
//        });
//    }

//    private void updatePrompt(final String prompt){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                textViewPrompt.append(prompt);
//            }
//        });
//    }

    public void receiveBroadcast() {

    }

    public synchronized String getLogChange() {
        String logCopy = log;
        log = "";
        return logCopy;
    }

    synchronized void log(String newLog) {
        log = log.concat("\n" + newLog);
        Log.e("UDP", newLog);
    }
}


