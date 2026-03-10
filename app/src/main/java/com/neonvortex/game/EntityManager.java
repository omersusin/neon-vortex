package com.neonvortex.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.Iterator;
import java.util.Random;

public class EntityManager {
    private final GameState gs;
    private final PaintFactory pf;
    private final ParticleSystem ps;
    private final Random rand=new Random();
    private android.os.Vibrator vib;

    public EntityManager(GameState gs, PaintFactory pf, ParticleSystem ps) {
        this.gs=gs; this.pf=pf; this.ps=ps;
    }

    public void setVibrator(android.os.Vibrator v){this.vib=v;}
    private void vib(long ms){try{if(vib!=null)vib.vibrate(ms);}catch(Exception e){}}

    public void spawnObstacle() {
        float[] o=new float[8];
        o[0]=rand.nextFloat()*360;o[1]=rand.nextBoolean()?1:0;
        o[2]=(rand.nextFloat()*0.8f+0.3f)*(rand.nextBoolean()?1:-1);
        o[3]=20+rand.nextFloat()*30;o[4]=300+rand.nextFloat()*200;
        o[5]=0;o[6]=0;o[7]=0;
        if(gs.playTime>1800&&rand.nextFloat()<0.15f){
            o[6]=1;o[3]=15+rand.nextFloat()*20;o[4]=80+rand.nextFloat()*60;o[7]=o[4];
        }
        gs.obstacles.add(o);
        if(gs.playTime>600&&rand.nextFloat()<0.3f){
            float[] o2=new float[8];
            o2[0]=(o[0]+120+rand.nextFloat()*120)%360;o2[1]=o[1]==1?0:1;
            o2[2]=(rand.nextFloat()*0.6f+0.3f)*(rand.nextBoolean()?1:-1);
            o2[3]=20+rand.nextFloat()*25;o2[4]=300;o2[5]=0;o2[6]=0;o2[7]=0;
            gs.obstacles.add(o2);
        }
        if(gs.playTime>1200&&rand.nextFloat()<0.2f){
            float[] o3=new float[8];
            o3[0]=(o[0]+180)%360;o3[1]=o[1];o3[2]=-o[2];
            o3[3]=25+rand.nextFloat()*20;o3[4]=250;o3[5]=0;o3[6]=0;o3[7]=0;
            gs.obstacles.add(o3);
        }
    }

