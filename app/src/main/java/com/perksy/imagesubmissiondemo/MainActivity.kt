package com.perksy.imagesubmissiondemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.perksy.imagesubmissiondemo.ImageUri.latestTmpUri
import com.perksy.imagesubmissiondemo.ui.theme.ImageSubmissionDemoTheme
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageSubmissionDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

//                    Gallery()

//                    Greeting("Hello World!")

//                    Camera()

//                    StartCamera()

//                    LazyVerticalGridDemo()

//                    CameraPermissionUI()

//                    CameraPreview()

                    ImageSubmissionDemo()
                }
            }
        }
    }
}

@Composable
fun ImageSubmissionDemo() {

    var switchFlag by remember {
        mutableStateOf(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    var galleryUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var cameraUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current

    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        galleryUri = uri
        switchFlag = true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            cameraUri = latestTmpUri
            switchFlag = false
        }
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f, false)) {

            if (switchFlag) {
                GalleryBitmap(galleryUri, bitmap, context)
            } else {
                CameraBitmap(cameraUri, bitmap, context)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Button(onClick = {
                    lifecycleOwner.lifecycleScope.launchWhenStarted {
                        getTmpFileUri(context).let { uri ->
                            latestTmpUri = uri
                            cameraLauncher.launch(uri)
                        }
                    }
                }) {
                    Text(text = "Take Picture")
                }

                Button(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Text(text = "Photo Library")
                }
            }
        }
    }
}

