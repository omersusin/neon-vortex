package com.neonvortex.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

public class Renderer {
    private final GameState gs;
    private final PaintFactory pf;
    private final Random rand=new Random();

    public Renderer(GameState gs, PaintFactory pf) {
        this.gs=gs; this.pf=pf;
    }

    public void initStars() {
        gs.stars=new float[150][3];
        for(int i=0;i<150;i++){
            gs.stars[i][0]=rand.nextFloat()*gs.screenW;
            gs.stars[i][1]=rand.nextFloat()*gs.screenH;
            gs.stars[i][2]=rand.nextFloat()*2+1;
        }
    }

    public void drawBackground(Canvas c) {
        int bgR=(int)(10+8*Math.sin(gs.bgHue*0.017));
        int bgG=(int)(10+5*Math.sin(gs.bgHue*0.017+2));
        int bgB=(int)(46+15*Math.sin(gs.bgHue*0.017+4));
        if(gs.feverMode){
            bgR=(int)(20+20*Math.sin(gs.feverHue*0.017));
            bgG=(int)(5+15*Math.sin(gs.feverHue*0.017+2));
            bgB=(int)(40+30*Math.sin(gs.feverHue*0.017+4));
        }
        c.drawColor(Color.rgb(bgR,bgG,bgB));
    }

    public void drawStars(Canvas c) {
        if(gs.stars==null)return;
        for(float[] s:gs.stars){
            int a=(int)(150+105*Math.sin(gs.animTime*0.02+s[0]));
            pf.pStarPaint.setAlpha(Math.min(255,Math.max(0,a)));
            c.drawCircle(s[0],s[1],s[2],pf.pStarPaint);
        }
    }

