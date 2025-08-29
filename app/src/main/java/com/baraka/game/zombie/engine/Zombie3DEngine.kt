package com.baraka.game.zombie.engine

import android.view.MotionEvent
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

class Zombie3DEngine : ApplicationAdapter() {
    
    // Basic rendering
    private var shapeRenderer: ShapeRenderer? = null
    private var camera: OrthographicCamera? = null
    
    // Game state
    private var playerX = 0f
    private var playerY = 1f
    private var playerZ = 0f
    private var cameraRotationY = 0f
    private var cameraRotationX = 0f
    
    // Touch controls
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchSensitivity = 0.5f
    
    // Initialization state
    private var isInitialized = false
    
    override fun create() {
        // Don't initialize OpenGL components here - wait for onSurfaceCreated
        // This will be called by the renderer when OpenGL context is ready
    }
    
    fun onSurfaceCreated() {
        if (!isInitialized) {
            // Initialize basic rendering only when OpenGL context is ready
            shapeRenderer = ShapeRenderer()
            
            // Setup camera (2D view for now, we'll upgrade to 3D later)
            camera = OrthographicCamera()
            camera?.setToOrtho(false, 800f, 600f) // Default size, will be updated in resize
            camera?.update()
            
            isInitialized = true
        }
    }
    
    override fun render() {
        if (!isInitialized || shapeRenderer == null || camera == null) return
        
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        // Update camera
        camera?.update()
        shapeRenderer?.projectionMatrix = camera?.combined
        
        // Begin rendering
        shapeRenderer?.begin(ShapeRenderer.ShapeType.Filled)
        
        // Draw ground (simple rectangle)
        shapeRenderer?.setColor(0.5f, 0.5f, 0.5f, 1f)
        shapeRenderer?.rect(-25f, -25f, 50f, 50f)
        
        // Draw player (simple rectangle)
        shapeRenderer?.setColor(0f, 0f, 1f, 1f)
        shapeRenderer?.rect(playerX - 0.5f, playerY - 0.5f, 1f, 2f)
        
        shapeRenderer?.end()
    }
    
    override fun resize(width: Int, height: Int) {
        if (camera != null) {
            camera?.setToOrtho(false, width.toFloat(), height.toFloat())
            camera?.update()
        }
    }
    
    override fun dispose() {
        shapeRenderer?.dispose()
        isInitialized = false
    }
    
    // Touch controls for mobile
    fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.x - lastTouchX) * touchSensitivity
                val deltaY = (event.y - lastTouchY) * touchSensitivity
                
                // Rotate camera (look around)
                cameraRotationY -= deltaX * 0.5f
                cameraRotationX -= deltaY * 0.5f
                
                // Clamp vertical rotation
                cameraRotationX = cameraRotationX.coerceIn(-89f, 89f)
                
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
        }
        return false
    }
    
    // Player movement
    fun movePlayer(forward: Boolean, backward: Boolean, left: Boolean, right: Boolean) {
        val moveSpeed = 0.1f
        val angleRad = Math.toRadians(cameraRotationY.toDouble())
        
        if (forward) {
            playerX += (sin(angleRad) * moveSpeed).toFloat()
            playerZ -= (cos(angleRad) * moveSpeed).toFloat()
        }
        if (backward) {
            playerX -= (sin(angleRad) * moveSpeed).toFloat()
            playerZ += (cos(angleRad) * moveSpeed).toFloat()
        }
        if (left) {
            playerX -= (cos(angleRad) * moveSpeed).toFloat()
            playerZ -= (sin(angleRad) * moveSpeed).toFloat()
        }
        if (right) {
            playerX += (cos(angleRad) * moveSpeed).toFloat()
            playerZ += (sin(angleRad) * moveSpeed).toFloat()
        }
    }
    
    // Get player position
    fun getPlayerPosition(): Triple<Float, Float, Float> = Triple(playerX, playerY, playerZ)
    
    // Check if engine is ready
    fun isReady(): Boolean = isInitialized
}
