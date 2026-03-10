package com.neonvortex.game;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class PaintFactory {
    public Paint pPlayer, pPlayerInner, pGlow, pShieldRing;
    public Paint pOrbitInner, pOrbitOuter, pOrbitGlowInner, pOrbitGlowOuter;
    public Paint pObstacle, pObstacleCore, pObstacleGlow, pLightning;
    public Paint pOrbBody, pOrbGlow, pOrbShine;
    public Paint pDiamond, pDiamondGlow;
    public Paint pTrail, pParticle, pStarPaint, pVortex;
    public Paint pScore, pHighScore, pTitle, pSub, pGameOver, pRestart;
    public Paint pCombo, pNearMiss, pPopup, pPause, pLevel, pStat;
    public Paint pReverse, pFever;

    public PaintFactory() { init(); }

    private Paint mp(int c, Paint.Style s, float sw) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(c);p.setStyle(s);
        if(sw>0)p.setStrokeWidth(sw);return p;
    }
    private Paint tp(int c,float sz,boolean b) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(c);p.setTextSize(sz);
        p.setTextAlign(Paint.Align.CENTER);
        if(b)p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));return p;
    }

    private void init() {
        pOrbitInner=mp(0xFF00FFFF,Paint.Style.STROKE,5f);
        pOrbitOuter=mp(0xFFFF00FF,Paint.Style.STROKE,5f);
        pOrbitGlowInner=mp(0xFF00FFFF,Paint.Style.STROKE,20f);
        pOrbitGlowOuter=mp(0xFFFF00FF,Paint.Style.STROKE,20f);
        pPlayer=mp(Color.WHITE,Paint.Style.FILL,0);
        pPlayerInner=mp(Color.WHITE,Paint.Style.FILL,0);
        pGlow=mp(Color.WHITE,Paint.Style.FILL,0);
        pShieldRing=mp(0xFF4488FF,Paint.Style.STROKE,3f);
        pObstacle=mp(Color.WHITE,Paint.Style.STROKE,14f);pObstacle.setStrokeCap(Paint.Cap.ROUND);
        pObstacleCore=mp(Color.WHITE,Paint.Style.STROKE,6f);pObstacleCore.setStrokeCap(Paint.Cap.ROUND);
        pObstacleGlow=mp(Color.WHITE,Paint.Style.STROKE,24f);pObstacleGlow.setStrokeCap(Paint.Cap.ROUND);
        pLightning=mp(0xFFFFFFFF,Paint.Style.STROKE,8f);pLightning.setStrokeCap(Paint.Cap.ROUND);
        pOrbBody=mp(0xFFFFD700,Paint.Style.FILL,0);
        pOrbGlow=mp(0xFFFFD700,Paint.Style.FILL,0);
        pOrbShine=mp(Color.WHITE,Paint.Style.FILL,0);
        pDiamond=mp(0xFF00FFCC,Paint.Style.FILL,0);
        pDiamondGlow=mp(0xFF00FFCC,Paint.Style.FILL,0);
        pTrail=mp(Color.WHITE,Paint.Style.FILL,0);
        pParticle=mp(Color.WHITE,Paint.Style.FILL,0);
        pStarPaint=mp(Color.WHITE,Paint.Style.FILL,0);
        pVortex=mp(Color.WHITE,Paint.Style.STROKE,3f);
        pScore=tp(Color.WHITE,80,true);
        pHighScore=tp(0xFF888888,36,false);
        pTitle=tp(Color.WHITE,100,true);
        pSub=tp(0xFFAAAAAA,40,false);
        pGameOver=tp(0xFFFF4444,90,true);
        pRestart=tp(Color.WHITE,44,false);
        pCombo=tp(0xFFFFD700,50,true);
        pNearMiss=tp(0xFF00FFFF,44,true);
        pPopup=tp(Color.WHITE,36,true);
        pPause=tp(Color.WHITE,50,true);
        pLevel=tp(Color.WHITE,70,true);
        pStat=tp(0xFFAAAAAA,34,false);
        pReverse=tp(0xFFFF8800,48,true);
        pFever=tp(0xFFFF00FF,60,true);
    }

    public Paint makePaint(int c, Paint.Style s, float sw) { return mp(c,s,sw); }
    public Paint makeText(int c, float sz, boolean b) { return tp(c,sz,b); }
}
