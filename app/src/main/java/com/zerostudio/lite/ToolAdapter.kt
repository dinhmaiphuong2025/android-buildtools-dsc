package com.zerostudio.lite

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
        val tvToolSize: TextView = view.findViewById(R.id.tvToolSize)
        val viewStatusDot: View = view.findViewById(R.id.viewStatusDot)
        val tvStatusText: TextView = view.findViewById(R.id.tvStatusText)
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
        holder.tvToolSize.text = tool.sizeText

        val installed = tool.isInstalled(filesDir)

        when (tool.status) {
            ToolStatus.DOWNLOADING, ToolStatus.EXTRACTING -> {
                holder.viewStatusDot.setBackgroundResource(R.drawable.dot_processing)
                holder.tvStatusText.text = "Processing ${tool.progress}%"
                holder.tvStatusText.setTextColor(Color.parseColor("#F57C00"))
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
                    holder.viewStatusDot.setBackgroundResource(R.drawable.dot_installed)
                    holder.tvStatusText.text = "Installed"
                    holder.tvStatusText.setTextColor(Color.parseColor("#2E7D32"))
                    holder.btnAction.text = "Reinstall"
                    holder.btnDelete.visibility = View.VISIBLE
                } else {
                    holder.viewStatusDot.setBackgroundResource(R.drawable.dot_uninstalled)
                    holder.tvStatusText.text = "Not installed"
                    holder.tvStatusText.setTextColor(Color.parseColor("#757575"))
                    holder.btnAction.text = "Install"
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
