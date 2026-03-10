package com.neonvortex.game;

import android.graphics.Canvas;
import java.util.Iterator;
import java.util.Random;

public class ParticleSystem {
    private final GameState gs;
    private final PaintFactory pf;
    private final Random rand=new Random();

    public ParticleSystem(GameState gs, PaintFactory pf) {
        this.gs=gs; this.pf=pf;
    }

    public void spawnParticle(float x, float y, int color, boolean small) {
        float a=rand.nextFloat()*360;
        float sp=small?rand.nextFloat()*3+1:rand.nextFloat()*8+2;
        float sz=small?rand.nextFloat()*4+2:rand.nextFloat()*6+3;
        gs.particles.add(new float[]{x,y,sp*gs.cos(a),sp*gs.sin(a),color,1f,sz});
    }

    public void addPopup(float x, float y, String text, int color) {
        gs.popups.add(new float[]{x,y,1f,0,color});
        gs.popupTexts.add(text);
    }

    public void updateTrail(float px, float py, int color) {
        gs.trail.add(new float[]{px,py,color,1f});
        if(gs.trail.size()>30)gs.trail.remove(0);
    }

    public void updateParticles() {
        Iterator<float[]> it=gs.particles.iterator();
        while(it.hasNext()){float[] p=it.next();
            p[0]+=p[2];p[1]+=p[3];p[5]-=0.02f;p[6]*=0.97f;
            if(p[5]<=0||p[6]<0.5f)it.remove();}
    }

    public void updatePopups() {
        int idx=0;Iterator<float[]> it=gs.popups.iterator();
        while(it.hasNext()){float[] p=it.next();p[2]-=0.02f;p[3]-=1.5f;
            if(p[2]<=0){it.remove();gs.popupTexts.remove(idx);continue;}idx++;}
    }

    public void drawTrail(Canvas c) {
        for(int i=0;i<gs.trail.size();i++){float[] t=gs.trail.get(i);
            float alpha=(float)(i+1)/gs.trail.size();
            pf.pTrail.setColor((int)t[2]);pf.pTrail.setAlpha((int)(alpha*80));
            c.drawCircle(t[0],t[1],gs.playerSize*alpha*0.7f,pf.pTrail);}
    }

    public void drawParticles(Canvas c) {
        for(float[] p:gs.particles){pf.pParticle.setColor((int)p[4]);
            pf.pParticle.setAlpha((int)(p[5]*255));
            c.drawCircle(p[0],p[1],p[6],pf.pParticle);}
    }

    public void drawPopups(Canvas c) {
        for(int i=0;i<gs.popups.size();i++){float[] p=gs.popups.get(i);
            pf.pPopup.setColor((int)p[4]);pf.pPopup.setAlpha((int)(p[2]*255));
            c.drawText(gs.popupTexts.get(i),p[0],p[1]+p[3],pf.pPopup);}
    }
}
