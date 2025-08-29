package com.baraka.game.zombie.engine

import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class Zombie3DEngine {
    
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
    
    // OpenGL state
    private var initialized = false
    
    // Vertex buffers
    private lateinit var groundVertices: FloatBuffer
    private lateinit var playerVertices: FloatBuffer
    private lateinit var buildingVertices: FloatBuffer
    private lateinit var wallVertices: FloatBuffer
    
    fun create() {
        // Mark as initialized - OpenGL context will be set up in onSurfaceCreated
        initialized = true
    }
    
    fun onSurfaceCreated(gl: GL10) {
        // Set up OpenGL state
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        gl.glEnable(GL10.GL_DEPTH_TEST)
        gl.glDepthFunc(GL10.GL_LEQUAL)
        
        // Create vertex buffers
        createVertexBuffers()
        createEnvironmentVertices()
    }
    
    private fun createVertexBuffers() {
        // Ground vertices (simple quad)
        val groundData = floatArrayOf(
            -25f, 0f, -25f,  // bottom left
            25f, 0f, -25f,   // bottom right
            25f, 0f, 25f,    // top right
            -25f, 0f, 25f    // top left
        )
        
        groundVertices = ByteBuffer.allocateDirect(groundData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        groundVertices.put(groundData)
        groundVertices.position(0)
        
        // Player vertices (simple cube)
        val size = 0.5f
        val playerData = floatArrayOf(
            // Front face
            -size, 0f, -size,
            size, 0f, -size,
            size, size * 2, -size,
            -size, size * 2, -size,
            
            // Back face
            -size, 0f, size,
            size, 0f, size,
            size, size * 2, size,
            -size, size * 2, size,
            
            // Left face
            -size, 0f, -size,
            -size, 0f, size,
            -size, size * 2, size,
            -size, size * 2, -size,
            
            // Right face
            size, 0f, -size,
            size, 0f, size,
            size, size * 2, size,
            size, size * 2, -size,
            
            // Top face
            -size, size * 2, -size,
            size, size * 2, -size,
            size, size * 2, size,
            -size, size * 2, size
        )
        
        playerVertices = ByteBuffer.allocateDirect(playerData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        playerVertices.put(playerData)
        playerVertices.position(0)
    }
    
    private fun createEnvironmentVertices() {
        // Building vertices (simple cube)
        val buildingSize = 3f
        val buildingData = floatArrayOf(
            // Front face
            -buildingSize, 0f, -buildingSize,
            buildingSize, 0f, -buildingSize,
            buildingSize, buildingSize * 2, -buildingSize,
            -buildingSize, buildingSize * 2, -buildingSize,
            
            // Back face
            -buildingSize, 0f, buildingSize,
            buildingSize, 0f, buildingSize,
            buildingSize, buildingSize * 2, buildingSize,
            -buildingSize, buildingSize * 2, buildingSize,
            
            // Left face
            -buildingSize, 0f, -buildingSize,
            -buildingSize, 0f, buildingSize,
            -buildingSize, buildingSize * 2, buildingSize,
            -buildingSize, buildingSize * 2, -buildingSize,
            
            // Right face
            buildingSize, 0f, -buildingSize,
            buildingSize, 0f, buildingSize,
            buildingSize, buildingSize * 2, buildingSize,
            buildingSize, buildingSize * 2, -buildingSize,
            
            // Top face
            -buildingSize, buildingSize * 2, -buildingSize,
            buildingSize, buildingSize * 2, -buildingSize,
            buildingSize, buildingSize * 2, buildingSize,
            -buildingSize, buildingSize * 2, buildingSize
        )
        
        buildingVertices = ByteBuffer.allocateDirect(buildingData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buildingVertices.put(buildingData)
        buildingVertices.position(0)
        
        // Wall vertices (simple barrier)
        val wallHeight = 2f
        val wallWidth = 1f
        val wallData = floatArrayOf(
            // Front face
            -wallWidth, 0f, -wallWidth,
            wallWidth, 0f, -wallWidth,
            wallWidth, wallHeight, -wallWidth,
            -wallWidth, wallHeight, -wallWidth,
            
            // Back face
            -wallWidth, 0f, wallWidth,
            wallWidth, 0f, wallWidth,
            wallWidth, wallHeight, wallWidth,
            -wallWidth, wallHeight, wallWidth,
            
            // Left face
            -wallWidth, 0f, -wallWidth,
            -wallWidth, 0f, wallWidth,
            -wallWidth, wallHeight, wallWidth,
            -wallWidth, wallHeight, -wallWidth,
            
            // Right face
            wallWidth, 0f, -wallWidth,
            wallWidth, 0f, wallWidth,
            wallWidth, wallHeight, wallWidth,
            wallWidth, wallHeight, -wallWidth,
            
            // Top face
            -wallWidth, wallHeight, -wallWidth,
            wallWidth, wallHeight, -wallWidth,
            wallWidth, wallHeight, wallWidth,
            -wallWidth, wallHeight, wallWidth
        )
        
        wallVertices = ByteBuffer.allocateDirect(wallData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        wallVertices.put(wallData)
        wallVertices.position(0)
    }
    
    fun render(gl: GL10) {
        if (!initialized) return
        
        // Clear buffers
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        
        // Reset modelview matrix
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        
        // Apply camera transformations (positioned behind and above player)
        gl.glRotatef(-cameraRotationX, 1f, 0f, 0f)
        gl.glRotatef(-cameraRotationY, 0f, 1f, 0f)
        gl.glTranslatef(-playerX, -(playerY + 2f), -(playerZ + 5f))
        
        // Draw ground
        drawGround(gl)
        
        // Draw environment objects
        drawEnvironment(gl)
        
        // Draw player
        drawPlayer(gl)
    }
    
    private fun drawGround(gl: GL10) {
        gl.glColor4f(0.5f, 0.5f, 0.5f, 1.0f)
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, groundVertices)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f) // Reset color
    }
    
    private fun drawPlayer(gl: GL10) {
        gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f)
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, playerVertices)
        
        // Draw each face of the cube
        for (i in 0 until 5) {
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, i * 4, 4)
        }
        
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f) // Reset color
    }
    
    private fun drawEnvironment(gl: GL10) {
        // Draw buildings
        gl.glColor4f(0.8f, 0.6f, 0.4f, 1.0f) // Brown color for buildings
        
        // Building 1 (north)
        gl.glPushMatrix()
        gl.glTranslatef(0f, 0f, -15f)
        drawBuilding(gl)
        gl.glPopMatrix()
        
        // Building 2 (east)
        gl.glPushMatrix()
        gl.glTranslatef(15f, 0f, 0f)
        drawBuilding(gl)
        gl.glPopMatrix()
        
        // Building 3 (south)
        gl.glPushMatrix()
        gl.glTranslatef(0f, 0f, 15f)
        drawBuilding(gl)
        gl.glPopMatrix()
        
        // Building 4 (west)
        gl.glPushMatrix()
        gl.glTranslatef(-15f, 0f, 0f)
        drawBuilding(gl)
        gl.glPopMatrix()
        
        // Draw walls/barriers
        gl.glColor4f(0.7f, 0.7f, 0.7f, 1.0f) // Gray color for walls
        
        // Wall 1 (northeast)
        gl.glPushMatrix()
        gl.glTranslatef(8f, 0f, -8f)
        drawWall(gl)
        gl.glPopMatrix()
        
        // Wall 2 (southeast)
        gl.glPushMatrix()
        gl.glTranslatef(8f, 0f, 8f)
        drawWall(gl)
        gl.glPopMatrix()
        
        // Wall 3 (southwest)
        gl.glPushMatrix()
        gl.glTranslatef(-8f, 0f, 8f)
        drawWall(gl)
        gl.glPopMatrix()
        
        // Wall 4 (northwest)
        gl.glPushMatrix()
        gl.glTranslatef(-8f, 0f, -8f)
        drawWall(gl)
        gl.glPopMatrix()
        
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f) // Reset color
    }
    
    private fun drawBuilding(gl: GL10) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, buildingVertices)
        
        // Draw each face of the building
        for (i in 0 until 5) {
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, i * 4, 4)
        }
        
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
    
    private fun drawWall(gl: GL10) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, wallVertices)
        
        // Draw each face of the wall
        for (i in 0 until 5) {
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, i * 4, 4)
        }
        
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
    
    fun resize(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        
        // Set up perspective projection with wider view
        val aspect = width.toFloat() / height.toFloat()
        gl.glFrustumf(-aspect * 0.5f, aspect * 0.5f, -0.5f, 0.5f, 2f, 200f)
        
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
    }
    
    fun dispose() {
        initialized = false
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
    fun isReady(): Boolean = initialized
}
