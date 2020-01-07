package com.example.industrialproject

import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.graphics.Bitmap.createScaledBitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_analyse.*
import android.net.Uri
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import android.widget.Toast
import android.graphics.drawable.BitmapDrawable
import android.graphics.RectF
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.createBitmap

class AnalyseActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    var imageURI: Uri? = null
    //Indicate if a button feature is already active on the image
    var buttonFeatureIsActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        var analyseDone = false

        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_analyse)
        imageURI = Uri.parse(intent.getStringExtra("imageUri"))
        Log.d("INFO", "message : $imageURI")
        analyse_image_view.setImageURI(imageURI)

        go_back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {}

            startActivity(intent)
        }

        analyse_btn.setOnClickListener {
            val imageURI: String = intent.getStringExtra("imageUri")

            if (!analyseDone) {

                try {
                    analyseImage(imageURI)
                    analyseDone = true
                }
                catch (e: Exception){
                    Log.d("ERROR", "Error : " + e.message + "\n" + e.cause)
                    Toast.makeText(this, "Error :  " + e.message, Toast.LENGTH_SHORT).show()
                }

            }
        }

        val AnalyseThread = object : Thread() {

            override fun run() {

                try {
                    super.run()

                    sleep(1000)
                } catch (e: Exception) {
                    Log.d("ERROR", "ehshshshhqhshssh")
                    e.printStackTrace()
                    throw e
                } finally {
                    // If everything is okay, go automatically to the main screen
                    sleep(5000)

                    finish()
                }
            }
        }
    }



    fun getDipFromPixels(px: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            Resources.getSystem().displayMetrics
        )
    }

    private fun analyseImage(imageUri:String)
    {
        //Image needs to be obtained with mutable option
        val options = BitmapFactory.Options()
        options.inMutable=true
        //val bitmapToAnalyse = BitmapFactory.decodeFile(imageUri, options)
        val bitmapToAnalyse = analyse_image_view.drawable.toBitmap()
        
        //Paint object to display red squares for the faces
        val rectPaint = Paint()
        rectPaint.strokeWidth = getDipFromPixels(5.0f)
        rectPaint.color = Color.RED
        rectPaint.style = Paint.Style.STROKE

        //Paint object to display green circles for the face features
        val circlePaint = Paint()
        circlePaint.strokeWidth = getDipFromPixels(2.0f)
        circlePaint.color = Color.GREEN
        circlePaint.style = Paint.Style.STROKE

        //Temp bitmap and canvas to draw on
        val tempBitmap = Bitmap.createBitmap(bitmapToAnalyse.width, bitmapToAnalyse.height, Bitmap.Config.RGB_565)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(bitmapToAnalyse, 0.0f, 0.0f, null)

        //Face detector
        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        if(!faceDetector.isOperational){
            faceDetector.release()
            throw ClassNotFoundException("FaceDetector can't work, check Google Play Service")
        }

        // Create a frame from the bitmap and detect faces
        val frame = Frame.Builder().setBitmap(bitmapToAnalyse).build()
        val faces = faceDetector.detect(frame)

        // Display rectangle for every detected face
        var faceNumberDetected = 0
        val scale = 1.0f

        //Create face selection buttons
        var listOfButtons: MutableList<Button> = mutableListOf()
        var listOffFaceValues: MutableList<List<Float>> = mutableListOf()

        for (i in 0 until faces.size())
        {
            faceNumberDetected += 1
            val thisFace:Face = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 10f, 10f, rectPaint)

            //Create button dynamically to be able to click on someone's face
            val dynamicButtonsLayout = findViewById<FrameLayout>(R.id.dynamic_buttons_layout)
            val buttonDynamicFace = Button(this)

            // setting layout_width and layout_height using layout parameters
            val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

            val heightRatio = analyse_image_view.height / bitmapToAnalyse.height.toDouble()
            val widthRatio = analyse_image_view.width / bitmapToAnalyse.width.toDouble()

            var biggestRatio:Double = widthRatio

            if (heightRatio < widthRatio && heightRatio < 1) {
                biggestRatio = heightRatio
            }

            var dynamicLayoutX = analyse_image_view.left + (x1 * biggestRatio).toInt()
            var dynamicLayoutY = analyse_image_view.top + (y1 * biggestRatio).toInt()

            layout.width = (thisFace.width * biggestRatio).toInt()
            layout.height = (thisFace.height * biggestRatio).toInt()

            layout.setMargins(dynamicLayoutX, dynamicLayoutY,0,0)

            buttonDynamicFace.layoutParams = layout
            buttonDynamicFace.alpha = 0.1f //transparency

            // add Button to layout and to the list
            dynamicButtonsLayout.addView(buttonDynamicFace)
            listOfButtons.add(buttonDynamicFace)

            // add values (x, y, width and height) to a list for later use
            val tempList = listOf(x1, y1, x2, y2)
            listOffFaceValues.add(tempList)

            //Add landmark on eyes, mouth, etc...
            for (landmark in thisFace.landmarks) {
                val cx = (landmark.position.x * scale)
                val cy = (landmark.position.y * scale)
                tempCanvas.drawCircle(cx, cy, getDipFromPixels(3.0f), circlePaint)
            }
        }

        //Set listener for every buttons created
        var faceFeatureButton = Button(this)

        for(i in 0 until listOfButtons.size)
        {
            var button = listOfButtons[i]

            //Add button onClickListener
            button.setOnClickListener(View.OnClickListener {
                //Add face option buttons
                var currentFace = faces.valueAt(i)
                if(!buttonFeatureIsActive) //if no button activate
                {

                    faceFeatureButton = displayFacialFeatureButtons(button, currentFace)
                    buttonFeatureIsActive = true
                }
                else //Remove other regeneration button
                {
                    val dynamicButtonsLayout = findViewById<FrameLayout>(R.id.dynamic_buttons_layout)
                    dynamicButtonsLayout.removeView(faceFeatureButton)
                    faceFeatureButton = displayFacialFeatureButtons(button, currentFace)
                }
            })
        }

        Toast.makeText(this, "$faceNumberDetected face(s) detected", Toast.LENGTH_SHORT).show()
        analyse_image_view.setImageDrawable(BitmapDrawable(resources, tempBitmap))
    }

    private fun displayFacialFeatureButtons(parentButton:Button, face:Face) : Button
    {
        val dynamicButtonsLayout = findViewById<FrameLayout>(R.id.dynamic_buttons_layout)
        val buttonFacialFeature = Button(this)

        val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

        layout.setMargins(parentButton.right , parentButton.top,0,0)
        buttonFacialFeature.text = "Regeneration"
        buttonFacialFeature.layoutParams = layout
        buttonFacialFeature.setLines(1)

        // add Button to layout
        dynamicButtonsLayout.addView(buttonFacialFeature)

        buttonFacialFeature.setOnClickListener()
        {
            facialReconstruction(face, analyse_image_view.drawable.toBitmap())
        }

        buttonFeatureIsActive = true


        return buttonFacialFeature
    }

    // Copies newFace into currentBitmap's face at position X and Y, returns the complete bitmap with copied face
    private fun computeFace(faceToReplaceX:Int, faceToReplaceY:Int, faceToReplaceHeight:Int, faceToReplaceWidth:Int, currentBitmap:Bitmap, newFace:Bitmap):Bitmap{

        // Need to resize newBitmap to faceToReplace dimensions !!
        val newFaceResized = createScaledBitmap(newFace , faceToReplaceWidth , faceToReplaceHeight, true)
        val modifiedBitmap = createBitmap(currentBitmap)

        // Copy selected face into a bitmap
        for(x in 0 until newFaceResized.width){
            for(y in 0 until newFaceResized.height){
                val pix = newFaceResized.getPixel(x, y)
                modifiedBitmap.setPixel(x + faceToReplaceX,y + faceToReplaceY, pix)
            }
        }
        return modifiedBitmap
    }

    // TODO : prendre un visage et non un carré noir
    private fun facialReconstruction(currentFace:Face, currentBitmap: Bitmap)
    {
        Toast.makeText(this, "Face reconstruction", Toast.LENGTH_SHORT).show()

        var newFace = Bitmap.createBitmap(currentBitmap.width, currentBitmap.height, Bitmap.Config.RGB_565)

        var modifiedBitmap = computeFace(currentFace.position.x.toInt(), currentFace.position.y.toInt(), currentFace.height.toInt(), currentFace.width.toInt(), currentBitmap, newFace) // Need face to copy
        analyse_image_view.setImageDrawable(BitmapDrawable(resources, modifiedBitmap))
           // file:///android_res/drawable/
    }

}