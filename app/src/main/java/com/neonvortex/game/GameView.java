package com.neonvortex.game;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static final int STATE_PAUSED = 3;
    private static final int PU_SHIELD = 0;
    private static final int PU_SLOWMO = 1;
    private static final int PU_MAGNET = 2;

    private Thread gameThread;
    private volatile boolean isRunning;
    private int gameState = STATE_MENU;
    private int screenW, screenH;
    private float centerX, centerY, innerRadius, outerRadius;
    private float playerAngle = 0, playerSize = 24f;
    private boolean playerOnOuter = false;
    private float baseSpeed = 1.5f, currentSpeed;

    private ArrayList<float[]> obstacles = new ArrayList<>();
    private ArrayList<float[]> orbs = new ArrayList<>();
    private ArrayList<float[]> particles = new ArrayList<>();
    private ArrayList<float[]> trail = new ArrayList<>();
    private ArrayList<float[]> powerups = new ArrayList<>();
    private ArrayList<float[]> scorePopups = new ArrayList<>();
    private ArrayList<String> popupTexts = new ArrayList<>();

    private float obstacleTimer = 0, orbTimer = 0, powerupTimer = 0;
    private int score = 0, highScore = 0, combo = 0;
    private float comboTimer = 0, scoreMultiplier = 1;
    private float nearMissFlash = 0, nearMissTextTimer = 0;
    private String nearMissText = "";
    private float shakeX = 0, shakeY = 0, shakeMag = 0;
    private float vortexAngle = 0, deathFlash = 0, animTime = 0, playTime = 0;
    private float shieldTimer = 0, slowmoTimer = 0, magnetTimer = 0;
    private int orbsCollected = 0, nearMissCount = 0, currentLevel = 1;
    private float levelNotifyTimer = 0, bgPulse = 0, survivalTime = 0;
    private String levelText = "";
    private float[][] stars;
    private Random rand = new Random();
    private SharedPreferences prefs;

    private Paint pPlayer, pPlayerInner, pGlow, pShieldRing;
    private Paint pScore, pHighScore, pTitle, pSub;
    private Paint pGameOver, pRestart, pCombo, pNearMiss;
    private Paint pParticle, pTrail;
    private Paint pOrbitInner, pOrbitOuter, pOrbitGlowInner, pOrbitGlowOuter;
    private Paint pObstacle, pObstacleCore, pObstacleGlow;
    private Paint pOrbBody, pOrbGlow, pOrbShine;
    private Paint pVortex, pStarPaint;
    private Paint pPopup, pLevel, pStatLabel, pStatValue, pLine;
    private Paint pPauseText, pPauseSub, pPauseIcon;
    private Paint pPowerupBody, pPowerupGlow, pPowerupBorder, pPowerupSym;
    private Paint pIndicator, pMagnetLine;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        prefs = context.getSharedPreferences("neonvortex", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highscore", 0);
        initPaints();
    }

    private Paint mp(Paint.Style s, String col, float sw) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(col));
        p.setStyle(s); p.setStrokeWidth(sw); return p;
    }

    private Paint tp(String col, float sz, Paint.Align a, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(col));
        p.setTextSize(sz); p.setTextAlign(a);
        if (bold) p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return p;
    }

    private void initPaints() {
        pOrbitInner = mp(Paint.Style.STROKE, "#00FFFF", 5f);
        pOrbitOuter = mp(Paint.Style.STROKE, "#FF00FF", 5f);
        pOrbitGlowInner = mp(Paint.Style.STROKE, "#00FFFF", 20f);
        pOrbitGlowOuter = mp(Paint.Style.STROKE, "#FF00FF", 20f);
        pPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPlayer.setColor(Color.WHITE); pPlayer.setStyle(Paint.Style.FILL);
        pPlayerInner = new Paint(Paint.ANTI_ALIAS_FLAG); pPlayerInner.setStyle(Paint.Style.FILL);
        pGlow = new Paint(Paint.ANTI_ALIAS_FLAG); pGlow.setStyle(Paint.Style.FILL);
        pShieldRing = mp(Paint.Style.STROKE, "#00AAFF", 4f);
        pScore = tp("#FFFFFF", 80, Paint.Align.CENTER, true);
        pHighScore = tp("#888888", 36, Paint.Align.CENTER, false);
        pTitle = tp("#FFFFFF", 100, Paint.Align.CENTER, true);
        pSub = tp("#AAAAAA", 40, Paint.Align.CENTER, false);
        pGameOver = tp("#FF4444", 90, Paint.Align.CENTER, true);
        pRestart = tp("#FFFFFF", 44, Paint.Align.CENTER, false);
        pCombo = tp("#FFD700", 50, Paint.Align.CENTER, true);
        pNearMiss = tp("#00FFFF", 44, Paint.Align.CENTER, true);
        pPopup = tp("#FFFFFF", 36, Paint.Align.CENTER, true);
        pLevel = tp("#FFFFFF", 70, Paint.Align.CENTER, true);
        pStatLabel = tp("#AAAAAA", 32, Paint.Align.LEFT, false);
        pStatValue = tp("#FFFFFF", 32, Paint.Align.RIGHT, true);
        pPauseText = tp("#FFFFFF", 80, Paint.Align.CENTER, true);
        pPauseSub = tp("#AAAAAA", 40, Paint.Align.CENTER, false);
        pIndicator = tp("#FFFFFF", 28, Paint.Align.CENTER, true);
        pPowerupSym = tp("#FFFFFF", 20, Paint.Align.CENTER, true);
        pParticle = new Paint(Paint.ANTI_ALIAS_FLAG); pParticle.setStyle(Paint.Style.FILL);
        pTrail = new Paint(Paint.ANTI_ALIAS_FLAG); pTrail.setStyle(Paint.Style.FILL);
        pVortex = mp(Paint.Style.STROKE, "#7B2FBE", 3f);
        pObstacle = mp(Paint.Style.STROKE, "#FF4444", 14f); pObstacle.setStrokeCap(Paint.Cap.ROUND);
        pObstacleCore = mp(Paint.Style.STROKE, "#FFCC00", 6f); pObstacleCore.setStrokeCap(Paint.Cap.ROUND);
        pObstacleGlow = mp(Paint.Style.STROKE, "#FF4444", 24f); pObstacleGlow.setStrokeCap(Paint.Cap.ROUND);
        pOrbBody = new Paint(Paint.ANTI_ALIAS_FLAG); pOrbBody.setColor(0xFFFFD700); pOrbBody.setStyle(Paint.Style.FILL);
        pOrbGlow = new Paint(Paint.ANTI_ALIAS_FLAG); pOrbGlow.setColor(0xFFFFD700); pOrbGlow.setStyle(Paint.Style.FILL);
        pOrbShine = new Paint(Paint.ANTI_ALIAS_FLAG); pOrbShine.setColor(Color.WHITE); pOrbShine.setStyle(Paint.Style.FILL);
        pStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG); pStarPaint.setColor(Color.WHITE);
        pPauseIcon = new Paint(Paint.ANTI_ALIAS_FLAG); pPauseIcon.setColor(Color.WHITE); pPauseIcon.setAlpha(150);
        pPowerupBody = new Paint(Paint.ANTI_ALIAS_FLAG); pPowerupBody.setStyle(Paint.Style.FILL);
        pPowerupGlow = new Paint(Paint.ANTI_ALIAS_FLAG); pPowerupGlow.setStyle(Paint.Style.FILL);
        pPowerupBorder = mp(Paint.Style.STROKE, "#FFFFFF", 2f);
        pLine = new Paint(); pLine.setColor(0xFF444444); pLine.setStrokeWidth(2);
        pMagnetLine = mp(Paint.Style.STROKE, "#FF88FF", 2f); pMagnetLine.setAlpha(40);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = getWidth(); screenH = getHeight();
        centerX = screenW / 2f; centerY = screenH / 2f;
        innerRadius = screenW * 0.25f; outerRadius = screenW * 0.40f;
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
        screenW = w; screenH = ht; centerX = w / 2f; centerY = ht / 2f;
        innerRadius = w * 0.25f; outerRadius = w * 0.40f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try { if (gameThread != null) gameThread.join(); } catch (InterruptedException e) {}
    }

    @Override
    public void run() {
        long target = 1000000000L / 60;
        while (isRunning) {
            long start = System.nanoTime();
            if (gameState != STATE_PAUSED) update(); else animTime++;
            render();
            long elapsed = System.nanoTime() - start;
            long sleep = (target - elapsed) / 1000000;
            if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException e) {}
        }
    }

    private void update() {
        animTime++;
        bgPulse = (float)(Math.sin(animTime * 0.015) * 0.15);
        vortexAngle += 0.5f;
        shakeMag *= 0.92f;
        if (shakeMag > 0.5f) {
            shakeX = (rand.nextFloat() - 0.5f) * 2 * shakeMag;
            shakeY = (rand.nextFloat() - 0.5f) * 2 * shakeMag;
        } else { shakeX = 0; shakeY = 0; }
        updateParticles();
        updateScorePopups();
        if (gameState == STATE_MENU) {
            playerAngle += 1.0f;
            if (playerAngle >= 360) playerAngle -= 360;
            return;
        }
        if (gameState == STATE_GAME_OVER) {
            deathFlash = Math.max(0, deathFlash - 0.02f);
            return;
        }
        playTime++; survivalTime = playTime / 60f;
        float speedMul = slowmoTimer > 0 ? 0.5f : 1.0f;
        currentSpeed = (baseSpeed + (playTime / 600f) * 3f) * speedMul;
        playerAngle += currentSpeed;
        if (playerAngle >= 360) playerAngle -= 360;
        float pRad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + pRad * (float)Math.cos(Math.toRadians(playerAngle));
        float py = centerY + pRad * (float)Math.sin(Math.toRadians(playerAngle));
        int tc = playerOnOuter ? 0xFFFF00FF : 0xFF00FFFF;
        if (shieldTimer > 0) tc = 0xFF00AAFF;
        trail.add(new float[]{px, py, tc, 1f});
        if (trail.size() > 25) trail.remove(0);
        if (combo > 0) { comboTimer--; if (comboTimer <= 0) { combo = 0; scoreMultiplier = 1; } }
        if (nearMissTextTimer > 0) nearMissTextTimer--;
        if (nearMissFlash > 0) nearMissFlash -= 0.05f;
        if (shieldTimer > 0) shieldTimer--;
        if (slowmoTimer > 0) slowmoTimer--;
        if (magnetTimer > 0) magnetTimer--;
        int newLevel = score / 500 + 1;
        if (newLevel > currentLevel) {
            currentLevel = newLevel;
            levelText = "LEVEL " + currentLevel;
            levelNotifyTimer = 120; shakeMag = 5f;
        }
        if (levelNotifyTimer > 0) levelNotifyTimer--;
        obstacleTimer++;
        float spawnRate = Math.max(25, 85 - playTime / 25f);
        if (obstacleTimer >= spawnRate) { obstacleTimer = 0; spawnObstacle(); }
        orbTimer++;
        if (orbTimer >= 110) { orbTimer = 0; spawnOrb(); }
        powerupTimer++;
        if (powerupTimer >= 500 && rand.nextFloat() < 0.03f) { powerupTimer = 0; spawnPowerup(); }

        Iterator<float[]> oi = obstacles.iterator();
        while (oi.hasNext()) {
            float[] o = oi.next();
            o[0] += o[2] * speedMul;
            if (o[0] >= 360) o[0] -= 360; if (o[0] < 0) o[0] += 360;
            o[4]--;
            if (o[4] <= 0) { oi.remove(); continue; }
            boolean obsOuter = o[1] == 1;
            if (obsOuter == playerOnOuter) {
                float ad = Math.abs(normAngle(playerAngle - o[0]));
                float half = o[3] / 2 + 5;
                if (ad < half) {
                    if (shieldTimer > 0) {
                        shieldTimer = 0; shakeMag = 15f;
                        float oR = obsOuter ? outerRadius : innerRadius;
                        float ox = centerX + oR * (float)Math.cos(Math.toRadians(o[0]));
                        float oy = centerY + oR * (float)Math.sin(Math.toRadians(o[0]));
                        for (int i = 0; i < 20; i++) spawnParticle(ox, oy, 0xFF00AAFF, false);
                        addScorePopup(ox, oy, "SHIELD!", 0xFF00AAFF);
                        oi.remove(); continue;
                    }
                    gameOver(px, py); return;
                }
                if (ad < half + 12 && o[5] == 0) {
                    o[5] = 1; nearMissCount++;
                    int bonus = (int)(25 * scoreMultiplier); score += bonus;
                    nearMissFlash = 1f; nearMissText = "NEAR MISS! +" + bonus;
                    nearMissTextTimer = 45; shakeMag = 8f;
                    float oR = obsOuter ? outerRadius : innerRadius;
                    float ox = centerX + oR * (float)Math.cos(Math.toRadians(o[0]));
                    float oy = centerY + oR * (float)Math.sin(Math.toRadians(o[0]));
                    for (int i = 0; i < 8; i++) spawnParticle(ox, oy, 0xFF00FFFF, true);
                    addScorePopup(ox, oy, "+" + bonus, 0xFF00FFFF);
                }
            }
        }

        Iterator<float[]> ri = orbs.iterator();
        while (ri.hasNext()) {
            float[] orb = ri.next();
            orb[0] += orb[2] * speedMul;
            if (orb[0] >= 360) orb[0] -= 360; if (orb[0] < 0) orb[0] += 360;
            orb[3] += 0.1f; orb[4]--;
            if (orb[4] <= 0) { ri.remove(); continue; }
            boolean orbO = orb[1] == 1;
            if (magnetTimer > 0 && orbO == playerOnOuter) {
                orb[0] += normAngle(playerAngle - orb[0]) * 0.05f;
            }
            if (orbO == playerOnOuter) {
                float ad = Math.abs(normAngle(playerAngle - orb[0]));
                if (ad < (magnetTimer > 0 ? 25 : 12)) {
                    combo++; comboTimer = 180;
                    scoreMultiplier = 1 + (combo - 1) * 0.5f; orbsCollected++;
                    int bonus = (int)(50 * scoreMultiplier); score += bonus;
                    float oR = orbO ? outerRadius : innerRadius;
                    float ox = centerX + oR * (float)Math.cos(Math.toRadians(orb[0]));
                    float oy = centerY + oR * (float)Math.sin(Math.toRadians(orb[0]));
                    for (int i = 0; i < 15; i++) spawnParticle(ox, oy, 0xFFFFD700, false);
                    addScorePopup(ox, oy, "+" + bonus, 0xFFFFD700);
                    ri.remove();
                }
            }
        }

        Iterator<float[]> pi = powerups.iterator();
        while (pi.hasNext()) {
            float[] pu = pi.next();
            pu[0] += pu[2] * speedMul;
            if (pu[0] >= 360) pu[0] -= 360; if (pu[0] < 0) pu[0] += 360;
            pu[4]--; pu[5] += 0.08f;
            if (pu[4] <= 0) { pi.remove(); continue; }
            if ((pu[1] == 1) == playerOnOuter) {
                float ad = Math.abs(normAngle(playerAngle - pu[0]));
                if (ad < 15) {
                    int type = (int)pu[3];
                    float puR = pu[1] == 1 ? outerRadius : innerRadius;
                    float pux = centerX + puR * (float)Math.cos(Math.toRadians(pu[0]));
                    float puy = centerY + puR * (float)Math.sin(Math.toRadians(pu[0]));
                    if (type == PU_SHIELD) {
                        shieldTimer = 300;
                        for (int i = 0; i < 15; i++) spawnParticle(pux, puy, 0xFF00AAFF, false);
                        addScorePopup(pux, puy, "SHIELD!", 0xFF00AAFF);
                    } else if (type == PU_SLOWMO) {
                        slowmoTimer = 300;
                        for (int i = 0; i < 15; i++) spawnParticle(pux, puy, 0xFF00FF88, false);
                        addScorePopup(pux, puy, "SLOW-MO!", 0xFF00FF88);
                    } else {
                        magnetTimer = 300;
                        for (int i = 0; i < 15; i++) spawnParticle(pux, puy, 0xFFFF88FF, false);
                        addScorePopup(pux, puy, "MAGNET!", 0xFFFF88FF);
                    }
                    shakeMag = 5f; pi.remove();
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
            c.drawColor(getBgColor());
            drawStars(c); drawVortex(c); drawOrbits(c);
            drawObstacles(c); drawOrbs(c); drawPowerups(c);
            drawTrail(c);
            if (gameState != STATE_GAME_OVER) drawPlayer(c);
            drawParticles(c); drawScorePopups(c);
            if (gameState == STATE_MENU) drawMenu(c);
            else if (gameState == STATE_PLAYING) {
                drawHUD(c);
                if (levelNotifyTimer > 0) drawLevelNotify(c);
            } else if (gameState == STATE_GAME_OVER) drawGameOverUI(c);
            else if (gameState == STATE_PAUSED) { drawHUD(c); drawPauseUI(c); }
            if (deathFlash > 0) c.drawColor(Color.argb((int)(deathFlash * 200), 255, 255, 255));
            if (nearMissFlash > 0) {
                int nr = playerOnOuter ? 255 : 0, ng = playerOnOuter ? 0 : 255;
                c.drawColor(Color.argb((int)(nearMissFlash * 30), nr, ng, 255));
            }
            if (slowmoTimer > 0 && gameState == STATE_PLAYING) c.drawColor(Color.argb(20, 0, 255, 136));
            c.restore();
        } catch (Exception e) {
        } finally { if (c != null) try { h.unlockCanvasAndPost(c); } catch (Exception e) {} }
    }

    private void drawStars(Canvas c) {
        if (stars == null) return;
        for (float[] s : stars) {
            int a = Math.min(255, Math.max(0, (int)(150 + 105 * Math.sin(animTime * 0.02 + s[0]))));
            pStarPaint.setAlpha(a);
            c.drawCircle(s[0], s[1], s[2], pStarPaint);
        }
    }

    private void drawVortex(Canvas c) {
        for (int i = 0; i < 4; i++) {
            float ang = vortexAngle + i * 90;
            Path path = new Path();
            for (int j = 0; j <= 50; j++) {
                float t = j / 50f;
                float a = (float)Math.toRadians(ang + t * 360);
                float rad = innerRadius * 0.6f * t;
                float x = centerX + rad * (float)Math.cos(a);
                float y = centerY + rad * (float)Math.sin(a);
                if (j == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            float bl = (float)(Math.sin(animTime * 0.02 + i) * 0.5 + 0.5);
            pVortex.setColor(blendCol(0xFF7B2FBE, 0xFF00BFFF, bl));
            pVortex.setAlpha((int)(80 + 40 * Math.sin(animTime * 0.03 + i)));
            pVortex.setStrokeWidth(2 + i * 0.5f);
            c.drawPath(path, pVortex);
        }
    }

    private void drawOrbits(Canvas c) {
        pOrbitGlowInner.setAlpha((int)(30 + 15 * Math.sin(animTime * 0.03)));
        c.drawCircle(centerX, centerY, innerRadius, pOrbitGlowInner);
        pOrbitInner.setAlpha((int)(180 + 75 * Math.sin(animTime * 0.03)));
        c.drawCircle(centerX, centerY, innerRadius, pOrbitInner);
        pOrbitGlowOuter.setAlpha((int)(30 + 15 * Math.sin(animTime * 0.03 + 1)));
        c.drawCircle(centerX, centerY, outerRadius, pOrbitGlowOuter);
        pOrbitOuter.setAlpha((int)(180 + 75 * Math.sin(animTime * 0.03 + 1)));
        c.drawCircle(centerX, centerY, outerRadius, pOrbitOuter);
    }

    private void drawPlayer(Canvas c) {
        float rad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + rad * (float)Math.cos(Math.toRadians(playerAngle));
        float py = centerY + rad * (float)Math.sin(Math.toRadians(playerAngle));
        int gc = shieldTimer > 0 ? 0xFF00AAFF : (playerOnOuter ? 0xFFFF00FF : 0xFF00FFFF);
        pGlow.setColor(gc);
        pGlow.setAlpha(30); c.drawCircle(px, py, playerSize + 25, pGlow);
        pGlow.setAlpha(50); c.drawCircle(px, py, playerSize + 15, pGlow);
        pGlow.setAlpha(80); c.drawCircle(px, py, playerSize + 8, pGlow);
        c.drawCircle(px, py, playerSize, pPlayer);
        int ic = shieldTimer > 0 ? 0xFF88CCFF : (playerOnOuter ? 0xFFFF88FF : 0xFF88FFFF);
        pPlayerInner.setColor(ic);
        c.drawCircle(px, py, playerSize * 0.5f, pPlayerInner);
        if (shieldTimer > 0) {
            float sp = (float)(1 + 0.15 * Math.sin(animTime * 0.15));
            float sr = (playerSize + 18) * sp;
            boolean show = shieldTimer >= 90 || ((int)animTime % 10 >= 5);
            if (show) {
                pShieldRing.setAlpha(180); pShieldRing.setStrokeWidth(3f);
                c.drawCircle(px, py, sr, pShieldRing);
                pShieldRing.setAlpha(60); pShieldRing.setStrokeWidth(8f);
                c.drawCircle(px, py, sr + 4, pShieldRing);
            }
        }
    }

    private void drawObstacles(Canvas c) {
        for (float[] o : obstacles) {
            float rad = o[1] == 1 ? outerRadius : innerRadius;
            float start = o[0] - o[3] / 2;
            RectF ov = new RectF(centerX - rad, centerY - rad, centerX + rad, centerY + rad);
            float pulse = (float)(Math.sin(animTime * 0.15) * 0.5 + 0.5);
            int col = blendCol(0xFFFF4444, 0xFFFF8800, pulse);
            pObstacleGlow.setColor(col); pObstacleGlow.setAlpha(40);
            c.drawArc(ov, start, o[3], false, pObstacleGlow);
            pObstacle.setColor(col); pObstacle.setAlpha(230);
            c.drawArc(ov, start, o[3], false, pObstacle);
            pObstacleCore.setAlpha((int)(150 + 105 * pulse));
            c.drawArc(ov, start, o[3], false, pObstacleCore);
        }
    }

    private void drawOrbs(Canvas c) {
        for (float[] orb : orbs) {
            float rad = orb[1] == 1 ? outerRadius : innerRadius;
            float ox = centerX + rad * (float)Math.cos(Math.toRadians(orb[0]));
            float oy = centerY + rad * (float)Math.sin(Math.toRadians(orb[0]));
            float ps = (float)(1 + 0.2 * Math.sin(orb[3]));
            float sz = playerSize * 0.8f * ps;
            pOrbGlow.setAlpha(40); c.drawCircle(ox, oy, sz + 20, pOrbGlow);
            pOrbGlow.setAlpha(70); c.drawCircle(ox, oy, sz + 10, pOrbGlow);
            c.drawCircle(ox, oy, sz, pOrbBody);
            pOrbShine.setAlpha((int)(180 * Math.abs(Math.sin(orb[3] * 2))));
            c.drawCircle(ox - sz * 0.3f, oy - sz * 0.3f, sz * 0.3f, pOrbShine);
            if (magnetTimer > 0 && (orb[1] == 1) == playerOnOuter) {
                float pr = playerOnOuter ? outerRadius : innerRadius;
                float ppx = centerX + pr * (float)Math.cos(Math.toRadians(playerAngle));
                float ppy = centerY + pr * (float)Math.sin(Math.toRadians(playerAngle));
                c.drawLine(ox, oy, ppx, ppy, pMagnetLine);
            }
        }
    }

    private void drawPowerups(Canvas c) {
        for (float[] pu : powerups) {
            float rad = pu[1] == 1 ? outerRadius : innerRadius;
            float px = centerX + rad * (float)Math.cos(Math.toRadians(pu[0]));
            float py = centerY + rad * (float)Math.sin(Math.toRadians(pu[0]));
            float ps = (float)(1 + 0.3 * Math.sin(pu[5]));
            float sz = playerSize * 1.1f * ps;
            int type = (int)pu[3];
            int col; String sym;
            if (type == PU_SHIELD) { col = 0xFF00AAFF; sym = "S"; }
            else if (type == PU_SLOWMO) { col = 0xFF00FF88; sym = "T"; }
            else { col = 0xFFFF88FF; sym = "M"; }
            pPowerupGlow.setColor(col);
            pPowerupGlow.setAlpha(30); c.drawCircle(px, py, sz + 20, pPowerupGlow);
            pPowerupGlow.setAlpha(50); c.drawCircle(px, py, sz + 10, pPowerupGlow);
            pPowerupBody.setColor(col); pPowerupBody.setAlpha(220);
            c.drawCircle(px, py, sz, pPowerupBody);
            pPowerupBorder.setAlpha(200); c.drawCircle(px, py, sz, pPowerupBorder);
            pPowerupSym.setTextSize(sz * 1.2f);
            c.drawText(sym, px, py + sz * 0.4f, pPowerupSym);
        }
    }

    private void drawTrail(Canvas c) {
        for (int i = 0; i < trail.size(); i++) {
            float[] t = trail.get(i);
            float alpha = (float)(i + 1) / trail.size();
            pTrail.setColor((int)t[2]); pTrail.setAlpha((int)(alpha * 80));
            c.drawCircle(t[0], t[1], playerSize * alpha * 0.7f, pTrail);
        }
    }

    private void drawParticles(Canvas c) {
        for (float[] p : particles) {
            pParticle.setColor((int)p[4]);
            pParticle.setAlpha(Math.min(255, Math.max(0, (int)(p[5] * 255))));
            c.drawCircle(p[0], p[1], p[6], pParticle);
        }
    }

    private void drawScorePopups(Canvas c) {
        for (int i = 0; i < scorePopups.size(); i++) {
            float[] sp = scorePopups.get(i);
            pPopup.setColor((int)sp[4]);
            pPopup.setAlpha(Math.min(255, Math.max(0, (int)(sp[3] * 255))));
            pPopup.setTextSize(36 * (1f + (1f - sp[3]) * 0.3f));
            c.drawText(popupTexts.get(i), sp[0], sp[1], pPopup);
        }
    }

    private void drawMenu(Canvas c) {
        pTitle.setColor(0xFF00FFFF);
        pTitle.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05)));
        c.drawText("NEON", centerX, centerY - 180, pTitle);
        pTitle.setColor(0xFFFF00FF);
        pTitle.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05 + 1)));
        c.drawText("VORTEX", centerX, centerY - 80, pTitle);
        pSub.setAlpha((int)((Math.sin(animTime * 0.04) * 0.3 + 0.7) * 255));
        pSub.setTextSize(40);
        c.drawText("TAP TO START", centerX, centerY + 60, pSub);
        if (highScore > 0) { pHighScore.setAlpha(200); c.drawText("BEST: " + highScore, centerX, centerY + 140, pHighScore); }
        pSub.setTextSize(30); pSub.setAlpha(150);
        c.drawText("Tap to switch orbits", centerX, centerY + 220, pSub);
        c.drawText("Avoid barriers - Collect orbs", centerX, centerY + 270, pSub);
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
        float iy = combo > 1 ? 220 : 180;
        if (shieldTimer > 0) {
            pIndicator.setColor(0xFF00AAFF);
            c.drawText("SHIELD " + String.format("%.1f", shieldTimer / 60f) + "s", centerX, iy, pIndicator); iy += 32;
        }
        if (slowmoTimer > 0) {
            pIndicator.setColor(0xFF00FF88);
            c.drawText("SLOW-MO " + String.format("%.1f", slowmoTimer / 60f) + "s", centerX, iy, pIndicator); iy += 32;
        }
        if (magnetTimer > 0) {
            pIndicator.setColor(0xFFFF88FF);
            c.drawText("MAGNET " + String.format("%.1f", magnetTimer / 60f) + "s", centerX, iy, pIndicator);
        }
        pSub.setTextSize(24); pSub.setAlpha(120);
        c.drawText("LVL " + currentLevel, 70, 50, pSub);
        pPauseIcon.setAlpha(150);
        float pr = screenW - 50;
        c.drawRect(pr - 22, 35, pr - 14, 65, pPauseIcon);
        c.drawRect(pr - 8, 35, pr, 65, pPauseIcon);
    }

    private void drawLevelNotify(Canvas c) {
        float a = levelNotifyTimer > 90 ? (120 - levelNotifyTimer) / 30f : levelNotifyTimer / 90f;
        a = Math.min(1, Math.max(0, a));
        pLevel.setColor(blendCol(0xFF00FFFF, 0xFFFF00FF, (float)(Math.sin(animTime * 0.05) * 0.5 + 0.5)));
        pLevel.setAlpha((int)(a * 255));
        pLevel.setTextSize(70 * (1f + (1f - a) * 0.5f));
        c.drawText(levelText, centerX, centerY - outerRadius - 50, pLevel);
    }

    private void drawGameOverUI(Canvas c) {
        c.drawColor(Color.argb(180, 0, 0, 0));
        float y = centerY - 220;
        pGameOver.setAlpha((int)(200 + 55 * Math.sin(animTime * 0.05)));
        c.drawText("GAME OVER", centerX, y, pGameOver); y += 100;
        pScore.setAlpha(255);
        c.drawText(String.valueOf(score), centerX, y, pScore); y += 60;
        if (score >= highScore && score > 0) {
            pHighScore.setColor(0xFFFFD700); pHighScore.setAlpha(255);
            c.drawText("NEW BEST!", centerX, y, pHighScore);
            pHighScore.setColor(0xFF888888);
        } else { pHighScore.setAlpha(200); c.drawText("BEST: " + highScore, centerX, y, pHighScore); }
        y += 50;
        float sl = centerX - 140, sr = centerX + 140;
        c.drawLine(sl, y, sr, y, pLine); y += 40;
        pStatLabel.setAlpha(200); pStatValue.setAlpha(255);
        c.drawText("Time", sl, y, pStatLabel);
        c.drawText(String.format("%.1fs", survivalTime), sr, y, pStatValue); y += 38;
        c.drawText("Orbs", sl, y, pStatLabel);
        c.drawText(String.valueOf(orbsCollected), sr, y, pStatValue); y += 38;
        c.drawText("Near Misses", sl, y, pStatLabel);
        c.drawText(String.valueOf(nearMissCount), sr, y, pStatValue); y += 38;
        c.drawText("Level", sl, y, pStatLabel);
        c.drawText(String.valueOf(currentLevel), sr, y, pStatValue); y += 30;
        c.drawLine(sl, y, sr, y, pLine); y += 60;
        pRestart.setAlpha((int)((Math.sin(animTime * 0.04) * 0.3 + 0.7) * 255));
        c.drawText("TAP TO RETRY", centerX, y, pRestart);
    }

    private void drawPauseUI(Canvas c) {
        c.drawColor(Color.argb(180, 0, 0, 0));
        pPauseText.setAlpha(255);
        c.drawText("PAUSED", centerX, centerY - 40, pPauseText);
        pPauseSub.setAlpha((int)((Math.sin(animTime * 0.04) * 0.3 + 0.7) * 255));
        c.drawText("TAP TO RESUME", centerX, centerY + 40, pPauseSub);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX(), ty = event.getY();
            if (gameState == STATE_MENU) startGame();
            else if (gameState == STATE_PLAYING) {
                if (tx > screenW - 150 && ty < 150) gameState = STATE_PAUSED;
                else switchOrbit();
            } else if (gameState == STATE_PAUSED) gameState = STATE_PLAYING;
            else if (gameState == STATE_GAME_OVER && deathFlash <= 0) gameState = STATE_MENU;
        }
        return true;
    }

    private void startGame() {
        gameState = STATE_PLAYING;
        score = 0; combo = 0; comboTimer = 0; scoreMultiplier = 1;
        playTime = 0; currentSpeed = baseSpeed; playerAngle = 0;
        playerOnOuter = false; obstacles.clear(); orbs.clear();
        particles.clear(); trail.clear(); popups.clear(); powerups.clear();
        obstacleTimer = 0; orbTimer = 0; powerupTimer = 0;
        shakeMag = 0; nearMissFlash = 0; nearMissTextTimer = 0;
        deathFlash = 0; shieldActive = false; shieldTimer = 0;
        slowmoActive = false; slowmoTimer = 0; magnetActive = false;
        magnetTimer = 0; totalOrbs = 0; totalNearMiss = 0;
        level = 1; levelPopTimer = 0;
        bgHue = 0;
    }

    private void switchOrbit() {
        playerOnOuter = !playerOnOuter;
        float rad = playerOnOuter ? outerRadius : innerRadius;
        float px = centerX + rad * (float) Math.cos(Math.toRadians(playerAngle));
        float py = centerY + rad * (float) Math.sin(Math.toRadians(playerAngle));
        int col = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
        for (int i = 0; i < 10; i++) spawnParticle(px, py, col, true);
        shakeMag = 3f;
    }

    private void gameOver(float px, float py) {
        gameState = STATE_GAME_OVER;
        deathFlash = 1f;
        shakeMag = 30f;
        survivalTime = playTime / 60f;
        for (int i = 0; i < 60; i++) spawnParticle(px, py, Color.WHITE, false);
        for (int i = 0; i < 40; i++) spawnParticle(px, py, Color.parseColor("#FF4444"), false);
        for (int i = 0; i < 25; i++) {
            int col = playerOnOuter ? Color.parseColor("#FF00FF") : Color.parseColor("#00FFFF");
            spawnParticle(px, py, col, false);
        }
        if (score > highScore) {
            highScore = score;
            isNewBest = true;
            prefs.edit().putInt("highscore", highScore).apply();
        } else {
            isNewBest = false;
        }
    }

    private void spawnObstacle() {
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
        if (playTime > 1200 && rand.nextFloat() < 0.2f) {
            float[] o3 = new float[6];
            o3[0] = (o[0] + 180) % 360;
            o3[1] = o[1];
            o3[2] = -o[2];
            o3[3] = 25 + rand.nextFloat() * 20;
            o3[4] = 250;
            o3[5] = 0;
            obstacles.add(o3);
        }
    }

    private void spawnOrb() {
        float[] orb = new float[5];
        orb[0] = rand.nextFloat() * 360;
        orb[1] = rand.nextBoolean() ? 1 : 0;
        orb[2] = (rand.nextFloat() * 0.4f + 0.2f) * (rand.nextBoolean() ? 1 : -1);
        orb[3] = rand.nextFloat() * 6.28f;
        orb[4] = 400;
        orbs.add(orb);
    }

    private void spawnPowerup() {
        // [angle, isOuter, speed, type, life, pulse]
        // type: 0=shield, 1=slowmo, 2=magnet
        float[] p = new float[6];
        p[0] = rand.nextFloat() * 360;
        p[1] = rand.nextBoolean() ? 1 : 0;
        p[2] = (rand.nextFloat() * 0.3f + 0.1f) * (rand.nextBoolean() ? 1 : -1);
        p[3] = rand.nextInt(3);
        p[4] = 500;
        p[5] = 0;
        powerups.add(p);
    }

    private void spawnParticle(float x, float y, int color, boolean small) {
        float[] p = new float[7];
        p[0] = x; p[1] = y;
        float angle = rand.nextFloat() * 360;
        float speed = small ? rand.nextFloat() * 3 + 1 : rand.nextFloat() * 8 + 2;
        p[2] = speed * (float) Math.cos(Math.toRadians(angle));
        p[3] = speed * (float) Math.sin(Math.toRadians(angle));
        p[4] = color; p[5] = 1f;
        p[6] = small ? rand.nextFloat() * 4 + 2 : rand.nextFloat() * 6 + 3;
        particles.add(p);
    }

    private void addPopup(float x, float y, String text, int color) {
        // [x, y, alpha, offsetY, color, textLen] + text stored separately
        popups.add(new float[]{x, y, 1f, 0, color});
        popupTexts.add(text);
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
        int red = (int) (Color.red(c1) * (1 - r) + Color.red(c2) * r);
        int grn = (int) (Color.green(c1) * (1 - r) + Color.green(c2) * r);
        int blu = (int) (Color.blue(c1) * (1 - r) + Color.blue(c2) * r);
        return Color.rgb(red, grn, blu);
    }

    private int hsvToColor(float h, float s, float v) {
        float[] hsv = {h % 360, s, v};
        return Color.HSVToColor(hsv);
    }

    public void pause() {
        isRunning = false;
        try { if (gameThread != null) gameThread.join(); } catch (InterruptedException e) {}
    }

    public void resume() {
    }
}
