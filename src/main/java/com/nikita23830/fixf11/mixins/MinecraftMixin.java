package com.nikita23830.fixf11.mixins;

import com.nikita23830.fixf11.GuiError;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.stream.IStream;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Timer;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    private boolean fullscreen;
    @Shadow
    public int displayWidth;
    @Shadow
    public int displayHeight;
    @Shadow
    private int tempDisplayWidth;
    @Shadow
    private int tempDisplayHeight;
    @Shadow
    public GuiScreen currentScreen;
    @Shadow
    public GameSettings gameSettings;
    @Shadow
    public final Profiler mcProfiler = new Profiler();
    @Shadow
    private boolean isGamePaused;
    @Shadow
    public WorldClient theWorld;
    @Shadow
    private Timer timer;
    @Shadow
    private boolean refreshTexturePacksScheduled;
    @Shadow
    private SoundHandler mcSoundHandler;
    @Shadow
    public EntityClientPlayerMP thePlayer;
    @Shadow
    private Framebuffer framebufferMc;
    @Shadow
    public boolean skipRenderWorld;
    @Shadow
    public EntityRenderer entityRenderer;
    @Shadow
    long prevFrameTime = -1L;
    @Shadow
    public GuiAchievement guiAchievement;
    @Shadow
    private IStream field_152353_at;
    @Shadow
    int fpsCounter;
    @Shadow
    private IntegratedServer theIntegratedServer;
    @Shadow
    long debugUpdateTime;
    @Shadow
    private static int debugFPS;
    @Shadow
    public String debug = "";
    @Shadow
    private PlayerUsageSnooper usageSnooper;

    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
    public void toggleFullscreenM(CallbackInfo ci) {
        try {
            this.fullscreen = !this.fullscreen;


            if (this.fullscreen) {
                this.updateDisplayMode();
                this.displayWidth = Display.getDisplayMode().getWidth();
                this.displayHeight = Display.getDisplayMode().getHeight();

                if (this.displayWidth <= 0) {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0) {
                    this.displayHeight = 1;
                }
                if (!Display.isResizable()) {
                    Display.setResizable(true);
                }
            }  else {
                Display.setDisplayMode(new DisplayMode(this.tempDisplayWidth, this.tempDisplayHeight));
                this.displayWidth = this.tempDisplayWidth;
                this.displayHeight = this.tempDisplayHeight;

                if (this.displayWidth <= 0) {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0)  {
                    this.displayHeight = 1;
                }
                if (!Display.isResizable()) {
                    Display.setResizable(false);
                    Display.setResizable(true);
                }
            }

            if (this.currentScreen != null) {
                this.resize(this.displayWidth, this.displayHeight);
            } else {
                this.updateFramebufferSize();
            }

            Display.setFullscreen(this.fullscreen);
            Display.setVSyncEnabled(this.gameSettings.enableVsync);
            this.func_147120_f();

            if (!this.fullscreen) {
                Display.setResizable(false);
                Display.setResizable(true);
            }

        } catch (Exception exception) {
            System.out.println("Couldn\'t toggle fullscreen ");
            exception.printStackTrace();
        }
        ci.cancel();
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"), cancellable = true)
    private void runGameLoop(CallbackInfo ci) {

        Throwable error = null;
        this.mcProfiler.startSection("root");

        if (Display.isCreated() && Display.isCloseRequested()) {
            this.shutdown();
        }

        if (this.isGamePaused && this.theWorld != null) {
            float f = this.timer.renderPartialTicks;
            this.timer.updateTimer();
            this.timer.renderPartialTicks = f;
        } else {
            this.timer.updateTimer();
        }

        if ((this.theWorld == null || this.currentScreen == null) && this.refreshTexturePacksScheduled) {
            this.refreshTexturePacksScheduled = false;
            this.refreshResources();
        }

        long j = System.nanoTime();
        this.mcProfiler.startSection("tick");

        try {
            for (int i = 0; i < this.timer.elapsedTicks; ++i) {
                this.runTick();
            }
        } catch (Throwable e) {
            error = e;
        }

        this.mcProfiler.endStartSection("preRenderErrors");
        long k = System.nanoTime() - j;
        this.checkGLError("Pre render");
        RenderBlocks.fancyGrass = this.gameSettings.fancyGraphics;
        this.mcProfiler.endStartSection("sound");
        this.mcSoundHandler.setListener(this.thePlayer, this.timer.renderPartialTicks);
        this.mcProfiler.endSection();
        this.mcProfiler.startSection("render");
        GL11.glPushMatrix();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        this.framebufferMc.bindFramebuffer(true);
        this.mcProfiler.startSection("display");
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (this.thePlayer != null && this.thePlayer.isEntityInsideOpaqueBlock()) {
            this.gameSettings.thirdPersonView = 0;
        }

        this.mcProfiler.endSection();

        if (!this.skipRenderWorld) {
            try {
                FMLCommonHandler.instance().onRenderTickStart(this.timer.renderPartialTicks);
            } catch (Throwable e) {
                error = e;
            }
            this.mcProfiler.endStartSection("gameRenderer");
            try {
                this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
            } catch (Throwable e) {
                error = e;
            }
            this.mcProfiler.endSection();
            try {
                FMLCommonHandler.instance().onRenderTickEnd(this.timer.renderPartialTicks);
            } catch (Throwable e) {
                error = e;
            }
        }

        GL11.glFlush();
        this.mcProfiler.endSection();

        if (!Display.isActive() && this.fullscreen) {
            Minecraft.getMinecraft().toggleFullscreen();
        }

        if (this.gameSettings.showDebugInfo && this.gameSettings.showDebugProfilerChart) {
            if (!this.mcProfiler.profilingEnabled) {
                this.mcProfiler.clearProfiling();
            }

            this.mcProfiler.profilingEnabled = true;
            this.displayDebugInfo(k);
        } else {
            this.mcProfiler.profilingEnabled = false;
            this.prevFrameTime = System.nanoTime();
        }

        try {
            this.guiAchievement.func_146254_a();
            this.framebufferMc.unbindFramebuffer();
        } catch (Throwable e) {
            error = e;
        }
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        try {
            this.framebufferMc.framebufferRender(this.displayWidth, this.displayHeight);
        } catch (Throwable e) {
            error = e;
        }
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        try {
            this.entityRenderer.func_152430_c(this.timer.renderPartialTicks);
        } catch (Throwable e) {
            error = e;
        }
        GL11.glPopMatrix();
        this.mcProfiler.startSection("root");
        try {
            this.func_147120_f();
        } catch (Throwable e) {
            error = e;
        }
        Thread.yield();
        this.mcProfiler.startSection("stream");
        this.mcProfiler.startSection("update");
        try {
            this.field_152353_at.func_152935_j();
        } catch (Throwable e) {
            error = e;
        }
        this.mcProfiler.endStartSection("submit");
        try {
            this.field_152353_at.func_152922_k();
        } catch (Throwable e) {
            error = e;
        }
        this.mcProfiler.endSection();
        this.mcProfiler.endSection();
        this.checkGLError("Post render");
        ++this.fpsCounter;
        this.isGamePaused = this.isSingleplayer() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame() && !this.theIntegratedServer.getPublic();

        while (Minecraft.getMinecraft().getSystemTime() >= this.debugUpdateTime + 1000L) {
            debugFPS = this.fpsCounter;
            this.debug = debugFPS + " fps, " + WorldRenderer.chunksUpdated + " chunk updates";
            WorldRenderer.chunksUpdated = 0;
            this.debugUpdateTime += 1000L;
            this.fpsCounter = 0;
            this.usageSnooper.addMemoryStatsToSnooper();

            if (!this.usageSnooper.isSnooperRunning()) {
                this.usageSnooper.startSnooper();
            }
        }

        this.mcProfiler.endSection();

        if (this.isFramerateLimitBelowMax()) {
            Display.sync(this.getLimitFramerate());
        }

        if (error != null) {
            this.theWorld.sendQuittingDisconnectingPacket();
            this.loadWorld((WorldClient)null);
            this.displayGuiScreen(new GuiError(error));
        }
        ci.cancel();
    }

    @Shadow
    public abstract void displayGuiScreen(GuiScreen p_147108_1_);

    @Shadow
    public abstract void loadWorld(WorldClient p_71403_1_);

    @Shadow
    public abstract void runTick();

    @Shadow
    public abstract void displayDebugInfo(long p_71366_1_);

    @Shadow
    public abstract void checkGLError(String p_71361_1_);

    @Shadow
    public abstract boolean isSingleplayer();

    @Shadow
    public abstract boolean isFramerateLimitBelowMax();

    @Shadow
    public abstract int getLimitFramerate();

    @Shadow
    public abstract void refreshResources();

    @Shadow
    public abstract void shutdown();

    @Shadow
    public abstract void resize(int p_71370_1_, int p_71370_2_);

    @Shadow
    public abstract void func_147120_f();

    @Shadow
    public abstract void updateDisplayMode() throws LWJGLException;

    @Shadow
    public abstract void updateFramebufferSize();
}
