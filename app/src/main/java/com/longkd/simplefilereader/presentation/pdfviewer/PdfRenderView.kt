package com.longkd.simplefilereader.presentation.pdfviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.longkd.simplefilereader.R
import com.longkd.simplefilereader.util.CacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

class PdfRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    lateinit var recyclerView: PinchZoomRecyclerView
    private lateinit var pageNo: TextView
    private lateinit var pdfRendererCore: PdfRenderCore
    private lateinit var pdfViewAdapter: PdfViewAdapter
    private var runnable = Runnable {}
    private var pageMargin: Rect = Rect(0, 0, 0, 0)
    var statusListener: StatusCallBack? = null
    private var positionToUseForState: Int = 0
    private var restoredScrollPosition: Int = NO_POSITION
    private var cacheStrategy: CacheStrategy = CacheStrategy.MAXIMIZE_PERFORMANCE
    private var pdfRendererCoreInitialised = false

    val totalPageCount: Int
        get() {
            return pdfRendererCore.getPageCount()
        }

    init {
        getAttrs(attrs, defStyleAttr)
    }

    interface StatusCallBack {
        fun onPageChanged(currentPage: Int, totalPage: Int) {}
    }

    @Throws(FileNotFoundException::class)
    fun initWithUri(uri: Uri) {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return
        val cacheIdentifier = uri.toString().hashCode().toString()
        initializeRenderer(fileDescriptor, cacheIdentifier)
    }

    private fun initializeRenderer(fileDescriptor: ParcelFileDescriptor, cacheIdentifier: String) {
        // Proceed with safeFile
        PdfRenderCore.enableDebugMetrics = true
        pdfRendererCore =
            PdfRenderCore(context, fileDescriptor, cacheIdentifier, this.cacheStrategy)
        pdfRendererCoreInitialised = true
        pdfViewAdapter = PdfViewAdapter(context, pdfRendererCore, pageMargin)
        val v = LayoutInflater.from(context).inflate(R.layout.view_pdf_render, this, false)
        addView(v)
        recyclerView = findViewById(R.id.recyclerView)
        pageNo = findViewById(R.id.pageNo)
        recyclerView.apply {
            adapter = pdfViewAdapter
            addOnScrollListener(scrollListener)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (restoredScrollPosition != NO_POSITION) {
                recyclerView.scrollToPosition(restoredScrollPosition)
                restoredScrollPosition = NO_POSITION  // Reset after applying
            }
        }, 500) // Adjust delay as needed

        runnable = Runnable {
            pageNo.visibility = GONE
        }

        // Start preloading cache into memory immediately after setting up adapter and RecyclerView
        preloadCacheIntoMemory()
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var lastDisplayedPage = NO_POSITION
        private var lastScrollDirection = 0 // 1 = Down, -1 = Up

        private val hideRunnable = Runnable {
            if (pageNo.isVisible) {
                pageNo.visibility = GONE
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager

            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val firstCompletelyVisiblePosition =
                layoutManager.findFirstCompletelyVisibleItemPosition()
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
            val lastCompletelyVisiblePosition =
                layoutManager.findLastCompletelyVisibleItemPosition()

            // Determine scroll direction
            val scrollDirection = when {
                dy > 0 -> 1  // Scrolling Down
                dy < 0 -> -1 // Scrolling Up
                else -> lastScrollDirection
            }
            lastScrollDirection = scrollDirection

            // Determine the most dominant page to display
            val pageToShow = when (scrollDirection) {
                1 -> lastCompletelyVisiblePosition.takeIf { it != NO_POSITION }
                    ?: lastVisiblePosition.takeIf { it != NO_POSITION }
                    ?: firstVisiblePosition // Scrolling Down - Prefer the last fully visible, then partially visible
                -1 -> firstCompletelyVisiblePosition.takeIf { it != NO_POSITION }
                    ?: firstVisiblePosition.takeIf { it != NO_POSITION }
                    ?: lastVisiblePosition // Scrolling Up - Prefer the first fully visible, then partially visible
                else -> firstVisiblePosition // Default case
            }

            // Ensure updates happen when the page actually changes
            if (pageToShow != lastDisplayedPage) {
                updatePageNumberDisplay(pageToShow)
                lastDisplayedPage = pageToShow
            }
        }

        fun updatePageNumberDisplay(position: Int) {
            if (position != NO_POSITION) {
                pageNo.text =
                    context.getString(R.string.pdfView_page_no, position + 1, totalPageCount)
                pageNo.visibility = VISIBLE

                // Remove any existing hide delays before scheduling a new one
                pageNo.removeCallbacks(hideRunnable)
                pageNo.postDelayed(hideRunnable, 3000)

                statusListener?.onPageChanged(position + 1, totalPageCount)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                pageNo.removeCallbacks(hideRunnable)
                pageNo.postDelayed(hideRunnable, 3000)
            } else {
                pageNo.removeCallbacks(hideRunnable)
            }
        }
    }

    @SuppressLint("UseKtx", "CustomViewStyleable")
    private fun getAttrs(attrs: AttributeSet?, defStyle: Int) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PdfRenderView, defStyle, 0)

        val marginDim =
            typedArray.getDimensionPixelSize(R.styleable.PdfRenderView_pdfView_page_margin, 0)
        pageMargin.set(
            typedArray.getDimensionPixelSize(
                R.styleable.PdfRenderView_pdfView_page_marginLeft,
                marginDim
            ),
            typedArray.getDimensionPixelSize(
                R.styleable.PdfRenderView_pdfView_page_marginTop,
                marginDim
            ),
            typedArray.getDimensionPixelSize(
                R.styleable.PdfRenderView_pdfView_page_marginRight,
                marginDim
            ),
            typedArray.getDimensionPixelSize(
                R.styleable.PdfRenderView_pdfView_page_marginBottom,
                marginDim
            )
        )
        typedArray.recycle()
    }


    private fun preloadCacheIntoMemory() {
        CoroutineScope(Dispatchers.IO).launch {
            pdfRendererCore.let { renderer ->
                (0 until renderer.getPageCount()).forEach { pageNo ->
                    renderer.getBitmapFromCache(pageNo)
                }
            }
        }
    }

    fun closePdfRender() {
        if (pdfRendererCoreInitialised) {
            pdfRendererCore.closePdfRender()
            pdfRendererCoreInitialised = false
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = Bundle()
        savedState.putParcelable("superState", superState)
        if (this::recyclerView.isInitialized) {
            savedState.putInt("scrollPosition", positionToUseForState)
        }
        return savedState
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("superState", Parcelable::class.java)
            } else {
                state.getParcelable("superState")
            }
            super.onRestoreInstanceState(superState)
            restoredScrollPosition = state.getInt("scrollPosition", positionToUseForState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

}