package com.neonvortex.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int STATE_MENU=0, STATE_PLAYING=1, STATE_GAME_OVER=2, STATE_PAUSED=3;
    private static final long DOUBLE_TAP_TIME=250;

    private Thread gameThread;
    private volatile boolean isRunning;
    private int gameState=STATE_MENU;
    private int screenW, screenH;
    private float centerX, centerY, innerRadius, outerRadius;
    private float playerAngle=0, playerSize=24f;
    private boolean playerOnOuter=false;
    private float baseSpeed=1.5f, currentSpeed;
    private int moveDirection=1;
    private float reverseFlash=0;

    private long lastTapTime=0;
    private boolean waitingDoubleTap=false;
    private final Runnable singleTapAction=new Runnable(){
        @Override public void run(){if(waitingDoubleTap){waitingDoubleTap=false;switchOrbit();}}
    };
    private final android.os.Handler tapHandler=new android.os.Handler(android.os.Looper.getMainLooper());

    private final ArrayList<float[]> obstacles=new ArrayList<>();
    private final ArrayList<float[]> orbs=new ArrayList<>();
    private final ArrayList<float[]> diamonds=new ArrayList<>();
    private final ArrayList<float[]> particles=new ArrayList<>();
    private final ArrayList<float[]> trail=new ArrayList<>();
    private final ArrayList<float[]> popups=new ArrayList<>();
    private final ArrayList<String> popupTexts=new ArrayList<>();
    private final ArrayList<float[]> powerups=new ArrayList<>();

    private float obstacleTimer=0, orbTimer=0, powerupTimer=0, diamondTimer=0;
    private int score=0, highScore=0, combo=0;
    private float comboTimer=0, scoreMultiplier=1;
    private float nearMissFlash=0, nearMissTextTimer=0;
    private String nearMissText="";
    private float shakeX=0, shakeY=0, shakeMag=0;
    private float vortexAngle=0, deathFlash=0, animTime=0, playTime=0;
    private boolean shieldActive=false, slowmoActive=false, magnetActive=false;
    private float shieldTimer=0, slowmoTimer=0, magnetTimer=0;
    private int totalOrbs=0, totalNearMiss=0, totalReverse=0, totalDiamonds=0;
    private float survivalTime=0;
    private boolean isNewBest=false;
    private int level=1;
    private float levelPopTimer=0, bgHue=0;
    private boolean feverMode=false;
    private float feverHue=0, feverFlash=0;
    private float[][] stars;
    private final Random rand=new Random();
    private SharedPreferences prefs;
    private Vibrator vibrator;

    private Paint pPlayer, pScore, pHighScore, pTitle, pSub, pGameOver, pRestart;
    private Paint pCombo, pNearMiss, pParticle, pTrail, pGlow;
    private Paint pOrbitInner, pOrbitOuter, pOrbitGlowInner, pOrbitGlowOuter;
    private Paint pObstacle, pObstacleCore, pObstacleGlow;
    private Paint pOrbBody, pOrbGlow, pOrbShine, pVortex, pPlayerInner, pStarPaint;
    private Paint pPopup, pPause, pLevel, pStat, pShieldRing, pReverse, pFever;
    private Paint pDiamond, pDiamondGlow, pLightning;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        prefs=context.getSharedPreferences("neonvortex", Context.MODE_PRIVATE);
        highScore=prefs.getInt("highscore",0);
        vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        initPaints();
    }

    private void vib(long ms){try{if(vibrator!=null)vibrator.vibrate(ms);}catch(Exception e){}}

    private Paint mp(int c, Paint.Style s, float sw){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(c);p.setStyle(s);
        if(sw>0)p.setStrokeWidth(sw);return p;
    }
    private Paint tp(int c, float sz, boolean b){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(c);p.setTextSize(sz);
        p.setTextAlign(Paint.Align.CENTER);
        if(b)p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));return p;
    }

    private void initPaints(){
        pOrbitInner=mp(0xFF00FFFF,Paint.Style.STROKE,5f);
        pOrbitOuter=mp(0xFFFF00FF,Paint.Style.STROKE,5f);
        pOrbitGlowInner=mp(0xFF00FFFF,Paint.Style.STROKE,20f);
        pOrbitGlowOuter=mp(0xFFFF00FF,Paint.Style.STROKE,20f);
        pPlayer=mp(Color.WHITE,Paint.Style.FILL,0);
        pPlayerInner=mp(Color.WHITE,Paint.Style.FILL,0);
        pGlow=mp(Color.WHITE,Paint.Style.FILL,0);
        pShieldRing=mp(0xFF4488FF,Paint.Style.STROKE,3f);
        pObstacle=mp(Color.WHITE,Paint.Style.STROKE,14f);
        pObstacle.setStrokeCap(Paint.Cap.ROUND);
        pObstacleCore=mp(Color.WHITE,Paint.Style.STROKE,6f);
        pObstacleCore.setStrokeCap(Paint.Cap.ROUND);
        pObstacleGlow=mp(Color.WHITE,Paint.Style.STROKE,24f);
        pObstacleGlow.setStrokeCap(Paint.Cap.ROUND);
        pOrbBody=mp(0xFFFFD700,Paint.Style.FILL,0);
        pOrbGlow=mp(0xFFFFD700,Paint.Style.FILL,0);
        pOrbShine=mp(Color.WHITE,Paint.Style.FILL,0);
        pTrail=mp(Color.WHITE,Paint.Style.FILL,0);
        pParticle=mp(Color.WHITE,Paint.Style.FILL,0);
        pStarPaint=mp(Color.WHITE,Paint.Style.FILL,0);
        pVortex=mp(Color.WHITE,Paint.Style.STROKE,3f);
        pLightning=mp(0xFFFFFFFF,Paint.Style.STROKE,8f);
        pLightning.setStrokeCap(Paint.Cap.ROUND);
        pDiamond=mp(0xFF00FFCC,Paint.Style.FILL,0);
        pDiamondGlow=mp(0xFF00FFCC,Paint.Style.FILL,0);
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

    @Override public void surfaceCreated(SurfaceHolder holder){
        screenW=getWidth();screenH=getHeight();
        centerX=screenW/2f;centerY=screenH/2f;
        innerRadius=screenW*0.25f;outerRadius=screenW*0.40f;
        playerSize=screenW*0.025f;
        stars=new float[150][3];
        for(int i=0;i<150;i++){
            stars[i][0]=rand.nextFloat()*screenW;
            stars[i][1]=rand.nextFloat()*screenH;
            stars[i][2]=rand.nextFloat()*2+1;
        }
        isRunning=true;gameThread=new Thread(this);gameThread.start();
    }
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){
        screenW=w;screenH=ht;centerX=w/2f;centerY=ht/2f;
        innerRadius=w*0.25f;outerRadius=w*0.40f;
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder){
        isRunning=false;try{if(gameThread!=null)gameThread.join();}catch(InterruptedException e){}
    }

    @Override public void run(){
        long target=1000000000L/60;
        while(isRunning){
            long s=System.nanoTime();animTime++;
            if(gameState!=STATE_PAUSED)update();
            render();
            long sl=(target-(System.nanoTime()-s))/1000000;
            if(sl>0)try{Thread.sleep(sl);}catch(InterruptedException e){}
        }
    }

    private float cos(float d){return(float)Math.cos(Math.toRadians(d));}
    private float sin(float d){return(float)Math.sin(Math.toRadians(d));}

    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            float tx=event.getX(),ty=event.getY();
            long now=System.currentTimeMillis();
            if(gameState==STATE_MENU){startGame();}
            else if(gameState==STATE_PLAYING){
                if(tx>screenW-150&&ty<150){gameState=STATE_PAUSED;}
                else{
                    long diff=now-lastTapTime;
                    if(diff<DOUBLE_TAP_TIME){
                        tapHandler.removeCallbacks(singleTapAction);
                        waitingDoubleTap=false;reverseDirection();
                    }else{
                        waitingDoubleTap=true;
                        tapHandler.removeCallbacks(singleTapAction);
                        tapHandler.postDelayed(singleTapAction,DOUBLE_TAP_TIME);
                    }
                    lastTapTime=now;
                }
            }else if(gameState==STATE_PAUSED){gameState=STATE_PLAYING;}
            else if(gameState==STATE_GAME_OVER&&deathFlash<=0){gameState=STATE_MENU;}
        }
        return true;
    }

    private void reverseDirection(){
        moveDirection*=-1;totalReverse++;reverseFlash=1f;
        float rad=playerOnOuter?outerRadius:innerRadius;
        float px=centerX+rad*cos(playerAngle),py=centerY+rad*sin(playerAngle);
        for(int i=0;i<12;i++)spawnParticle(px,py,0xFFFF8800,true);
        addPopup(px,py-50,"REVERSE!",0xFFFF8800);shakeMag=5f;vib(30);
    }

    private void update(){
        vortexAngle+=0.5f;
        shakeMag*=0.92f;
        if(shakeMag>0.5f){shakeX=(rand.nextFloat()-0.5f)*2*shakeMag;shakeY=(rand.nextFloat()-0.5f)*2*shakeMag;}
        else{shakeX=0;shakeY=0;}
        updateParticles();updatePopups();
        if(reverseFlash>0)reverseFlash-=0.03f;
        if(feverMode)feverHue=(feverHue+5)%360;

        if(gameState==STATE_MENU){playerAngle+=1.0f;if(playerAngle>=360)playerAngle-=360;return;}
        if(gameState==STATE_GAME_OVER){deathFlash=Math.max(0,deathFlash-0.02f);return;}

        playTime++;
        float tm=slowmoActive?0.4f:1.0f;
        currentSpeed=(baseSpeed+(playTime/600f)*3f)*tm*moveDirection;
        playerAngle+=currentSpeed;
        if(playerAngle>=360)playerAngle-=360;if(playerAngle<0)playerAngle+=360;
        bgHue=(score/10f)%360;
        int nl=score/500+1;
        if(nl>level){level=nl;levelPopTimer=120;shakeMag=10f;vib(50);}
        if(levelPopTimer>0)levelPopTimer--;

        boolean wasFever=feverMode;
        feverMode=combo>=5;
        if(feverMode&&!wasFever){feverFlash=1f;shakeMag=12f;vib(80);
            addPopup(centerX,centerY,"FEVER MODE!",0xFFFF00FF);}
        if(feverFlash>0)feverFlash-=0.02f;
        float feverMult=feverMode?3f:1f;

        float pRad=playerOnOuter?outerRadius:innerRadius;
        float px=centerX+pRad*cos(playerAngle),py=centerY+pRad*sin(playerAngle);
        int tc=shieldActive?0xFF4488FF:(playerOnOuter?0xFFFF00FF:0xFF00FFFF);
        if(feverMode)tc=Color.HSVToColor(new float[]{feverHue,1,1});
        trail.add(new float[]{px,py,tc,1f});if(trail.size()>30)trail.remove(0);

        if(combo>0){comboTimer--;if(comboTimer<=0){combo=0;scoreMultiplier=1;}}
        if(nearMissTextTimer>0)nearMissTextTimer--;
        if(nearMissFlash>0)nearMissFlash-=0.05f;
        if(shieldActive){shieldTimer--;if(shieldTimer<=0)shieldActive=false;}
        if(slowmoActive){slowmoTimer--;if(slowmoTimer<=0)slowmoActive=false;}
        if(magnetActive){magnetTimer--;if(magnetTimer<=0)magnetActive=false;}

        obstacleTimer++;
        float spRate=Math.max(20,85-playTime/25f);
        if(obstacleTimer>=spRate){obstacleTimer=0;spawnObstacle();}
        orbTimer++;if(orbTimer>=100){orbTimer=0;spawnOrb();}
        powerupTimer++;if(powerupTimer>=600){powerupTimer=0;spawnPowerup();}
        diamondTimer++;if(diamondTimer>=900&&rand.nextFloat()<0.4f){diamondTimer=0;spawnDiamond();}

        if(magnetActive){for(float[] o:orbs)
            if((o[1]==1)==playerOnOuter)o[0]+=normAngle(playerAngle-o[0])*0.05f;
            for(float[] d:diamonds)
            if((d[1]==1)==playerOnOuter)d[0]+=normAngle(playerAngle-d[0])*0.05f;
        }

        Iterator<float[]> oi=obstacles.iterator();
        while(oi.hasNext()){float[] o=oi.next();
            o[0]+=o[2]*tm;if(o[0]>=360)o[0]-=360;if(o[0]<0)o[0]+=360;
            if(--o[4]<=0){oi.remove();continue;}
            if(o[6]==1){o[7]-=1;if(o[7]<=0){oi.remove();continue;}}
            if((o[1]==1)==playerOnOuter){
                float ad=Math.abs(normAngle(playerAngle-o[0])),half=o[3]/2+5;
                if(ad<half){
                    if(shieldActive){shieldActive=false;shieldTimer=0;shakeMag=15f;
                        for(int i=0;i<20;i++)spawnParticle(px,py,0xFF4488FF,false);
                        addPopup(px,py-50,"SHIELD!",0xFF4488FF);oi.remove();vib(40);continue;}
                    gameOver(px,py);return;
                }
                if(ad<half+10&&o[5]==0){
                    o[5]=1;totalNearMiss++;
                    int pts=(int)(25*scoreMultiplier*feverMult);score+=pts;
                    nearMissFlash=1f;nearMissText="NEAR MISS! +"+pts;
                    nearMissTextTimer=45;shakeMag=8f;vib(15);
                    float oR=(o[1]==1)?outerRadius:innerRadius;
                    float ox=centerX+oR*cos(o[0]),oy=centerY+oR*sin(o[0]);
                    for(int i=0;i<8;i++)spawnParticle(ox,oy,0xFF00FFFF,true);
                    addPopup(ox,oy-30,"+"+pts,0xFF00FFFF);
                }
            }
        }

        Iterator<float[]> ri=orbs.iterator();
        while(ri.hasNext()){float[] o=ri.next();
            o[0]+=o[2]*tm;if(o[0]>=360)o[0]-=360;o[3]+=0.1f;
            if(--o[4]<=0){ri.remove();continue;}
            if((o[1]==1)==playerOnOuter&&Math.abs(normAngle(playerAngle-o[0]))<12){
                combo++;totalOrbs++;comboTimer=180;
                scoreMultiplier=1+(combo-1)*0.5f;
                int pts=(int)(50*scoreMultiplier*feverMult);score+=pts;
                float oR=(o[1]==1)?outerRadius:innerRadius;
                float ox=centerX+oR*cos(o[0]),oy=centerY+oR*sin(o[0]);
                for(int i=0;i<15;i++)spawnParticle(ox,oy,0xFFFFD700,false);
                addPopup(ox,oy-30,"+"+pts,0xFFFFD700);vib(20);ri.remove();
            }
        }

        Iterator<float[]> di=diamonds.iterator();
        while(di.hasNext()){float[] d=di.next();
            d[0]+=d[2]*tm;if(d[0]>=360)d[0]-=360;d[3]+=0.12f;
            if(--d[4]<=0){di.remove();continue;}
            if((d[1]==1)==playerOnOuter&&Math.abs(normAngle(playerAngle-d[0]))<14){
                totalDiamonds++;combo+=2;comboTimer=240;
                scoreMultiplier=1+(combo-1)*0.5f;
                int pts=(int)(500*scoreMultiplier*feverMult);score+=pts;
                float dR=(d[1]==1)?outerRadius:innerRadius;
                float dx=centerX+dR*cos(d[0]),dy=centerY+dR*sin(d[0]);
                for(int i=0;i<30;i++)spawnParticle(dx,dy,0xFF00FFCC,false);
                for(int i=0;i<15;i++)spawnParticle(dx,dy,Color.WHITE,false);
                addPopup(dx,dy-40,"DIAMOND +"+pts,0xFF00FFCC);
                shakeMag=10f;vib(60);di.remove();
            }
        }

        Iterator<float[]> pi=powerups.iterator();
        while(pi.hasNext()){float[] pw=pi.next();
            pw[0]+=pw[2]*tm;if(pw[0]>=360)pw[0]-=360;pw[5]+=0.08f;
            if(--pw[4]<=0){pi.remove();continue;}
            if((pw[1]==1)==playerOnOuter&&Math.abs(normAngle(playerAngle-pw[0]))<15){
                int t=(int)pw[3];float pR=(pw[1]==1)?outerRadius:innerRadius;
                float pwx=centerX+pR*cos(pw[0]),pwy=centerY+pR*sin(pw[0]);
                if(t==0){shieldActive=true;shieldTimer=300;
                    for(int i=0;i<20;i++)spawnParticle(pwx,pwy,0xFF4488FF,false);
                    addPopup(pwx,pwy-30,"SHIELD!",0xFF4488FF);
                }else if(t==1){slowmoActive=true;slowmoTimer=240;
                    for(int i=0;i<20;i++)spawnParticle(pwx,pwy,0xFF44FF44,false);
                    addPopup(pwx,pwy-30,"SLOW-MO!",0xFF44FF44);
                }else{magnetActive=true;magnetTimer=300;
                    for(int i=0;i<20;i++)spawnParticle(pwx,pwy,0xFFFFAA00,false);
                    addPopup(pwx,pwy-30,"MAGNET!",0xFFFFAA00);
                }
                shakeMag=5f;vib(30);pi.remove();
            }
        }
        if((int)playTime%6==0)score++;
    }

    private void render(){
        Canvas c=null;SurfaceHolder h=getHolder();
        try{c=h.lockCanvas();if(c==null)return;
            c.save();c.translate(shakeX,shakeY);
            int bgR=(int)(10+8*Math.sin(bgHue*0.017));
            int bgG=(int)(10+5*Math.sin(bgHue*0.017+2));
            int bgB=(int)(46+15*Math.sin(bgHue*0.017+4));
            if(feverMode){bgR=(int)(20+20*Math.sin(feverHue*0.017));bgG=(int)(5+15*Math.sin(feverHue*0.017+2));bgB=(int)(40+30*Math.sin(feverHue*0.017+4));}
            c.drawColor(Color.rgb(bgR,bgG,bgB));
            float pulse=(float)(1+0.02*Math.sin(animTime*0.05));
            c.scale(pulse,pulse,centerX,centerY);
            drawStars(c);drawVortex(c);drawOrbits(c);
            drawPowerups(c);drawObstacles(c);drawOrbs(c);drawDiamonds(c);
            drawTrail(c);
            if(gameState!=STATE_GAME_OVER)drawPlayer(c);
            drawParticles(c);drawPopups(c);
            c.restore();
            if(gameState==STATE_MENU)drawMenu(c);
            else if(gameState==STATE_PLAYING)drawHUD(c);
            else if(gameState==STATE_PAUSED)drawPauseScreen(c);
            else drawGameOverUI(c);
            if(levelPopTimer>0){pLevel.setColor(hsvToColor(bgHue+180,0.8f,1f));
                pLevel.setAlpha((int)(levelPopTimer/120f*255));
                c.drawText("LEVEL "+level,centerX,centerY-outerRadius-60,pLevel);}
            if(deathFlash>0)c.drawColor(Color.argb((int)(deathFlash*200),255,255,255));
            if(nearMissFlash>0){int nr=playerOnOuter?255:0,ng=playerOnOuter?0:255;
                c.drawColor(Color.argb((int)(nearMissFlash*30),nr,ng,255));}
            if(reverseFlash>0)c.drawColor(Color.argb((int)(reverseFlash*40),255,136,0));
            if(feverFlash>0)c.drawColor(Color.argb((int)(feverFlash*50),255,0,255));
        }catch(Exception e){}
        finally{if(c!=null)try{h.unlockCanvasAndPost(c);}catch(Exception e){}}
    }

    private void drawStars(Canvas c){
        if(stars==null)return;
        for(float[] s:stars){int a=(int)(150+105*Math.sin(animTime*0.02+s[0]));
            pStarPaint.setAlpha(Math.min(255,Math.max(0,a)));
            c.drawCircle(s[0],s[1],s[2],pStarPaint);}
    }

    private void drawVortex(Canvas c){
        int spirals=feverMode?6:4;
        for(int i=0;i<spirals;i++){
            float ang=vortexAngle+i*(360f/spirals);Path path=new Path();
            for(int j=0;j<=50;j++){float t=j/50f;
                float a=(float)Math.toRadians(ang+t*360);
                float rad=innerRadius*0.6f*t;
                float x=centerX+rad*(float)Math.cos(a),y=centerY+rad*(float)Math.sin(a);
                if(j==0)path.moveTo(x,y);else path.lineTo(x,y);}
            float bl=(float)(Math.sin(animTime*0.02+i)*0.5+0.5);
            if(feverMode)pVortex.setColor(Color.HSVToColor(new float[]{(feverHue+i*60)%360,1,1}));
            else pVortex.setColor(blendCol(0xFF7B2FBE,0xFF00BFFF,bl));
            pVortex.setAlpha((int)(80+40*Math.sin(animTime*0.03+i)));
            pVortex.setStrokeWidth(2+i*0.5f);c.drawPath(path,pVortex);}
    }

    private void drawOrbits(Canvas c){
        if(feverMode){
            int ci=Color.HSVToColor(new float[]{feverHue,1,1});
            int co=Color.HSVToColor(new float[]{(feverHue+180)%360,1,1});
            pOrbitGlowInner.setColor(ci);pOrbitInner.setColor(ci);
            pOrbitGlowOuter.setColor(co);pOrbitOuter.setColor(co);
        }else{
            pOrbitGlowInner.setColor(0xFF00FFFF);pOrbitInner.setColor(0xFF00FFFF);
            pOrbitGlowOuter.setColor(0xFFFF00FF);pOrbitOuter.setColor(0xFFFF00FF);
        }
        pOrbitGlowInner.setAlpha((int)(30+15*Math.sin(animTime*0.03)));
        c.drawCircle(centerX,centerY,innerRadius,pOrbitGlowInner);
        pOrbitInner.setAlpha((int)(180+75*Math.sin(animTime*0.03)));
        c.drawCircle(centerX,centerY,innerRadius,pOrbitInner);
        pOrbitGlowOuter.setAlpha((int)(30+15*Math.sin(animTime*0.03+1)));
        c.drawCircle(centerX,centerY,outerRadius,pOrbitGlowOuter);
        pOrbitOuter.setAlpha((int)(180+75*Math.sin(animTime*0.03+1)));
        c.drawCircle(centerX,centerY,outerRadius,pOrbitOuter);
    }

    private void drawPlayer(Canvas c){
        float rad=playerOnOuter?outerRadius:innerRadius;
        float px=centerX+rad*cos(playerAngle),py=centerY+rad*sin(playerAngle);
        int gc=shieldActive?0xFF4488FF:(playerOnOuter?0xFFFF00FF:0xFF00FFFF);
        if(feverMode)gc=Color.HSVToColor(new float[]{feverHue,1,1});
        pGlow.setColor(gc);
        pGlow.setAlpha(30);c.drawCircle(px,py,playerSize+25,pGlow);
        pGlow.setAlpha(50);c.drawCircle(px,py,playerSize+15,pGlow);
        pGlow.setAlpha(80);c.drawCircle(px,py,playerSize+8,pGlow);
        c.drawCircle(px,py,playerSize,pPlayer);
        int ic=playerOnOuter?0xFFFF88FF:0xFF88FFFF;
        if(feverMode)ic=Color.HSVToColor(new float[]{(feverHue+90)%360,0.5f,1});
        pPlayerInner.setColor(ic);
        c.drawCircle(px,py,playerSize*0.5f,pPlayerInner);
        if(shieldActive){float sa=(float)(Math.sin(animTime*0.1)*0.3+0.7);
            pShieldRing.setAlpha((int)(sa*200));pShieldRing.setStrokeWidth(3);
            c.drawCircle(px,py,playerSize+18,pShieldRing);
            pShieldRing.setAlpha((int)(sa*100));pShieldRing.setStrokeWidth(6);
            c.drawCircle(px,py,playerSize+22,pShieldRing);}
        if(moveDirection==-1){Paint ar=tp(0xFFFF8800,playerSize*1.5f,true);
            ar.setAlpha((int)(150+100*Math.sin(animTime*0.1)));
            c.drawText("\u25C0",px,py-playerSize-10,ar);}
    }

    private void drawObstacles(Canvas c){
        for(float[] o:obstacles){
            float rad=o[1]==1?outerRadius:innerRadius;
            float start=o[0]-o[3]/2;
            RectF ov=new RectF(centerX-rad,centerY-rad,centerX+rad,centerY+rad);
            float p=(float)(Math.sin(animTime*0.15)*0.5+0.5);
            if(o[6]==1){
                pLightning.setColor(Color.WHITE);
                pLightning.setAlpha((int)(200+55*Math.sin(animTime*0.3)));
                pLightning.setStrokeWidth(10);
                c.drawArc(ov,start,o[3],false,pLightning);
                Paint lg=mp(0xFF88CCFF,Paint.Style.STROKE,20f);
                lg.setAlpha((int)(80+40*Math.sin(animTime*0.3)));
                c.drawArc(ov,start,o[3],false,lg);
            }else{
                int col=blendCol(0xFFFF4444,0xFFFF8800,p);
                pObstacleGlow.setColor(col);pObstacleGlow.setAlpha(40);
                c.drawArc(ov,start,o[3],false,pObstacleGlow);
                pObstacle.setColor(col);pObstacle.setAlpha(230);
                c.drawArc(ov,start,o[3],false,pObstacle);
                pObstacleCore.setColor(0xFFFFCC00);pObstacleCore.setAlpha((int)(150+105*p));
                c.drawArc(ov,start,o[3],false,pObstacleCore);
            }
        }
    }

    private void drawOrbs(Canvas c){
        for(float[] o:orbs){float rad=o[1]==1?outerRadius:innerRadius;
            float ox=centerX+rad*cos(o[0]),oy=centerY+rad*sin(o[0]);
            float ps=(float)(1+0.2*Math.sin(o[3])),sz=playerSize*0.8f*ps;
            pOrbGlow.setAlpha(40);c.drawCircle(ox,oy,sz+20,pOrbGlow);
            pOrbGlow.setAlpha(70);c.drawCircle(ox,oy,sz+10,pOrbGlow);
            c.drawCircle(ox,oy,sz,pOrbBody);
            pOrbShine.setAlpha((int)(180*Math.abs(Math.sin(o[3]*2))));
            c.drawCircle(ox-sz*0.3f,oy-sz*0.3f,sz*0.3f,pOrbShine);}
    }

    private void drawDiamonds(Canvas c){
        for(float[] d:diamonds){float rad=d[1]==1?outerRadius:innerRadius;
            float dx=centerX+rad*cos(d[0]),dy=centerY+rad*sin(d[0]);
            float ps=(float)(1+0.3*Math.sin(d[3])),sz=playerSize*1.2f*ps;
            pDiamondGlow.setAlpha(50);c.drawCircle(dx,dy,sz+22,pDiamondGlow);
            pDiamondGlow.setAlpha(90);c.drawCircle(dx,dy,sz+12,pDiamondGlow);
            Path dm=new Path();
            dm.moveTo(dx,dy-sz);dm.lineTo(dx+sz*0.7f,dy);
            dm.lineTo(dx,dy+sz);dm.lineTo(dx-sz*0.7f,dy);dm.close();
            pDiamond.setAlpha(220);c.drawPath(dm,pDiamond);
            Paint sh=mp(Color.WHITE,Paint.Style.FILL,0);
            sh.setAlpha((int)(200*Math.abs(Math.sin(d[3]*1.5))));
            c.drawCircle(dx-sz*0.2f,dy-sz*0.3f,sz*0.25f,sh);
            Paint tx=tp(Color.WHITE,sz*0.8f,true);tx.setAlpha(255);
            c.drawText("\u2666",dx,dy+sz*0.3f,tx);}
    }

    private void drawPowerups(Canvas c){
        int[] colors={0xFF4488FF,0xFF44FF44,0xFFFFAA00};
        String[] labels={"S","T","M"};
        Paint pp=mp(Color.WHITE,Paint.Style.FILL,0);
        Paint pl=tp(Color.WHITE,playerSize,true);
        for(float[] pw:powerups){int t=(int)pw[3];
            float rad=pw[1]==1?outerRadius:innerRadius;
            float px=centerX+rad*cos(pw[0]),py=centerY+rad*sin(pw[0]);
            float ps=(float)(1+0.3*Math.sin(pw[5])),sz=playerSize*1.1f*ps;
            pp.setColor(colors[t]);pp.setAlpha(40);c.drawCircle(px,py,sz+18,pp);
            pp.setAlpha(80);c.drawCircle(px,py,sz+8,pp);
            pp.setAlpha(200);c.drawCircle(px,py,sz,pp);
            pl.setTextSize(sz*1.2f);pl.setAlpha(255);
            c.drawText(labels[t],px,py+sz*0.4f,pl);}
    }

    private void drawTrail(Canvas c){
        for(int i=0;i<trail.size();i++){float[] t=trail.get(i);
            float alpha=(float)(i+1)/trail.size();
            pTrail.setColor((int)t[2]);pTrail.setAlpha((int)(alpha*80));
            c.drawCircle(t[0],t[1],playerSize*alpha*0.7f,pTrail);}
    }

    private void drawParticles(Canvas c){
        for(float[] p:particles){pParticle.setColor((int)p[4]);
            pParticle.setAlpha((int)(p[5]*255));
            c.drawCircle(p[0],p[1],p[6],pParticle);}
    }

    private void drawPopups(Canvas c){
        for(int i=0;i<popups.size();i++){float[] p=popups.get(i);
            pPopup.setColor((int)p[4]);pPopup.setAlpha((int)(p[2]*255));
            c.drawText(popupTexts.get(i),p[0],p[1]+p[3],pPopup);}
    }

    private void drawMenu(Canvas c){
        pTitle.setColor(0xFF00FFFF);
        pTitle.setAlpha((int)(200+55*Math.sin(animTime*0.05)));
        c.drawText("NEON",centerX,centerY-180,pTitle);
        pTitle.setColor(0xFFFF00FF);
        pTitle.setAlpha((int)(200+55*Math.sin(animTime*0.05+1)));
        c.drawText("VORTEX",centerX,centerY-80,pTitle);
        pSub.setTextSize(40);pSub.setAlpha((int)(Math.sin(animTime*0.04)*80+175));
        c.drawText("TAP TO START",centerX,centerY+60,pSub);
        if(highScore>0){pHighScore.setAlpha(200);c.drawText("BEST: "+highScore,centerX,centerY+130,pHighScore);}
        pSub.setTextSize(26);pSub.setAlpha(150);
        c.drawText("Single Tap = Switch Orbit",centerX,centerY+200,pSub);
        c.drawText("Double Tap = Reverse Direction",centerX,centerY+236,pSub);
        c.drawText("Collect orbs & diamonds",centerX,centerY+272,pSub);
        c.drawText("5+ Combo = FEVER MODE!",centerX,centerY+308,pSub);
    }

    private void drawHUD(Canvas c){
        pScore.setAlpha(220);c.drawText(String.valueOf(score),centerX,120,pScore);
        if(combo>1){pCombo.setAlpha((int)(200+55*Math.sin(animTime*0.1)));
            pCombo.setTextSize(Math.min(70,40+combo*3));
            c.drawText("x"+String.format("%.1f",scoreMultiplier)+" COMBO!",centerX,185,pCombo);}
        if(feverMode){pFever.setColor(Color.HSVToColor(new float[]{feverHue,1,1}));
            pFever.setAlpha((int)(200+55*Math.sin(animTime*0.15)));pFever.setTextSize(35);
            c.drawText("FEVER x3",centerX,225,pFever);}
        if(nearMissTextTimer>0){pNearMiss.setAlpha((int)(nearMissTextTimer/45f*255));
            c.drawText(nearMissText,centerX,centerY+outerRadius+80,pNearMiss);}
        pPause.setTextSize(40);pPause.setAlpha(150);pPause.setTextAlign(Paint.Align.RIGHT);
        c.drawText("||",screenW-40,60,pPause);pPause.setTextAlign(Paint.Align.CENTER);
        if(moveDirection==-1){pReverse.setAlpha((int)(150+100*Math.sin(animTime*0.08)));
            pReverse.setTextSize(26);c.drawText("< REVERSE >",centerX,screenH-100,pReverse);}
        float barY=screenH-60;
        if(shieldActive)drawPowerBar(c,30,barY,shieldTimer/300f,0xFF4488FF,"SHIELD");
        if(slowmoActive)drawPowerBar(c,30,barY-25,slowmoTimer/240f,0xFF44FF44,"SLOW");
        if(magnetActive)drawPowerBar(c,30,barY-50,magnetTimer/300f,0xFFFFAA00,"MAGNET");
    }

    private void drawPowerBar(Canvas c,float x,float y,float pct,int col,String label){
        Paint bg=mp(0xFF333333,Paint.Style.FILL,0);float w=screenW-60;
        c.drawRect(x,y,x+w,y+16,bg);
        Paint fg=mp(col,Paint.Style.FILL,0);fg.setAlpha(200);
        c.drawRect(x,y,x+w*pct,y+16,fg);
        Paint tl=tp(Color.WHITE,14,false);tl.setTextAlign(Paint.Align.LEFT);tl.setAlpha(200);
        c.drawText(label,x+5,y+13,tl);
    }

    private void drawPauseScreen(Canvas c){
        c.drawColor(Color.argb(180,0,0,0));
        pPause.setTextSize(80);pPause.setAlpha(255);pPause.setTextAlign(Paint.Align.CENTER);
        c.drawText("PAUSED",centerX,centerY-40,pPause);
        pSub.setAlpha((int)(Math.sin(animTime*0.04)*80+175));pSub.setTextSize(40);
        c.drawText("TAP TO RESUME",centerX,centerY+40,pSub);
    }

    private void drawGameOverUI(Canvas c){
        c.drawColor(Color.argb(180,0,0,0));
        pGameOver.setAlpha((int)(200+55*Math.sin(animTime*0.05)));
        c.drawText("GAME OVER",centerX,centerY-180,pGameOver);
        pScore.setAlpha(255);pScore.setTextSize(90);
        c.drawText(String.valueOf(score),centerX,centerY-70,pScore);pScore.setTextSize(80);
        if(isNewBest){pHighScore.setColor(0xFFFFD700);
            c.drawText("NEW BEST!",centerX,centerY-10,pHighScore);pHighScore.setColor(0xFF888888);
        }else{pHighScore.setAlpha(200);c.drawText("BEST: "+highScore,centerX,centerY-10,pHighScore);}
        pStat.setAlpha(180);
        c.drawText("Time: "+String.format("%.1f",survivalTime)+"s",centerX,centerY+50,pStat);
        c.drawText("Orbs: "+totalOrbs+"  Diamonds: "+totalDiamonds,centerX,centerY+90,pStat);
        c.drawText("Near Miss: "+totalNearMiss+"  Reverses: "+totalReverse,centerX,centerY+130,pStat);
        c.drawText("Level: "+level,centerX,centerY+170,pStat);
        float ra=(float)(Math.sin(animTime*0.04)*0.3+0.7);pRestart.setAlpha((int)(ra*255));
        c.drawText("TAP TO RETRY",centerX,centerY+240,pRestart);
    }

    private void startGame(){
        gameState=STATE_PLAYING;score=0;combo=0;comboTimer=0;scoreMultiplier=1;
        playTime=0;currentSpeed=baseSpeed;playerAngle=0;playerOnOuter=false;
        moveDirection=1;reverseFlash=0;feverMode=false;feverHue=0;feverFlash=0;
        obstacles.clear();orbs.clear();diamonds.clear();particles.clear();
        trail.clear();popups.clear();popupTexts.clear();powerups.clear();
        obstacleTimer=0;orbTimer=0;powerupTimer=0;diamondTimer=0;
        shakeMag=0;nearMissFlash=0;nearMissTextTimer=0;deathFlash=0;
        shieldActive=false;shieldTimer=0;slowmoActive=false;slowmoTimer=0;
        magnetActive=false;magnetTimer=0;totalOrbs=0;totalNearMiss=0;
        totalReverse=0;totalDiamonds=0;level=1;levelPopTimer=0;bgHue=0;isNewBest=false;
    }

    private void switchOrbit(){
        playerOnOuter=!playerOnOuter;
        float rad=playerOnOuter?outerRadius:innerRadius;
        float px=centerX+rad*cos(playerAngle),py=centerY+rad*sin(playerAngle);
        int col=playerOnOuter?0xFFFF00FF:0xFF00FFFF;
        for(int i=0;i<10;i++)spawnParticle(px,py,col,true);shakeMag=3f;vib(15);
    }

    private void gameOver(float px,float py){
        gameState=STATE_GAME_OVER;deathFlash=1f;shakeMag=30f;survivalTime=playTime/60f;
        for(int i=0;i<60;i++)spawnParticle(px,py,Color.WHITE,false);
        for(int i=0;i<40;i++)spawnParticle(px,py,0xFFFF4444,false);
        for(int i=0;i<25;i++)spawnParticle(px,py,playerOnOuter?0xFFFF00FF:0xFF00FFFF,false);
        if(score>highScore){highScore=score;isNewBest=true;prefs.edit().putInt("highscore",highScore).apply();}
        else isNewBest=false;vib(200);
    }

    private void spawnObstacle(){
        float[] o=new float[8];
        o[0]=rand.nextFloat()*360;o[1]=rand.nextBoolean()?1:0;
        o[2]=(rand.nextFloat()*0.8f+0.3f)*(rand.nextBoolean()?1:-1);
        o[3]=20+rand.nextFloat()*30;o[4]=300+rand.nextFloat()*200;o[5]=0;
        o[6]=0;o[7]=0;
        if(playTime>1800&&rand.nextFloat()<0.15f){
            o[6]=1;o[3]=15+rand.nextFloat()*20;o[4]=80+rand.nextFloat()*60;o[7]=o[4];}
        obstacles.add(o);
        if(playTime>600&&rand.nextFloat()<0.3f){
            float[] o2=new float[8];
            o2[0]=(o[0]+120+rand.nextFloat()*120)%360;o2[1]=o[1]==1?0:1;
            o2[2]=(rand.nextFloat()*0.6f+0.3f)*(rand.nextBoolean()?1:-1);
            o2[3]=20+rand.nextFloat()*25;o2[4]=300;o2[5]=0;o2[6]=0;o2[7]=0;
            obstacles.add(o2);}
        if(playTime>1200&&rand.nextFloat()<0.2f){
            float[] o3=new float[8];
            o3[0]=(o[0]+180)%360;o3[1]=o[1];o3[2]=-o[2];
            o3[3]=25+rand.nextFloat()*20;o3[4]=250;o3[5]=0;o3[6]=0;o3[7]=0;
            obstacles.add(o3);}
    }

    private void spawnOrb(){
        orbs.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.4f+0.2f)*(rand.nextBoolean()?1:-1),rand.nextFloat()*6.28f,400});}

    private void spawnDiamond(){
        diamonds.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.3f+0.1f)*(rand.nextBoolean()?1:-1),rand.nextFloat()*6.28f,350});}

    private void spawnPowerup(){
        powerups.add(new float[]{rand.nextFloat()*360,rand.nextBoolean()?1:0,
            (rand.nextFloat()*0.3f+0.1f)*(rand.nextBoolean()?1:-1),rand.nextInt(3),500,0});}

    private void spawnParticle(float x,float y,int color,boolean small){
        float a=rand.nextFloat()*360,sp=small?rand.nextFloat()*3+1:rand.nextFloat()*8+2;
        float sz=small?rand.nextFloat()*4+2:rand.nextFloat()*6+3;
        particles.add(new float[]{x,y,sp*cos(a),sp*sin(a),color,1f,sz});}

    private void addPopup(float x,float y,String text,int color){
        popups.add(new float[]{x,y,1f,0,color});popupTexts.add(text);}

    private void updateParticles(){Iterator<float[]> it=particles.iterator();
        while(it.hasNext()){float[] p=it.next();p[0]+=p[2];p[1]+=p[3];p[5]-=0.02f;p[6]*=0.97f;
            if(p[5]<=0||p[6]<0.5f)it.remove();}}

    private void updatePopups(){int idx=0;Iterator<float[]> it=popups.iterator();
        while(it.hasNext()){float[] p=it.next();p[2]-=0.02f;p[3]-=1.5f;
            if(p[2]<=0){it.remove();popupTexts.remove(idx);continue;}idx++;}}

    private float normAngle(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;}

    private int blendCol(int c1,int c2,float r){
        return Color.rgb((int)(Color.red(c1)*(1-r)+Color.red(c2)*r),
            (int)(Color.green(c1)*(1-r)+Color.green(c2)*r),
            (int)(Color.blue(c1)*(1-r)+Color.blue(c2)*r));}

    private int hsvToColor(float h,float s,float v){
        return Color.HSVToColor(new float[]{h%360,s,v});}

    public void pause(){isRunning=false;
        try{if(gameThread!=null)gameThread.join();}catch(InterruptedException e){}}

    public void resume(){}
}