    public void drawVortex(Canvas c) {
        int spirals=gs.feverMode?6:4;
        for(int i=0;i<spirals;i++){
            float ang=gs.vortexAngle+i*(360f/spirals);
            Path path=new Path();
            for(int j=0;j<=50;j++){
                float t=j/50f;
                float a=(float)Math.toRadians(ang+t*360);
                float rad=gs.innerRadius*0.6f*t;
                float x=gs.centerX+rad*(float)Math.cos(a);
                float y=gs.centerY+rad*(float)Math.sin(a);
                if(j==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            float bl=(float)(Math.sin(gs.animTime*0.02+i)*0.5+0.5);
            if(gs.feverMode)pf.pVortex.setColor(Color.HSVToColor(new float[]{(gs.feverHue+i*60)%360,1,1}));
            else pf.pVortex.setColor(gs.blendCol(0xFF7B2FBE,0xFF00BFFF,bl));
            pf.pVortex.setAlpha((int)(80+40*Math.sin(gs.animTime*0.03+i)));
            pf.pVortex.setStrokeWidth(2+i*0.5f);
            c.drawPath(path,pf.pVortex);
        }
    }

    public void drawOrbits(Canvas c) {
        if(gs.feverMode){
            int ci=Color.HSVToColor(new float[]{gs.feverHue,1,1});
            int co=Color.HSVToColor(new float[]{(gs.feverHue+180)%360,1,1});
            pf.pOrbitGlowInner.setColor(ci);pf.pOrbitInner.setColor(ci);
            pf.pOrbitGlowOuter.setColor(co);pf.pOrbitOuter.setColor(co);
        }else{
            pf.pOrbitGlowInner.setColor(0xFF00FFFF);pf.pOrbitInner.setColor(0xFF00FFFF);
            pf.pOrbitGlowOuter.setColor(0xFFFF00FF);pf.pOrbitOuter.setColor(0xFFFF00FF);
        }
        pf.pOrbitGlowInner.setAlpha((int)(30+15*Math.sin(gs.animTime*0.03)));
        c.drawCircle(gs.centerX,gs.centerY,gs.innerRadius,pf.pOrbitGlowInner);
        pf.pOrbitInner.setAlpha((int)(180+75*Math.sin(gs.animTime*0.03)));
        c.drawCircle(gs.centerX,gs.centerY,gs.innerRadius,pf.pOrbitInner);
        pf.pOrbitGlowOuter.setAlpha((int)(30+15*Math.sin(gs.animTime*0.03+1)));
        c.drawCircle(gs.centerX,gs.centerY,gs.outerRadius,pf.pOrbitGlowOuter);
        pf.pOrbitOuter.setAlpha((int)(180+75*Math.sin(gs.animTime*0.03+1)));
        c.drawCircle(gs.centerX,gs.centerY,gs.outerRadius,pf.pOrbitOuter);
    }

    public void drawMenu(Canvas c) {
        pf.pTitle.setColor(0xFF00FFFF);
        pf.pTitle.setAlpha((int)(200+55*Math.sin(gs.animTime*0.05)));
        c.drawText("NEON",gs.centerX,gs.centerY-180,pf.pTitle);
        pf.pTitle.setColor(0xFFFF00FF);
        pf.pTitle.setAlpha((int)(200+55*Math.sin(gs.animTime*0.05+1)));
        c.drawText("VORTEX",gs.centerX,gs.centerY-80,pf.pTitle);
        pf.pSub.setTextSize(40);
        pf.pSub.setAlpha((int)(Math.sin(gs.animTime*0.04)*80+175));
        c.drawText("TAP TO START",gs.centerX,gs.centerY+60,pf.pSub);
        if(gs.highScore>0){pf.pHighScore.setAlpha(200);
            c.drawText("BEST: "+gs.highScore,gs.centerX,gs.centerY+130,pf.pHighScore);}
        pf.pSub.setTextSize(26);pf.pSub.setAlpha(150);
        c.drawText("Single Tap = Switch Orbit",gs.centerX,gs.centerY+200,pf.pSub);
        c.drawText("Double Tap = Reverse Direction",gs.centerX,gs.centerY+236,pf.pSub);
        c.drawText("Collect orbs & diamonds",gs.centerX,gs.centerY+272,pf.pSub);
        c.drawText("5+ Combo = FEVER MODE!",gs.centerX,gs.centerY+308,pf.pSub);
    }

    public void drawHUD(Canvas c) {
        pf.pScore.setAlpha(220);
        c.drawText(String.valueOf(gs.score),gs.centerX,120,pf.pScore);
        if(gs.combo>1){
            pf.pCombo.setAlpha((int)(200+55*Math.sin(gs.animTime*0.1)));
            pf.pCombo.setTextSize(Math.min(70,40+gs.combo*3));
            c.drawText("x"+String.format("%.1f",gs.scoreMultiplier)+" COMBO!",gs.centerX,185,pf.pCombo);
        }
        if(gs.feverMode){
            pf.pFever.setColor(Color.HSVToColor(new float[]{gs.feverHue,1,1}));
            pf.pFever.setAlpha((int)(200+55*Math.sin(gs.animTime*0.15)));
            pf.pFever.setTextSize(35);
            c.drawText("FEVER x3",gs.centerX,225,pf.pFever);
        }
        if(gs.nearMissTextTimer>0){
            pf.pNearMiss.setAlpha((int)(gs.nearMissTextTimer/45f*255));
            c.drawText(gs.nearMissText,gs.centerX,gs.centerY+gs.outerRadius+80,pf.pNearMiss);
        }
        pf.pPause.setTextSize(40);pf.pPause.setAlpha(150);
        pf.pPause.setTextAlign(Paint.Align.RIGHT);
        c.drawText("||",gs.screenW-40,60,pf.pPause);
        pf.pPause.setTextAlign(Paint.Align.CENTER);
        if(gs.moveDirection==-1){
            pf.pReverse.setAlpha((int)(150+100*Math.sin(gs.animTime*0.08)));
            pf.pReverse.setTextSize(26);
            c.drawText("< REVERSE >",gs.centerX,gs.screenH-100,pf.pReverse);
        }
        float barY=gs.screenH-60;
        if(gs.shieldActive)drawPowerBar(c,30,barY,gs.shieldTimer/300f,0xFF4488FF,"SHIELD");
        if(gs.slowmoActive)drawPowerBar(c,30,barY-25,gs.slowmoTimer/240f,0xFF44FF44,"SLOW");
        if(gs.magnetActive)drawPowerBar(c,30,barY-50,gs.magnetTimer/300f,0xFFFFAA00,"MAGNET");
    }

    private void drawPowerBar(Canvas c,float x,float y,float pct,int col,String label){
        Paint bg=pf.makePaint(0xFF333333,Paint.Style.FILL,0);
        float w=gs.screenW-60;
        c.drawRect(x,y,x+w,y+16,bg);
        Paint fg=pf.makePaint(col,Paint.Style.FILL,0);fg.setAlpha(200);
        c.drawRect(x,y,x+w*pct,y+16,fg);
        Paint tl=pf.makeText(Color.WHITE,14,false);
        tl.setTextAlign(Paint.Align.LEFT);tl.setAlpha(200);
        c.drawText(label,x+5,y+13,tl);
    }

    public void drawPauseScreen(Canvas c) {
        c.drawColor(Color.argb(180,0,0,0));
        pf.pPause.setTextSize(80);pf.pPause.setAlpha(255);
        pf.pPause.setTextAlign(Paint.Align.CENTER);
        c.drawText("PAUSED",gs.centerX,gs.centerY-40,pf.pPause);
        pf.pSub.setAlpha((int)(Math.sin(gs.animTime*0.04)*80+175));
        pf.pSub.setTextSize(40);
        c.drawText("TAP TO RESUME",gs.centerX,gs.centerY+40,pf.pSub);
    }

    public void drawGameOverUI(Canvas c) {
        c.drawColor(Color.argb(180,0,0,0));
        pf.pGameOver.setAlpha((int)(200+55*Math.sin(gs.animTime*0.05)));
        c.drawText("GAME OVER",gs.centerX,gs.centerY-180,pf.pGameOver);
        pf.pScore.setAlpha(255);pf.pScore.setTextSize(90);
        c.drawText(String.valueOf(gs.score),gs.centerX,gs.centerY-70,pf.pScore);
        pf.pScore.setTextSize(80);
        if(gs.isNewBest){pf.pHighScore.setColor(0xFFFFD700);
            c.drawText("NEW BEST!",gs.centerX,gs.centerY-10,pf.pHighScore);
            pf.pHighScore.setColor(0xFF888888);
        }else{pf.pHighScore.setAlpha(200);
            c.drawText("BEST: "+gs.highScore,gs.centerX,gs.centerY-10,pf.pHighScore);}
        pf.pStat.setAlpha(180);
        c.drawText("Time: "+String.format("%.1f",gs.survivalTime)+"s",gs.centerX,gs.centerY+50,pf.pStat);
        c.drawText("Orbs: "+gs.totalOrbs+"  Diamonds: "+gs.totalDiamonds,gs.centerX,gs.centerY+90,pf.pStat);
        c.drawText("Near Miss: "+gs.totalNearMiss+"  Reverses: "+gs.totalReverse,gs.centerX,gs.centerY+130,pf.pStat);
        c.drawText("Level: "+gs.level,gs.centerX,gs.centerY+170,pf.pStat);
        float ra=(float)(Math.sin(gs.animTime*0.04)*0.3+0.7);
        pf.pRestart.setAlpha((int)(ra*255));
        c.drawText("TAP TO RETRY",gs.centerX,gs.centerY+240,pf.pRestart);
    }

    public void drawOverlays(Canvas c) {
        if(gs.levelPopTimer>0){
            pf.pLevel.setColor(gs.hsvColor(gs.bgHue+180,0.8f,1f));
            pf.pLevel.setAlpha((int)(gs.levelPopTimer/120f*255));
            c.drawText("LEVEL "+gs.level,gs.centerX,gs.centerY-gs.outerRadius-60,pf.pLevel);
        }
        if(gs.deathFlash>0)c.drawColor(Color.argb((int)(gs.deathFlash*200),255,255,255));
        if(gs.nearMissFlash>0){
            int nr=gs.playerOnOuter?255:0,ng=gs.playerOnOuter?0:255;
            c.drawColor(Color.argb((int)(gs.nearMissFlash*30),nr,ng,255));
        }
        if(gs.reverseFlash>0)c.drawColor(Color.argb((int)(gs.reverseFlash*40),255,136,0));
        if(gs.feverFlash>0)c.drawColor(Color.argb((int)(gs.feverFlash*50),255,0,255));
    }
}