    public void spawnOrb() {
        gs.orbs.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.4f+0.2f)*(rand.nextBoolean()?1:-1),rand.nextFloat()*6.28f,400});
    }

    public void spawnDiamond() {
        gs.diamonds.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.3f+0.1f)*(rand.nextBoolean()?1:-1),rand.nextFloat()*6.28f,350});
    }

    public void spawnPowerup() {
        gs.powerups.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.3f+0.1f)*(rand.nextBoolean()?1:-1),rand.nextInt(3),500,0});
    }

    public boolean updateObstacles(float tm, float px, float py, float feverMult) {
        Iterator<float[]> oi=gs.obstacles.iterator();
        while(oi.hasNext()){
            float[] o=oi.next();
            o[0]+=o[2]*tm;
            if(o[0]>=360)o[0]-=360;if(o[0]<0)o[0]+=360;
            if(--o[4]<=0){oi.remove();continue;}
            if(o[6]==1){o[7]-=1;if(o[7]<=0){oi.remove();continue;}}
            if((o[1]==1)==gs.playerOnOuter){
                float ad=Math.abs(gs.normAngle(gs.playerAngle-o[0]));
                float half=o[3]/2+5;
                if(ad<half){
                    if(gs.shieldActive){
                        gs.shieldActive=false;gs.shieldTimer=0;gs.shakeMag=15f;
                        for(int i=0;i<20;i++)ps.spawnParticle(px,py,0xFF4488FF,false);
                        ps.addPopup(px,py-50,"SHIELD!",0xFF4488FF);
                        oi.remove();vib(40);continue;
                    }
                    return true; // player died
                }
                if(ad<half+10&&o[5]==0){
                    o[5]=1;gs.totalNearMiss++;
                    int pts=(int)(25*gs.scoreMultiplier*feverMult);gs.score+=pts;
                    gs.nearMissFlash=1f;gs.nearMissText="NEAR MISS! +"+pts;
                    gs.nearMissTextTimer=45;gs.shakeMag=8f;vib(15);
                    float oR=(o[1]==1)?gs.outerRadius:gs.innerRadius;
                    float ox=gs.centerX+oR*gs.cos(o[0]),oy=gs.centerY+oR*gs.sin(o[0]);
                    for(int i=0;i<8;i++)ps.spawnParticle(ox,oy,0xFF00FFFF,true);
                    ps.addPopup(ox,oy-30,"+"+pts,0xFF00FFFF);
                }
            }
        }
        return false;
    }

    public void updateOrbs(float tm, float feverMult) {
        Iterator<float[]> ri=gs.orbs.iterator();
        while(ri.hasNext()){
            float[] o=ri.next();
            o[0]+=o[2]*tm;if(o[0]>=360)o[0]-=360;
            o[3]+=0.1f;if(--o[4]<=0){ri.remove();continue;}
            if((o[1]==1)==gs.playerOnOuter&&Math.abs(gs.normAngle(gs.playerAngle-o[0]))<12){
                gs.combo++;gs.totalOrbs++;gs.comboTimer=180;
                gs.scoreMultiplier=1+(gs.combo-1)*0.5f;
                int pts=(int)(50*gs.scoreMultiplier*feverMult);gs.score+=pts;
                float oR=(o[1]==1)?gs.outerRadius:gs.innerRadius;
                float ox=gs.centerX+oR*gs.cos(o[0]),oy=gs.centerY+oR*gs.sin(o[0]);
                for(int i=0;i<15;i++)ps.spawnParticle(ox,oy,0xFFFFD700,false);
                ps.addPopup(ox,oy-30,"+"+pts,0xFFFFD700);vib(20);ri.remove();
            }
        }
    }

    public void updateDiamonds(float tm, float feverMult) {
        Iterator<float[]> di=gs.diamonds.iterator();
        while(di.hasNext()){
            float[] d=di.next();
            d[0]+=d[2]*tm;if(d[0]>=360)d[0]-=360;
            d[3]+=0.12f;if(--d[4]<=0){di.remove();continue;}
            if((d[1]==1)==gs.playerOnOuter&&Math.abs(gs.normAngle(gs.playerAngle-d[0]))<14){
                gs.totalDiamonds++;gs.combo+=2;gs.comboTimer=240;
                gs.scoreMultiplier=1+(gs.combo-1)*0.5f;
                int pts=(int)(500*gs.scoreMultiplier*feverMult);gs.score+=pts;
                float dR=(d[1]==1)?gs.outerRadius:gs.innerRadius;
                float dx=gs.centerX+dR*gs.cos(d[0]),dy=gs.centerY+dR*gs.sin(d[0]);
                for(int i=0;i<30;i++)ps.spawnParticle(dx,dy,0xFF00FFCC,false);
                for(int i=0;i<15;i++)ps.spawnParticle(dx,dy,Color.WHITE,false);
                ps.addPopup(dx,dy-40,"DIAMOND +"+pts,0xFF00FFCC);
                gs.shakeMag=10f;vib(60);di.remove();
            }
        }
    }

    public void updatePowerups(float tm) {
        Iterator<float[]> pi=gs.powerups.iterator();
        while(pi.hasNext()){
            float[] pw=pi.next();
            pw[0]+=pw[2]*tm;if(pw[0]>=360)pw[0]-=360;
            pw[5]+=0.08f;if(--pw[4]<=0){pi.remove();continue;}
            if((pw[1]==1)==gs.playerOnOuter&&Math.abs(gs.normAngle(gs.playerAngle-pw[0]))<15){
                int t=(int)pw[3];
                float pR=(pw[1]==1)?gs.outerRadius:gs.innerRadius;
                float pwx=gs.centerX+pR*gs.cos(pw[0]),pwy=gs.centerY+pR*gs.sin(pw[0]);
                if(t==0){gs.shieldActive=true;gs.shieldTimer=300;
                    for(int i=0;i<20;i++)ps.spawnParticle(pwx,pwy,0xFF4488FF,false);
                    ps.addPopup(pwx,pwy-30,"SHIELD!",0xFF4488FF);
                }else if(t==1){gs.slowmoActive=true;gs.slowmoTimer=240;
                    for(int i=0;i<20;i++)ps.spawnParticle(pwx,pwy,0xFF44FF44,false);
                    ps.addPopup(pwx,pwy-30,"SLOW-MO!",0xFF44FF44);
                }else{gs.magnetActive=true;gs.magnetTimer=300;
                    for(int i=0;i<20;i++)ps.spawnParticle(pwx,pwy,0xFFFFAA00,false);
                    ps.addPopup(pwx,pwy-30,"MAGNET!",0xFFFFAA00);
                }
                gs.shakeMag=5f;vib(30);pi.remove();
            }
        }
    }

    public void applyMagnet() {
        if(!gs.magnetActive)return;
        for(float[] o:gs.orbs)
            if((o[1]==1)==gs.playerOnOuter)o[0]+=gs.normAngle(gs.playerAngle-o[0])*0.05f;
        for(float[] d:gs.diamonds)
            if((d[1]==1)==gs.playerOnOuter)d[0]+=gs.normAngle(gs.playerAngle-d[0])*0.05f;
    }

    public void drawObstacles(Canvas c) {
        for(float[] o:gs.obstacles){
            float rad=o[1]==1?gs.outerRadius:gs.innerRadius;
            float start=o[0]-o[3]/2;
            RectF ov=new RectF(gs.centerX-rad,gs.centerY-rad,gs.centerX+rad,gs.centerY+rad);
            float p=(float)(Math.sin(gs.animTime*0.15)*0.5+0.5);
            if(o[6]==1){
                pf.pLightning.setColor(Color.WHITE);
                pf.pLightning.setAlpha((int)(200+55*Math.sin(gs.animTime*0.3)));
                pf.pLightning.setStrokeWidth(10);
                c.drawArc(ov,start,o[3],false,pf.pLightning);
                Paint lg=pf.makePaint(0xFF88CCFF,Paint.Style.STROKE,20f);
                lg.setAlpha((int)(80+40*Math.sin(gs.animTime*0.3)));
                c.drawArc(ov,start,o[3],false,lg);
            }else{
                int col=gs.blendCol(0xFFFF4444,0xFFFF8800,p);
                pf.pObstacleGlow.setColor(col);pf.pObstacleGlow.setAlpha(40);
                c.drawArc(ov,start,o[3],false,pf.pObstacleGlow);
                pf.pObstacle.setColor(col);pf.pObstacle.setAlpha(230);
                c.drawArc(ov,start,o[3],false,pf.pObstacle);
                pf.pObstacleCore.setColor(0xFFFFCC00);
                pf.pObstacleCore.setAlpha((int)(150+105*p));
                c.drawArc(ov,start,o[3],false,pf.pObstacleCore);
            }
        }
    }

    public void drawOrbs(Canvas c) {
        for(float[] o:gs.orbs){
            float rad=o[1]==1?gs.outerRadius:gs.innerRadius;
            float ox=gs.centerX+rad*gs.cos(o[0]),oy=gs.centerY+rad*gs.sin(o[0]);
            float p=(float)(1+0.2*Math.sin(o[3])),sz=gs.playerSize*0.8f*p;
            pf.pOrbGlow.setAlpha(40);c.drawCircle(ox,oy,sz+20,pf.pOrbGlow);
            pf.pOrbGlow.setAlpha(70);c.drawCircle(ox,oy,sz+10,pf.pOrbGlow);
            c.drawCircle(ox,oy,sz,pf.pOrbBody);
            pf.pOrbShine.setAlpha((int)(180*Math.abs(Math.sin(o[3]*2))));
            c.drawCircle(ox-sz*0.3f,oy-sz*0.3f,sz*0.3f,pf.pOrbShine);
        }
    }

    public void drawDiamonds(Canvas c) {
        for(float[] d:gs.diamonds){
            float rad=d[1]==1?gs.outerRadius:gs.innerRadius;
            float dx=gs.centerX+rad*gs.cos(d[0]),dy=gs.centerY+rad*gs.sin(d[0]);
            float p=(float)(1+0.3*Math.sin(d[3])),sz=gs.playerSize*1.2f*p;
            pf.pDiamondGlow.setAlpha(50);c.drawCircle(dx,dy,sz+22,pf.pDiamondGlow);
            pf.pDiamondGlow.setAlpha(90);c.drawCircle(dx,dy,sz+12,pf.pDiamondGlow);
            Path dm=new Path();
            dm.moveTo(dx,dy-sz);dm.lineTo(dx+sz*0.7f,dy);
            dm.lineTo(dx,dy+sz);dm.lineTo(dx-sz*0.7f,dy);dm.close();
            pf.pDiamond.setAlpha(220);c.drawPath(dm,pf.pDiamond);
            Paint sh=pf.makePaint(Color.WHITE,Paint.Style.FILL,0);
            sh.setAlpha((int)(200*Math.abs(Math.sin(d[3]*1.5))));
            c.drawCircle(dx-sz*0.2f,dy-sz*0.3f,sz*0.25f,sh);
        }
    }

    public void drawPowerups(Canvas c) {
        int[] colors={0xFF4488FF,0xFF44FF44,0xFFFFAA00};
        String[] labels={"S","T","M"};
        Paint pp=pf.makePaint(Color.WHITE,Paint.Style.FILL,0);
        Paint pl=pf.makeText(Color.WHITE,gs.playerSize,true);
        for(float[] pw:gs.powerups){
            int t=(int)pw[3];
            float rad=pw[1]==1?gs.outerRadius:gs.innerRadius;
            float px=gs.centerX+rad*gs.cos(pw[0]),py=gs.centerY+rad*gs.sin(pw[0]);
            float p=(float)(1+0.3*Math.sin(pw[5])),sz=gs.playerSize*1.1f*p;
            pp.setColor(colors[t]);pp.setAlpha(40);c.drawCircle(px,py,sz+18,pp);
            pp.setAlpha(80);c.drawCircle(px,py,sz+8,pp);
            pp.setAlpha(200);c.drawCircle(px,py,sz,pp);
            pl.setTextSize(sz*1.2f);pl.setAlpha(255);
            c.drawText(labels[t],px,py+sz*0.4f,pl);
        }
    }
}
