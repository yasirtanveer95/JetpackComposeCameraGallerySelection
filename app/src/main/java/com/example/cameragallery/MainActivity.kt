package com.example.cameragallery

import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.cameragallery.ui.theme.CameraGalleryTheme
import com.example.cameragallery.ui.theme.Shapes
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class MainActivity : ComponentActivity() {
    lateinit var file: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var result by remember { mutableStateOf<Bitmap?>(null) }
            result?.let { bitmap ->
                AddImageView(bitmap)
            }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Greeting("Android")
                val takePicture =
                    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
                        result = it
                    }
                val selectGallery =
                    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                        result = BitmapFactory.decodeStream(it?.let { uri ->
                            contentResolver.openInputStream(uri)
                        })
                        it?.let { file = copyFileToInternalStorage(it).toString() }
                        // Log.d("showfile", file)
                    }
                /*For Multiple Photo selection */
//                rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(2)) {
//                    result = BitmapFactory.decodeStream(it.let { uri ->
//                        contentResolver.openInputStream(uri[0])
//                    })
//                    it.let { file = copyFileToInternalStorage(it[0]).toString() }
//                    // Log.d("showfile", file)
//                }
                AddButton("Open Camera") {
                    takePicture.launch()
                }
                AddButton("Open Gallery") {
                    selectGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                AddButton("Share Doc") {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "*/*"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "test")
                    shareIntent.putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                            applicationContext,
                            "com.example.cameragallery.provider",
                            File(file)
                        )
                    )
                    startActivity(Intent.createChooser(shareIntent, "Share"))
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        CameraGalleryTheme {
        }
    }

    @Composable
    fun Greeting(name: String) {
        Text(
            text = "Hello $name!",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun AddButton(text: String, onClick: () -> Unit) {
        Button(
            onClick = { onClick() },
            shape = Shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = text,
                color = Color.Green,
                fontSize = 18.sp,
                maxLines = 1,
                textAlign = TextAlign.Justify
            )
        }
    }

    @Composable
    fun AddImageView(bitmap: Bitmap?) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(10.dp),
            backgroundColor = Color.Magenta,
            elevation = 2.dp,
            border = BorderStroke(2.dp, Color.Green),
            shape = CircleShape
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp, Color.Black,
                            CircleShape
                        )
                )
            }
        }
    }

    private fun copyFileToInternalStorage(uri: Uri, newDirName: String = "exportedFiles"): String? {
        val returnUri: Uri = uri
        val returnCursor: Cursor = applicationContext.getContentResolver().query(
            returnUri, arrayOf(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )!!


        /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val output: File
        if (newDirName != "") {
            val dir: File =
                File(applicationContext.getExternalFilesDir(null).toString() + "/" + newDirName)
            if (!dir.exists()) {
                dir.mkdir()
            }
            output =
                File(
                    applicationContext.getExternalFilesDir(null)
                        .toString() + "/" + newDirName + "/" + name
                )
        } else {
            output = File(applicationContext.getExternalFilesDir(null).toString() + "/" + name)
        }
        try {
            val inputStream: InputStream =
                applicationContext.getContentResolver().openInputStream(uri)!!
            val outputStream = FileOutputStream(output)
            var read = 0
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message.toString())
        }
        return output.path
    }

    private fun loadFilesFromSharedStorage() {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            val selection = when (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                true -> "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                else -> MediaStore.Images.Media.DATA + " like ? "
            }

            val selectionArgs = arrayOf("%test%") // Test was my folder name
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            val uriExternal = MediaStore.Files.getContentUri("external")
            contentResolver.query(
                uriExternal,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (it.moveToNext()) {
                    try {
                        val contentUri: Uri = ContentUris.withAppendedId(
                            uriExternal,
                            it.getLong(idColumn)
                        )/*Use this Uri next*/
//                        copyFileToInternalStorage(contentUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}