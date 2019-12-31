package com.pad.feature.ar.viewModels

import android.graphics.Point
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pad.feature.menu.models.Product
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.pad.util.gestures.CustomGestureDetector
import java.util.LinkedHashMap

class ArViewModel(private val transformationSystem: TransformationSystem, private val scene: Scene): ViewModel() {
    private val anchorsMap = LinkedHashMap<Long, AnchorNode?>()
    private lateinit var mViewTouchListener: Node.OnTouchListener
    private var nodeToDelete: Node? = null
    private var nodeIndexToDelete: Long = 0

    private val anchorNodeIntoSceneMutableLiveData = MutableLiveData<AnchorNode>()
    val anchorNodeIntoSceneLiveData: LiveData<AnchorNode>
        get() = anchorNodeIntoSceneMutableLiveData

    private val setNodeNameMutableLiveData = MutableLiveData<Long>()
    val setNodeNameLiveData: LiveData<Long>
        get() = setNodeNameMutableLiveData

    private val removeNodeNameMutableLiveData = MutableLiveData<Long>()
    val removeNodeNameLiveData: LiveData<Long>
        get() = removeNodeNameMutableLiveData

    private val loadModelMutableLiveData = MutableLiveData<Product>()
    val loadModelLiveData: LiveData<Product>
        get() = loadModelMutableLiveData

    fun addObject(modelName: String, frame: Frame, screenCenter: Point) {
        val pt = screenCenter
        val hits: List<HitResult>
        hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
        for (hit in hits) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                val product = Product()
                product.modelsName = modelName
                product.anchor = hit.createAnchor()
                loadModelMutableLiveData.value = product
                break
            }
        }
    }

    fun addAnchorToScene(anchor: Anchor, modelRenderable: ModelRenderable) {
        val key = System.currentTimeMillis()

        val anchorNode = AnchorNode(anchor)
        anchorsMap[key] = anchorNode

        val node = TransformableNode(transformationSystem)
        node.setParent(anchorNode)
        node.name = "" + key
        node.renderable = modelRenderable
        node.select()
        node.setOnTouchListener(mViewTouchListener)

        setNodeNameMutableLiveData.value = key
        anchorNodeIntoSceneMutableLiveData.value = anchorNode
    }

    fun listenForModelUserInteraction(mGestureDetector: CustomGestureDetector) {
        mViewTouchListener = Node.OnTouchListener { hitTestResult, event ->
            if (mGestureDetector.onTouchEvent(event)) {
                nodeToDelete = hitTestResult.node

                nodeToDelete?.let { node ->
                    node.name.let {
                        nodeIndexToDelete = java.lang.Long.parseLong(node.name)
                    }
                }

                return@OnTouchListener true
            }
            false
        }
    }

    fun removeAllModels() {
        nodeIndexToDelete = 0L
        val iter = anchorsMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            var node: Node? = entry.value!!.children[0]
            if (node != null) {
                scene.removeChild(node)
                entry.value?.anchor!!.detach()
                entry.value?.setParent(null)
                iter.remove()
                node = null
            }
            removeNodeNameMutableLiveData.value = nodeIndexToDelete
            nodeIndexToDelete++
        }
        nodeIndexToDelete = -1L
    }

    fun removeModel() {
        nodeToDelete?.let { childNode ->
            scene.removeChild(childNode)
            anchorsMap[java.lang.Long.valueOf(childNode.name)]?.let { anchorNode ->
                anchorNode.removeChild(childNode)
                anchorNode.anchor?.detach()
                anchorNode.setParent(null)
            }
            anchorsMap.remove(java.lang.Long.valueOf(childNode.name))
            nodeToDelete = null
            removeNodeNameMutableLiveData.value = nodeIndexToDelete
            nodeIndexToDelete = -1L
        }
    }
}