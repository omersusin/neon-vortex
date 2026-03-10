package com.neonvortex.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

public class SettingsRenderer {
    private final GameState gs;
    private final Settings st;
    private final RectF[] diffB=new RectF[5];
    private final RectF[] vibB=new RectF[2];
    private final RectF[] themeB=new RectF[2];
    private final RectF[] trailB=new RectF[2];
    private RectF backB=new RectF();
    private boolean ready=false;
    private Paint titleP,labelP,bsP,bfP,btP,descP;

    public SettingsRenderer(GameState gs, Settings st) {
        this.gs=gs; this.st=st;
        titleP=mkT(0xFF00FFFF,70,true);
        labelP=mkT(0xFFFFFFFF,34,true);
        labelP.setTextAlign(Paint.Align.LEFT);
        bsP=new Paint(Paint.ANTI_ALIAS_FLAG);
        bsP.setStyle(Paint.Style.STROKE); bsP.setStrokeWidth(2);
        bfP=new Paint(Paint.ANTI_ALIAS_FLAG);
        bfP.setStyle(Paint.Style.FILL);
        btP=mkT(0xFFFFFFFF,26,true);
        descP=mkT(0xFF888888,22,false);
    }

    private Paint mkT(int c,float s,boolean b){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c);p.setTextSize(s);p.setTextAlign(Paint.Align.CENTER);
        if(b)p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
        return p;
    }

    private void layout(){
        float w=gs.screenW,h=gs.screenH,m=w*0.05f,bh=h*0.055f,g=w*0.02f;
        float bw5=(w-2*m-4*g)/5f,bw2=(w-2*m-g)/2f;
        float y=h*0.22f;
        for(int i=0;i<5;i++){float x=m+i*(bw5+g);diffB[i]=new RectF(x,y,x+bw5,y+bh);}
        y=h*0.40f;
        for(int i=0;i<2;i++){float x=m+i*(bw2+g);vibB[i]=new RectF(x,y,x+bw2,y+bh);}
        y=h*0.52f;
        for(int i=0;i<2;i++){float x=m+i*(bw2+g);themeB[i]=new RectF(x,y,x+bw2,y+bh);}
        y=h*0.64f;
        for(int i=0;i<2;i++){float x=m+i*(bw2+g);trailB[i]=new RectF(x,y,x+bw2,y+bh);}
        float bwb=w*0.5f;y=h*0.80f;
        backB=new RectF(w/2f-bwb/2,y,w/2f+bwb/2,y+bh*1.3f);
        ready=true;
    }

    public void draw(Canvas c,float anim){
        if(!ready)layout();
        boolean dk=st.darkMode;
        c.drawColor(dk?0xFF0A0A2E:0xFFE8E8F0);
        int tc=dk?0xFFFFFFFF:0xFF1A1A2E;

        titleP.setAlpha((int)(180+75*Math.sin(anim*0.05)));
        c.drawText("\u2699 SETTINGS",gs.centerX,gs.screenH*0.10f,titleP);
        labelP.setColor(tc);descP.setColor(dk?0xFF888888:0xFF666688);

        c.drawText("DIFFICULTY",gs.screenW*0.05f,gs.screenH*0.18f,labelP);
        String[] dn={"V.Easy","Easy","Normal","Hard","Insane"};
        int[] dc={0xFF44FF44,0xFF88FF44,0xFFFFDD44,0xFFFF8844,0xFFFF4444};
        for(int i=0;i<5;i++){boolean s=st.difficulty==i;
            if(s){bfP.setColor(dc[i]);bfP.setAlpha(200);c.drawRoundRect(diffB[i],14,14,bfP);}
            bsP.setColor(s?dc[i]:0xFF666666);c.drawRoundRect(diffB[i],14,14,bsP);
            btP.setColor(s?0xFF000000:tc);
            c.drawText(dn[i],diffB[i].centerX(),diffB[i].centerY()+10,btP);}
        String[] dd={"Very slow, few obstacles","Slower pace","Balanced gameplay","Fast & intense","Extreme!"};
        descP.setTextAlign(Paint.Align.CENTER);
        c.drawText(dd[st.difficulty],gs.centerX,gs.screenH*0.32f,descP);

        c.drawText("VIBRATION",gs.screenW*0.05f,gs.screenH*0.37f,labelP);
        drawTgl(c,vibB,new String[]{"ON","OFF"},st.vibrationEnabled?0:1,
            new int[]{0xFF44FF44,0xFFFF4444},tc);

        c.drawText("THEME",gs.screenW*0.05f,gs.screenH*0.49f,labelP);
        drawTgl(c,themeB,new String[]{"DARK","LIGHT"},st.darkMode?0:1,
            new int[]{0xFF6644CC,0xFFFFCC44},tc);

        c.drawText("TRAIL EFFECT",gs.screenW*0.05f,gs.screenH*0.61f,labelP);
        drawTgl(c,trailB,new String[]{"ON","OFF"},st.trailEnabled?0:1,
            new int[]{0xFF00FFFF,0xFFFF4444},tc);

        float ba=(float)(0.7+0.3*Math.sin(anim*0.04));
        bfP.setColor(0xFFFF00FF);bfP.setAlpha((int)(ba*180));
        c.drawRoundRect(backB,20,20,bfP);
        bsP.setColor(0xFFFF00FF);c.drawRoundRect(backB,20,20,bsP);
        btP.setColor(0xFFFFFFFF);btP.setTextSize(36);
        c.drawText("\u25C0 BACK",backB.centerX(),backB.centerY()+13,btP);
        btP.setTextSize(26);
    }

    private void drawTgl(Canvas c,RectF[] b,String[] n,int sel,int[] cl,int tc){
        for(int i=0;i<b.length;i++){boolean s=i==sel;
            if(s){bfP.setColor(cl[i]);bfP.setAlpha(200);c.drawRoundRect(b[i],14,14,bfP);}
            bsP.setColor(s?cl[i]:0xFF666666);c.drawRoundRect(b[i],14,14,bsP);
            btP.setColor(s?0xFF000000:tc);
            c.drawText(n[i],b[i].centerX(),b[i].centerY()+10,btP);}
    }

    public int handleTouch(float x,float y){
        if(!ready)return 0;
        for(int i=0;i<5;i++)if(diffB[i].contains(x,y)){st.difficulty=i;st.save();return 1;}
        if(vibB[0].contains(x,y)){st.vibrationEnabled=true;st.save();return 1;}
        if(vibB[1].contains(x,y)){st.vibrationEnabled=false;st.save();return 1;}
        if(themeB[0].contains(x,y)){st.darkMode=true;st.save();return 1;}
        if(themeB[1].contains(x,y)){st.darkMode=false;st.save();return 1;}
        if(trailB[0].contains(x,y)){st.trailEnabled=true;st.save();return 1;}
        if(trailB[1].contains(x,y)){st.trailEnabled=false;st.save();return 1;}
        if(backB.contains(x,y))return -1;
        return 0;
    }
}
