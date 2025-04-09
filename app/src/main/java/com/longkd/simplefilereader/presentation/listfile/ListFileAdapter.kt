package com.longkd.simplefilereader.presentation.listfile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.longkd.simplefilereader.R
import com.longkd.simplefilereader.databinding.ItemFileBinding
import com.longkd.simplefilereader.domain.model.FileType
import com.longkd.simplefilereader.presentation.listfile.model.File

class ListFileAdapter(
    private val onItemClick: (File) -> Unit
) : ListAdapter<File, ListFileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFileBinding.inflate(inflater, parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) = with(binding) {
            tvName.text = file.name
            tvDesc.text = file.desc

            root.setOnClickListener {
                onItemClick(file)
            }

            ivType.setImageResource(
                when (file.fileType) {
                    FileType.PDF -> R.drawable.ic_pdf
                    FileType.DOCX -> R.drawable.ic_doc
                    FileType.XLSX -> R.drawable.ic_xls
                    FileType.UNKNOWN -> TODO()
                }
            )
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean =
            oldItem.contentUri == newItem.contentUri

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean =
            oldItem == newItem
    }
}
