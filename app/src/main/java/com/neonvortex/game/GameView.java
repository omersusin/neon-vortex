package com.neonvortex.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAME_OVER = 2;

    private Thread gameThread;
    private boolean isRunning;
    private int gameState = STATE_MENU;

    private int screenW, screenH;
    private float centerX, centerY;
    private float innerRadius, outerRadius;

    private float playerAngle = 0;
    private boolean playerOnOuter = false;
    private float playerSize = 24f;

    private float baseSpeed = 1.5f;
    private float currentSpeed;

    private ArrayList<float[]> obstacles = new ArrayList<>();
    private ArrayList<float[]> orbs = new ArrayList<>();
    private ArrayList<float[]> particles = new ArrayList<>();
    private ArrayList<float[]> trail = new ArrayList<>();

    private float obstacleTimer = 0;
    private float orbTimer = 0;

    private int score = 0;
    private int highScore = 0;
    private int combo = 0;
    private float comboTimer = 0;
    private float scoreMultiplier = 1;

    private float nearMissFlash = 0;
    private float nearMissTextTimer = 0;
    private String nearMissText = "";

    private float shakeX = 0, shakeY = 0;
    private float shakeMag = 0;
    private float vortexAngle = 0;
    private float deathFlash = 0;
    private float animTime = 0;
    private float playTime = 0;

    private float[][] stars;
    private Random rand = new Random();
    private SharedPreferences prefs;

    private Paint pBg, pInnerOrbit, pOuterOrbit, pPlayer, pPlayerGlow;
    private Paint pScore, pHighScore, pTitle, pSub, pGameOver, pRestart;
    private Paint pCombo, pNearMiss, pParticle, pTrail, pVortex;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        prefs = context.getSharedPreferences("neonvortex", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highscore", 0);
        initPaints();
    }

    private void initPaints() {
        pBg = new Paint();
        pBg.setColor(Color.parseColor("#0A0A2E"));

        pInnerOrbit = new Paint(Paint.ANTI_ALIAS_FLAG);
        pInnerOrbit.setColor(Color.parseColor("#00FFFF"));
        pInnerOrbit.setStyle(Paint.Style.STROKE);
        pInnerOrbit.setStrokeWidth(6f);

        pOuterOrbit = new Paint(Paint.ANTI_ALIAS_FLAG);
        pOuterOrbit.setColor(Color.parseColor("#FF00FF"));
        pOuterOrbit.setStyle(Paint.Style.STROKE);
        pOuterOrbit.setStrokeWidth(6f);

        pPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPlayer.setColor(Color.WHITE);
        pPlayer.setStyle(Paint.Style.FILL);

        pPlayerGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPlayerGlow.setStyle(Paint.Style.FILL);
        pPlayerGlow.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.OUTER));

        pScore = new Paint(Paint.ANTI_ALIAS_FLAG);
        pScore.setColor(Color.WHITE);
        pScore.setTextSize(80);
        pScore.setTextAlign(Paint.Align.CENTER);
        pScore.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pHighScore = new Paint(Paint.ANTI_ALIAS_FLAG);
        pHighScore.setColor(Color.parseColor("#888888"));
        pHighScore.setTextSize(36);
        pHighScore.setTextAlign(Paint.Align.CENTER);

        pTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTitle.setColor(Color.WHITE);
        pTitle.setTextSize(100);
        pTitle.setTextAlign(Paint.Align.CENTER);
        pTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        pSub.setColor(Color.parseColor("#AAAAAA"));
        pSub.setTextSize(40);
        pSub.setTextAlign(Paint.Align.CENTER);

        pGameOver = new Paint(Paint.ANTI_ALIAS_FLAG);
        pGameOver.setColor(Color.parseColor("#FF4444"));
        pGameOver.setTextSize(90);
        pGameOver.setTextAlign(Paint.Align.CENTER);
        pGameOver.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pRestart = new Paint(Paint.ANTI_ALIAS_FLAG);
        pRestart.setColor(Color.WHITE);
        pRestart.setTextSize(44);
        pRestart.setTextAlign(Paint.Align.CENTER);

        pCombo = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCombo.setColor(Color.parseColor("#FFD700"));
        pCombo.setTextSize(50);
        pCombo.setTextAlign(Paint.Align.CENTER);
        pCombo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pNearMiss = new Paint(Paint.ANTI_ALIAS_FLAG);
        pNearMiss.setColor(Color.parseColor("#00FFFF"));
        pNearMiss.setTextSize(44);
        pNearMiss.setTextAlign(Paint.Align.CENTER);
        pNearMiss.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pParticle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pParticle.setStyle(Paint.Style.FILL);

        pTrail = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTrail.setStyle(Paint.Style.FILL);

        pVortex = new Paint(Paint.ANTI_ALIAS_FLAG);
        pVortex.setStyle(Paint.Style.STROKE);
        pVortex.setStrokeWidth(3f);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = getWidth();
        screenH = getHeight();
        centerX = screenW / 2f;
        centerY = screenH / 2f;
        innerRadius = screenW * 0.25f;
        outerRadius = screenW * 0.40f;
        playerSize = screenW * 0.025f;

        stars = new float[150][3];
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = rand.nextFloat() * screenW;
            stars[i][1] = rand.nextFloat() * screenH;
            stars[i][2] = rand.nextFloat() * 2 + 1;
        }

        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {
        screenW = w; screenH = ht;
        centerX = w / 2f; centerY = ht / 2f;
        innerRadius = w * 0.25f; outerRadius = w * 0.40f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try { gameThread.join(); } catch (InterruptedException e) {}
    }

    @Override
    public void run() {
        long target = 1000000000L / 60;
        while (isRunning) {
            long start = System.nanoTime();
            update();
            render();
            long elapsed = System.nanoTime() - start;
            long sleep = (target - elapsed) / 1000000;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException e) {}
            }
        }
    }

    private void update() {
        animTime++;
        vortexAngle += 0.5f;

        shakeMag *= 0.92f;
        if (shakeMag > 0.5f) {
            shakeX = (rand.nextFloat() - 0.5f) * 2 * shakeMag;
            shakeY = (rand.nextFloat() - 0.5f) * 2 * shakeMag;
        } else { shakeX = 0; shakeY = 0; }

        updateParticles();

        if (gameState == STATE_MENU) {
            playerAngle += 1.0f;
            if (playerAngle >= 360) playerAngle -= 360;
            return;
        }

        if (gameState == STATE_GAME_OVER) {
            deathFlash = Math.max(0, deathFlash - 0.02f);
            return;
        }

        playTime++;
        currentSpeed = baseSpeed + (playTime / 600f) * 3f;
        playerAngle += currentSpeed;
        if (playerAngle >= 360) playerAngle -= 360;

        float pRad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + pRad * (float) Math.cos(Math.toRadians(playerAngle));
        float py = centerY + pRad * (float) Math.sin(Math.toRadians(playerAngle));

        int trailColor = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
        trail.add(new float[]{px, py, trailColor, 1f});
        if (trail.size() > 25) trail.remove(0);

        if (combo > 0) {
            comboTimer--;
            if (comboTimer <= 0) { combo = 0; scoreMultiplier = 1; }
        }
        if (nearMissTextTimer > 0) nearMissTextTimer--;
        if (nearMissFlash > 0) nearMissFlash -= 0.05f;

        obstacleTimer++;
        float spawnRate = Math.max(30, 90 - playTime / 30f);
        if (obstacleTimer >= spawnRate) {
            obstacleTimer = 0;
            spawnObstacle();
        }

        orbTimer++;
        if (orbTimer >= 120) { orbTimer = 0; spawnOrb(); }

        Iterator<float[]> oi = obstacles.iterator();
        while (oi.hasNext()) {
            float[] o = oi.next();
            o[0] += o[2]; // angle += speed
            if (o[0] >= 360) o[0] -= 360;
            if (o[0] < 0) o[0] += 360;
            o[4]--; // life
            if (o[4] <= 0) { oi.remove(); continue; }

            boolean obsOuter = o[1] == 1;
            if (obsOuter == playerOnOuter) {
                float ad = Math.abs(normAngle(playerAngle - o[0]));
                float half = o[3] / 2 + 5;
                if (ad < half) { gameOver(px, py); return; }
                if (ad < half + 10 && o[5] == 0) {
                    o[5] = 1;
                    score += (int)(25 * scoreMultiplier);
                    nearMissFlash = 1f;
                    nearMissText = "NEAR MISS! +" + (int)(25 * scoreMultiplier);
                    nearMissTextTimer = 45;
                    shakeMag = 8f;
                    float oRad = obsOuter ? outerRadius : innerRadius;
                    float ox = centerX + oRad * (float)Math.cos(Math.toRadians(o[0]));
                    float oy = centerY + oRad * (float)Math.sin(Math.toRadians(o[0]));
                    for (int i = 0; i < 8; i++) spawnParticle(ox, oy, Color.parseColor("#00FFFF"), true);
                }
            }
        }

        Iterator<float[]> ri = orbs.iterator();
        while (ri.hasNext()) {
            float[] orb = ri.next();
            orb[0] += orb[2];
            if (orb[0] >= 360) orb[0] -= 360;
            orb[3] += 0.1f; // pulse
            orb[4]--;
            if (orb[4] <= 0) { ri.remove(); continue; }

            boolean orbOuter = orb[1] == 1;
            if (orbOuter == playerOnOuter) {
                float ad = Math.abs(normAngle(playerAngle - orb[0]));
                if (ad < 12) {
                    combo++;
                    comboTimer = 180;
                    scoreMultiplier = 1 + (combo - 1) * 0.5f;
                    score += (int)(50 * scoreMultiplier);
                    float oRad = orbOuter ? outerRadius : innerRadius;
                    float ox = centerX + oRad * (float)Math.cos(Math.toRadians(orb[0]));
                    float oy = centerY + oRad * (float)Math.sin(Math.toRadians(orb[0]));
                    for (int i = 0; i < 15; i++) spawnParticle(ox, oy, Color.parseColor("#FFD700"), false);
                    ri.remove();
                }
            }
        }

        if ((int)playTime % 6 == 0) score++;
    }

    private void render() {
        Canvas c = null;
        SurfaceHolder h = getHolder();
        try {
            c = h.lockCanvas();
            if (c == null) return;
            c.save();
            c.translate(shakeX, shakeY);
            c.drawColor(Color.parseColor("#0A0A2E"));
            drawStars(c);
            drawVortex(c);
            drawOrbits(c);
            drawObstacles(c);
            drawOrbs(c);
            drawTrail(c);
            if (gameState != STATE_GAME_OVER) drawPlayer(c);
            drawParticles(c);
            if (gameState == STATE_MENU) drawMenu(c);
            else if (gameState == STATE_PLAYING) drawHUD(c);
            else drawGameOverUI(c);
            if (deathFlash > 0) c.drawColor(Color.argb((int)(deathFlash * 255), 255, 255, 255));
            if (nearMissFlash > 0) {
                int nc = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
                c.drawColor(Color.argb((int)(nearMissFlash * 40), Color.red(nc), Color.green(nc), Color.blue(nc)));
            }
            c.restore();
        } catch (Exception e) {
        } finally {
            if (c != null) try { h.unlockCanvasAndPost(c); } catch (Exception e) {}
        }
    }

    private void drawStars(Canvas c) {
        if (stars == null) return;
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (float[] s : stars) {
            int a = (int)(150 + 105 * Math.sin(animTime * 0.02 + s[0]));
            sp.setColor(Color.argb(Math.min(255, Math.max(0, a)), 255, 255, 255));
            c.drawCircle(s[0], s[1], s[2], sp);
        }
    }

    private void drawVortex(Canvas c) {
        for (int i = 0; i < 4; i++) {
            float ang = vortexAngle + i * 90;
            float r = innerRadius * 0.6f;
            Path path = new Path();
            for (int j = 0; j <= 50; j++) {
                float t = j / 50f;
                float a = (float)Math.toRadians(ang + t * 360);
                float rad = r * t;
                float x = centerX + rad * (float)Math.cos(a);
                float y = centerY + rad * (float)Math.sin(a);
                if (j == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            float blend = (float)(Math.sin(animTime * 0.02 + i) * 0.5 + 0.5);
            int col = blendCol(Color.parseColor("#7B2FBE"), Color.parseColor("#00BFFF"), blend);
            pVortex.setColor(col);
            pVortex.setAlpha((int)(100 + 50 * Math.sin(animTime * 0.03 + i)));
            pVortex.setStrokeWidth(2 + i * 0.5f);
            c.drawPath(path, pVortex);
        }
    }

    private void drawOrbits(Canvas c) {
        Paint gi = new Paint(Paint.ANTI_ALIAS_FLAG);
        gi.setColor(Color.parseColor("#00FFFF"));
        gi.setStyle(Paint.Style.STROKE);
        gi.setStrokeWidth(18);
        gi.setAlpha(40);
        gi.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(centerX, centerY, innerRadius, gi);
        pInnerOrbit.setAlpha((int)(180 + 75 * Math.sin(animTime * 0.03)));
        c.drawCircle(centerX, centerY, innerRadius, pInnerOrbit);

        Paint go = new Paint(Paint.ANTI_ALIAS_FLAG);
        go.setColor(Color.parseColor("#FF00FF"));
        go.setStyle(Paint.Style.STROKE);
        go.setStrokeWidth(18);
        go.setAlpha(40);
        go.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(centerX, centerY, outerRadius, go);
        pOuterOrbit.setAlpha((int)(180 + 75 * Math.sin(animTime * 0.03 + 1)));
        c.drawCircle(centerX, centerY, outerRadius, pOuterOrbit);
    }

    private void drawPlayer(Canvas c) {
        float rad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + rad * (float)Math.cos(Math.toRadians(playerAngle));
        float py = centerY + rad * (float)Math.sin(Math.toRadians(playerAngle));
        pPlayerGlow.setColor(playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF"));
        pPlayerGlow.setAlpha(120);
        c.drawCircle(px, py, playerSize + 12, pPlayerGlow);
        c.drawCircle(px, py, playerSize, pPlayer);
        Paint ic = new Paint(Paint.ANTI_ALIAS_FLAG);
        ic.setColor(playerOnOuter ? Color.parseColor("#FF88FF") : Color.parseColor("#88FFFF"));
        c.drawCircle(px, py, playerSize * 0.5f, ic);
    }

    private void drawObstacles(Canvas c) {
        for (float[] o : obstacles) {
            float rad = o[1] == 1 ? outerRadius : innerRadius;
            float start = o[0] - o[3] / 2;
            RectF ov = new RectF(centerX - rad, centerY - rad, centerX + rad, centerY + rad);
            float pulse = (float)(Math.sin(animTime * 0.15) * 0.5 + 0.5);
            int col = blendCol(Color.parseColor("#FF4444"), Color.parseColor("#FF8800"), pulse);

            Paint gl = new Paint(Paint.ANTI_ALIAS_FLAG);
            gl.setColor(col); gl.setStyle(Paint.Style.STROKE);
            gl.setStrokeWidth(22); gl.setAlpha(60); gl.setStrokeCap(Paint.Cap.ROUND);
            gl.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
            c.drawArc(ov, start, o[3], false, gl);

            Paint sl = new Paint(Paint.ANTI_ALIAS_FLAG);
            sl.setColor(col); sl.setStyle(Paint.Style.STROKE);
            sl.setStrokeWidth(14); sl.setStrokeCap(Paint.Cap.ROUND);
            c.drawArc(ov, start, o[3], false, sl);

            Paint br = new Paint(Paint.ANTI_ALIAS_FLAG);
            br.setColor(Color.parseColor("#FFCC00")); br.setStyle(Paint.Style.STROKE);
            br.setStrokeWidth(6); br.setStrokeCap(Paint.Cap.ROUND);
            br.setAlpha((int)(150 + 105 * pulse));
            c.drawArc(ov, start, o[3], false, br);
        }
    }

    private void drawOrbs(Canvas c) {
        Paint og = new Paint(Paint.ANTI_ALIAS_FLAG);
        og.setColor(Color.parseColor("#FFD700"));
        og.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.OUTER));
        Paint ob = new Paint(Paint.ANTI_ALIAS_FLAG);
        ob.setColor(Color.parseColor("#FFD700"));
        Paint sk = new Paint(Paint.ANTI_ALIAS_FLAG);
        sk.setColor(Color.WHITE);

        for (float[] orb : orbs) {
            float rad = orb[1] == 1 ? outerRadius : innerRadius;
            float ox = centerX + rad * (float)Math.cos(Math.toRadians(orb[0]));
            float oy = centerY + rad * (float)Math.sin(Math.toRadians(orb[0]));
            float ps = (float)(1 + 0.2 * Math.sin(orb[3]));
            float sz = playerSize * 0.8f * ps;
            og.setAlpha(100);
            c.drawCircle(ox, oy, sz + 15, og);
            c.drawCircle(ox, oy, sz, ob);
            sk.setAlpha((int)(200 * Math.abs(Math.sin(orb[3] * 2))));
            c.drawCircle(ox - sz * 0.3f, oy - sz * 0.3f, sz * 0.3f, sk);
        }
    }

    private void drawTrail(Canvas c) {
        for (int i = 0; i < trail.size(); i++) {
            float[] t = trail.get(i);
            float alpha = (float)(i + 1) / trail.size();
            pTrail.setColor((int)t[2]);
            pTrail.setAlpha((int)(alpha * 100));
            c.drawCircle(t[0], t[1], playerSize * alpha * 0.7f, pTrail);
        }
    }

    private void drawParticles(Canvas c) {
        for (float[] p : particles) {
            pParticle.setColor((int)p[4]);
            pParticle.setAlpha((int)(p[5] * 255));
            c.drawCircle(p[0], p[1], p[6], pParticle);
        }
    }

    private void drawMenu(Canvas c) {
        pTitle.setColor(Color.parseColor("#00FFFF"));
        pTitle.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05)));
        c.drawText("NEON", centerX, centerY - 180, pTitle);
        pTitle.setColor(Color.parseColor("#FF00FF"));
        pTitle.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05 + 1)));
        c.drawText("VORTEX", centerX, centerY - 80, pTitle);
        float sa = (float)(Math.sin(animTime * 0.04) * 0.3 + 0.7);
        pSub.setAlpha((int)(sa * 255));
        pSub.setTextSize(40);
        c.drawText("TAP TO START", centerX, centerY + 60, pSub);
        if (highScore > 0) {
            pHighScore.setAlpha(200);
            c.drawText("BEST: " + highScore, centerX, centerY + 140, pHighScore);
        }
        pSub.setTextSize(30); pSub.setAlpha(150);
        c.drawText("Tap to switch orbits", centerX, centerY + 220, pSub);
        c.drawText("Avoid barriers \u2022 Collect orbs", centerX, centerY + 270, pSub);
    }

    private void drawHUD(Canvas c) {
        pScore.setAlpha(220);
        c.drawText(String.valueOf(score), centerX, 120, pScore);
        if (combo > 1) {
            pCombo.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.1)));
            pCombo.setTextSize(Math.min(70, 40 + combo * 3));
            c.drawText("x" + String.format("%.1f", scoreMultiplier) + " COMBO!", centerX, 180, pCombo);
        }
        if (nearMissTextTimer > 0) {
            pNearMiss.setAlpha((int)(nearMissTextTimer / 45f * 255));
            c.drawText(nearMissText, centerX, centerY + outerRadius + 80, pNearMiss);
        }
    }

    private void drawGameOverUI(Canvas c) {
        c.drawColor(Color.argb(150, 0, 0, 0));
        pGameOver.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05)));
        c.drawText("GAME OVER", centerX, centerY - 100, pGameOver);
        pScore.setAlpha(255);
        c.drawText(String.valueOf(score), centerX, centerY + 20, pScore);
        if (score >= highScore && score > 0) {
            pHighScore.setColor(Color.parseColor("#FFD700"));
            c.drawText("\u2605 NEW BEST! \u2605", centerX, centerY + 80, pHighScore);
            pHighScore.setColor(Color.parseColor("#888888"));
        } else {
            pHighScore.setAlpha(200);
            c.drawText("BEST: " + highScore, centerX, centerY + 80, pHighScore);
        }
        float ra = (float)(Math.sin(animTime * 0.04) * 0.3 + 0.7);
        pRestart.setAlpha((int)(ra * 255));
        c.drawText("TAP TO RETRY", centerX, centerY + 180, pRestart);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameState == STATE_MENU) startGame();
            else if (gameState == STATE_PLAYING) switchOrbit();
            else if (gameState == STATE_GAME_OVER && deathFlash <= 0) gameState = STATE_MENU;
        }
        return true;
    }

    private void startGame() {
        gameState = STATE_PLAYING;
        score = 0; combo = 0; comboTimer = 0; scoreMultiplier = 1;
        playTime = 0; currentSpeed = baseSpeed; playerAngle = 0;
        playerOnOuter = false; obstacles.clear(); orbs.clear();
        particles.clear(); trail.clear(); obstacleTimer = 0;
        orbTimer = 0; shakeMag = 0; nearMissFlash = 0;
        nearMissTextTimer = 0; deathFlash = 0;
    }

    private void switchOrbit() {
        playerOnOuter = !playerOnOuter;
        float rad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + rad * (float)Math.cos(Math.toRadians(playerAngle));
        float py = centerY + rad * (float)Math.sin(Math.toRadians(playerAngle));
        int col = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
        for (int i = 0; i < 8; i++) spawnParticle(px, py, col, true);
        shakeMag = 3f;
    }

    private void gameOver(float px, float py) {
        gameState = STATE_GAME_OVER;
        deathFlash = 1f; shakeMag = 30f;
        for (int i = 0; i < 50; i++) spawnParticle(px, py, Color.WHITE, false);
        for (int i = 0; i < 30; i++) spawnParticle(px, py, Color.parseColor("#FF4444"), false);
        for (int i = 0; i < 20; i++) {
            int col = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
            spawnParticle(px, py, col, false);
        }
        if (score > highScore) {
            highScore = score;
            prefs.edit().putInt("highscore", highScore).apply();
        }
    }

    private void spawnObstacle() {
        // [angle, isOuter, speed, arcLength, life, nearMissGiven]
        float[] o = new float[6];
        o[0] = rand.nextFloat() * 360;
        o[1] = rand.nextBoolean() ? 1 : 0;
        o[2] = (rand.nextFloat() * 0.8f + 0.3f) * (rand.nextBoolean() ? 1 : -1);
        o[3] = 20 + rand.nextFloat() * 30;
        o[4] = 300 + rand.nextFloat() * 200;
        o[5] = 0;
        obstacles.add(o);
        if (playTime > 600 && rand.nextFloat() < 0.3f) {
            float[] o2 = new float[6];
            o2[0] = (o[0] + 120 + rand.nextFloat() * 120) % 360;
            o2[1] = o[1] == 1 ? 0 : 1;
            o2[2] = (rand.nextFloat() * 0.6f + 0.3f) * (rand.nextBoolean() ? 1 : -1);
            o2[3] = 20 + rand.nextFloat() * 25;
            o2[4] = 300 + rand.nextFloat() * 200;
            o2[5] = 0;
            obstacles.add(o2);
        }
    }

    private void spawnOrb() {
        // [angle, isOuter, speed, pulse, life]
        float[] orb = new float[5];
        orb[0] = rand.nextFloat() * 360;
        orb[1] = rand.nextBoolean() ? 1 : 0;
        orb[2] = (rand.nextFloat() * 0.4f + 0.2f) * (rand.nextBoolean() ? 1 : -1);
        orb[3] = rand.nextFloat() * 6.28f;
        orb[4] = 400;
        orbs.add(orb);
    }

    private void spawnParticle(float x, float y, int color, boolean small) {
        // [x, y, vx, vy, color, alpha, size]
        float[] p = new float[7];
        p[0] = x; p[1] = y;
        float angle = rand.nextFloat() * 360;
        float speed = small ? rand.nextFloat() * 3 + 1 : rand.nextFloat() * 8 + 2;
        p[2] = speed * (float)Math.cos(Math.toRadians(angle));
        p[3] = speed * (float)Math.sin(Math.toRadians(angle));
        p[4] = color;
        p[5] = 1f;
        p[6] = small ? rand.nextFloat() * 4 + 2 : rand.nextFloat() * 6 + 3;
        particles.add(p);
    }

    private void updateParticles() {
        Iterator<float[]> it = particles.iterator();
        while (it.hasNext()) {
            float[] p = it.next();
            p[0] += p[2]; p[1] += p[3];
            p[5] -= 0.02f; p[6] *= 0.97f;
            if (p[5] <= 0 || p[6] < 0.5f) it.remove();
        }
    }

    private float normAngle(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    private int blendCol(int c1, int c2, float r) {
        int red = (int)(Color.red(c1) * (1 - r) + Color.red(c2) * r);
        int grn = (int)(Color.green(c1) * (1 - r) + Color.green(c2) * r);
        int blu = (int)(Color.blue(c1) * (1 - r) + Color.blue(c2) * r);
        return Color.rgb(red, grn, blu);
    }

    public void pause() {
        isRunning = false;
        try { if (gameThread != null) gameThread.join(); } catch (InterruptedException e) {}
    }

    public void resume() {}
}
