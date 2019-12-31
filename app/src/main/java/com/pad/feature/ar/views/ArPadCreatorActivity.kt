package com.pad.feature.ar.views

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pad.R
import com.pad.feature.ar.viewModels.ArViewModel
import com.pad.feature.base.BaseActivity
import com.pad.feature.menu.models.Product
import com.pad.feature.takepictures.TakePicturesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.pad.feature.menu.views.IFurniture
import com.pad.feature.menu.views.ProductsAdapter
import com.pad.util.UtilMethods
import com.pad.util.gestures.CustomGestureDetector
import com.pad.util.gestures.CustomOnGestureListener
import com.pad.util.gestures.IGesture
import com.pad.util.graphics.PointerDrawable

class ArPadCreatorActivity : BaseActivity(), IFurniture, IGesture {
    private lateinit var arFragment: ArFragment
    private lateinit var btnRemove: Button
    private lateinit var btnTakePicture: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ProductsAdapter

    private val pointer = PointerDrawable()
    private var isTracking: Boolean = false
    private var isHitting: Boolean = false

    private lateinit var mGestureDetector: CustomGestureDetector
    private lateinit var gestureListener: CustomOnGestureListener
    private lateinit var arViewModel: ArViewModel
    private lateinit var takePicturesViewModel: TakePicturesViewModel
    private lateinit var scene: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)

        initArMenu()
        listenMenuSelection()
        viewModelAndArCoreSetup()
        arCorListenersSetup()
        btnSetup()
        initiateRecyclerView()
        modelsAnchorsAndNodesObserver()
        takePicturesObserver()
    }

    private fun viewModelAndArCoreSetup() {
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment
        scene = arFragment.arSceneView.scene

        arViewModel = ViewModelProviders.of(this, viewModelFactory {
            ArViewModel(arFragment.transformationSystem, scene)
        }).get(ArViewModel::class.java)

        takePicturesViewModel = ViewModelProviders.of(this, viewModelFactory {
            TakePicturesViewModel(application)
        }).get(TakePicturesViewModel::class.java)
    }

    private fun arCorListenersSetup() {
        gestureListener = CustomOnGestureListener(this)
        mGestureDetector = CustomGestureDetector(this, gestureListener)
        mGestureDetector.setOnDoubleTapListener(gestureListener)

        arViewModel.listenForModelUserInteraction(mGestureDetector)

        scene.addOnUpdateListener { frameTime ->
            arFragment.onUpdate(frameTime)
            onUpdate()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun btnSetup() {
        btnRemove = findViewById(R.id.remove)
        btnRemove.visibility = View.GONE
        btnTakePicture = findViewById(R.id.takePicture)
        btnRemove.setOnClickListener { view ->
            arViewModel.removeAllModels()
            btnRemove.visibility = View.GONE
        }
        btnTakePicture.setOnClickListener { view -> takePicture() }
    }

    private fun takePicturesObserver() {
        takePicturesViewModel.startActivityLiveData.observe(this, Observer<Intent> { intent ->
            startActivity(intent)
        })
    }

    @SuppressLint("RestrictedApi")
    private fun modelsAnchorsAndNodesObserver() {
        arViewModel.setNodeNameLiveData.observe(this, Observer<Long> { key ->
            gestureListener.nodeList[key] = gestureListener.nodeList.size
        })
        arViewModel.anchorNodeIntoSceneLiveData.observe(this, Observer<AnchorNode> { anchorNode ->
            scene.addChild(anchorNode)
        })
        arViewModel.removeNodeNameLiveData.observe(this, Observer<Long> { nodeIndexToDelete ->
            gestureListener.nodeList.remove(nodeIndexToDelete)
            if (gestureListener.nodeList.isEmpty()) {
                btnRemove.visibility = View.GONE
            }
        })
        arViewModel.loadModelLiveData.observe(this, Observer<Product> { product ->
            if (product.modelsName.startsWith("https")) {
                ModelRenderable
                        .builder()
                        .setSource(
                        this,
                                RenderableSource
                                        .builder()
                                        .setSource(this, Uri.parse(product.modelsName), RenderableSource.SourceType.GLTF2)
                                        .setScale(0.75f)  // Scale the original model to 50%.
                                        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                                        .build()
                        )
                        .setRegistryId(product.modelsName)
                        .build()
                        .thenAccept { modelRenderable ->
                            arViewModel.addAnchorToScene(product.anchor, modelRenderable)
                            UtilMethods.hideLoading(window, progressBar)
                            btnRemove.visibility = View.VISIBLE
                        }
                        .exceptionally { throwable ->
                            UtilMethods.hideLoading(window, progressBar)
                            null
                        }
            } else {
                ModelRenderable.builder()
                        .setSource(this, Uri.parse(product.modelsName))
                        .setRegistryId(product.modelsName)
                        .build()
                        .thenAccept { modelRenderable ->
                            arViewModel.addAnchorToScene(product.anchor, modelRenderable)
                            UtilMethods.hideLoading(window, progressBar)
                            btnRemove.visibility = View.VISIBLE
                        }
                        .exceptionally { throwable ->
                            UtilMethods.hideLoading(window, progressBar)
                            null
                        }
            }
        })
    }

    private fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView = findViewById<View>(android.R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }

        if (isTracking) {
            val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                pointer.isEnabled = isHitting
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame?.camera?.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }

    private fun updateHitTest(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val pt = screenCenter
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun initiateRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = layoutManager
        adapter = ProductsAdapter(
            this.applicationContext,
            mutableListOf(),
            this
        )
        recyclerView.adapter = adapter
    }

    fun takePicture() {
        val snackbar = Snackbar.make(findViewById(android.R.id.content),
                "Photo saved", Snackbar.LENGTH_LONG)
        takePicturesViewModel.takePhoto(arFragment.arSceneView, snackbar, getPackageName())
    }

    fun listenMenuSelection() {
        menuOptionSelectedLiveData.observe(this, Observer<Int> { menuOptionSelected ->
            if (menuOptionSelected >= 0 && this.menuViewModel.categoriesList.isNotEmpty()) {
                adapter.updateProductList(this.menuViewModel.getProductsFromCategory(menuOptionSelected))
                toolbarTitleTxt.text = this.menuViewModel.categoriesList[menuOptionSelected].name
            }
        })
    }

    override fun onModelItemClick(modelName: String) {
        val frame = arFragment.arSceneView.arFrame
        frame?.let {
            UtilMethods.showLoading(window, progressBar)
            arViewModel.addObject(modelName, it, screenCenter)
        }
    }

    override fun onLongPressItem() {
        arViewModel.removeModel()
    }

    private val screenCenter: android.graphics.Point
        get() {
            val vw = findViewById<View>(android.R.id.content)
            return android.graphics.Point(vw.width / 2, vw.height / 2)
        }
}