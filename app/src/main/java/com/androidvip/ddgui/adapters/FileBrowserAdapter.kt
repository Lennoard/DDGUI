package com.androidvip.ddgui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.ddgui.R
import com.androidvip.ddgui.domain.files.RootFileEntry
import com.androidvip.ddgui.helpers.OnFileSelectedListener

class FileBrowserAdapter(
    allFiles: List<RootFileEntry>,
    private val context: Context,
    private val onFileSelectedListener: OnFileSelectedListener?
) : RecyclerView.Adapter<FileBrowserAdapter.ViewHolder>() {
    private val dataSet = mutableListOf<RootFileEntry>()

    init {
        filterAndSortByName(allFiles)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var name: TextView = v.findViewById(R.id.listFileBrowserName)
        var icon: ImageView = v.findViewById(R.id.listFileBrowserIcon)
        var itemLayout: LinearLayout = v.findViewById(R.id.listFileBrowserLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.list_item_file_browser, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = dataSet[position]

        if (file.isDirectory) {
            with(holder) {
                name.setTextColor(Color.WHITE)
                icon.setImageResource(R.drawable.ic_folder)
                icon.setColorFilter(Color.parseColor("#FFEA00"), PorterDuff.Mode.SRC_IN)
            }
        } else {
            with(holder) {
                name.setTextColor(Color.parseColor("#99FFFFFF"))
                icon.setImageResource(R.drawable.ic_file)
                icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
        }

        holder.name.text = file.name
        holder.itemLayout.setOnClickListener {
            onFileSelectedListener?.onFileSelected(file)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateData(newData: List<RootFileEntry>) {
        filterAndSortByName(newData)
        notifyDataSetChanged()
    }

    private fun filterAndSortByName(files: List<RootFileEntry>) {
        dataSet.clear()
        dataSet.addAll(
            files.sortedWith(
                compareByDescending<RootFileEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        )
    }
}
