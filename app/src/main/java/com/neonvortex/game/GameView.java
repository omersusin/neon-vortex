package com.neonvortex.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private volatile boolean isRunning;
    private final GameState gs;
    private final PaintFactory pf;
    private final Player player;
    private final ParticleSystem ps;
    private final EntityManager em;
    private final Renderer renderer;
    private final Vibrator vibrator;

    private long lastTapTime=0;
    private boolean waitingDoubleTap=false;
    private final android.os.Handler tapHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable singleTapAction=new Runnable(){
        @Override public void run(){
            if(waitingDoubleTap){waitingDoubleTap=false;player.switchOrbit(ps,vibrator);}
        }
    };

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        gs=new GameState();
        pf=new PaintFactory();
        ps=new ParticleSystem(gs,pf);
        player=new Player(gs,pf);
        em=new EntityManager(gs,pf,ps);
        renderer=new Renderer(gs,pf);
        vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        em.setVibrator(vibrator);
        gs.highScore=context.getSharedPreferences("neonvortex",Context.MODE_PRIVATE).getInt("highscore",0);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        gs.initScreen(getWidth(),getHeight());
        gs.playerSize=gs.screenW*0.025f;
        renderer.initStars();
        isRunning=true;
        gameThread=new Thread(this);
        gameThread.start();
    }

    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){gs.initScreen(w,ht);}

    @Override public void surfaceDestroyed(SurfaceHolder holder){
        isRunning=false;
        try{if(gameThread!=null)gameThread.join();}catch(InterruptedException e){}
    }

    @Override public void run() {
        long target=1000000000L/60;
        while(isRunning){
            long s=System.nanoTime();
            gs.animTime++;
            if(gs.gameState!=GameState.STATE_PAUSED)update();
            render();
            long sl=(target-(System.nanoTime()-s))/1000000;
            if(sl>0)try{Thread.sleep(sl);}catch(InterruptedException e){}
        }
    }

    private void update() {
        gs.vortexAngle+=0.5f;
        gs.shakeMag*=0.92f;
        java.util.Random rand=new java.util.Random();
        if(gs.shakeMag>0.5f){
            gs.shakeX=(rand.nextFloat()-0.5f)*2*gs.shakeMag;
            gs.shakeY=(rand.nextFloat()-0.5f)*2*gs.shakeMag;
        }else{gs.shakeX=0;gs.shakeY=0;}
        ps.updateParticles();
        ps.updatePopups();
        if(gs.reverseFlash>0)gs.reverseFlash-=0.03f;
        if(gs.feverMode)gs.feverHue=(gs.feverHue+5)%360;

        if(gs.gameState==GameState.STATE_MENU){
            gs.playerAngle+=1.0f;
            if(gs.playerAngle>=360)gs.playerAngle-=360;
            return;
        }
        if(gs.gameState==GameState.STATE_GAME_OVER){
            gs.deathFlash=Math.max(0,gs.deathFlash-0.02f);
            return;
        }

        gs.playTime++;
        float tm=gs.slowmoActive?0.4f:1.0f;
        player.update(tm);
        gs.bgHue=(gs.score/10f)%360;

        int nl=gs.score/500+1;
        if(nl>gs.level){gs.level=nl;gs.levelPopTimer=120;gs.shakeMag=10f;
            try{if(vibrator!=null)vibrator.vibrate(50);}catch(Exception e){}}
        if(gs.levelPopTimer>0)gs.levelPopTimer--;

        boolean wasFever=gs.feverMode;
        gs.feverMode=gs.combo>=5;
        if(gs.feverMode&&!wasFever){gs.feverFlash=1f;gs.shakeMag=12f;
            try{if(vibrator!=null)vibrator.vibrate(80);}catch(Exception e){}
            ps.addPopup(gs.centerX,gs.centerY,"FEVER MODE!",0xFFFF00FF);}
        if(gs.feverFlash>0)gs.feverFlash-=0.02f;
        float feverMult=gs.feverMode?3f:1f;

        float px=player.getX(),py=player.getY();
        int tc=gs.shieldActive?0xFF4488FF:(gs.playerOnOuter?0xFFFF00FF:0xFF00FFFF);
        if(gs.feverMode)tc=Color.HSVToColor(new float[]{gs.feverHue,1,1});
        ps.updateTrail(px,py,tc);

        if(gs.combo>0){gs.comboTimer--;if(gs.comboTimer<=0){gs.combo=0;gs.scoreMultiplier=1;}}
        if(gs.nearMissTextTimer>0)gs.nearMissTextTimer--;
        if(gs.nearMissFlash>0)gs.nearMissFlash-=0.05f;
        if(gs.shieldActive){gs.shieldTimer--;if(gs.shieldTimer<=0)gs.shieldActive=false;}
        if(gs.slowmoActive){gs.slowmoTimer--;if(gs.slowmoTimer<=0)gs.slowmoActive=false;}
        if(gs.magnetActive){gs.magnetTimer--;if(gs.magnetTimer<=0)gs.magnetActive=false;}

        gs.obstacleTimer++;
        if(gs.obstacleTimer>=Math.max(20,85-gs.playTime/25f)){gs.obstacleTimer=0;em.spawnObstacle();}
        gs.orbTimer++;if(gs.orbTimer>=100){gs.orbTimer=0;em.spawnOrb();}
        gs.powerupTimer++;if(gs.powerupTimer>=600){gs.powerupTimer=0;em.spawnPowerup();}
        gs.diamondTimer++;
        if(gs.diamondTimer>=900&&new java.util.Random().nextFloat()<0.4f){gs.diamondTimer=0;em.spawnDiamond();}

        em.applyMagnet();

        if(em.updateObstacles(tm,px,py,feverMult)){gameOver(px,py);return;}
        em.updateOrbs(tm,feverMult);
        em.updateDiamonds(tm,feverMult);
        em.updatePowerups(tm);

        if((int)gs.playTime%6==0)gs.score++;
    }

    private void render() {
        Canvas c=null;SurfaceHolder h=getHolder();
        try{c=h.lockCanvas();if(c==null)return;
            c.save();c.translate(gs.shakeX,gs.shakeY);
            renderer.drawBackground(c);
            float pulse=(float)(1+0.02*Math.sin(gs.animTime*0.05));
            c.scale(pulse,pulse,gs.centerX,gs.centerY);
            renderer.drawStars(c);renderer.drawVortex(c);renderer.drawOrbits(c);
            em.drawPowerups(c);em.drawObstacles(c);em.drawOrbs(c);em.drawDiamonds(c);
            ps.drawTrail(c);
            if(gs.gameState!=GameState.STATE_GAME_OVER)player.draw(c);
            ps.drawParticles(c);ps.drawPopups(c);
            c.restore();
            if(gs.gameState==GameState.STATE_MENU)renderer.drawMenu(c);
            else if(gs.gameState==GameState.STATE_PLAYING)renderer.drawHUD(c);
            else if(gs.gameState==GameState.STATE_PAUSED)renderer.drawPauseScreen(c);
            else renderer.drawGameOverUI(c);
            renderer.drawOverlays(c);
        }catch(Exception e){}
        finally{if(c!=null)try{h.unlockCanvasAndPost(c);}catch(Exception e){}}
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            float tx=event.getX(),ty=event.getY();
            long now=System.currentTimeMillis();
            if(gs.gameState==GameState.STATE_MENU){gs.reset();}
            else if(gs.gameState==GameState.STATE_PLAYING){
                if(tx>gs.screenW-150&&ty<150){gs.gameState=GameState.STATE_PAUSED;}
                else{
                    long diff=now-lastTapTime;
                    if(diff<GameState.DOUBLE_TAP_TIME){
                        tapHandler.removeCallbacks(singleTapAction);
                        waitingDoubleTap=false;player.reverse(ps,vibrator);
                    }else{
                        waitingDoubleTap=true;
                        tapHandler.removeCallbacks(singleTapAction);
                        tapHandler.postDelayed(singleTapAction,GameState.DOUBLE_TAP_TIME);
                    }
                    lastTapTime=now;
                }
            }else if(gs.gameState==GameState.STATE_PAUSED){gs.gameState=GameState.STATE_PLAYING;}
            else if(gs.gameState==GameState.STATE_GAME_OVER&&gs.deathFlash<=0){gs.gameState=GameState.STATE_MENU;}
        }
        return true;
    }

    private void gameOver(float px, float py) {
        gs.gameState=GameState.STATE_GAME_OVER;
        gs.deathFlash=1f;gs.shakeMag=30f;gs.survivalTime=gs.playTime/60f;
        for(int i=0;i<60;i++)ps.spawnParticle(px,py,Color.WHITE,false);
        for(int i=0;i<40;i++)ps.spawnParticle(px,py,0xFFFF4444,false);
        for(int i=0;i<25;i++)ps.spawnParticle(px,py,gs.playerOnOuter?0xFFFF00FF:0xFF00FFFF,false);
        if(gs.score>gs.highScore){
            gs.highScore=gs.score;gs.isNewBest=true;
            getContext().getSharedPreferences("neonvortex",0).edit().putInt("highscore",gs.highScore).apply();
        }else gs.isNewBest=false;
        try{if(vibrator!=null)vibrator.vibrate(200);}catch(Exception e){}
    }

    public void pause(){
        isRunning=false;
        try{if(gameThread!=null)gameThread.join();}catch(InterruptedException e){}
    }
    public void resume(){}
}
