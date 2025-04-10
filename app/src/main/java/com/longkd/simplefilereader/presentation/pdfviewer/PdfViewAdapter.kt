package com.longkd.simplefilereader.presentation.pdfviewer

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.longkd.simplefilereader.databinding.ItemPdfPageBinding
import com.longkd.simplefilereader.util.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class PdfViewAdapter(
    private val context: Context,
    private val renderer: PdfRenderCore,
    private val pageSpacing: Rect
) : RecyclerView.Adapter<PdfViewAdapter.PdfPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder =
        PdfPageViewHolder(
            ItemPdfPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = renderer.getPageCount()

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PdfPageViewHolder(private val itemBinding: ItemPdfPageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(position: Int) {
            with(itemBinding) {
                pdfViewPageLoadingProgress.visibility = View.VISIBLE
                // Before we trigger rendering, explicitly ensure that cached bitmaps are used
                renderer.getBitmapFromCache(position)?.let { cachedBitmap ->
                    pageView.setImageBitmap(cachedBitmap)
                    pdfViewPageLoadingProgress.visibility = View.GONE
                    updateLayoutParams()
                    applyFadeInAnimation(pageView)

                    return
                }

                renderer.getPageDimensionsAsync(position) { size ->
                    val width = pageView.width.takeIf { it > 0 }
                        ?: context.resources.displayMetrics.widthPixels
                    val aspectRatio = size.width.toFloat() / size.height.toFloat()
                    val height = (width / aspectRatio).toInt()

                    updateLayoutParams(height)
                    val bitmap = CommonUtils.Companion.BitmapPool.getBitmap(width, maxOf(1, height))
                    renderer.renderPage(position, bitmap) { success, pageNo, renderedBitmap ->
                        if (success && pageNo == position) {
                            CoroutineScope(Dispatchers.Main).launch {
                                pageView.setImageBitmap(renderedBitmap ?: bitmap)
                                applyFadeInAnimation(pageView)
                                pdfViewPageLoadingProgress.visibility = View.GONE

                                // Prefetch here
                                renderer.prefetchPagesAround(
                                    currentPage = position,
                                    width = pageView.width.takeIf { it > 0 }
                                        ?: context.resources.displayMetrics.widthPixels,
                                    height = pageView.height.takeIf { it > 0 }
                                        ?: context.resources.displayMetrics.heightPixels
                                )

                            }
                        } else {
                            CommonUtils.Companion.BitmapPool.recycleBitmap(bitmap)
                        }
                    }
                }
            }
        }

        private fun ItemPdfPageBinding.updateLayoutParams(height: Int? = null) {
            root.layoutParams = root.layoutParams.apply {
                if (height != null) {
                    this.height = height
                }
                (this as? ViewGroup.MarginLayoutParams)?.setMargins(
                    pageSpacing.left, pageSpacing.top, pageSpacing.right, pageSpacing.bottom
                )
            }
        }

        private fun applyFadeInAnimation(view: View) {
            view.startAnimation(AlphaAnimation(0F, 1F).apply {
                interpolator = LinearInterpolator()
                duration = 300
            })
        }

    }
}