@Composable
private fun CameraBitmap(
    cameraUri: Uri?,
    bitmap: MutableState<Bitmap?>,
    context: Context
) {
    cameraUri?.let {
        if (Build.VERSION.SDK_INT < 28) {
            bitmap.value = MediaStore.Images
                .Media.getBitmap(context.contentResolver, it)

        } else {
            val source = ImageDecoder
                .createSource(context.contentResolver, it)
            bitmap.value = ImageDecoder.decodeBitmap(source)
        }

        bitmap.value?.let { btm ->
            Image(
                bitmap = btm.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

private fun getTmpFileUri(context: Context): Uri {
    val tmpFile = File.createTempFile("tmp_image_file_", ".jpeg").apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
}

@Composable
private fun GalleryBitmap(
    imageUri: Uri?,
    bitmap: MutableState<Bitmap?>,
    context: Context
) {
    imageUri?.let {
        if (Build.VERSION.SDK_INT < 28) {
            bitmap.value = MediaStore.Images
                .Media.getBitmap(context.contentResolver, it)

        } else {
            val source = ImageDecoder
                .createSource(context.contentResolver, it)
            bitmap.value = ImageDecoder.decodeBitmap(source)
        }

        bitmap.value?.let { btm ->
            Image(
                bitmap = btm.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
fun Camera() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.previewView
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    /*val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Accepted
            Log.d("TAG", "Camera: GRANTED")
        } else {
            // Permission Denied
            Log.d("TAG", "Camera: DENIED")
        }
    }*/

    /*val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultsMap ->
        resultsMap.forEach {
            Log.i("TAG", "Permission: ${it.key}, granted: ${it.value}")
        }
    }*/


    AndroidView(factory = {
        previewView
    }, modifier = Modifier.fillMaxSize()) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val faceAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    faceAnalysis
                )
                /*when {
                    //Check permission
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PERMISSION_GRANTED -> {
                        // You can use the API that requires the permission.
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            faceAnalysis
                        )
                    }
                    else -> {
                        // Asking for permission
                        launcher.launch(Manifest.permission.CAMERA)
                        *//*launcher.launch(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                            )
                        )*//*
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            faceAnalysis
                        )
                    }
                }*/
            } catch (e: Exception) {
                Log.e("TAG", "Camera: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}


@Composable
fun Gallery() {

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val context = LocalContext.current
    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column {
        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = "Pick image")
        }

        Spacer(modifier = Modifier.height(12.dp))

        imageUri?.let {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap.value = MediaStore.Images
                    .Media.getBitmap(context.contentResolver, it)

            } else {
                val source = ImageDecoder
                    .createSource(context.contentResolver, it)
                bitmap.value = ImageDecoder.decodeBitmap(source)
            }

            bitmap.value?.let { btm ->
                Image(
                    bitmap = btm.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(400.dp)
                )
                /*AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            id = R.id.image_preview
                            setImageBitmap(btm)
                        }
                    })*/
            }
        }
    }
}

@Composable
fun CameraPreview() {

    val lifecycleOwner = LocalLifecycleOwner.current

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var tempUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current

    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            imageUri = latestTmpUri
            Log.d("TAG", "CameraPreview: $imageUri")
        }
        Log.d("TAG", "CameraPreview: $imageUri")
    }

    Column {
        Button(onClick = {
            //launcher.launch("image/*")
            lifecycleOwner.lifecycleScope.launchWhenStarted {
                getTmpFileUri(context).let { uri ->
                    latestTmpUri = uri
                    launcher.launch(uri)
                }
            }
        }) {
            Text(text = "Capture image")
        }

        Spacer(modifier = Modifier.height(12.dp))

        imageUri?.let {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap.value = MediaStore.Images
                    .Media.getBitmap(context.contentResolver, it)

            } else {
                val source = ImageDecoder
                    .createSource(context.contentResolver, it)
                bitmap.value = ImageDecoder.decodeBitmap(source)
            }

            bitmap.value?.let { btm ->
                /*AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            id = R.id.image_preview
                            setImageBitmap(btm)
                        }
                    })*/
                Image(
                    bitmap = btm.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(400.dp)
                )
            }
        }
        DisposableEffect(Unit) {
            bitmap.value = null
            onDispose {
                bitmap.value = null
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyVerticalGridDemo() {
    val list = (1..10).map { it.toString() }

    LazyVerticalGrid(
        cells = GridCells.Fixed(2),

        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp,
            end = 12.dp,
            bottom = 16.dp
        ),
        content = {
            items(list.size) { index ->
                Card(
                    backgroundColor = Color.Red,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    elevation = 8.dp,
                ) {
                    Text(
                        text = list[index],
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String) {
    Row {
        AndroidView(
            factory = {
                TextView(it).apply {
                    id = R.id.hello
                    this.text = name
                }
            },
            modifier = Modifier
                .height(50.dp)
                .background(Color.Yellow)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AndroidView(
            factory = {
                TextView(it).apply {
                    id = R.id.hello
                    this.text = name + "Parth"
                }
            },
            modifier = Modifier
                .height(50.dp)
                .background(Color.Red)
        )
    }
}

@Composable
fun PermissionNotGrantedUI(
    onYesClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {

            Text(
                text = "Camera permission",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The app needs Camera permission to be able to capture a picture",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onYesClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "OK")
                }

                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text(text = "No")
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun CameraPermissionUI() {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var doNotShowMeRationale by rememberSaveable {
        mutableStateOf(false)
    }

    PermissionRequired(
        permissionState = permissionState,
        permissionNotGrantedContent = {
            if (doNotShowMeRationale) {
                Text(
                    text = "Camera permission deni  ed. " +
                            "You can accept in settings for app to work properly"
                )
            } else {
                PermissionNotGrantedUI(
                    onYesClick = { permissionState.launchPermissionRequest() },
                    onCancelClick = { doNotShowMeRationale = true })
            }
        },
        permissionNotAvailableContent = {

            PermissionNotAvailableContent(onOpenSettingsClick = {
                context.openSettings()
            })
        },
        content = {
            Camera()
        })
}

fun Context.openSettings() {

    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}

@Composable
fun PermissionNotAvailableContent(
    onOpenSettingsClick: () -> Unit
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera Permission",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The app requires Camera permission to be " +
                        "able to capture photos and display." +
                        " Please accept open settings " +
                        "and accept the permission"
            )
            Button(onClick = onOpenSettingsClick) {
                Text(text = "Open Settings")
            }
        }
    }
}

@Composable
private fun StartCamera() {


    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.previewView
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    AndroidView(factory = {
        previewView
    }, modifier = Modifier.fillMaxSize()) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }
}

private class FaceAnalyzer() : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val image = image.image
        image?.close()
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ImageSubmissionDemoTheme {
        Greeting("Android")
    }
}