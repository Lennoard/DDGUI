package com.androidvip.ddgui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.ddgui.R
import com.androidvip.ddgui.helpers.FileChangedListener
import java.io.File

class FileBrowserAdapter(
    allFiles: Array<File>,
    private val context: Context,
    var container: EditText,
    private val fileChangedListener: FileChangedListener
) : RecyclerView.Adapter<FileBrowserAdapter.ViewHolder>() {
    private val dataSet = mutableListOf<File>()

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
            with (holder) {
                name.setTextColor(Color.WHITE)
                icon.setImageResource(R.drawable.ic_folder)
                icon.setColorFilter(Color.parseColor("#FFEA00"), PorterDuff.Mode.SRC_IN)
            }
        } else {
            with (holder) {
                name.setTextColor(Color.parseColor("#99FFFFFF"))
                icon.setImageResource(R.drawable.ic_file)
                icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
        }

        holder.name.text = file.name
        holder.itemLayout.setOnClickListener {
            fileChangedListener.onFileChanged(file, container)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateData(newData: Array<File>) {
        filterAndSortByName(newData)
        notifyDataSetChanged()
    }

    private fun filterAndSortByName(files: Array<File>) {
        dataSet.clear()
        files.forEach {
            if ((it.exists() || it.isDirectory)) {
                dataSet.add(it)
            }
        }

        dataSet.sort()
        dataSet.sortWith(Comparator { f1, f2 ->
            f2.isDirectory.compareTo(f1.isDirectory)
        })
    }
}
