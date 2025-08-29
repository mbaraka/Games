package com.baraka.game.zombie.engine

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ZombieGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    
    private lateinit var zombieEngine: Zombie3DEngine
    
    init {
        setupOpenGL()
    }
    
    private fun setupOpenGL() {
        // Configure OpenGL ES 1.0 (compatible with our current engine)
        setEGLContextClientVersion(1)
        
        // Set renderer
        val renderer = ZombieGLRenderer()
        setRenderer(renderer)
        
        // Configure rendering
        renderMode = RENDERMODE_CONTINUOUSLY
        
        // Initialize the engine
        zombieEngine = Zombie3DEngine()
        zombieEngine.create()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle touch events for camera control
        zombieEngine.handleTouch(event)
        return true
    }
    
    // Get engine for external control
    fun getEngine(): Zombie3DEngine = zombieEngine
    
    // Cleanup
    fun dispose() {
        zombieEngine.dispose()
    }
    
    // Custom renderer
    private inner class ZombieGLRenderer : Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            gl?.let { zombieEngine.onSurfaceCreated(it) }
        }
        
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            gl?.let { zombieEngine.resize(it, width, height) }
        }
        
        override fun onDrawFrame(gl: GL10?) {
            gl?.let { zombieEngine.render(it) }
        }
    }
}
