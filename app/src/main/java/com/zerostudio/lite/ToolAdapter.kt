package com.zerostudio.lite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

class ToolAdapter(
    private val tools: List<ToolItem>,
    private val filesDir: File,
    private val onActionClick: (ToolItem) -> Unit,
    private val onDeleteClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ToolViewHolder>() {

    class ToolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvToolTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvToolDesc)
        val tvSize: TextView = view.findViewById(R.id.tvToolSize)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val progress: LinearProgressIndicator = view.findViewById(R.id.progressIndicator)
        val btnAction: MaterialButton = view.findViewById(R.id.btnAction)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tool_card, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.tvTitle.text = tool.title
        holder.tvDesc.text = tool.description
        holder.tvSize.text = tool.sizeText

        val installed = tool.isInstalled(filesDir)

        when (tool.status) {
            ToolStatus.DOWNLOADING, ToolStatus.EXTRACTING -> {
                holder.chipStatus.text = "Đang xử lý ${tool.progress}%"
                holder.chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_primaryContainer)
                holder.progress.visibility = View.VISIBLE
                holder.progress.isIndeterminate = (tool.progress == 0)
                if (tool.progress > 0) {
                    holder.progress.progress = tool.progress
                }
                holder.btnAction.isEnabled = false
                holder.btnDelete.visibility = View.GONE
            }
            else -> {
                holder.progress.visibility = View.GONE
                holder.btnAction.isEnabled = true
                if (installed) {
                    holder.chipStatus.text = "Đã cài đặt"
                    holder.chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_primaryContainer)
                    holder.btnAction.text = "Cài lại"
                    holder.btnDelete.visibility = View.VISIBLE
                } else {
                    holder.chipStatus.text = "Chưa cài đặt"
                    holder.chipStatus.setChipBackgroundColorResource(android.R.color.transparent)
                    holder.btnAction.text = "Cài đặt"
                    holder.btnDelete.visibility = View.GONE
                }
            }
        }

        holder.btnAction.setOnClickListener {
            onActionClick(tool)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(tool)
        }
    }

    override fun getItemCount(): Int = tools.size
}
