package com.neonvortex.game;

import java.util.ArrayList;

public class GameState {
    public static final int STATE_MENU=0, STATE_PLAYING=1, STATE_GAME_OVER=2, STATE_PAUSED=3, STATE_SETTINGS=4;
    public static final long DOUBLE_TAP_TIME=250;

    public Settings settings;
    public int gameState=STATE_MENU;
    public int screenW, screenH;
    public float centerX, centerY, innerRadius, outerRadius;
    public float playerAngle=0, playerSize=24f;
    public boolean playerOnOuter=false;
    public float baseSpeed=1.5f, currentSpeed;
    public int moveDirection=1;
    public float reverseFlash=0;

    public final ArrayList<float[]> obstacles=new ArrayList<>();
    public final ArrayList<float[]> orbs=new ArrayList<>();
    public final ArrayList<float[]> diamonds=new ArrayList<>();
    public final ArrayList<float[]> particles=new ArrayList<>();
    public final ArrayList<float[]> trail=new ArrayList<>();
    public final ArrayList<float[]> popups=new ArrayList<>();
    public final ArrayList<String> popupTexts=new ArrayList<>();
    public final ArrayList<float[]> powerups=new ArrayList<>();

    public float obstacleTimer=0, orbTimer=0, powerupTimer=0, diamondTimer=0;
    public int score=0, highScore=0, combo=0;
    public float comboTimer=0, scoreMultiplier=1;
    public float nearMissFlash=0, nearMissTextTimer=0;
    public String nearMissText="";
    public float shakeX=0, shakeY=0, shakeMag=0;
    public float vortexAngle=0, deathFlash=0, animTime=0, playTime=0;
    public boolean shieldActive=false, slowmoActive=false, magnetActive=false;
    public float shieldTimer=0, slowmoTimer=0, magnetTimer=0;
    public int totalOrbs=0, totalNearMiss=0, totalReverse=0, totalDiamonds=0;
    public float survivalTime=0;
    public boolean isNewBest=false;
    public int level=1;
    public float levelPopTimer=0, bgHue=0;
    public boolean feverMode=false;
    public float feverHue=0, feverFlash=0;
    public float[][] stars;

    public void initScreen(int w, int h) {
        screenW=w;screenH=h;centerX=w/2f;centerY=h/2f;
        innerRadius=w*0.25f;outerRadius=w*0.40f;playerSize=w*0.025f;
    }

    public void reset() {
        gameState=STATE_PLAYING;
        score=0;combo=0;comboTimer=0;scoreMultiplier=1;
        playTime=0;currentSpeed=baseSpeed;playerAngle=0;
        playerOnOuter=false;moveDirection=1;reverseFlash=0;
        feverMode=false;feverHue=0;feverFlash=0;
        obstacles.clear();orbs.clear();diamonds.clear();
        particles.clear();trail.clear();
        popups.clear();popupTexts.clear();powerups.clear();
        obstacleTimer=0;orbTimer=0;powerupTimer=0;diamondTimer=0;
        shakeMag=0;nearMissFlash=0;nearMissTextTimer=0;
        deathFlash=0;shieldActive=false;shieldTimer=0;
        slowmoActive=false;slowmoTimer=0;
        magnetActive=false;magnetTimer=0;
        totalOrbs=0;totalNearMiss=0;totalReverse=0;totalDiamonds=0;
        level=1;levelPopTimer=0;bgHue=0;isNewBest=false;
    }

    public float cos(float d){return(float)Math.cos(Math.toRadians(d));}
    public float sin(float d){return(float)Math.sin(Math.toRadians(d));}
    public float normAngle(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;}
    public int blendCol(int c1,int c2,float r){
        return android.graphics.Color.rgb(
            (int)(android.graphics.Color.red(c1)*(1-r)+android.graphics.Color.red(c2)*r),
            (int)(android.graphics.Color.green(c1)*(1-r)+android.graphics.Color.green(c2)*r),
            (int)(android.graphics.Color.blue(c1)*(1-r)+android.graphics.Color.blue(c2)*r));
    }
    public int hsvColor(float h,float s,float v){
        return android.graphics.Color.HSVToColor(new float[]{h%360,s,v});
    }
    public boolean vibEnabled(){return settings==null||settings.vibrationEnabled;}
    public float speedMult(){return settings==null?1f:settings.speedMult;}
    public float spawnMult(){return settings==null?1f:settings.spawnMult;}
    public boolean isDark(){return settings==null||settings.darkMode;}
    public boolean trailOn(){return settings==null||settings.trailEnabled;}
}
