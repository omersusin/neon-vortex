package com.neonvortex.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Player {
    private final GameState gs;
    private final PaintFactory pf;

    public Player(GameState gs, PaintFactory pf) {
        this.gs=gs; this.pf=pf;
    }

    public void update(float timeMult) {
        gs.currentSpeed=(gs.baseSpeed+(gs.playTime/600f)*3f)*timeMult*gs.moveDirection;
        gs.playerAngle+=gs.currentSpeed;
        if(gs.playerAngle>=360)gs.playerAngle-=360;
        if(gs.playerAngle<0)gs.playerAngle+=360;
    }

    public float getX() {
        float rad=gs.playerOnOuter?gs.outerRadius:gs.innerRadius;
        return gs.centerX+rad*gs.cos(gs.playerAngle);
    }
    public float getY() {
        float rad=gs.playerOnOuter?gs.outerRadius:gs.innerRadius;
        return gs.centerY+rad*gs.sin(gs.playerAngle);
    }

    public void draw(Canvas c) {
        float px=getX(), py=getY();
        int gc=gs.shieldActive?0xFF4488FF:(gs.playerOnOuter?0xFFFF00FF:0xFF00FFFF);
        if(gs.feverMode)gc=Color.HSVToColor(new float[]{gs.feverHue,1,1});
        pf.pGlow.setColor(gc);
        pf.pGlow.setAlpha(30);c.drawCircle(px,py,gs.playerSize+25,pf.pGlow);
        pf.pGlow.setAlpha(50);c.drawCircle(px,py,gs.playerSize+15,pf.pGlow);
        pf.pGlow.setAlpha(80);c.drawCircle(px,py,gs.playerSize+8,pf.pGlow);
        c.drawCircle(px,py,gs.playerSize,pf.pPlayer);
        int ic=gs.playerOnOuter?0xFFFF88FF:0xFF88FFFF;
        if(gs.feverMode)ic=Color.HSVToColor(new float[]{(gs.feverHue+90)%360,0.5f,1});
        pf.pPlayerInner.setColor(ic);
        c.drawCircle(px,py,gs.playerSize*0.5f,pf.pPlayerInner);
        if(gs.shieldActive){
            float sa=(float)(Math.sin(gs.animTime*0.1)*0.3+0.7);
            pf.pShieldRing.setAlpha((int)(sa*200));pf.pShieldRing.setStrokeWidth(3);
            c.drawCircle(px,py,gs.playerSize+18,pf.pShieldRing);
            pf.pShieldRing.setAlpha((int)(sa*100));pf.pShieldRing.setStrokeWidth(6);
            c.drawCircle(px,py,gs.playerSize+22,pf.pShieldRing);
        }
        if(gs.moveDirection==-1){
            Paint ar=pf.makeText(0xFFFF8800,gs.playerSize*1.5f,true);
            ar.setAlpha((int)(150+100*Math.sin(gs.animTime*0.1)));
            c.drawText("\u25C0",px,py-gs.playerSize-10,ar);
        }
    }

    public void switchOrbit(ParticleSystem ps, android.os.Vibrator vib) {
        gs.playerOnOuter=!gs.playerOnOuter;
        int col=gs.playerOnOuter?0xFFFF00FF:0xFF00FFFF;
        for(int i=0;i<10;i++)ps.spawnParticle(getX(),getY(),col,true);
        gs.shakeMag=3f;
        try{if(vib!=null)vib.vibrate(15);}catch(Exception e){}
    }

    public void reverse(ParticleSystem ps, android.os.Vibrator vib) {
        gs.moveDirection*=-1;gs.totalReverse++;gs.reverseFlash=1f;
        for(int i=0;i<12;i++)ps.spawnParticle(getX(),getY(),0xFFFF8800,true);
        ps.addPopup(getX(),getY()-50,"REVERSE!",0xFFFF8800);
        gs.shakeMag=5f;
        try{if(vib!=null)vib.vibrate(30);}catch(Exception e){}
    }
}
