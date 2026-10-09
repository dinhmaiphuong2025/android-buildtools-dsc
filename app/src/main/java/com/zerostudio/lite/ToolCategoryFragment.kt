package com.zerostudio.lite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToolCategoryFragment : Fragment() {

    private lateinit var rvCategoryTools: RecyclerView
    private lateinit var adapter: ToolAdapter
    private var categoryName: String = ""

    companion object {
        private const val ARG_CAT = "category_name"
        fun newInstance(category: ToolCategory): ToolCategoryFragment {
            val f = ToolCategoryFragment()
            val args = Bundle()
            args.putString(ARG_CAT, category.name)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = arguments?.getString(ARG_CAT) ?: ToolCategory.COMPILER.name
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tool_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvCategoryTools = view.findViewById(R.id.rvCategoryTools)
        rvCategoryTools.layoutManager = LinearLayoutManager(requireContext())

        val currentCat = ToolCategory.valueOf(categoryName)
        val filteredTools = ToolRepository.tools.filter { it.category == currentCat }

        adapter = ToolAdapter(
            tools = filteredTools,
            filesDir = requireContext().filesDir,
            onActionClick = { tool -> performInstall(tool) },
            onDeleteClick = { tool -> performDelete(tool) }
        )
        rvCategoryTools.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun performInstall(tool: ToolItem) {
        tool.status = ToolStatus.DOWNLOADING
        tool.progress = 0
        adapter.notifyDataSetChanged()

        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val success = InstallerEngine.installTool(
                context = appContext,
                tool = tool,
                onProgress = { p ->
                    tool.progress = p
                    lifecycleScope.launch(Dispatchers.Main) { adapter.notifyDataSetChanged() }
                },
                onLog = { msg ->
                    lifecycleScope.launch { ToolRepository.emitLog(msg) }
                }
            )

            tool.status = if (success) ToolStatus.INSTALLED else ToolStatus.NOT_INSTALLED
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun performDelete(tool: ToolItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val ok = InstallerEngine.deleteTool(requireContext().filesDir, tool)
            val msg = if (ok) ">> Đã xóa ${tool.title}" else "!! Không thể xóa ${tool.title}"
            ToolRepository.emitLog(msg)
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }
}
